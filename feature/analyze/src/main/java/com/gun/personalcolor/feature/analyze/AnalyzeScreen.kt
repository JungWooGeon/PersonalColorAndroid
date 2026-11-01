package com.gun.personalcolor.feature.analyze

import com.gun.personalcolor.core.designsystem.PropTile
import com.gun.personalcolor.core.designsystem.PcDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.gun.personalcolor.core.designsystem.DropGlyph
import com.gun.personalcolor.core.designsystem.PcColors
import com.gun.personalcolor.core.designsystem.PcMotion
import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.designsystem.TintyProp
import com.gun.personalcolor.core.designsystem.TintyHeader
import com.gun.personalcolor.core.designsystem.TintyWorking
import com.gun.personalcolor.core.designsystem.tintyEntrance
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import com.gun.personalcolor.core.designsystem.Eyebrow
import com.gun.personalcolor.core.designsystem.IconTile
import com.gun.personalcolor.core.designsystem.TintyTip
import com.gun.personalcolor.core.designsystem.aurora
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FaceRetouchingOff
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.ads.AdManager
import com.gun.personalcolor.core.ads.LocalAdManager
import com.gun.personalcolor.core.ads.RewardRequest
import com.gun.personalcolor.core.ads.rememberRewardGate
import com.gun.personalcolor.core.analysis.QualityIssue
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.BottomActions
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionCard
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.TintyLoading
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.domain.PhotoFailure
import com.gun.personalcolor.core.vision.PhotoInput

private const val MAX_PHOTOS = 3

@Composable
internal fun AnalyzeRoute(onBack: () -> Unit, onResult: () -> Unit, onQuiz: () -> Unit, vm: AnalyzeViewModel = hiltViewModel()) {
    TrackScreen("analyze")
    val context = LocalContext.current
    val activity = LocalActivity.current
    val ads = LocalAdManager.current
    val analytics = LocalAnalytics.current
    val state by vm.state.collectAsStateWithLifecycle()
    val showGuide by vm.showGuide.collectAsStateWithLifecycle()
    val step by vm.step.collectAsStateWithLifecycle()

    val run = { inputs: List<PhotoInput> ->
        vm.analyze(inputs) {
            val watchedReward = ads?.consumeUnlock(AdManager.KEY_PHOTO) == true
            // One ad per diagnosis: if a rewarded ad unlocked it, no interstitial before the result.
            if (watchedReward || ads == null) onResult() else ads.onResult(activity, onResult)
        }
    }
    // Up to 3 photos: averaging several shots cancels out lighting differences.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)) { uris ->
        run(uris.map { PhotoInput(it) })
    }
    val pick: () -> Unit = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    // Guided camera needs CAMERA; without it (or without a camera) we fall back to the album.
    var rationale by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.openCamera() else {
            Toast.makeText(context, "앨범에서 사진을 골라 주세요", Toast.LENGTH_SHORT).show()
            pick()
        }
    }
    val takePhoto: () -> Unit = {
        when {
            !context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) -> pick()
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> vm.openCamera()
            activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) -> rationale = true
            else -> permission.launch(Manifest.permission.CAMERA)
        }
    }
    if (rationale) {
        PcDialog(
            "카메라 권한이 필요해요", "얼굴과 조명을 확인해 잘 나온 순간에 자동으로 찍어요.", "허용하기",
            onConfirm = { rationale = false; permission.launch(Manifest.permission.CAMERA) },
            onDismiss = { rationale = false; pick() }, dismissText = "앨범에서 고르기", prop = TintyProp.Magnifier,
        )
    }

    // Opt-in rewarded ad before a diagnosis. If no ad is ready (or one was watched recently), it just continues.
    val gate = rememberRewardGate()
    val gated = { action: () -> Unit ->
        gate(RewardRequest(AdManager.KEY_PHOTO, "광고 보고 진단하기", "짧은 광고 하나만 보면 무료예요.", AdManager.PHOTO_UNLOCK_MS, action))
    }

    // While working, back is ignored (analysis takes a second or two); otherwise it steps back inside the flow.
    BackHandler(enabled = state != AnalyzeState.Idle) { vm.back() }

    when (val s = state) {
        AnalyzeState.Camera -> GuidedCamera(onClose = vm::back, onCaptured = vm::captured)
        is AnalyzeState.WhiteRef -> WhiteRefScreen(s.uri, onRetake = vm::back) { whiteRef ->
            analytics?.log("capture", "mode" to if (s.auto) "auto" else "manual", "white_ref" to if (whiteRef != null) "yes" else "no")
            run(listOf(PhotoInput(s.uri, whiteRef)))
        }
        else -> AnalyzeScreen(state, showGuide, onBack, { gated(takePhoto) }, { gated(pick) }, onQuiz, step)
    }
}

