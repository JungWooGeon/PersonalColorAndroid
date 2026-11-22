package com.gun.personalcolor.core.ads

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.gun.personalcolor.core.analytics.Analytics
import com.gun.personalcolor.core.analytics.LocalAnalytics

/** What a native ad slot shows. Premium sees nothing; no ad (failed, offline, no consent) → our own "추천 앱" card. */
internal enum class AdSlot { NOTHING, AD, HOUSE }

internal fun adSlot(adFree: Boolean, loaded: Boolean, failed: Boolean): AdSlot = when {
    adFree -> AdSlot.NOTHING
    loaded -> AdSlot.AD
    failed -> AdSlot.HOUSE
    else -> AdSlot.NOTHING // still loading: take no space
}

/** Free cross-promotion of the owner's other app, 온이 일기. */
object OnDiaryPromo {
    const val TITLE = "온이 일기 — AI 감정일기"
    fun url(medium: String) =
        "https://play.google.com/store/apps/details?id=com.pass.diary&referrer=utm_source%3Dpersonalcolor%26utm_medium%3D$medium"

    fun open(context: Context, medium: String, analytics: Analytics?) {
        analytics?.log("promo_click", "app" to "ondiary", "placement" to medium)
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url(medium).toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "링크를 열 수 있는 앱이 없어요", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Same frame as [NativeAdCard] (r24, 1dp outline, badge + title row, body, 48dp CTA), labelled "추천 앱". */
@Composable
internal fun HouseCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val analytics = LocalAnalytics.current
    LaunchedEffect(Unit) { analytics?.log("promo_impression", "app" to "ondiary", "placement" to "house_card") }
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val open = { OnDiaryPromo.open(context, "house_card", analytics) }
    Column(
        modifier.fillMaxWidth().background(c.surfaceContainer, shape).border(1.dp, c.outlineVariant, shape)
            .clickable(onClick = open).padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OnLamp(Modifier.size(40.dp))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    "추천 앱", fontSize = 11.sp, color = c.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 2.dp).background(c.secondaryContainer, RoundedCornerShape(99.dp)).padding(horizontal = 8.dp, vertical = 1.dp),
                )
                Text("기분이 빛이 되는 일기장, 온이", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text("쓰는 동안 AI가 마음의 색을 읽어요 · 무료", fontSize = 13.sp, color = c.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Box(
            Modifier.padding(top = 2.dp).fillMaxWidth().height(48.dp).background(c.primary, RoundedCornerShape(16.dp)).clickable(onClick = open),
            contentAlignment = Alignment.Center,
        ) { Text("온이 만나러 가기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.onPrimary) }
    }
}

/** 온이 glyph: a small mood lamp — warm glow, rounded shade, stem and base. Drawn, no bitmap assets. */
@Composable
private fun OnLamp(modifier: Modifier) = Canvas(modifier) {
    val w = size.width; val h = size.height
    val warm = Color(0xFFFFC978)
    drawCircle(Brush.radialGradient(listOf(warm.copy(alpha = 0.55f), Color.Transparent), Offset(w / 2, h * 0.42f), w * 0.5f), w * 0.5f, Offset(w / 2, h * 0.42f))
    val shade = Path().apply {
        moveTo(w * 0.34f, h * 0.18f); lineTo(w * 0.66f, h * 0.18f)
        lineTo(w * 0.80f, h * 0.56f); lineTo(w * 0.20f, h * 0.56f); close()
    }
    drawPath(shade, Brush.verticalGradient(listOf(Color(0xFFFFE3B0), Color(0xFFFFB45C)), h * 0.18f, h * 0.56f))
    drawCircle(Color(0xFF6B4A2E), w * 0.035f, Offset(w * 0.43f, h * 0.42f)) // eyes: 온이 is a character
    drawCircle(Color(0xFF6B4A2E), w * 0.035f, Offset(w * 0.57f, h * 0.42f))
    drawRect(Color(0xFFB08A6A), Offset(w * 0.47f, h * 0.56f), Size(w * 0.06f, h * 0.22f))
    drawRoundRect(Color(0xFFB08A6A), Offset(w * 0.30f, h * 0.78f), Size(w * 0.40f, h * 0.08f), CornerRadius(w * 0.04f))
}
