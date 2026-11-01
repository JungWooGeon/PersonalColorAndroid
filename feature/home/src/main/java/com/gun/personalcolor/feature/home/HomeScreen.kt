package com.gun.personalcolor.feature.home

import com.gun.personalcolor.core.ui.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.ads.NativeAdCard
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.Eyebrow
import com.gun.personalcolor.core.designsystem.IconTile
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcCard
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.PcTonalButton
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionCard
import com.gun.personalcolor.core.designsystem.SectionHeader
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.aurora
import com.gun.personalcolor.core.designsystem.isWideWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.Article
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.Swatch
import com.gun.personalcolor.core.ui.ToneAvatar
import com.gun.personalcolor.core.ui.seasonKo
import com.gun.personalcolor.core.ui.softBrush
import com.gun.personalcolor.core.ui.temperatureKo
import com.gun.personalcolor.core.ui.tintyColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeActions(
    val onAnalyze: () -> Unit, val onQuiz: () -> Unit, val onTone: (String) -> Unit, val onTones: () -> Unit,
    val onColorMatch: () -> Unit, val onLearn: () -> Unit, val onArticle: (String) -> Unit, val onSettings: () -> Unit,
    val onCoordi: () -> Unit = {},
)

@Composable
internal fun HomeRoute(actions: HomeActions, vm: HomeViewModel = hiltViewModel()) {
    TrackScreen("home")
    val state by vm.state.collectAsStateWithLifecycle()
    HomeScreen(state, actions)
}

private val HomeMax = 880.dp

@Composable
internal fun HomeScreen(state: HomeState, a: HomeActions) {
    PcScaffold(
        title = "나의 컬러는?",
        actions = { IconButton(onClick = a.onSettings) { Icon(Icons.Outlined.Settings, "설정") } },
    ) { pad ->
        val wide = isWideWindow()
        val my = state.myTone
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad).padding(top = 4.dp, bottom = 24.dp)) {
            Column(Modifier.readableWidth(HomeMax).padding(horizontal = ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (my == null) {
                    // First run: one clear job — find my tone.
                    BrandHero(a.onAnalyze, a.onQuiz)
                } else if (wide) {
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TodayHero(my, state.todayColor, a.onCoordi, Modifier.weight(1f).fillMaxHeight())
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MyToneCard(my) { a.onTone(my.id) }
                            Rediagnose(a.onAnalyze, a.onQuiz)
                        }
                    }
                } else {
                    TodayHero(my, state.todayColor, a.onCoordi, Modifier)
                    MyToneCard(my) { a.onTone(my.id) }
                }
                Tools(a, wide)
                if (my != null && !wide) Rediagnose(a.onAnalyze, a.onQuiz)
                SectionHeader("12톤 컬러 도감", Modifier.padding(top = 8.dp), eyebrow = "톤 둘러보기") {
                    TextButton(onClick = a.onTones) { Text("전체 보기") }
                }
            }
            // Full-bleed carousel, aligned with the content column's start padding.
            LazyRow(
                Modifier.readableWidth(HomeMax),
                contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) { items(state.tones, key = { it.id }) { t -> ToneTile(t) { a.onTone(t.id) } } }

            Column(Modifier.readableWidth(HomeMax).padding(horizontal = ScreenPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("퍼스널컬러 가이드", Modifier.padding(top = 8.dp), eyebrow = "알아 두면 좋아요") {
                    TextButton(onClick = a.onLearn) { Text("전체 보기") }
                }
                state.articles.chunked(if (wide) 2 else 1).forEach { row ->
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { ArticleRow(it, Modifier.weight(1f).fillMaxHeight()) { a.onArticle(it.id) } }
                    }
                }
                NativeAdCard(Modifier.padding(top = 14.dp))
            }
        }
    }
}

private val DateLabel = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)

