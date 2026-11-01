package com.gun.personalcolor.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.designsystem.BottomActions
import com.gun.personalcolor.core.designsystem.IconTile
import com.gun.personalcolor.core.designsystem.PcCard
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.aurora
import com.gun.personalcolor.core.designsystem.elevation
import com.gun.personalcolor.core.designsystem.elevationBorder
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.accent
import com.gun.personalcolor.core.ui.hexColor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

private val ContentMax = 560.dp

@HiltViewModel
internal class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    content: ContentRepository,
) : ViewModel() {
    val tones: List<Tone> = content.tones
    private var finishing = false

    fun finish(onDone: () -> Unit) {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            prefs.setOnboardingDone(true)
            onDone()
        }
    }
}

@Composable
internal fun OnboardingRoute(onDone: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    TrackScreen("onboarding")
    BackHandler { vm.finish(onDone) }
    OnboardingScreen(vm.tones, onFinish = { vm.finish(onDone) })
}

private class Page(val title: String, val body: String, val points: List<Pair<ImageVector, String>>, val art: @Composable (List<Tone>) -> Unit)

private val pages = listOf(
    Page(
        "사진 한 장으로 찾는\n나의 퍼스널컬러",
        "피부·눈동자·입술 색을 보고 12톤 중 내 톤을 찾아요.",
        listOf(Icons.Outlined.Colorize to "피부·눈동자·입술·머리카락 색을 봐요", Icons.Outlined.AutoAwesome to "봄·여름·가을·겨울 12톤으로 알려줘요"),
    ) { ToneRingArt(it) },
    Page(
        "사진은 휴대폰 밖으로\n나가지 않아요",
        "얼굴 분석은 휴대폰 안에서만 해요.",
        listOf(Icons.Outlined.CloudOff to "서버로 보내지 않아요", Icons.Outlined.History to "진단 기록도 이 휴대폰에만 남아요"),
    ) { PrivatePhoneArt(it) },
    Page(
        "톤을 알았다면\n이렇게 써 보세요",
        "매일 쓰기 좋은 컬러 도구예요.",
        listOf(
            Icons.Outlined.Palette to "12톤 컬러 도감",
            Icons.Outlined.Checkroom to "컬러 드레이핑으로 얼굴에 색 대 보기",
            Icons.Outlined.Colorize to "컬러 체크로 쇼핑 전에 확인하기",
        ),
    ) { FeatureCardsArt(it) },
    Page(
        "이렇게 찍으면\n더 잘 보여요",
        "조명에 따라 결과가 달라져요.",
        listOf(
            Icons.Outlined.WbSunny to "밝은 자연광 아래에서",
            Icons.Outlined.FilterNone to "필터·보정 없이",
            Icons.Outlined.Visibility to "정면을 바라보고",
        ),
    ) { PhotoTipArt(it) },
)

@Composable
internal fun OnboardingScreen(tones: List<Tone>, onFinish: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.weight(1f).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            Row(Modifier.readableWidth(ContentMax).heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                if (!last) TextButton(onFinish) { Text("건너뛰기", maxLines = 1) }
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { index ->
                PageContent(pages[index], tones)
            }
        }
        BottomActions {
            Row(Modifier.readableWidth(ContentMax), verticalAlignment = Alignment.CenterVertically) {
                PageIndicator(pages.size, pager.currentPage, Modifier.weight(1f))
                PcButton(
                    if (last) "시작하기" else "다음",
                    onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    modifier = Modifier.widthIn(min = 136.dp),
                )
            }
        }
    }
}