@Composable
internal fun AnalyzeScreen(
    state: AnalyzeState,
    showGuide: Boolean,
    onBack: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onQuiz: () -> Unit,
    step: Int = 0,
) {
    PcScaffold(
        title = "AI 사진 진단",
        onBack = onBack,
        bottomBar = {
            if (state != AnalyzeState.Working) BottomActions {
                PcButton(if (state is AnalyzeState.Failed) "다시 찍기" else "카메라로 찍기", onCamera, Modifier.fillMaxWidth(), Icons.Outlined.CameraAlt)
                PcOutlinedButton("앨범에서 고르기 (최대 3장)", onGallery, Modifier.fillMaxWidth(), Icons.Outlined.PhotoLibrary)
            }
        },
    ) { pad ->
        AnimatedContent(state, label = "analyze") { s ->
            when (s) {
                AnalyzeState.Working -> Centered(pad) { Working(step) }
                is AnalyzeState.Failed -> Centered(pad) {
                    val (title, message) = s.failure.message()
                    Box(Modifier.size(176.dp).aurora(PcTheme.colors), contentAlignment = Alignment.Center) {
                        Tinty(Modifier.size(136.dp), expression = TintyExpression.Surprised)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(message, style = Muted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onQuiz) { Text("사진 없이 질문으로 진단") }
                }
                else -> Guide(pad, showGuide, onQuiz)
            }
        }
    }
}

private val CAPTIONS = listOf("사진을 살펴보는 중", "얼굴을 찾는 중", "피부색을 재는 중", "어울리는 톤을 고르는 중", "찾았어요!")
private val ORBIT = TintyColors.Brand.scarf + listOf(PcColors.Peach, PcColors.Lavender)

/**
 * 틴티 at work with a magnifier while paint chips orbit it; the caption follows the real steps (see AnalyzeViewModel)
 * and four drops in the season colors fill up. At the end 틴티 cheers and the chips burst outward.
 */
@Composable
private fun Working(step: Int) {
    val found = step >= AnalyzeViewModel.FOUND
    val spin = rememberInfiniteTransition("orbit").animateFloat(0f, 360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "a")
    val burst by animateFloatAsState(if (found) 1f else 0f, tween(700, easing = PcMotion.Out), label = "burst")
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(found) { if (found) haptic.performHapticFeedback(HapticFeedbackType.Confirm) }
    Box(Modifier.size(280.dp, 250.dp), contentAlignment = Alignment.Center) {
        Orbit(spin::value, burst, front = false)
        if (found) Tinty(Modifier.size(180.dp).tintyEntrance(), expression = TintyExpression.Cheer)
        else TintyWorking(Modifier.size(180.dp), prop = TintyProp.Magnifier)
        Orbit(spin::value, burst, front = true)
    }
    Spacer(Modifier.height(12.dp))
    AnimatedContent(
        step, label = "step",
        transitionSpec = {
            ((fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 3 }) togetherWith (fadeOut(tween(140)) + slideOutVertically(tween(160)) { -it / 3 }))
                .using(SizeTransform(clip = false))
        },
        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center,
    ) { i -> Text(CAPTIONS[i.coerceIn(0, CAPTIONS.lastIndex)], style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        repeat(4) { i ->
            val done = step > i || found
            val current = step == i && !found
            val fill by animateColorAsState(if (done) TintyColors.Brand.scarf[i] else MaterialTheme.colorScheme.outlineVariant, tween(PcMotion.BASE), label = "drop")
            val pulse = if (current) rememberInfiniteTransition("pulse").animateFloat(0.82f, 1.12f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "p").value else 1f
            DropGlyph(fill, Modifier.size(14.dp, 18.dp).graphicsLayer { scaleX = pulse; scaleY = pulse; transformOrigin = TransformOrigin(0.5f, 0.9f) })
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("사진은 휴대폰 안에서만 봐요", style = Muted, textAlign = TextAlign.Center)
}

/** Half of the orbit: chips behind 틴티 ([front] false) or in front. They fly outward and fade when [burst] → 1. */
@Composable
private fun BoxScope.Orbit(angle: () -> Float, burst: Float, front: Boolean) {
    val dark = PcTheme.colors.isDark
    val card = MaterialTheme.colorScheme.surfaceContainerLowest
    Canvas(Modifier.matchParentSize()) {
        val c = Offset(size.width / 2, size.height * 0.56f)
        val rx = size.width * 0.44f * (1f + burst * 0.5f); val ry = size.height * 0.16f * (1f + burst * 0.9f)
        val u = 1.dp.toPx()
        ORBIT.forEachIndexed { i, color ->
            val a = Math.toRadians((angle() + i * 360.0 / ORBIT.size)).toFloat()
            val depth = kotlin.math.sin(a) // > 0: in front of 틴티
            if ((depth > 0f) != front) return@forEachIndexed
            val s = (0.78f + 0.26f * depth) * u
            val p = Offset(c.x + rx * kotlin.math.cos(a), c.y + ry * depth - burst * 30f * u)
            val alpha = (1f - burst) * (0.65f + 0.35f * (depth + 1f) / 2f)
            rotate(18f * kotlin.math.cos(a), p) {
                drawRoundRect(if (dark) card else Color.White, p - Offset(11f * s, 14f * s), Size(22f * s, 28f * s), CornerRadius(4f * s), alpha = alpha)
                drawRoundRect(color, p - Offset(8f * s, 11f * s), Size(16f * s, 15f * s), CornerRadius(3f * s), alpha = alpha)
            }
        }
    }
}

@Composable
private fun Guide(pad: PaddingValues, showGuide: Boolean, onQuiz: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(showGuide) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad)
            .readableWidth().padding(horizontal = ScreenPadding).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TintyHeader("사진 한 장이면 돼요", "피부·눈동자·입술·머리카락 색으로\n어울리는 톤을 찾아요", TintyProp.Magnifier, Modifier.padding(top = 4.dp))

        SectionCard("이렇게 찍어 주세요") {
            Step(1, TintyProp.Sun, "낮에 창가 자연광에서", "노란 조명은 피해 주세요")
            Step(2, TintyProp.Paper, "흰 종이를 함께", "A4 용지를 턱 아래에 들면 색이 더 정확해요")
            Step(3, TintyProp.Camera, "정면으로 얼굴을 크게", "위치와 빛이 맞으면 자동으로 찍어요")
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tip(TintyProp.Mirror, "필터·보정 없이", "맨얼굴이나 가벼운 메이크업이 좋아요")
                    Tip(TintyProp.Magnifier, "눈이 잘 보이게", "앞머리·안경이 눈을 가리지 않게 해 주세요")
                    Tip(TintyProp.Photos, "2~3장이면 더 좋아요", "여러 장을 고르면 평균으로 진단해요")
                }
            }
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (expanded) "팁 접기" else "팁 더 보기")
                Icon(if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, Modifier.padding(start = 4.dp).size(18.dp))
            }
        }

        TintyTip("사진은 휴대폰 안에서만 봐요", Modifier.fillMaxWidth())
        TextButton(onClick = onQuiz, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("사진 없이 질문으로 진단") }
    }
}

