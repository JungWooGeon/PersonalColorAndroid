package com.gun.personalcolor.feature.home

import com.gun.personalcolor.core.ui.prop
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.ads.NativeAdCard
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.Eyebrow
import com.gun.personalcolor.core.designsystem.IconTile
import com.gun.personalcolor.core.designsystem.ListRow
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcCard
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.PropTile
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionHeader
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.TintyBubble
import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.TintyProp
import com.gun.personalcolor.core.designsystem.aurora
import com.gun.personalcolor.core.designsystem.isWideWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.designsystem.tintyEntrance
import com.gun.personalcolor.core.model.Article
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.Swatch
import com.gun.personalcolor.core.ui.hexColor
import com.gun.personalcolor.core.ui.softBrush
import com.gun.personalcolor.core.ui.tintyColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val wide = isWideWindow()
    val my = state.myTone
    val colors = remember(my) { my?.tintyColors ?: TintyColors.Brand }
    val scroll = rememberScrollState()
    val bg = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxSize().background(bg)) {
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 24.dp)) {
        // 틴티 hosts: the hero is the screen's one living 틴티.
        HostHero(state, colors, a)
        Column(Modifier.readableWidth(HomeMax).padding(horizontal = ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(8.dp))
            Tools(a, colors, wide)
            if (my != null) PcCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    ListRow("다시 진단해 볼까요?", subtitle = "조명이나 메이크업이 달라졌다면", art = TintyProp.Magnifier, artColors = colors, onClick = a.onAnalyze)
                    ListRow("질문으로 셀프 진단", subtitle = "사진 없이 질문에 답해요", art = TintyProp.Cards, artColors = colors, onClick = a.onQuiz)
                }
            }
            SectionHeader("12톤 컬러 도감", Modifier.padding(top = 12.dp), eyebrow = "톤 둘러보기") {
                TextButton(onClick = a.onTones) { Text("전체 보기") }
            }
        }
        LazyRow(
            Modifier.readableWidth(HomeMax),
            contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) { items(state.tones, key = { it.id }) { t -> ToneTile(t, t.id == my?.id) { a.onTone(t.id) } } }

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
    // Status bar scrim once the hero has scrolled away (the hero itself draws under the status bar).
    Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).drawBehind {
        drawRect(bg, alpha = ((scroll.value - 120.dp.toPx()) / 60.dp.toPx()).coerceIn(0f, 1f))
    })
    }
}

private val DateLabel = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
private val HeroShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)

/**
 * Full-bleed top of home on my tone's wash: 틴티 holding up today's color card, with a line for the moment.
 * First run: brand 틴티 with a magnifier and the one job — find my tone.
 */
