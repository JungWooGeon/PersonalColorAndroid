package com.gun.personalcolor.feature.result

import androidx.compose.material3.Surface
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.ads.RewardRequest
import com.gun.personalcolor.core.ads.rememberRewardGate
import com.gun.personalcolor.core.analysis.ColorMath
import com.gun.personalcolor.core.analysis.Lab
import com.gun.personalcolor.core.ads.NativeAdCard
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionCard
import com.gun.personalcolor.core.designsystem.isExpandedWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.domain.Diagnosis
import com.gun.personalcolor.core.model.ResultSource
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.ReportRenderer
import com.gun.personalcolor.core.ui.ToneAvatar
import com.gun.personalcolor.core.ui.ToneDetails
import com.gun.personalcolor.core.ui.ToneHeader
import com.gun.personalcolor.core.ui.accent
import kotlin.math.roundToInt

@Composable
internal fun ResultRoute(
    onBack: () -> Unit, onDrape: () -> Unit, onTone: (String) -> Unit, onRetryPhoto: () -> Unit, onColorMatch: () -> Unit,
    onWallpaper: (String) -> Unit = {},
    onRefine: () -> Unit = {},
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
        gate(RewardRequest(key = "report", title = "광고 보고 저장할까요?", message = "내 톤의 컬러·메이크업·코디를 한 장으로 저장해요.") {
            analytics?.log("report_save", "tone" to tone.id)
            saveImage("personal-color-report") { ReportRenderer.render(context, tone) }
        })
    }

    PcScaffold(
        title = "진단 결과",
        onBack = onBack,
        actions = {
            ShareMenuButton(tone)
        },
    ) { pad ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad)
                    .readableWidth(if (isExpandedWindow()) 1040.dp else 680.dp).padding(horizontal = ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val summary: @Composable (Modifier) -> Unit = { m ->
                    Column(m, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(if (diagnosis.source == ResultSource.QUIZ) "셀프 진단 결과" else if (diagnosis.photoCount > 1) "사진 ${diagnosis.photoCount}장으로 분석한 나의 퍼스널컬러" else "AI가 분석한 나의 퍼스널컬러",
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        ToneHeader(tone)
                        Actions(
                            hasPhoto = diagnosis.photo != null,
                            isMyTone = myToneId == tone.id,
                            onDrape = onDrape,
                            onSetMyTone = { vm.setMyTone(tone.id) },
                        )
                        ReportActions(onReport = saveReport, onWallpaper = { onWallpaper(tone.id) })
                        // Draping confirmation is how consultants decide; most accuracy gain for one extra minute.
                        if (diagnosis.photo != null) RefineCard(onRefine)
                    }
                }
                val analysis: @Composable (Modifier) -> Unit = { m ->
                    Column(m, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Scores(diagnosis, vm.content, onTone)
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

                Text("${tone.nameKo} 스타일 가이드", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                ToneDetails(tone)

                SectionCard("다음으로 해 볼 것") {
                    PcOutlinedButton("쇼핑할 때 컬러 체크하기", onColorMatch, Modifier.fillMaxWidth(), Icons.Outlined.Colorize)
                    PcOutlinedButton(if (diagnosis.source == ResultSource.AI) "다른 사진으로 다시 진단" else "사진으로 정밀 진단",
                        onRetryPhoto, Modifier.fillMaxWidth(), Icons.Outlined.Refresh)
                }
                NativeAdCard()
                Text(
                    "조명·카메라에 따라 결과가 달라질 수 있어요. 참고용으로 활용해 주세요.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
        }
    }
}

/** Drape + set-as-mine side by side (share lives in the top bar). Stacked only on very narrow screens. */
@Composable
private fun Actions(hasPhoto: Boolean, isMyTone: Boolean, onDrape: () -> Unit, onSetMyTone: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val save: @Composable (Modifier) -> Unit = { m ->
            FilledTonalButton(onClick = onSetMyTone, enabled = !isMyTone, modifier = m.heightIn(min = 54.dp), shape = MaterialTheme.shapes.medium) {
                Icon(if (isMyTone) Icons.Outlined.CheckCircle else Icons.Outlined.FavoriteBorder, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isMyTone) "내 톤으로 저장됨" else "내 톤으로 저장", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val drape: @Composable (Modifier) -> Unit = { m -> PcButton("드레이핑", onDrape, m, Icons.Outlined.Checkroom) }
        when {
            !hasPhoto -> save(Modifier.fillMaxWidth())
            maxWidth < 320.dp -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { drape(Modifier.fillMaxWidth()); save(Modifier.fillMaxWidth()) }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { drape(Modifier.weight(1f)); save(Modifier.weight(1f)) }
        }
    }
}

/** Report image (reward-gated) + wallpaper maker; labels kept short so they stay single-line at 320dp. */
@Composable
private fun ReportActions(onReport: () -> Unit, onWallpaper: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val report: @Composable (Modifier) -> Unit = { m -> PcOutlinedButton("리포트 저장", onReport, m) }
        val wallpaper: @Composable (Modifier) -> Unit = { m -> PcOutlinedButton("배경화면", onWallpaper, m) }
        if (maxWidth < 320.dp) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { report(Modifier.fillMaxWidth()); wallpaper(Modifier.fillMaxWidth()) }
        else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { report(Modifier.weight(1f)); wallpaper(Modifier.weight(1f)) }
    }
}

@Composable
private fun Scores(d: Diagnosis, content: ContentRepository, onTone: (String) -> Unit) {
    SectionCard("톤 일치도") {
        d.scores.take(3).forEachIndexed { i, s ->
            val t: Tone = content.tone(s.toneId)
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { onTone(t.id) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToneAvatar(t, 32.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. ${t.nameKo}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${(s.probability * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { s.probability.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = t.accent, trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {},
                    )
                }
            }
        }
        Text("2·3순위 톤의 컬러도 잘 어울릴 수 있어요", style = Muted)
    }
}

@Composable
private fun Basis(d: Diagnosis) {
    SectionCard("분석 근거") {
        Meter("쿨", "웜", d.axes.warmth)
        Meter("딥", "라이트", d.axes.value)
        Meter("뮤트", "클리어", d.axes.clarity)
        d.features?.let { f ->
            Spacer(Modifier.height(4.dp))
            Text("측정된 색", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Measured("피부", f.skin)
                f.iris?.let { Measured("눈동자", it) }
                f.lips?.let { Measured("입술", it) }
                f.hair?.let { Measured("머리카락", it) }
            }
            Text("조명 색을 보정한 값이에요.", style = Muted)
        }
    }
}

/** A -1..1 value shown as a dot on a track between two labels. */
@Composable
private fun Meter(left: String, right: String, value: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(left, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.width(44.dp))
        BoxWithConstraints(Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
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

@Composable
private fun RefineCard(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Tune, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text("정밀 진단으로 확인하기", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, maxLines = 1)
                Text("천을 대 보고 고르면 더 정확해져요 · 1분", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}