@Composable
private fun PageContent(page: Page, tones: List<Tone>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pageHeight = maxHeight // min height of the scrolling columns, so short content stays centered
        if (maxWidth > maxHeight) {
            // Landscape: art beside the text.
            Row(
                Modifier.fillMaxSize().padding(horizontal = ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).widthIn(max = 320.dp).fillMaxHeight().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    ArtFrame(page, tones)
                }
                Column(Modifier.weight(1f).widthIn(max = 420.dp).verticalScroll(rememberScrollState()).heightIn(min = pageHeight), verticalArrangement = Arrangement.Center) {
                    PageText(page, centered = false)
                }
            }
        } else {
            val artHeight = min(pageHeight.value * 0.45f, 300f).dp
            Column(
                Modifier.readableWidth(ContentMax).verticalScroll(rememberScrollState()).heightIn(min = pageHeight).padding(horizontal = ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Art gets up to ~45% of the height so the text always fits on small phones.
                Box(Modifier.fillMaxWidth().height(artHeight).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    ArtFrame(page, tones)
                }
                Spacer(Modifier.height(24.dp))
                PageText(page, centered = true)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ArtFrame(page: Page, tones: List<Tone>) =
    Box(Modifier.sizeIn(maxWidth = 300.dp, maxHeight = 300.dp).aspectRatio(1f).aurora(PcTheme.colors), contentAlignment = Alignment.Center) { page.art(tones) }

@Composable
private fun PageText(page: Page, centered: Boolean) {
    Column(horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        Text(
            page.title, style = MaterialTheme.typography.headlineMedium,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
        Spacer(Modifier.height(12.dp))
        Text(page.body, style = Muted, textAlign = if (centered) TextAlign.Center else TextAlign.Start)
        Spacer(Modifier.height(20.dp))
        PcCard(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                page.points.forEach { (icon, text) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(icon, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(text, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 22.dp else 8.dp, label = "dot")
            val color by animateColorAsState(
                if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, label = "dotColor",
            )
            Box(Modifier.height(8.dp).width(width).clip(CircleShape).background(color))
        }
    }
}

// --- Illustrations (shapes + icons only, sized to their square parent) ---

private fun List<Tone>.colors(fallback: List<Color>) = map { it.accent }.ifEmpty { fallback }

@Composable
private fun fallbackColors() = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)

/** Places children evenly on a circle (fraction of the parent's half-size). */
@Composable
private fun Ring(radiusFraction: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier.fillMaxSize()) { measurables, constraints ->
        val size = min(constraints.maxWidth, constraints.maxHeight)
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        layout(size, size) {
            val r = size / 2f * radiusFraction
            placeables.forEachIndexed { i, p ->
                val angle = Math.toRadians(i * 360.0 / placeables.size - 90)
                p.place(
                    (size / 2f + r * cos(angle) - p.width / 2f).roundToInt(),
                    (size / 2f + r * sin(angle) - p.height / 2f).roundToInt(),
                )
            }
        }
    }
}

@Composable
private fun CenterBadge(icon: ImageVector, size: Dp, modifier: Modifier = Modifier) =
    PcCard(modifier.size(size), shape = CircleShape, elevation = 2) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(size * 0.5f), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

/** 12 tone colors around 틴티. */
@Composable
private fun ToneRingArt(tones: List<Tone>) {
    val colors = tones.colors(fallbackColors())
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val s = min(maxWidth.value, maxHeight.value).dp
        Box(Modifier.size(s * 0.78f).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)))
        Ring(0.78f) {
            colors.forEach { c ->
                Box(Modifier.size(s * 0.17f).clip(CircleShape).background(c)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape))
            }
        }
        Tinty(Modifier.size(s * 0.6f))
    }
}

/** 틴티 winking ("사진은 휴대폰 안에서만 봐요") next to a lock tile. */
@Composable
private fun PrivatePhoneArt(@Suppress("UNUSED_PARAMETER") tones: List<Tone>) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val s = min(maxWidth.value, maxHeight.value).dp
        Tinty(Modifier.size(s * 0.72f), expression = TintyExpression.Happy)
        IconTile(Icons.Filled.Lock, Modifier.offset(x = -s * 0.3f, y = s * 0.22f), size = s * 0.22f, drop = true,
            tint = MaterialTheme.colorScheme.onPrimary, container = MaterialTheme.colorScheme.primary)
    }
}