@Composable
private fun HostHero(state: HomeState, colors: TintyColors, a: HomeActions) {
    val tone = state.myTone
    val today = state.todayColor
    val date = remember { LocalDate.now().format(DateLabel) }
    val wash = if (tone != null) Modifier.background(tone.softBrush()) else Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest).aurora(PcTheme.colors)
    Box(Modifier.fillMaxWidth().clip(HeroShape).then(wash)) {
        Column(Modifier.readableWidth(HomeMax).statusBarsPadding().padding(start = ScreenPadding, end = 8.dp, bottom = 22.dp)) {
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(date, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                IconButton(onClick = a.onSettings) { Icon(Icons.Outlined.Settings, "설정") }
            }
            if (!state.loaded) { Spacer(Modifier.height(340.dp)); return@Column }
            val expression = when {
                tone == null -> TintyExpression.Default
                state.hour in 5..10 -> TintyExpression.Happy
                state.hour >= 22 || state.hour < 5 -> TintyExpression.Sleep
                else -> TintyExpression.Default
            }
            Stage(
                lines = state.lines, colors = colors, expression = expression,
                prop = if (tone != null) TintyProp.ColorCard else TintyProp.Magnifier,
                propColor = today?.let { hexColor(it.hex) } ?: colors.scarf[0],
            ) {
                if (tone != null && today != null) {
                    Eyebrow("오늘의 컬러")
                    Text(today.name, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                } else {
                    Eyebrow("12톤 퍼스널컬러")
                    Text("사진 한 장이면\n내 컬러를 찾아요", style = MaterialTheme.typography.headlineMedium)
                }
            }
            Column(Modifier.padding(end = 12.dp)) {
                if (tone != null && today != null) {
                    Text("${tone.nameKo}에게 잘 어울리는 색이에요", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    val chips = remember(tone, today) { (listOf(today) + tone.bestColors.filter { it != today }).take(6) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chips.forEachIndexed { i, c ->
                            val m = Modifier.weight(1f).aspectRatio(1.3f)
                            Swatch(c.hex, if (i == 0) m.border(2.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small) else m)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PcButton("오늘의 코디 보기", a.onCoordi, Modifier.weight(1.3f))
                        PcOutlinedButton("${tone.nameKo} 가이드", { a.onTone(tone.id) }, Modifier.weight(1f))
                    }
                } else {
                    Text("사진은 휴대폰 밖으로 나가지 않아요", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(18.dp))
                    PcButton("사진으로 진단하기", a.onAnalyze, Modifier.fillMaxWidth(), icon = Icons.Outlined.CameraAlt)
                    Spacer(Modifier.height(10.dp))
                    PcOutlinedButton("질문으로 셀프 진단", a.onQuiz, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/**
 * Speech bubble + title on the left, 틴티 on the right. Tapping 틴티 makes it hop and cheer (with a tick of haptics)
 * and moves to its next line. 틴티 pops in once per visit; the bubble follows.
 */
@Composable
private fun Stage(
    lines: List<String>, colors: TintyColors, expression: TintyExpression, prop: TintyProp, propColor: androidx.compose.ui.graphics.Color,
    title: @Composable () -> Unit,
) {
    var line by rememberSaveable { mutableIntStateOf(0) }
    var cheering by remember { mutableStateOf(false) }
    val hop = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val poke = {
        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        if (lines.isNotEmpty()) line = (line + 1) % lines.size
        scope.launch {
            cheering = true
            hop.animateTo(-26f, tween(140))
            hop.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = 700f))
            delay(500); cheering = false
        }
        Unit
    }
    Row(Modifier.fillMaxWidth().height(204.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 4.dp)) {
            AnimatedContent(
                lines.getOrNull(line % lines.size.coerceAtLeast(1)).orEmpty(), label = "line",
                transitionSpec = { (fadeIn(tween(200)) + slideInVertically(tween(240)) { it / 4 }) togetherWith fadeOut(tween(120)) },
            ) { text ->
                if (text.isNotEmpty()) TintyBubble(text, Modifier.tintyEntrance(delayMs = 260))
            }
            Spacer(Modifier.weight(1f))
            title()
        }
        Box(
            Modifier.width(176.dp).fillMaxHeight().offset(x = 4.dp)
                .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClickLabel = "틴티 쓰다듬기", onClick = poke),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Tinty(
                Modifier.size(176.dp).tintyEntrance().graphicsLayer { translationY = hop.value * density },
                colors, if (cheering) TintyExpression.Cheer else expression, prop = prop, propColor = propColor,
            )
        }
    }
}

private class ToolItem(val prop: TintyProp, val label: String, val sub: String, val onClick: () -> Unit)

/** 2×2 on phones, one row on wide windows. Each tool is the prop 틴티 uses for it. */
@Composable
private fun Tools(a: HomeActions, colors: TintyColors, wide: Boolean) {
    val tools = listOf(
        ToolItem(TintyProp.Eyedropper, "컬러 체크", "이 옷, 나한테 맞을까?", a.onColorMatch),
        ToolItem(TintyProp.Hanger, "오늘의 코디", "날씨 맞춤 추천", a.onCoordi),
        ToolItem(TintyProp.Palette, "컬러 도감", "12톤 둘러보기", a.onTones),
        ToolItem(TintyProp.Book, "가이드", "알아 두면 좋아요", a.onLearn),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tools.chunked(if (wide) 4 else 2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t ->
                    PcCard(Modifier.weight(1f), onClick = t.onClick) {
                        Column(Modifier.padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)) {
                            PropTile(t.prop, colors, size = 52.dp)
                            Spacer(Modifier.height(12.dp))
                            Text(t.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/** A 12-tone collection, so each tile may show its (still) 틴티. Mine is marked. */
@Composable
private fun ToneTile(t: Tone, mine: Boolean, onClick: () -> Unit) {
    PcCard(Modifier.width(120.dp), onClick = onClick) {
        Column(Modifier.padding(8.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium).background(t.softBrush()), Alignment.Center) {
                Tinty(Modifier.size(80.dp), remember(t) { t.tintyColors }, animated = false)
                if (mine) Text(
                    "MY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 1.dp),
                )
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
            PropTile(article.prop, size = 48.dp)
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
private fun HomePreview() = PcTheme { HomeScreen(HomeState(loaded = true), HomeActions({}, {}, {}, {}, {}, {}, {}, {})) }
