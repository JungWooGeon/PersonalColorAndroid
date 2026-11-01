package com.gun.personalcolor.feature.analyze

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
        AlertDialog(
            onDismissRequest = { rationale = false },
            title = { Text("카메라 권한이 필요해요") },
            text = { Text("얼굴과 조명을 확인해 잘 나온 순간에 자동으로 찍어요.") },
            confirmButton = { TextButton(onClick = { rationale = false; permission.launch(Manifest.permission.CAMERA) }) { Text("허용하기") } },
            dismissButton = { TextButton(onClick = { rationale = false; pick() }) { Text("앨범에서 고르기") } },
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
        else -> AnalyzeScreen(state, showGuide, onBack, { gated(takePhoto) }, { gated(pick) }, onQuiz)
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
                AnalyzeState.Working -> Centered(pad) { Working() }
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

/** 틴티 thinking while short step lines take turns (they're cosmetic; analysis takes a second or two). */
@Composable
private fun Working() {
    val steps = remember { listOf("피부 톤을 보고 있어요", "눈동자와 입술 색을 재고 있어요", "어울리는 톤을 찾고 있어요") }
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (step < steps.lastIndex) { delay(1200); step++ } }
    TintyLoading(size = 168.dp)
    Spacer(Modifier.height(20.dp))
    AnimatedContent(step, label = "step") { i ->
        Text(steps[i], style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(4.dp))
    Text("사진은 휴대폰 안에서만 봐요", style = Muted, textAlign = TextAlign.Center)
}

@Composable
private fun Guide(pad: PaddingValues, showGuide: Boolean, onQuiz: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(showGuide) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad)
            .readableWidth().padding(horizontal = ScreenPadding).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.padding(top = 4.dp)) {
            Eyebrow("12톤 퍼스널컬러")
            Spacer(Modifier.height(4.dp))
            Text("사진 한 장이면 돼요", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text("피부·눈동자·입술·머리카락 색으로 어울리는 톤을 찾아요.", style = Muted)
        }

        SectionCard("이렇게 찍어 주세요") {
            Step(1, Icons.Outlined.WbSunny, "낮에 창가 자연광에서", "노란 조명은 피해 주세요")
            Step(2, Icons.Outlined.Description, "흰 종이를 함께", "A4 용지를 턱 아래에 들면 색이 더 정확해요")
            Step(3, Icons.Outlined.CenterFocusStrong, "정면으로 얼굴을 크게", "위치와 빛이 맞으면 자동으로 찍어요")
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tip(Icons.Outlined.FaceRetouchingOff, "필터·보정 없이", "맨얼굴이나 가벼운 메이크업이 좋아요")
                    Tip(Icons.Outlined.Visibility, "눈이 잘 보이게", "앞머리·안경이 눈을 가리지 않게 해 주세요")
                    Tip(Icons.Outlined.PhotoLibrary, "2~3장이면 더 좋아요", "여러 장을 고르면 평균으로 진단해요")
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
private fun Step(n: Int, icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            IconTile(icon, size = 44.dp, drop = n == 1)
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
private fun Tip(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
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
