package com.gun.personalcolor.feature.my

import androidx.compose.material.icons.outlined.Widgets
import com.gun.personalcolor.core.ads.NativeAdCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gun.personalcolor.core.ads.RewardRequest
import com.gun.personalcolor.core.ads.rememberRewardGate
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.ListRow
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionHeader
import com.gun.personalcolor.core.designsystem.isWideWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.ColorDots
import com.gun.personalcolor.core.ui.ReportRenderer
import com.gun.personalcolor.core.ui.Tag
import com.gun.personalcolor.core.ui.gradient
import com.gun.personalcolor.core.ui.seasonKo
import com.gun.personalcolor.core.ui.temperatureKo

@Composable
fun MyScreen(
    state: MyUiState,
    onTone: (String) -> Unit,
    onAnalyze: () -> Unit,
    onQuiz: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onTones: () -> Unit,
    onWallpaper: (String?) -> Unit = {},
    onCloset: () -> Unit = {},
    onAddWidget: (() -> Unit)? = null,
) {
    TrackScreen("my")
    val context = LocalContext.current
    val analytics = LocalAnalytics.current
    val gate = rememberRewardGate()
    val snackbar = remember { SnackbarHostState() }
    val saveImage = rememberImageSave(snackbar)
    val saveReport = { tone: Tone ->
        gate(RewardRequest(key = "report", title = "광고 보고 저장하기", message = "짧은 광고 하나만 보면 무료예요.") {
            analytics?.log("report_save", "tone" to tone.id)
            saveImage("personal-color-report") { ReportRenderer.render(context, tone) }
        })
    }
    PcScaffold("마이", actions = {
        IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "설정") }
    }) { pad ->
        if (state.loading) return@PcScaffold
        val wide = isWideWindow()
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad)
                    .readableWidth(if (wide) 840.dp else 680.dp)
                    .padding(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                val toneCard: @Composable (Modifier) -> Unit = { m ->
                    val tone = state.myTone
                    if (tone != null) MyToneCard(tone, { onTone(tone.id) }, onAnalyze, { saveReport(tone) }, { onWallpaper(tone.id) }, m)
                    else NoToneCard(onAnalyze, onQuiz, onTones, m)
                }
                val rest: @Composable () -> Unit = {
                    if (state.entries.isNotEmpty()) Stats(state)
                    Favorites(state.favorites, onTone, onTones)
                    if (state.entries.isNotEmpty()) Recent(state, onTone, onHistory)
                }
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        toneCard(Modifier.weight(1f))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) { rest() }
                    }
                } else {
                    toneCard(Modifier)
                    rest()
                }
                Card {
                    ListRow("내 옷장", subtitle = "컬러 체크로 저장한 옷", icon = Icons.Outlined.Checkroom, onClick = onCloset)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListRow("배경화면 만들기", subtitle = "내 톤 컬러로 만드는 배경화면", icon = Icons.Outlined.Wallpaper,
                        onClick = { onWallpaper(state.myTone?.id) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (onAddWidget != null) {
                        ListRow("위젯 추가", subtitle = "홈 화면에서 오늘의 컬러 보기", icon = Icons.Outlined.Widgets, onClick = onAddWidget)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    ListRow("설정", icon = Icons.Outlined.Settings, onClick = onSettings)
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(pad).padding(16.dp))
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Column { content() } }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MyToneCard(tone: Tone, onGuide: () -> Unit, onAnalyze: () -> Unit, onReport: () -> Unit, onWallpaper: () -> Unit, modifier: Modifier) = Card(modifier) {
    Box(Modifier.fillMaxWidth().height(96.dp).background(tone.gradient)) {
        Tag("내 퍼스널컬러", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.surface)
    }
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column {
            Text("${tone.seasonKo} · ${tone.temperatureKo} · ${tone.nameEn}", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tone.nameKo, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tone.keywords.forEach { Tag("#$it") }
        }
        ColorDots(tone.bestColors.take(8), size = 22.dp)
        ButtonPair(Modifier.padding(top = 6.dp),
            first = { PcButton("가이드 보기", onGuide, it) },
            second = { PcOutlinedButton("다시 진단", onAnalyze, it) })
        ButtonPair(
            first = { TonalButton("리포트 저장", Icons.Outlined.Download, onReport, it) },
            second = { TonalButton("배경화면", Icons.Outlined.Wallpaper, onWallpaper, it) })
    }
}

@Composable
private fun TonalButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) =
    FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), shape = MaterialTheme.shapes.medium) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }

@Composable
private fun NoToneCard(onAnalyze: () -> Unit, onQuiz: () -> Unit, onTones: () -> Unit, modifier: Modifier) = Card(modifier) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("아직 내 퍼스널컬러가 없어요", style = MaterialTheme.typography.titleLarge, maxLines = 2)
        Text("사진이나 질문으로 금방 찾아요", style = Muted)
        PcButton("사진으로 진단", onAnalyze, Modifier.fillMaxWidth().padding(top = 6.dp), icon = Icons.Outlined.CameraAlt)
        PcOutlinedButton("셀프 진단", onQuiz, Modifier.fillMaxWidth(), icon = Icons.Outlined.Checklist)
        TextButton(onClick = onTones, modifier = Modifier.fillMaxWidth()) { Text("도감에서 고르기", maxLines = 1) }
    }
}

@Composable
private fun Stats(state: MyUiState) {
    val top = state.entries.groupingBy { it.toneId }.eachCount().maxBy { it.value }.key
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("${state.entries.size}회", "진단 횟수", Modifier.weight(1f))
        StatTile(state.tones[top]?.nameKo.orEmpty(), "자주 나온 톤", Modifier.weight(1f))
        StatTile(state.entries.first().dateTime().format(DATE).drop(2), "최근 진단", Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) =
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
            // Shrinks long tone names / dates instead of wrapping on 320dp phones.
            BasicText(value, maxLines = 1, autoSize = TextAutoSize.StepBased(12.sp, 22.sp),
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

@Composable
private fun Favorites(favorites: List<Tone>, onTone: (String) -> Unit, onTones: () -> Unit) = Column {
    SectionHeader("즐겨찾는 톤")
    if (favorites.isEmpty()) {
        Text("도감에서 ☆를 눌러 모아요", style = Muted)
        TextButton(onClick = onTones) { Text("도감 보기", maxLines = 1) }
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(favorites, key = { it.id }) { tone ->
                Surface(onClick = { onTone(tone.id) }, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.width(88.dp)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium).background(tone.gradient))
                        Text(tone.nameKo, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Recent(state: MyUiState, onTone: (String) -> Unit, onHistory: () -> Unit) = Column {
    SectionHeader("최근 진단", action = { TextButton(onClick = onHistory) { Text("전체 보기", maxLines = 1) } })
    Card {
        state.entries.take(3).forEachIndexed { i, e ->
            val tone = state.tones.getValue(e.toneId)
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ToneEntryRow(tone, "${e.source.label} · ${e.dateTime().format(DATE)}", { onTone(tone.id) })
        }
    }
    NativeAdCard(Modifier.padding(top = 16.dp))
}

@Preview(widthDp = 320)
@Composable
private fun MyPreview() = PcTheme { MyScreen(previewState(true), {}, {}, {}, {}, {}, {}) }

@Preview
@Composable
private fun MyEmptyPreview() = PcTheme { MyScreen(previewState(false), {}, {}, {}, {}, {}, {}) }
