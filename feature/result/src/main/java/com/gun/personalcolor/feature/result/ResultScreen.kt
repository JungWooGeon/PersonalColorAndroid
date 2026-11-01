package com.gun.personalcolor.feature.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.designsystem.TintyPaint
import com.gun.personalcolor.core.designsystem.TintyProp
import com.gun.personalcolor.core.designsystem.rememberReducedMotion
import com.gun.personalcolor.core.ui.hexColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.ads.NativeAdCard
import com.gun.personalcolor.core.ads.RewardRequest
import com.gun.personalcolor.core.ads.rememberRewardGate
import com.gun.personalcolor.core.analysis.ColorMath
import com.gun.personalcolor.core.analysis.Lab
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.designsystem.Eyebrow
import com.gun.personalcolor.core.designsystem.ListRow
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcCard
import com.gun.personalcolor.core.designsystem.PcMotion
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.PcTonalButton
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionCard
import com.gun.personalcolor.core.designsystem.SectionHeader
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.isExpandedWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.domain.Diagnosis
import com.gun.personalcolor.core.model.ResultSource
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.ReportRenderer
import com.gun.personalcolor.core.ui.Swatch
import com.gun.personalcolor.core.ui.Tag
import com.gun.personalcolor.core.ui.ToneAvatar
import com.gun.personalcolor.core.ui.ToneDetails
import com.gun.personalcolor.core.ui.seasonKo
import com.gun.personalcolor.core.ui.softBrush
import com.gun.personalcolor.core.ui.temperatureKo
import com.gun.personalcolor.core.ui.tintyColors