/** Today's color on my tone's pastel, with my-tone 틴티 winking — the screen's one looping 틴티. Tap → 오늘의 코디. */
@Composable
private fun TodayHero(tone: Tone, today: NamedColor?, onCoordi: () -> Unit, modifier: Modifier) {
    val date = remember { LocalDate.now().format(DateLabel) }
    val chips = remember(tone, today) { (listOfNotNull(today) + tone.bestColors.filter { it != today }).take(6) }
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(tone.softBrush()).clickable(onClick = onCoordi)
            .padding(start = 22.dp, end = 12.dp, top = 20.dp, bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("오늘의 컬러 · $date")
                Spacer(Modifier.height(6.dp))
                Text(today?.name ?: tone.nameKo, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("${tone.nameKo}에게 잘 어울리는 색이에요", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Tinty(Modifier.size(116.dp), remember(tone) { tone.tintyColors }, TintyExpression.Happy)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.padding(end = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chips.forEachIndexed { i, c ->
                val m = Modifier.weight(1f).aspectRatio(1.4f)
                Swatch(c.hex, if (i == 0 && today != null) m.border(2.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small) else m)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("날씨에 맞춘 오늘의 코디 보기", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(start = 4.dp).size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** My tone in one row; tap → tone guide. */
@Composable
private fun MyToneCard(tone: Tone, onClick: () -> Unit) {
    PcCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ToneAvatar(tone, 48.dp)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Eyebrow("내 퍼스널컬러")
                Text(tone.nameKo, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${tone.seasonKo} · ${tone.temperatureKo} · ${tone.keywords.take(2).joinToString(" ") { "#$it" }}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = PcTheme.colors.ink3)
        }
    }
}

/** First run: brand 틴티 on aurora, one primary action and the photo-free alternative. */
@Composable
private fun BrandHero(onAnalyze: () -> Unit, onQuiz: () -> Unit) {
    PcCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, elevation = 2) {
        Column(Modifier.aurora(PcTheme.colors).padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("12톤 퍼스널컬러")
                    Spacer(Modifier.height(6.dp))
                    Text("사진 한 장이면\n내 컬러를 찾아요", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("사진은 휴대폰 밖으로 나가지 않아요", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Tinty(Modifier.size(112.dp))
            }
            Spacer(Modifier.height(20.dp))
            PcButton("사진으로 진단하기", onAnalyze, Modifier.fillMaxWidth(), icon = Icons.Outlined.CameraAlt)
            Spacer(Modifier.height(10.dp))
            PcOutlinedButton("질문으로 셀프 진단", onQuiz, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Rediagnose(onAnalyze: () -> Unit, onQuiz: () -> Unit) {
    SectionCard("다시 진단해 볼까요?") {
        Text("조명이나 메이크업이 달라졌다면 다시 해 봐도 좋아요", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PcTonalButton("사진 진단", onAnalyze, Modifier.weight(1f))
            PcOutlinedButton("셀프 진단", onQuiz, Modifier.weight(1f))
        }
    }
}

private class ToolItem(val icon: ImageVector, val label: String, val onClick: () -> Unit)

/** 2×2 on phones (labels stay whole on narrow screens), one row on wide windows. */
@Composable
private fun Tools(a: HomeActions, wide: Boolean) {
    val tools = listOf(
        ToolItem(Icons.Outlined.Colorize, "컬러 체크", a.onColorMatch),
        ToolItem(Icons.Outlined.Checkroom, "오늘의 코디", a.onCoordi),
        ToolItem(Icons.Outlined.Palette, "컬러 도감", a.onTones),
        ToolItem(Icons.Outlined.AutoStories, "가이드", a.onLearn),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tools.chunked(if (wide) 4 else 2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t ->
                    PcCard(Modifier.weight(1f), onClick = t.onClick, shape = MaterialTheme.shapes.large) {
                        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconTile(t.icon, size = 40.dp)
                            Text(t.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                }
            }
        }
    }
}

/** A 12-tone collection, so each tile may show its (still) 틴티. */
@Composable
private fun ToneTile(t: Tone, onClick: () -> Unit) {
    PcCard(Modifier.width(120.dp), onClick = onClick) {
        Column(Modifier.padding(8.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium).background(t.softBrush()), Alignment.Center) {
                Tinty(Modifier.size(80.dp), remember(t) { t.tintyColors }, animated = false)
            }
            Text(t.nameKo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp))
            Text(t.keywords.take(2).joinToString(" · "), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp))
        }
    }
}


@Composable
private fun ArticleRow(article: Article, modifier: Modifier, onClick: () -> Unit) {
    PcCard(modifier, onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(article.icon, size = 44.dp)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(article.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${article.category} · ${article.readMinutes}분", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = PcTheme.colors.ink3)
        }
    }
}

@Preview(widthDp = 360, heightDp = 1400)
@Composable
private fun HomePreview() = PcTheme { HomeScreen(HomeState(), HomeActions({}, {}, {}, {}, {}, {}, {}, {})) }
