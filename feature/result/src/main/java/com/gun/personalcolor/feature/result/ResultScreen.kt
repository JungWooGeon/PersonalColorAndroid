package com.gun.personalcolor.feature.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
                    Hero(tone, diagnosis)
                    ShareMenu(tone) { open -> PcButton("결과 공유하기", open, Modifier.fillMaxWidth(), Icons.Outlined.Share) }
                    SetMyTone(isMyTone = myToneId == tone.id) { vm.setMyTone(tone.id) }
                    ListCard("결과로 해 보기") {
                        if (diagnosis.photo != null) ActionRow("컬러 드레이핑", "BEST와 WORST를 얼굴에 대 봐요", Icons.Outlined.Checkroom, onDrape)
                        ActionRow("리포트 이미지 저장", "한 장으로 정리한 내 컬러", Icons.Outlined.Image, saveReport)
                        ActionRow("배경화면 만들기", "내 톤 컬러로 꾸며요", Icons.Outlined.Wallpaper) { onWallpaper(tone.id) }
                    }
                }
            }
            val analysis: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

            SectionHeader("${tone.nameKo} 스타일 가이드", Modifier.padding(top = 8.dp))
            ToneDetails(tone)

            ListCard("다음 단계") {
                ActionRow("쇼핑할 때 컬러 체크", "옷 색이 나한테 맞는지 바로 봐요", Icons.Outlined.Colorize, onColorMatch)
                ActionRow(if (diagnosis.source == ResultSource.AI) "다른 사진으로 다시 진단" else "사진으로 진단하기",
                    if (diagnosis.source == ResultSource.AI) "조명이 다른 사진이면 좋아요" else "사진 한 장이면 돼요",
                    Icons.Outlined.CameraAlt, onRetryPhoto)
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

/** Celebration: user-tone 틴티 cheering on a pastel wash of the tone, then name, keywords and best colors. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hero(tone: Tone, d: Diagnosis) {
    // 틴티 pops in once (Compose honors the system animator scale, so this is instant with animations off).
    val pop = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(PcMotion.SLOW, easing = PcMotion.Pop)) }
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(tone.softBrush())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Tinty(
            Modifier.size(168.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value; transformOrigin = TransformOrigin(0.5f, 0.88f) },
            remember(tone) { tone.tintyColors }, TintyExpression.Cheer,
        )
        Eyebrow(
            when {
                d.source == ResultSource.QUIZ -> "셀프 진단 결과"
                d.photoCount > 1 -> "사진 ${d.photoCount}장 진단 결과"
                else -> "진단 결과"
            },
        )
        Spacer(Modifier.height(4.dp))
        Text(tone.nameKo, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        Text("${tone.nameEn} · ${tone.seasonKo} ${tone.temperatureKo}", style = Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tone.keywords.forEach { Tag("#$it", color = MaterialTheme.colorScheme.surfaceContainer) }
        }
        Spacer(Modifier.height(16.dp))
        PcCard(Modifier.fillMaxWidth(), elevation = 0) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tone.bestColors.take(8).forEach { Swatch(it.hex, Modifier.weight(1f).aspectRatio(1f), RoundedCornerShape(10.dp)) }
                }
                Text(tone.summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

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
private fun ActionRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) =
    ListRow(title, Modifier.fillMaxWidth(), subtitle = subtitle, icon = icon, onClick = onClick)

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

