package com.gun.personalcolor.core.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

/**
 * Ad policy (AdMob previously limited serving for "more ads than publisher content"; the old app blocked the AI test
 * behind a rewarded ad and showed an error screen when none filled):
 * - Rewarded (opt-in, never blocking): before a photo diagnosis (consumed only when the analysis succeeds), to unlock
 *   premium tools (lip try-on, report) for the session, and the daily outfit after its free trial. No fill → continue.
 * - Native: at most one content-styled card per screen, inside the content flow (see [NativeAdCard]).
 * - "프리미엄" purchase ([Billing]) turns every ad off and unlocks premium tools and the daily outfit.
 * - Interstitial: at the natural break before each result, skipped right after another full-screen ad (back-to-back).
 *   Also set a frequency cap in the AdMob console.
 * - No banners. Features never wait for an ad.
 */
@Singleton
class AdManager @Inject constructor(val billing: Billing) {
    private val started = AtomicBoolean(false)
    /** Mobile Ads SDK initialized (consent allows ads). Compose-observable for native ad slots. */
    var ready by mutableStateOf(false)
        private set
    private val adFree get() = billing.adFree.value

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var rewarded: RewardedAd? = null
    private var rewardedLoading = false
    private val pacing = AdPacing(SystemClock::elapsedRealtime)

    /** Call from Activity.onCreate. Shows the UMP consent form when required (EEA/UK etc.). */
    fun gatherConsent(activity: Activity) {
        billing.start()
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    error?.let { Log.w(TAG, "consent form: ${it.message}") }
                    startIfAllowed(activity)
                }
            },
            { error ->
                Log.w(TAG, "consent update: ${error.message}")
                startIfAllowed(activity)
            },
        )
        // Consent from a previous session lets ads start without waiting for the update.
        startIfAllowed(activity)
    }

    fun privacyOptionsRequired(context: Context) =
        UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) =
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { it?.let { e -> Log.w(TAG, e.message) } }

    private fun startIfAllowed(activity: Activity) {
        if (!UserMessagingPlatform.getConsentInformation(activity).canRequestAds()) return
        if (!started.compareAndSet(false, true)) return
        val app = activity.applicationContext
        thread {
            MobileAds.initialize(app) {
                activity.runOnUiThread {
                    ready = true
                    loadInterstitial(app)
                    loadRewarded(app)
                }
            }
        }
    }

    private fun loadInterstitial(context: Context) {
        if (!ready || interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(context, BuildConfig.INTERSTITIAL_AD_UNIT_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; interstitialLoading = false }
                override fun onAdFailedToLoad(error: LoadAdError) { Log.w(TAG, "interstitial: ${error.message}"); interstitialLoading = false }
            })
    }

    private fun loadRewarded(context: Context) {
        if (!ready || rewarded != null || rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(context, BuildConfig.REWARD_AD_UNIT_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; rewardedLoading = false }
                override fun onAdFailedToLoad(error: LoadAdError) { Log.w(TAG, "rewarded: ${error.message}"); rewardedLoading = false }
            })
    }

    /** Feature [key] usable without (another) ad: premium purchase, or a reward watched within its unlock window. */
    fun isUnlocked(key: String) = adFree || pacing.isUnlocked(key)

    /**
     * Ends an unlock early. Photo diagnosis calls this after a SUCCESSFUL analysis, so failed retakes stay free.
     * Returns true if that diagnosis was paid for with a rewarded ad — then its result must not get an interstitial too.
     */
    fun consumeUnlock(key: String): Boolean = pacing.consumeUnlock(key)

    /** True when the UI should ask "광고 보고 무료로 …할까요?" first. False = just continue (unlocked, or no ad ready). */
    fun shouldOfferReward(key: String): Boolean = !isUnlocked(key) && rewarded != null

    /**
     * Shows the rewarded ad; [onUnlocked] runs only if the user earned the reward (closing early = stay on screen),
     * and unlocks [key] for [unlockMs]. If no ad is ready (or it fails to show), [onUnlocked] runs anyway — never blocked.
     */
    fun showRewarded(activity: Activity?, key: String, unlockMs: Long, onUnlocked: () -> Unit) {
        val ad = rewarded
        if (activity == null || activity.isStale() || ad == null || isUnlocked(key)) return onUnlocked()
        rewarded = null
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = done()
            override fun onAdFailedToShowFullScreenContent(e: AdError) { earned = true; done() }
            fun done() {
                pacing.onFullScreenShown()
                loadRewarded(activity.applicationContext)
                if (earned) { pacing.unlock(key, unlockMs); onUnlocked() }
            }
        }
        ad.show(activity) { earned = true }
    }

    /**
     * Called when a result is about to be shown. Shows an interstitial unless another full-screen ad ran in the last
     * [FULLSCREEN_GAP_MS], then runs [next]. [next] always runs — immediately if no ad shows.
     */
    fun onResult(activity: Activity?, next: () -> Unit) {
        if (activity == null || activity.isStale() || adFree) return next()
        val ad = interstitial
        val due = pacing.interstitialDue()
        if (ad == null || !due) {
            next(); loadInterstitial(activity.applicationContext); return
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = done()
            override fun onAdFailedToShowFullScreenContent(e: AdError) = done()
            fun done() {
                pacing.onFullScreenShown()
                loadInterstitial(activity.applicationContext)
            }
        }
        next() // navigate underneath so the result is already there when the ad closes
        ad.show(activity)
    }

    companion object {
        /** Photo diagnosis: one reward per successful diagnosis (failed retakes don't cost another ad). */
        const val KEY_PHOTO = "photo"
        const val PHOTO_UNLOCK_MS = 60 * 60_000L   // safety cap; normally consumed on success
        /** Daily outfit after the free trial: one reward unlocks it until midnight (see [untilMidnightMs]). */
        const val KEY_COORDI = "coordi"
        /** Premium tools (lip try-on, report): one reward unlocks them until the app process ends. */
        const val SESSION_UNLOCK_MS = Long.MAX_VALUE / 4
        private const val TAG = "Ads"
        const val BACK_TO_BACK_GAP_MS = 90_000L

        /** Milliseconds from now until local midnight — unlock window for once-a-day features. */
        fun untilMidnightMs(): Long {
            val now = java.time.LocalDateTime.now()
            return java.time.Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis()
        }
    }
}

/** Finishing/destroyed activity: showing a full-screen ad on it can crash, so callers skip the ad. */
private fun Activity.isStale() = isFinishing || isDestroyed

/** Provided by MainActivity. Null in previews (no ads). */
val LocalAdManager = staticCompositionLocalOf<AdManager?> { null }
