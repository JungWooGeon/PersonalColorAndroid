package com.gun.personalcolor.feature.onboarding

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.designsystem.BottomActions
import com.gun.personalcolor.core.designsystem.DropGlyph
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.TintyPaint
import com.gun.personalcolor.core.designsystem.TintyProp
import com.gun.personalcolor.core.designsystem.aurora
import com.gun.personalcolor.core.designsystem.drawDropGlyph
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.accent
import com.gun.personalcolor.core.ui.softBrush
import com.gun.personalcolor.core.ui.tintyColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

private val ContentMax = 560.dp

@HiltViewModel
internal class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    content: ContentRepository,
) : ViewModel() {
    val tones: List<Tone> = content.tones
    private var finishing = false

    fun finish(onDone: () -> Unit) {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            // A failed write only means onboarding shows again next launch; never block or crash here.
            runCatching { prefs.setOnboardingDone(true) }.onFailure { Log.w("Onboarding", "save failed", it) }
            onDone()
        }
    }
}

@Composable
internal fun OnboardingRoute(onDone: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    TrackScreen("onboarding")
    BackHandler { vm.finish(onDone) }
    OnboardingScreen(vm.tones, onFinish = { vm.finish(onDone) })
}

/** One beat of 틴티's story: what it says, and how it looks while saying it. */
private class Beat(val title: String, val body: String, val expression: TintyExpression, val prop: TintyProp)

private val beats = listOf(
    Beat("안녕하세요,\n틴티예요", "물감 한 방울에서 태어난 컬러 친구예요.\n나한테 어울리는 색, 같이 찾아봐요.", TintyExpression.Cheer, TintyProp.None),
    Beat("사진 한 장이면\n충분해요", "피부·눈동자·입술·머리카락 색을 보고\n12톤 중에서 내 톤을 골라요.", TintyExpression.Thinking, TintyProp.Magnifier),
    Beat("사진은 휴대폰 밖으로\n나가지 않아요", "얼굴 분석은 휴대폰 안에서만 해요.\n서버로 보내지도, 저장하지도 않아요.", TintyExpression.Happy, TintyProp.Lock),
    Beat("내 색으로 물들면\n매일이 쉬워져요", "옷 색 체크, 옷장, 오늘의 코디까지\n틴티가 내 톤에 맞춰 골라 줄게요.", TintyExpression.Cheer, TintyProp.Hanger),
)

/**
 * Onboarding as a short story told by 틴티: the same 틴티 stays on stage while the pages turn, changing pose and
 * prop with a little hop; on the last page it gets painted into a tone (the same moment the result screen plays).
 */
@Composable
internal fun OnboardingScreen(tones: List<Tone>, onFinish: () -> Unit) {
    val pager = rememberPagerState { beats.size }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage
    val last = page == beats.lastIndex

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.weight(1f).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            Row(Modifier.readableWidth(ContentMax).heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                if (!last) TextButton(onFinish) { Text("건너뛰기", maxLines = 1) }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val landscape = maxWidth > maxHeight
                val stageSize = if (landscape) min(maxHeight * 0.9f, 320.dp) else min(maxHeight * 0.56f, min(maxWidth - 40.dp, 340.dp))
                val text: @Composable (Modifier) -> Unit = { m ->
                    HorizontalPager(pager, m) { i -> BeatText(beats[i], centered = !landscape) }
                }
                if (landscape) Row(Modifier.fillMaxSize().padding(horizontal = ScreenPadding), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Stage(page, tones, stageSize) }
                    text(Modifier.weight(1f))
                } else Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(0.3f))
                    Stage(page, tones, stageSize)
                    Spacer(Modifier.height(20.dp))
                    text(Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
        BottomActions {
            Row(Modifier.readableWidth(ContentMax), verticalAlignment = Alignment.CenterVertically) {
                DropIndicator(beats.size, page, Modifier.weight(1f))
                PcButton(
                    if (last) "시작하기" else "다음",
                    onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(page + 1) } },
                    modifier = Modifier.widthIn(min = 136.dp),
                )
            }
        }
    }
}

@Composable
private fun BeatText(beat: Beat, centered: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = ScreenPadding),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        val align = if (centered) TextAlign.Center else TextAlign.Start
        Text(beat.title, style = MaterialTheme.typography.headlineMedium, textAlign = align)
        Spacer(Modifier.height(12.dp))
        Text(beat.body, style = Muted.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight), textAlign = align)
    }
}

/** The stage: a scene per beat (cross-fading) behind the one 틴티, who hops when the beat changes. */
@Composable
private fun Stage(page: Int, tones: List<Tone>, size: Dp) {
    val beat = beats[page]
    val painted = remember(tones) { tones.firstOrNull { it.id == "spring_true" } ?: tones.getOrNull(1) }
    val hop = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(page) {
        if (page > 0) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        hop.snapTo(0f); hop.animateTo(-22f, tween(150)); hop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 650f))
    }
    // Last beat: brand 틴티 gets painted into a tone.
    val flood = remember { Animatable(0f) }
    LaunchedEffect(page == beats.lastIndex) {
        if (page == beats.lastIndex) { flood.snapTo(0f); flood.animateTo(1f, tween(2100, delayMillis = 250, easing = LinearEasing)) } else flood.snapTo(0f)
    }
    val paint = remember(flood) {
        TintyPaint(TintyColors.Brand, drop = { (flood.value / 0.25f).coerceIn(0f, 1f) },
            flood = { FastOutSlowInEasing.transform(((flood.value - 0.25f) / 0.4f).coerceIn(0f, 1f)) },
            chips = { 4f * ((flood.value - 0.55f) / 0.35f).coerceIn(0f, 1f) })
    }
    val toneColors = remember(painted) { painted?.tintyColors ?: TintyColors.Brand }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        AnimatedContent(page, label = "scene", transitionSpec = { (fadeIn(tween(320)) + scaleIn(tween(320), 0.94f)) togetherWith fadeOut(tween(200)) }) { p ->
            Scene(p, tones, painted, Modifier.fillMaxSize())
        }
        Tinty(
            Modifier.size(size * 0.66f).graphicsLayer { translationY = hop.value * density },
            colors = if (page == beats.lastIndex) toneColors else TintyColors.Brand,
            expression = if (page == beats.lastIndex && flood.value < 0.9f) TintyExpression.Surprised else beat.expression,
            prop = beat.prop,
            propColor = if (page == beats.lastIndex) toneColors.scarf[0] else MaterialTheme.colorScheme.primary,
            paint = if (page == beats.lastIndex) paint else null,
        )
    }
}