/** Three fanned palette cards, one per feature. */
@Composable
private fun FeatureCardsArt(tones: List<Tone>) {
    // One tone per season for variety; fall back to theme colors in previews.
    val picks = tones.groupBy { it.season }.values.mapNotNull { it.firstOrNull() }.take(3)
    val icons = listOf(Icons.Outlined.Palette, Icons.Outlined.Checkroom, Icons.Outlined.Colorize)
    val fallback = fallbackColors()
    val dark = PcTheme.colors.isDark
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val s = min(maxWidth.value, maxHeight.value).dp
        listOf(-14f, 0f, 14f).forEachIndexed { i, angle ->
            val swatches = picks.getOrNull(i)?.bestColors?.take(4)?.map { hexColor(it.hex) } ?: listOf(fallback[i % fallback.size])
            Surface(
                Modifier.width(s * 0.42f).height(s * 0.6f)
                    .graphicsLayer { rotationZ = angle; translationX = (i - 1) * s.toPx() * 0.22f; translationY = if (i == 1) -s.toPx() * 0.04f else 0f }
                    .elevation(RoundedCornerShape(s * 0.06f), dark, 2),
                shape = RoundedCornerShape(s * 0.06f), color = MaterialTheme.colorScheme.surfaceContainer,
                border = elevationBorder(2),
            ) {
                Column(Modifier.padding(s * 0.035f), verticalArrangement = Arrangement.spacedBy(s * 0.02f)) {
                    Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(s * 0.035f))) {
                        swatches.forEach { Box(Modifier.weight(1f).fillMaxWidth().background(it)) }
                    }
                    Icon(icons[i], null, Modifier.size(s * 0.08f).align(Alignment.CenterHorizontally), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Face in a camera frame with a sun. */
@Composable
private fun PhotoTipArt(tones: List<Tone>) {
    val warm = tones.firstOrNull { it.season == "spring" }?.accent ?: Color(0xFFFFC857)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val s = min(maxWidth.value, maxHeight.value).dp
        Box(Modifier.size(s * 0.86f).clip(CircleShape)
            .background(Brush.radialGradient(listOf(warm.copy(alpha = 0.45f), Color.Transparent))))
        // Viewfinder corners
        Box(Modifier.size(s * 0.6f)) {
            val corner = s * 0.14f
            val stroke = 4.dp
            val c = MaterialTheme.colorScheme.onSurfaceVariant
            listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).forEach { a ->
                Box(Modifier.align(a).size(corner)) {
                    val top = a == Alignment.TopStart || a == Alignment.TopEnd
                    val start = a == Alignment.TopStart || a == Alignment.BottomStart
                    Box(Modifier.align(if (top) Alignment.TopCenter else Alignment.BottomCenter).fillMaxWidth().height(stroke).clip(CircleShape).background(c))
                    Box(Modifier.align(if (start) Alignment.CenterStart else Alignment.CenterEnd).fillMaxHeight().width(stroke).clip(CircleShape).background(c))
                }
            }
        }
        CenterBadge(Icons.Outlined.Face, s * 0.34f)
        Surface(Modifier.size(s * 0.2f).offset(x = s * 0.3f, y = -s * 0.3f), shape = CircleShape, color = warm) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.WbSunny, null, Modifier.size(s * 0.11f), tint = com.gun.personalcolor.core.ui.contentColorOn(warm))
            }
        }
    }
}

@Preview(widthDp = 320, heightDp = 640)
@Composable
private fun OnboardingPreview() = PcTheme { OnboardingScreen(emptyList()) {} }

@Preview(widthDp = 780, heightDp = 360)
@Composable
private fun OnboardingLandscapePreview() = PcTheme { OnboardingScreen(emptyList()) {} }