/** Numbered key step: icon tile with a small step number. */
@Composable
private fun Step(n: Int, prop: TintyProp, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            PropTile(prop, size = 48.dp)
            Box(
                Modifier.align(Alignment.TopStart).offset((-4).dp, (-4).dp).size(18.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("$n", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = Muted)
        }
    }
}

@Composable
private fun Tip(prop: TintyProp, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PropTile(prop, size = 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = Muted)
        }
    }
}

@Composable
private fun Centered(pad: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

private fun PhotoFailure.message(): Pair<String, String> = when (this) {
    PhotoFailure.BadImage -> "사진을 열 수 없어요" to "다른 사진으로 다시 시도해 주세요."
    PhotoFailure.NoFace -> "얼굴을 찾지 못했어요" to "정면 얼굴이 크게 나온 사진을 써 주세요."
    PhotoFailure.ManyFaces -> "여러 사람이 나왔어요" to "한 사람만 나온 사진을 써 주세요."
    is PhotoFailure.Quality -> when (issue) {
        QualityIssue.NO_SKIN -> "피부가 잘 보이지 않아요" to "얼굴을 가리는 것 없이 찍어 주세요."
        QualityIssue.TOO_DARK -> "사진이 너무 어두워요" to "밝은 자연광에서 다시 찍어 주세요."
        QualityIssue.TOO_BRIGHT -> "사진이 너무 밝아요" to "직사광선·플래시 없이 찍어 주세요."
        QualityIssue.STRONG_COLOR_CAST -> "조명 색이 너무 강해요" to "자연광에서 필터 없이 찍어 주세요."
    }
}

@Preview
@Composable
private fun AnalyzePreview() = PcTheme { AnalyzeScreen(AnalyzeState.Idle, true, {}, {}, {}, {}) }