/** Per-beat backdrop, drawn in the app's own shapes: drops, paint chips, a phone, a tone wash. */
@Composable
private fun Scene(page: Int, tones: List<Tone>, painted: Tone?, modifier: Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    val card = MaterialTheme.colorScheme.surfaceContainerLowest
    val spin by rememberInfiniteTransition("scene").animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "spin")
    when (page) {
        // 12 tone drops slowly circling: the whole color world.
        0 -> Box(modifier.aurora(PcTheme.colors)) {
            val colors = remember(tones) { tones.map { it.accent } }
            Canvas(Modifier.fillMaxSize()) {
                val c = center; val r = size.minDimension * 0.44f
                colors.forEachIndexed { i, col ->
                    val a = Math.toRadians((spin + i * 30.0) - 90).toFloat()
                    val p = Offset(c.x + r * cos(a), c.y + r * sin(a))
                    val s = size.minDimension * 0.075f
                    drawDrop(col, p, Size(s, s * 1.3f))
                }
            }
        }
        // A viewfinder with paint chips floating around: 틴티 is looking closely.
        1 -> Box(modifier.aurora(PcTheme.colors)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.minDimension; val l = w * 0.12f; val t = w * 0.08f; val r = w * 0.88f; val b = w * 0.92f; val k = w * 0.12f
                val st = Stroke(3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                for ((x, y, dx, dy) in listOf(Quad(l, t, 1f, 1f), Quad(r, t, -1f, 1f), Quad(l, b, 1f, -1f), Quad(r, b, -1f, -1f))) {
                    drawLine(outline, Offset(x, y), Offset(x + k * dx, y), st.width, st.cap); drawLine(outline, Offset(x, y), Offset(x, y + k * dy), st.width, st.cap)
                }
                TintyColors.Brand.scarf.forEachIndexed { i, col ->
                    val p = listOf(Offset(0.2f, 0.28f), Offset(0.84f, 0.24f), Offset(0.16f, 0.7f), Offset(0.86f, 0.66f))[i]
                    rotate(-12f + i * 9f + 6f * sin(Math.toRadians(spin * 4.0 + i * 80).toFloat()), Offset(p.x * w, p.y * w)) {
                        val cw = w * 0.11f
                        drawRoundRect(card, Offset(p.x * w - cw / 2, p.y * w - cw * 0.65f), Size(cw, cw * 1.3f), CornerRadius(cw * 0.14f))
                        drawRoundRect(col, Offset(p.x * w - cw * 0.38f, p.y * w - cw * 0.52f), Size(cw * 0.76f, cw * 0.72f), CornerRadius(cw * 0.1f))
                    }
                }
            }
        }
        // 틴티 inside a phone: the photo stays here.
        2 -> Box(modifier.aurora(PcTheme.colors)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.minDimension
                val pw = w * 0.64f; val ph = w * 0.92f
                val tl = Offset((size.width - pw) / 2, (size.height - ph) / 2)
                drawRoundRect(card, tl, Size(pw, ph), CornerRadius(w * 0.09f), alpha = 0.85f)
                drawRoundRect(outline, tl, Size(pw, ph), CornerRadius(w * 0.09f), style = Stroke(3.dp.toPx()))
                drawRoundRect(outline, Offset(size.width / 2 - w * 0.06f, tl.y + w * 0.035f), Size(w * 0.12f, w * 0.022f), CornerRadius(w * 0.011f))
            }
        }
        // The tone wash 틴티 is painted into.
        else -> Box(modifier, contentAlignment = Alignment.Center) {
            if (painted != null) Box(Modifier.fillMaxSize(0.92f).background(painted.softBrush(), androidx.compose.foundation.shape.CircleShape))
        }
    }
}

private data class Quad(val a: Float, val b: Float, val c: Float, val d: Float)

/** A faceless 틴티 drop of size [s] centered at [c]. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDrop(color: Color, c: Offset, s: Size) =
    inset(c.x - s.width / 2, c.y - s.height / 2, size.width - c.x - s.width / 2, size.height - c.y - s.height / 2) { drawDropGlyph(color) }

/** Page dots as little drops: the current one is violet and a bit larger. */
@Composable
private fun DropIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        repeat(count) { i ->
            val h by animateDpAsState(if (i == current) 18.dp else 12.dp, label = "dot")
            val color by animateColorAsState(if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, label = "dotColor")
            DropGlyph(color, Modifier.size(h * 0.8f, h))
        }
    }
}

@Preview(widthDp = 360, heightDp = 720)
@Composable
private fun OnboardingPreview() = PcTheme { OnboardingScreen(emptyList()) {} }

@Preview(widthDp = 780, heightDp = 360)
@Composable
private fun OnboardingLandscapePreview() = PcTheme { OnboardingScreen(emptyList()) {} }
