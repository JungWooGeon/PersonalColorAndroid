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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FaceRetouchingOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.domain.PhotoFailure
import com.gun.personalcolor.core.vision.PhotoInput

private const val MAX_PHOTOS = 3

@Composable
internal fun AnalyzeRoute(onBack: () -> Unit, onResult: () -> Unit, onQuiz: () -> Unit, onRefine: () -> Unit, vm: AnalyzeViewModel = hiltViewModel()) {
    TrackScreen("analyze")
    val context = LocalContext.current
    val activity = LocalActivity.current
    val ads = LocalAdManager.current
    val analytics = LocalAnalytics.current
    val state by vm.state.collectAsStateWithLifecycle()
    val showGuide by vm.showGuide.collectAsStateWithLifecycle()

    val run = { inputs: List<PhotoInput> ->
        vm.analyze(inputs) { needsRefine ->
            ads?.consumeUnlock(AdManager.KEY_PHOTO)
            // Unsure result → a short draping check first (invisible to the user why); otherwise straight to the result.
            val next = if (needsRefine) onRefine else onResult
            ads?.onResult(activity, next) ?: next()
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
            Toast.makeText(context, "카메라 권한이 없어 앨범에서 골라 주세요", Toast.LENGTH_SHORT).show()
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
            text = { Text("얼굴 위치와 조명을 실시간으로 확인해 잘 나온 순간에 찍어 드려요. 사진은 기기 안에서만 분석해요.") },
            confirmButton = { TextButton(onClick = { rationale = false; permission.launch(Manifest.permission.CAMERA) }) { Text("허용하기") } },
            dismissButton = { TextButton(onClick = { rationale = false; pick() }) { Text("앨범에서 고르기") } },
        )
    }

    // Opt-in rewarded ad before a diagnosis. If no ad is ready (or one was watched recently), it just continues.
    val gate = rememberRewardGate()
    val gated = { action: () -> Unit ->
        gate(RewardRequest(AdManager.KEY_PHOTO, "광고 보고 무료로 진단할까요?", "광고 후 바로 진단해요. 사진이 잘 안 나와 다시 찍을 땐 광고가 없어요.", AdManager.PHOTO_UNLOCK_MS, action))
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
        title = "AI 퍼스널컬러 진단",
        onBack = onBack,
        bottomBar = {
            if (state != AnalyzeState.Working) BottomActions {
                PcButton("카메라로 촬영", onCamera, Modifier.fillMaxWidth(), Icons.Outlined.CameraAlt)
                PcOutlinedButton("앨범에서 선택 (최대 3장)", onGallery, Modifier.fillMaxWidth(), Icons.Outlined.PhotoLibrary)
            }
        },
    ) { pad ->
        AnimatedContent(state, label = "analyze") { s ->
            when (s) {
                AnalyzeState.Working -> Centered(pad) {
                    CircularProgressIndicator(Modifier.size(56.dp), strokeWidth = 5.dp)
                    Spacer(Modifier.height(24.dp))
                    Text("얼굴을 분석하고 있어요", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text("잠시만 기다려 주세요", style = Muted, textAlign = TextAlign.Center)
                }
                is AnalyzeState.Failed -> Centered(pad) {
                    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(Icons.Outlined.ErrorOutline, null, Modifier.padding(16.dp).size(36.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.height(20.dp))
                    val (title, message) = s.failure.message()
                    Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(message, style = Muted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onQuiz) { Text("사진 없이 질문으로 진단하기") }
                }
                else -> Guide(pad, showGuide, onQuiz)
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
        Spacer(Modifier.height(4.dp))
        Text("사진 한 장으로 12톤 진단", style = MaterialTheme.typography.headlineSmall)
        Text("피부·눈동자·입술·머리카락 색을 측정해요.", style = Muted)

        SectionCard("정확하게 찍는 법", action = {
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "접기" else "펼치기") }
        }) {
            Tip(Icons.Outlined.WbSunny, "낮 시간 창가의 자연광", "노란 조명은 피해 주세요")
            Tip(Icons.Outlined.Description, "흰 종이를 함께 찍기", "A4 용지를 턱 아래에 들고 찍으면 더 정확해요")
            if (expanded) {
                Tip(Icons.Outlined.FaceRetouchingOff, "필터·보정 없이", "맨얼굴이나 가벼운 메이크업")
                Tip(Icons.Outlined.CenterFocusStrong, "정면, 얼굴을 크게", "카메라가 위치와 조명을 확인해 자동으로 찍어요")
                Tip(Icons.Outlined.Visibility, "눈을 또렷하게", "앞머리·안경이 눈을 가리지 않게")
                Tip(Icons.Outlined.PhotoLibrary, "2~3장이면 더 정확해요", "앨범에서 여러 장을 고르면 평균을 내요")
            }
        }

        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.tertiaryContainer) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(Modifier.width(12.dp))
                Text("사진은 기기 안에서만 분석하고 저장하지 않아요", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
        TextButton(onClick = onQuiz, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("사진 없이 질문으로 진단하기") }
    }
}

@Composable
private fun Tip(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp).size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = Muted)
        }
    }
}

@Composable
private fun Centered(pad: PaddingValues, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

private fun PhotoFailure.message(): Pair<String, String> = when (this) {
    PhotoFailure.BadImage -> "사진을 열 수 없어요" to "다른 사진으로 다시 시도해 주세요."
    PhotoFailure.NoFace -> "얼굴을 찾지 못했어요" to "정면 얼굴이 크게 나온 사진을 써 주세요."
    PhotoFailure.ManyFaces -> "얼굴이 여러 명 있어요" to "한 사람만 나온 사진으로 진단해 주세요."
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