@Composable
internal fun ResultRoute(
    onBack: () -> Unit, onDrape: () -> Unit, onTone: (String) -> Unit, onRetryPhoto: () -> Unit, onColorMatch: () -> Unit,
    onWallpaper: (String) -> Unit = {},
    vm: ResultViewModel = hiltViewModel(),
) {
    TrackScreen("result")
    val d by vm.diagnosis.collectAsStateWithLifecycle()
    val myToneId by vm.myToneId.collectAsStateWithLifecycle()
    val diagnosis = d ?: run { LaunchedEffect(Unit) { onBack() }; return }
    val tone = vm.content.tone(diagnosis.top.toneId)
    val context = LocalContext.current
    val analytics = LocalAnalytics.current
    val gate = rememberRewardGate()
    val snackbar = remember { SnackbarHostState() }
    val saveImage = rememberImageSave(snackbar)
    val saveReport = {
        gate(RewardRequest(key = "report", title = "광고 보고 저장하기", message = "짧은 광고 하나만 보면 무료예요.") {
            analytics?.log("report_save", "tone" to tone.id)
            saveImage("personal-color-report") { ReportRenderer.render(context, tone) }
        })
    }

    // The reveal plays once per result (not again when coming back from draping).
    var revealed by rememberSaveable { mutableStateOf(false) }
    val reveal = rememberReveal(revealed) { revealed = true }

    PcScaffold(
        title = "진단 결과",
        onBack = onBack,
        actions = { ShareMenu(tone) { open -> IconButton(onClick = open) { Icon(Icons.Outlined.Share, "결과 공유") } } },
        snackbarHostState = snackbar,
    ) { pad ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad)
                .readableWidth(if (isExpandedWindow()) 1040.dp else 680.dp).padding(horizontal = ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val summary: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Hero(tone, diagnosis, reveal)
                    Column(Modifier.revealed(reveal), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ShareMenu(tone) { open -> PcButton("결과 공유하기", open, Modifier.fillMaxWidth(), Icons.Outlined.Share) }
                        SetMyTone(isMyTone = myToneId == tone.id) { vm.setMyTone(tone.id) }
                        ListCard("결과로 해 보기") {
                            val art = remember(tone) { tone.tintyColors }
                            if (diagnosis.photo != null) ActionRow("컬러 드레이핑", "BEST와 WORST를 얼굴에 대 봐요", TintyProp.Fabric, art, onDrape)
                            ActionRow("리포트 이미지 저장", "한 장으로 정리한 내 컬러", TintyProp.Book, art, saveReport)
                            ActionRow("배경화면 만들기", "내 톤 컬러로 꾸며요", TintyProp.ColorCard, art) { onWallpaper(tone.id) }
                        }
                    }
                }
            }
            val analysis: @Composable (Modifier) -> Unit = { m ->
                Column(m.revealed(reveal), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Similar(diagnosis, vm.content, onTone)
                    Basis(diagnosis)
                }
            }
            Spacer(Modifier.height(0.dp))
            if (isExpandedWindow()) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    summary(Modifier.weight(1f)); analysis(Modifier.weight(1f))
                }
            } else {
                summary(Modifier); analysis(Modifier)
            }

            SectionHeader("${tone.nameKo} 스타일 가이드", Modifier.padding(top = 8.dp).revealed(reveal))
            ToneDetails(tone)

            ListCard("다음 단계") {
                val art = remember(tone) { tone.tintyColors }
                ActionRow("쇼핑할 때 컬러 체크", "옷 색이 나한테 맞는지 바로 봐요", TintyProp.Eyedropper, art, onColorMatch)
                ActionRow(if (diagnosis.source == ResultSource.AI) "다른 사진으로 다시 진단" else "사진으로 진단하기",
                    if (diagnosis.source == ResultSource.AI) "조명이 다른 사진이면 좋아요" else "사진 한 장이면 돼요",
                    TintyProp.Magnifier, art, onRetryPhoto)
            }
            NativeAdCard()
            Text(
                "조명과 카메라에 따라 결과가 조금 달라질 수 있어요. 참고용으로 봐 주세요.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * The signature moment: brand 틴티 gets a drop of the tone's paint, floods into the tone's colors (the card washes
 * over from 틴티 outward), the scarf chips flip one by one, then it cheers and the name and colors come in.
 * Tap to skip. Once revealed (or with system animations off) it's simply the final card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hero(tone: Tone, d: Diagnosis, r: Reveal) {
    val toneColors = remember(tone) { tone.tintyColors }
    val wash = tone.softBrush()
    val base = MaterialTheme.colorScheme.surfaceContainerLowest
    val aurora = PcTheme.colors
    val paint = remember(r) { TintyPaint(TintyColors.Clear, drop = { r.drop }, flood = { r.flood }, chips = { r.chips }) }
    val confetti = remember(tone) { confettiOf(tone) }
    val expression by remember(r) {
        derivedStateOf { when { r.t < LAND -> TintyExpression.Default; r.t < CHEER -> TintyExpression.Surprised; else -> TintyExpression.Cheer } }
    }
    Box(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
            .drawBehind {
                // Brand canvas, then the tone wash growing from 틴티 (its center is ~106dp from the top).
                drawRect(base)
                if (r.flood < 1f) drawRect(Brush.radialGradient(listOf(aurora.auroraLavender, aurora.auroraPeach.copy(alpha = 0f)), Offset(size.width * 0.7f, 0f), size.width))
                val c = Offset(size.width / 2, 106.dp.toPx())
                val reach = kotlin.math.hypot(size.width, size.height)
                if (r.flood >= 1f) drawRect(wash) else if (r.flood > 0f) drawCircle(wash, reach * EaseInOut.transform(r.flood), c)
            }
            .clickable(remember { MutableInteractionSource() }, indication = null, enabled = !r.done) { r.skip() },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Tinty(Modifier.size(188.dp), toneColors, expression, paint = paint)
            Column(
                Modifier.graphicsLayer {
                    alpha = r.text; val s = 0.94f + 0.06f * r.text; scaleX = s; scaleY = s; translationY = (1f - r.text) * 12.dp.toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Eyebrow(
                    when {
                        d.source == ResultSource.QUIZ -> "셀프 진단 결과"
                        d.photoCount > 1 -> "사진 ${d.photoCount}장 진단 결과"
                        else -> "찾았어요, 내 퍼스널컬러"
                    },
                )
                Spacer(Modifier.height(4.dp))
                Text(tone.nameKo, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                Text("${tone.nameEn} · ${tone.seasonKo} ${tone.temperatureKo}", style = Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tone.keywords.forEach { Tag("#$it", color = MaterialTheme.colorScheme.surfaceContainer) }
                }
            }
            Spacer(Modifier.height(16.dp))
            PcCard(Modifier.fillMaxWidth().graphicsLayer { alpha = r.card }, elevation = 0) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tone.bestColors.take(8).forEachIndexed { i, c ->
                            Swatch(c.hex, Modifier.weight(1f).aspectRatio(1f).graphicsLayer {
                                val k = r.swatch(i); alpha = k; scaleX = 0.6f + 0.4f * k; scaleY = 0.6f + 0.4f * k
                            }, RoundedCornerShape(10.dp))
                        }
                    }
                    Text(tone.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        // Confetti bursts from 틴티 when it cheers.
        Canvas(Modifier.matchParentSize()) {
            val k = r.confetti
            if (k <= 0f || k >= 1f) return@Canvas
            val origin = Offset(size.width / 2, 100.dp.toPx())
            val u = 1.dp.toPx()
            for (p in confetti) {
                val x = origin.x + p.vx * u * k * 260f
                val y = origin.y + p.vy * u * k * 260f + 340f * u * k * k
                val a = if (k > 0.7f) (1f - k) / 0.3f else 1f
                rotate(p.spin * k * 360f, Offset(x, y)) {
                    if (p.round) drawCircle(p.color, p.size * u * 0.5f, Offset(x, y), alpha = a)
                    else drawRoundRect(p.color, Offset(x - p.size * u / 2, y - p.size * u * 0.3f), Size(p.size * u, p.size * u * 0.6f), CornerRadius(2f * u), alpha = a)
                }
            }
        }
    }
}

private class Confetto(val vx: Float, val vy: Float, val size: Float, val color: Color, val round: Boolean, val spin: Float)

private fun confettiOf(tone: Tone): List<Confetto> {
    val rnd = java.util.Random(tone.id.hashCode().toLong())
    val colors = tone.bestColors.map { hexColor(it.hex) }
    return List(26) { i ->
        val a = Math.toRadians(-160.0 + 140.0 * rnd.nextDouble()) // upward fan
        val speed = 0.55f + 0.6f * rnd.nextFloat()
        Confetto(
            (kotlin.math.cos(a) * speed).toFloat(), (kotlin.math.sin(a) * speed).toFloat(), 6f + 6f * rnd.nextFloat(),
            colors[i % colors.size], rnd.nextBoolean(), (rnd.nextFloat() - 0.5f) * 3f,
        )
    }
}

// Reveal timeline (ms).
private const val DROP = 150f
private const val LAND = 650f
private const val CHIPS = 1450f
private const val CHEER = 2200f
private const val END = 3500f
private val EaseInOut = androidx.compose.animation.core.CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/** One clock drives the whole reveal; every part is a slice of it (read only while drawing). */
@Stable
private class Reveal(private val clock: Animatable<Float, *>, private val scope: kotlinx.coroutines.CoroutineScope) {
    val t get() = clock.value
    private fun span(from: Float, len: Float) = ((t - from) / len).coerceIn(0f, 1f)
    val done get() = t >= END
    val drop get() = span(DROP, LAND - DROP)
    val flood get() = EaseInOut.transform(span(LAND, 1150f))
    val chips get() = 4f * span(CHIPS, 640f)
    val confetti get() = span(CHEER, 1300f)
    val text get() = PcMotion.Pop.transform(span(CHEER + 80f, 420f))
    val card get() = span(CHEER + 260f, 260f)
    fun swatch(i: Int) = PcMotion.Pop.transform(span(CHEER + 320f + i * 45f, 280f))
    val content get() = PcMotion.Out.transform(span(CHEER + 520f, 480f))
    fun skip() { scope.launch { clock.snapTo(END) } }
}

@Composable
private fun rememberReveal(alreadyPlayed: Boolean, onDone: () -> Unit): Reveal {
    val reduced = rememberReducedMotion()
    val clock = remember { Animatable(if (alreadyPlayed || reduced) END else 0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        if (clock.value >= END) return@LaunchedEffect
        // The interstitial before the result covers this screen: start only once it's actually visible.
        lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
        delay(250)
        launch {
            // Taps of haptics on the landing and the cheer (skipped if the user skipped ahead).
            snapshotFlow { clock.value >= LAND }.first { it }; if (clock.value < CHEER) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            snapshotFlow { clock.value >= CHEER }.first { it }; if (clock.value < END) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        clock.animateTo(END, tween(END.toInt(), easing = LinearEasing))
    }
    LaunchedEffect(Unit) { snapshotFlow { clock.value >= END }.first { it }; onDone() }
    return remember { Reveal(clock, scope) }
}

/** Content below the hero waits for the reveal, then rises in. */
private fun Modifier.revealed(r: Reveal) = graphicsLayer { alpha = r.content; translationY = (1f - r.content) * 24.dp.toPx() }

@Composable
private fun SetMyTone(isMyTone: Boolean, onSet: () -> Unit) =
    PcTonalButton(if (isMyTone) "내 톤으로 저장했어요" else "내 톤으로 설정", onSet, Modifier.fillMaxWidth(),
        if (isMyTone) Icons.Outlined.CheckCircle else Icons.Outlined.FavoriteBorder, enabled = !isMyTone)

/** Card of tappable rows: rows run edge to edge so their own padding lines up with the title. */
@Composable
private fun ListCard(title: String, content: @Composable () -> Unit) = PcCard(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp))
        content()
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, prop: TintyProp, colors: TintyColors, onClick: () -> Unit) =
    ListRow(title, Modifier.fillMaxWidth(), subtitle = subtitle, art = prop, artColors = colors, onClick = onClick)

/** Runner-up tones by name only — never a score or percentage. */
@Composable
private fun Similar(d: Diagnosis, content: ContentRepository, onTone: (String) -> Unit) {
    val others = d.scores.drop(1).take(2).map { content.tone(it.toneId) }
    if (others.isEmpty()) return
    SectionCard("비슷한 톤") {
        Column {
            others.forEach { t ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { onTone(t.id) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToneAvatar(t, 36.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(t.nameKo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${t.seasonKo} · ${t.temperatureKo}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = PcTheme.colors.ink3)
                }
            }
        }
        Text("이 톤의 컬러도 잘 어울릴 수 있어요", style = Muted)
    }
}

@Composable
private fun Basis(d: Diagnosis) {
    SectionCard("이렇게 봤어요") {
        Meter("쿨", "웜", d.axes.warmth)
        Meter("딥", "라이트", d.axes.value)
        Meter("뮤트", "클리어", d.axes.clarity)
        d.features?.let { f ->
            Spacer(Modifier.height(4.dp))
            Text("측정한 색", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Measured("피부", f.skin)
                f.iris?.let { Measured("눈동자", it) }
                f.lips?.let { Measured("입술", it) }
                f.hair?.let { Measured("머리카락", it) }
            }
            Text("조명 색을 보정한 값이에요", style = Muted)
        }
    }
}

/** A -1..1 value shown as a dot on a track between two labels. */
@Composable
private fun Meter(left: String, right: String, value: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(left, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.width(44.dp))
        BoxWithConstraints(Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primaryContainer))
            Box(Modifier.align(Alignment.Center).size(2.dp, 12.dp).background(MaterialTheme.colorScheme.outline))
            val x = (maxWidth - 18.dp) * ((value.coerceIn(-1.0, 1.0) + 1) / 2).toFloat()
            Box(Modifier.offset(x = x).size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                .border(3.dp, MaterialTheme.colorScheme.surfaceContainer, CircleShape))
        }
        Text(right, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.width(56.dp).padding(start = 10.dp))
    }
}

@Composable
private fun Measured(label: String, lab: Lab) {
    val rgb = ColorMath.labToRgb(lab)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(rgb.r, rgb.g, rgb.b))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

