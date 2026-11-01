package com.gun.personalcolor.core.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-time "프리미엄" purchase (no ads + unlimited daily outfit + premium tools) (Play Console in-app product id [PRODUCT_ID]).
 * The last known state is cached so ads stay off offline and on cold start before Play answers.
 */
@Singleton
class Billing @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("billing", Context.MODE_PRIVATE)
    private val _adFree = MutableStateFlow(prefs.getBoolean(KEY_AD_FREE, false))
    val adFree: StateFlow<Boolean> = _adFree.asStateFlow()

    /** Localized price ("₩3,900"), null until Play answers or if the product isn't set up. */
    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price.asStateFlow()

    private var product: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases?.forEach(::handle) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Connects, loads the price and restores an existing purchase. Safe to call repeatedly. */
    fun start() {
        if (client.isReady) return refresh()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
                else Log.w(TAG, "setup: ${result.debugMessage}")
            }
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun refresh() {
        val query = QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build(),
        )).build()
        client.queryProductDetailsAsync(query) { _, result ->
            product = result.productDetailsList.firstOrNull()
            _price.value = product?.oneTimePurchaseOfferDetails?.formattedPrice
        }
        restore()
    }

    /**
     * Re-reads owned purchases from Play ("구매 복원"). [onResult] runs exactly once, possibly on a background thread;
     * without Play it reports FAILED after [RESTORE_TIMEOUT_MS] instead of waiting out the reconnection.
     */
    fun restore(onResult: (RestoreResult) -> Unit = {}) {
        val once = OnceResult(onResult)
        Handler(Looper.getMainLooper()).postDelayed({ once(RestoreResult.FAILED) }, RESTORE_TIMEOUT_MS)
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            val ok = result.responseCode == BillingClient.BillingResponseCode.OK
            val owned = ok && purchases.any { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            if (ok) {
                setAdFree(owned)
                purchases.forEach(::handle)
            } else Log.w(TAG, "restore: ${result.responseCode} ${result.debugMessage}")
            once(restoreResult(ok, owned))
        }
    }

    /** Opens the Play purchase sheet. Returns false if the product isn't available (not set up / offline). */
    fun purchase(activity: Activity): Boolean {
        val details = product ?: return false.also { start() }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build(),
        )).build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun handle(p: Purchase) {
        if (PRODUCT_ID !in p.products || p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        setAdFree(true)
        if (!p.isAcknowledged) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) {
                Log.i(TAG, "acknowledge: ${it.responseCode}")
            }
        }
    }

    private fun setAdFree(v: Boolean) {
        _adFree.value = v
        prefs.edit().putBoolean(KEY_AD_FREE, v).apply()
    }

    companion object {
        /** Create this in Play Console → 수익 창출 → 인앱 상품 (one-time). */
        const val PRODUCT_ID = "premium"
        private const val TAG = "Billing"
        private const val KEY_AD_FREE = "ad_free"
        private const val RESTORE_TIMEOUT_MS = 6_000L
    }
}

enum class RestoreResult { RESTORED, NOTHING, FAILED }

internal fun restoreResult(ok: Boolean, owned: Boolean) = when {
    !ok -> RestoreResult.FAILED
    owned -> RestoreResult.RESTORED
    else -> RestoreResult.NOTHING
}

/** Delivers only the first result (Play's answer or the timeout, whichever comes first). */
internal class OnceResult(private val onResult: (RestoreResult) -> Unit) : (RestoreResult) -> Unit {
    private val done = AtomicBoolean(false)
    override fun invoke(r: RestoreResult) { if (done.compareAndSet(false, true)) onResult(r) }
}
