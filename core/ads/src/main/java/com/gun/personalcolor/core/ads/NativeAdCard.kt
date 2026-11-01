package com.gun.personalcolor.core.ads

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * In-feed native ad styled like the app's content cards, clearly labelled "광고".
 * Put at most one per screen, inside the content flow (end of an article, between list sections).
 * Takes no space until an ad loads; never shown to ad-free users.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val ads = LocalAdManager.current ?: return
    val adFree by ads.billing.adFree.collectAsState()
    if (adFree || !ads.ready) return
    val context = LocalContext.current
    var ad by remember { mutableStateOf<NativeAd?>(null) }
    DisposableEffect(Unit) {
        var disposed = false
        AdLoader.Builder(context, BuildConfig.NATIVE_AD_UNIT_ID)
            .forNativeAd { if (disposed) it.destroy() else ad = it }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(e: LoadAdError) { Log.w("Ads", "native: ${e.message}") }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build())
            .build()
            .loadAd(AdRequest.Builder().build())
        onDispose { disposed = true; ad?.destroy() }
    }
    val loaded = ad ?: return
    val c = MaterialTheme.colorScheme
    val colors = CardColors(c.surfaceContainer, c.outlineVariant, c.onSurface, c.onSurfaceVariant, c.primary, c.onPrimary, c.secondaryContainer, c.onSecondaryContainer)
    AndroidView(
        factory = { buildCard(it, colors) },
        update = { bind(it, loaded) },
        modifier = modifier.fillMaxWidth(),
    )
}

private class CardColors(
    val bg: Color, val border: Color, val text: Color, val muted: Color,
    val cta: Color, val onCta: Color, val badge: Color, val onBadge: Color,
)

private fun Context.dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

private fun rounded(color: Color, radius: Int, stroke: Color? = null, ctx: Context) = GradientDrawable().apply {
    setColor(color.toArgb()); cornerRadius = ctx.dp(radius).toFloat()
    if (stroke != null) setStroke(ctx.dp(1), stroke.toArgb())
}

/** Card layout: [광고] icon + headline/advertiser, body, media (16:9), CTA. */
private fun buildCard(ctx: Context, c: CardColors): NativeAdView {
    val view = NativeAdView(ctx)
    val root = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(c.bg, 24, c.border, ctx) // r-lg, same as content cards
        setPadding(ctx.dp(18), ctx.dp(16), ctx.dp(18), ctx.dp(18))
    }
    val header = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val icon = ImageView(ctx).apply { clipToOutline = true; background = rounded(Color.Transparent, 12, null, ctx); layoutParams = LinearLayout.LayoutParams(ctx.dp(40), ctx.dp(40)).apply { marginEnd = ctx.dp(12) } }
    val titles = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f) }
    val badge = TextView(ctx).apply {
        text = "광고"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f); setTextColor(c.onBadge.toArgb())
        background = rounded(c.badge, 99, null, ctx); setPadding(ctx.dp(8), ctx.dp(1), ctx.dp(8), ctx.dp(1))
        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { bottomMargin = ctx.dp(2) }
    }
    val headline = TextView(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f); setTypeface(typeface, Typeface.BOLD); setTextColor(c.text.toArgb())
        maxLines = 1; ellipsize = TextUtils.TruncateAt.END
    }
    val advertiser = TextView(ctx).apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setTextColor(c.muted.toArgb()); maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
    titles.addView(badge); titles.addView(headline); titles.addView(advertiser)
    header.addView(icon); header.addView(titles)
    val body = TextView(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); setTextColor(c.muted.toArgb()); maxLines = 2; ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = ctx.dp(10) }
    }
    val media = AspectMediaView(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = ctx.dp(10) }
        setImageScaleType(ImageView.ScaleType.CENTER_CROP)
    }
    val cta = Button(ctx).apply {
        isAllCaps = false; setTextColor(c.onCta.toArgb()); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setTypeface(typeface, Typeface.BOLD); background = rounded(c.cta, 16, null, ctx); stateListAnimator = null
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, ctx.dp(48)).apply { topMargin = ctx.dp(12) }
    }
    root.addView(header); root.addView(body); root.addView(media); root.addView(cta)
    view.addView(root)
    view.iconView = icon; view.headlineView = headline; view.advertiserView = advertiser
    view.bodyView = body; view.mediaView = media; view.callToActionView = cta
    return view
}

private fun bind(view: NativeAdView, ad: NativeAd) {
    (view.headlineView as TextView).text = ad.headline
    (view.bodyView as TextView).apply { text = ad.body; visibility = if (ad.body.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE }
    (view.advertiserView as TextView).apply { text = ad.advertiser; visibility = if (ad.advertiser.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE }
    (view.iconView as ImageView).apply { setImageDrawable(ad.icon?.drawable); visibility = if (ad.icon == null) android.view.View.GONE else android.view.View.VISIBLE }
    (view.callToActionView as Button).apply { text = ad.callToAction; visibility = if (ad.callToAction.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE }
    view.mediaView?.mediaContent = ad.mediaContent
    view.setNativeAd(ad)
}

/** MediaView locked to 16:9 so tall creatives can't take over the screen. */
private class AspectMediaView(ctx: Context) : MediaView(ctx) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = android.view.View.MeasureSpec.getSize(widthMeasureSpec)
        val h = android.view.View.MeasureSpec.makeMeasureSpec(w * 9 / 16, android.view.View.MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, h)
    }
}
