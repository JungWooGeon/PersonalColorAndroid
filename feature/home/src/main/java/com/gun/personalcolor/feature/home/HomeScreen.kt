package com.gun.personalcolor.feature.home

import com.gun.personalcolor.core.ads.NativeAdCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.ui.tintyColors
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionHeader
import com.gun.personalcolor.core.designsystem.isWideWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.Article
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.ColorDots
import com.gun.personalcolor.core.ui.Swatch
import com.gun.personalcolor.core.ui.gradient
import com.gun.personalcolor.core.ui.hexColor
import com.gun.personalcolor.core.ui.seasonKo
import com.gun.personalcolor.core.ui.temperatureKo

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

@Composable
internal fun HomeScreen(state: HomeState, a: HomeActions) {
    PcScaffold(
        title = "나의 컬러는?",
        actions = { IconButton(onClick = a.onSettings) { Icon(Icons.Outlined.Settings, "설정") } },
    ) { pad ->
        val wide = isWideWindow()
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad).padding(bottom = 24.dp)) {
            Column(Modifier.readableWidth(880.dp).padding(horizontal = ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Spacer(Modifier.height(0.dp))
                val my = state.myTone
                if (my != null) {
                    if (wide) Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MyToneCard(my, { a.onTone(my.id) }, Modifier.weight(1f).fillMaxHeight())
                        state.todayColor?.let { TodayColor(it, a.onCoordi, Modifier.weight(1f).fillMaxHeight()) }
                    } else {
                        MyToneCard(my, { a.onTone(my.id) }, Modifier)
                        state.todayColor?.let { TodayColor(it, a.onCoordi, Modifier) }
                    }
                }
                if (wide) Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PhotoHero(a.onAnalyze, compact = my != null, Modifier.weight(1f).fillMaxHeight())
                    QuizCard(a.onQuiz, Modifier.weight(1f).fillMaxHeight())
                } else {
                    PhotoHero(a.onAnalyze, compact = my != null, Modifier)
                    QuizCard(a.onQuiz, Modifier)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tool(Icons.Outlined.Colorize, "컬러 체크", a.onColorMatch, Modifier.weight(1f))
                    Tool(Icons.Outlined.Checkroom, "오늘 코디", a.onCoordi, Modifier.weight(1f))
                    Tool(Icons.Outlined.Palette, "컬러 도감", a.onTones, Modifier.weight(1f))
                    Tool(Icons.Outlined.AutoStories, "가이드", a.onLearn, Modifier.weight(1f))
                }
                SectionHeader("12톤 컬러 도감", Modifier.padding(top = 8.dp)) { TextButton(onClick = a.onTones) { Text("전체 보기") } }
            }
            // Full-bleed carousel, aligned with the content column's start padding.
            LazyRow(
                Modifier.readableWidth(880.dp),
                contentPadding = PaddingValues(horizontal = ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) { items(state.tones, key = { it.id }) { t -> ToneTile(t) { a.onTone(t.id) } } }

            Column(Modifier.readableWidth(880.dp).padding(horizontal = ScreenPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("퍼스널컬러 가이드", Modifier.padding(top = 16.dp)) { TextButton(onClick = a.onLearn) { Text("전체 보기") } }
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

@Composable
private fun MyToneCard(tone: Tone, onClick: () -> Unit, modifier: Modifier) {
    Box(modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(tone.gradient).clickable(onClick = onClick)) {
        Column(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.25f)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("내 퍼스널컬러", style = MaterialTheme.typography.labelLarge, color = Color(0xFF1D1B1A).copy(alpha = 0.75f))
                    Text(tone.nameKo, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF1D1B1A), maxLines = 1)
                    Text("${tone.seasonKo} · ${tone.temperatureKo} · ${tone.keywords.take(3).joinToString(" ") { "#$it" }}",
                        style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1D1B1A).copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val colors = remember(tone) { tone.tintyColors }
                Tinty(Modifier.size(84.dp), colors, TintyExpression.Happy)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDots(tone.bestColors.take(6), 22.dp)
                Spacer(Modifier.weight(1f))
                Text("가이드 보기", style = MaterialTheme.typography.labelLarge, color = Color(0xFF1D1B1A))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.padding(start = 4.dp).size(18.dp), tint = Color(0xFF1D1B1A))
            }
        }
    }
}

@Composable
private fun TodayColor(color: NamedColor, onCoordi: () -> Unit, modifier: Modifier) {
    Surface(onClick = onCoordi, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Swatch(color.hex, Modifier.size(56.dp))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text("오늘의 컬러", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(color.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("날씨 맞춤 코디 보기", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val HeroGradient = Brush.linearGradient(listOf(Color(0xFFFFCBB0), Color(0xFFF6C6D6), Color(0xFFC9D3F5), Color(0xFFB9A8E6)))

@Composable
private fun PhotoHero(onClick: () -> Unit, compact: Boolean, modifier: Modifier) {
    val ink = Color(0xFF1D1B1A)
    Box(modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(HeroGradient).clickable(onClick = onClick)) {
        // 틴티 greets first-time users; once a tone is set it lives on the tone card instead.
        if (!compact) Tinty(Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp).size(116.dp))
        Column(Modifier.padding(22.dp)) {
            Text(if (compact) "다시 진단해 볼까요?" else "AI 퍼스널컬러 진단", style = MaterialTheme.typography.headlineSmall, color = ink)
            Spacer(Modifier.height(6.dp))
            Text("사진 한 장으로 12톤 중 내 톤 찾기",
                style = MaterialTheme.typography.bodyMedium, color = ink.copy(alpha = 0.8f))
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Outlined.CameraAlt, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("사진으로 진단하기", maxLines = 1)
            }
        }
    }
}

@Composable
private fun QuizCard(onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(20.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)) {
                Icon(Icons.Outlined.Checklist, null, Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text("질문으로 셀프 진단", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1)
                Text("사진 없이 12문항 · 약 2분", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f), maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun Tool(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 84.dp), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ToneTile(t: Tone, onClick: () -> Unit) {
    Column(Modifier.width(128.dp).clip(MaterialTheme.shapes.large).clickable(onClick = onClick).padding(bottom = 4.dp)) {
        Box(Modifier.fillMaxWidth().height(100.dp).clip(MaterialTheme.shapes.large).background(t.gradient))
        Text(t.nameKo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp))
        Text(t.keywords.take(2).joinToString(" · "), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun ArticleRow(article: Article, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(article.emoji, style = MaterialTheme.typography.titleLarge) }
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(article.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${article.category} · ${article.readMinutes}분", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 1400)
@Composable
private fun HomePreview() = PcTheme { HomeScreen(HomeState(), HomeActions({}, {}, {}, {}, {}, {}, {}, {})) }
