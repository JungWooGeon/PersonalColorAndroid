package com.gun.personalcolor.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

// ---------------------------------------------------------------------------------------------------------------------
// Depth (BRAND_SYSTEM §4): low violet-tinted shadows in light, a hairline instead of a shadow in dark.

private val E1Near = Shadow(radius = 2.dp, color = Color(0x0D2B2236), offset = DpOffset(0.dp, 1.dp))
private val E1Far = Shadow(radius = 24.dp, color = Color(0x145B4B8A), offset = DpOffset(0.dp, 8.dp))
private val E2Near = Shadow(radius = 4.dp, color = Color(0x0F2B2236), offset = DpOffset(0.dp, 2.dp))
private val E2Far = Shadow(radius = 44.dp, color = Color(0x295B4B8A), offset = DpOffset(0.dp, 18.dp))
private val E2Dark = Shadow(radius = 44.dp, color = Color(0x66000000), offset = DpOffset(0.dp, 18.dp))

/** Elevation e1 (cards) or e2 (floating: sheets, hero cards, previews). Pair with [elevationBorder] in dark. */
fun Modifier.elevation(shape: Shape, dark: Boolean, level: Int = 1): Modifier = when {
    level <= 0 -> this
    dark -> if (level >= 2) dropShadow(shape, E2Dark) else this
    level >= 2 -> dropShadow(shape, E2Far).dropShadow(shape, E2Near)
    else -> dropShadow(shape, E1Far).dropShadow(shape, E1Near)
}

/** Dark-mode hairline that replaces the shadow (null in light). */
@Composable
fun elevationBorder(level: Int = 1): BorderStroke? = when {
    level <= 0 || !PcTheme.colors.isDark -> null
    else -> BorderStroke(1.dp, if (level >= 2) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant)
}

/** Primary-button gloss: one highlight layer at the top. */
private val Gloss = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.18f), 0.55f to Color.White.copy(alpha = 0f))

/**
 * Soft brand blobs behind a section (lavender top-right, peach bottom-left), like the website's `.aurora`.
 * Each blob is an ellipse that fades out before the bounds, so it also works on unclipped boxes.
 */
fun Modifier.aurora(colors: PcColors): Modifier = drawBehind {
    blob(colors.auroraLavender, 0.64f, 0.40f)
    blob(colors.auroraPeach, 0.36f, 0.62f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.blob(color: Color, fx: Float, fy: Float) {
    val c = Offset(size.width * fx, size.height * fy)
    val rx = minOf(c.x, size.width - c.x); val ry = minOf(c.y, size.height - c.y)
    if (rx <= 0f || ry <= 0f) return
    scale(rx / ry, 1f, pivot = c) {
        drawCircle(Brush.radialGradient(0f to color, 1f to color.copy(alpha = 0f), center = c, radius = ry), ry, c)
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Surfaces

/** White content card (r-lg, e1). Clickable when [onClick] is set. */
@Composable
fun PcCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    elevation: Int = 1,
    content: @Composable () -> Unit,
) {
    val m = modifier.elevation(shape, PcTheme.colors.isDark, elevation)
    val border = elevationBorder(elevation)
    if (onClick != null) Surface(onClick = onClick, modifier = m, shape = shape, color = color, border = border, content = content)
    else Surface(modifier = m, shape = shape, color = color, border = border, content = content)
}

/** White card with a title. The main content block of most screens. */
@Composable
fun SectionCard(
    title: String?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    PcCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null || action != null) {
                Row(Modifier.heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title.orEmpty(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    action?.invoke()
                }
            }
            content()
        }
    }
}

/** Small accent label above a title ("TODAY'S COLOR", "오늘의 컬러"). */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) =
    Text(text, modifier, style = MaterialTheme.typography.labelLarge, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)

/** Section title above a group of content, with optional eyebrow and trailing action ("전체 보기"). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, eyebrow: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) Eyebrow(eyebrow)
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        action?.invoke()
    }
}

/** Feature icon in an accent-soft tile (BRAND_SYSTEM §5). [drop] = drop corner (1–2 per screen). */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    drop: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = if (tint == MaterialTheme.colorScheme.primary) MaterialTheme.colorScheme.primaryContainer else tint.copy(alpha = 0.14f),
) {
    val shape = when {
        drop && size >= 56.dp -> DropShape
        drop -> DropShapeSmall
        size >= 56.dp -> MaterialTheme.shapes.large
        else -> MaterialTheme.shapes.small
    }
    Box(modifier.size(size).background(container, shape), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.5f), tint = tint)
    }
}

/** A 틴티 prop on a soft tile: the feature "icon" of this app (instead of a Material symbol). */
@Composable
fun PropTile(prop: TintyProp, colors: TintyColors = TintyColors.Brand, modifier: Modifier = Modifier, size: Dp = 48.dp, propColor: Color = colors.scarf[0]) {
    Box(modifier.size(size).background(MaterialTheme.colorScheme.surfaceContainerHigh, if (size >= 56.dp) DropShape else DropShapeSmall), contentAlignment = Alignment.Center) {
        TintyPropArt(prop, Modifier.size(size * 0.78f), colors, propColor)
    }
}

/** Tappable row: icon tile, title, optional subtitle and trailing content (chevron by default). */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    /** 틴티 prop spot art instead of [icon]. */
    art: TintyProp? = null,
    artColors: TintyColors = TintyColors.Brand,
    trailing: @Composable (() -> Unit)? = if (onClick != null) {{ Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = PcTheme.colors.ink3) }} else null,
) {
    val body: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (art != null) {
                PropTile(art, artColors, size = 40.dp)
                Spacer(Modifier.width(14.dp))
            } else if (icon != null) {
                IconTile(icon, size = 36.dp, tint = iconTint)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (iconTint == MaterialTheme.colorScheme.error) iconTint else MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) { Spacer(Modifier.width(12.dp)); trailing() }
        }
    }
    if (onClick != null) Surface(onClick = onClick, modifier = modifier, color = Color.Transparent) { body() }
    else Box(modifier) { body() }
}

// ---------------------------------------------------------------------------------------------------------------------
// Empty states and 틴티

/** Centered icon + message (+ optional action) for small empty or error states. Main empty states use [TintyEmptyState]. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) = EmptyStateLayout(title, message, modifier, action) { IconTile(icon, size = 72.dp, drop = true) }

/** [EmptyState] with 틴티 (holding the screen's [prop]) on a soft aurora halo, for the main empty/error states of a screen. */
@Composable
fun TintyEmptyState(
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    expression: TintyExpression = TintyExpression.Surprised,
    colors: TintyColors = TintyColors.Brand,
    prop: TintyProp = TintyProp.None,
    action: (@Composable () -> Unit)? = null,
) = EmptyStateLayout(title, message, modifier, action) {
    Box(Modifier.size(184.dp).aurora(PcTheme.colors), contentAlignment = Alignment.Center) {
        Tinty(Modifier.size(148.dp).tintyEntrance(), colors, expression, prop = prop)
    }
}

/** 틴티 pops in once (scale from its feet, small overshoot). Instant when system animations are off. */
@Composable
fun Modifier.tintyEntrance(delayMs: Int = 0): Modifier {
    val pop = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        launch { alpha.animateTo(1f, tween(PcMotion.FAST)) }
        pop.animateTo(1f, tween(PcMotion.SLOW, easing = PcMotion.Pop))
    }
    return graphicsLayer { scaleX = pop.value; scaleY = pop.value; this.alpha = alpha.value; transformOrigin = TransformOrigin(0.5f, 0.9f) }
}

@Composable
private fun EmptyStateLayout(title: String, message: String?, modifier: Modifier, action: (@Composable () -> Unit)?, art: @Composable () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        art()
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (message != null) {
            Spacer(Modifier.height(6.dp))
            Text(message, style = Muted, textAlign = TextAlign.Center)
        }
        if (action != null) { Spacer(Modifier.height(24.dp)); action() }
    }
}

/** Where a speech bubble's tail points: at 틴티 on the left, or down-right at 틴티 below the bubble. */
enum class BubbleTail { Start, BottomEnd }

/** Rounded bubble (r 18) with a small curved tail; the tail sits outside the content padding. */
class BubbleShape(private val tail: BubbleTail) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = with(density) {
        val r = 18.dp.toPx(); val t = 9.dp.toPx()
        val body = when (tail) {
            BubbleTail.Start -> Rect(t, 0f, size.width, size.height)
            BubbleTail.BottomEnd -> Rect(0f, 0f, size.width, size.height - t)
        }
        val path = Path().apply {
            addRoundRect(RoundRect(body, CornerRadius(r)))
            when (tail) {
                BubbleTail.Start -> { // from the left edge, low, curling toward 틴티
                    val y = body.bottom - r * 1.1f
                    moveTo(t + 1f, y - t); quadraticTo(t * 0.6f, y + t * 0.2f, 0f, y + t * 0.9f); quadraticTo(t * 1.4f, y + t * 0.7f, t + 1f, y + t * 0.2f); close()
                }
                BubbleTail.BottomEnd -> { // from the bottom edge near the end, pointing down-right
                    val x = body.right - r * 1.6f
                    moveTo(x - t, body.bottom - 1f); quadraticTo(x + t * 0.4f, body.bottom + t * 0.5f, x + t * 1.3f, size.height); quadraticTo(x + t * 0.9f, body.bottom + t * 0.3f, x + t * 0.6f, body.bottom - 1f); close()
                }
            }
        }
        Outline.Generic(path)
    }
}

/**
 * Top of a feature screen, hosted by 틴티 holding the feature's prop: a short title and line on a soft aurora card.
 * This 틴티 is the screen's living one; keep others on the screen still.
 */
@Composable
fun TintyHeader(
    title: String,
    body: String?,
    prop: TintyProp,
    modifier: Modifier = Modifier,
    colors: TintyColors = TintyColors.Brand,
    expression: TintyExpression = TintyExpression.Default,
    propColor: Color = colors.scarf[0],
) {
    PcCard(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(Modifier.aurora(PcTheme.colors).padding(start = 20.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 18.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (body != null) { Spacer(Modifier.height(4.dp)); Text(body, style = Muted) }
            }
            Tinty(Modifier.size(112.dp).tintyEntrance(), colors, expression, prop = prop, propColor = propColor)
        }
    }
}

/** 틴티's speech bubble: one short line (≈20 chars), tail toward 틴티 ([tailAtStart] = 틴티 on the left, else below-right). */
@Composable
fun TintyBubble(text: String, modifier: Modifier = Modifier, tailAtStart: Boolean = false) {
    val tail = if (tailAtStart) BubbleTail.Start else BubbleTail.BottomEnd
    PcCard(modifier, shape = remember(tail) { BubbleShape(tail) }, elevation = 2) {
        Text(
            text, style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = if (tailAtStart) 25.dp else 16.dp, end = 16.dp, top = 10.dp, bottom = if (tailAtStart) 10.dp else 19.dp),
        )
    }
}

/** 틴티 (≥ 64dp) next to a bubble: tips, reassurance (wink), small guidance. */
@Composable
fun TintyTip(
    text: String,
    modifier: Modifier = Modifier,
    expression: TintyExpression = TintyExpression.Happy,
    colors: TintyColors = TintyColors.Brand,
    tintySize: Dp = 72.dp,
    animated: Boolean = false,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Tinty(Modifier.size(tintySize), colors, expression, animated)
        TintyBubble(text, Modifier.weight(1f, fill = false), tailAtStart = true)
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Verdict: BEST / 무난 / WORST (color + label, never color alone)

enum class Verdict(val label: String, val sentence: String) {
    BEST("BEST", "잘 어울려요"), OKAY("무난", "무난해요"), WORST("WORST", "피하는 게 좋아요");
}

/** Foreground (text) and background colors of a verdict. */
@Composable
fun Verdict.colors(): Pair<Color, Color> = PcTheme.colors.let {
    when (this) {
        Verdict.BEST -> it.best to it.bestBg
        Verdict.OKAY -> it.okay to it.okayBg
        Verdict.WORST -> it.worst to it.worstBg
    }
}

/** Pill badge "BEST" / "무난" / "WORST". [text] overrides the label (e.g. the sentence). */
@Composable
fun VerdictBadge(verdict: Verdict, modifier: Modifier = Modifier, text: String = verdict.label) {
    val (fg, bg) = verdict.colors()
    Box(modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = if (text == verdict.label && verdict != Verdict.OKAY) 0.6.sp else 0.sp),
            color = fg, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Buttons

/** Primary button: accent + gloss, 56dp, presses down to .97. Single line, never wraps. */
@Composable
fun PcButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    PressableButton(
        onClick, modifier, enabled,
        container = if (enabled) cs.primary else cs.onSurface.copy(alpha = 0.12f),
        content = if (enabled) cs.onPrimary else cs.onSurface.copy(alpha = 0.38f),
        border = null, gloss = enabled, text = text, icon = icon,
    )
}

/** Destructive primary (delete, reset): error color, same shape and press as [PcButton]. */
@Composable
fun PcDangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    PressableButton(onClick, modifier, true, container = cs.error, content = cs.onError, border = null, gloss = false, text = text, icon = null)
}

/** Secondary button: surface + line-strong outline ("ghost"). */
@Composable
fun PcOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    PressableButton(
        onClick, modifier, enabled,
        container = cs.surfaceContainer,
        content = if (enabled) cs.onSurface else cs.onSurface.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, if (enabled) cs.outline else cs.outlineVariant), gloss = false, text = text, icon = icon,
    )
}

/** Soft accent button for secondary actions inside cards. */
@Composable
fun PcTonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    PressableButton(
        onClick, modifier, enabled,
        container = if (enabled) cs.primaryContainer else cs.onSurface.copy(alpha = 0.08f),
        content = if (enabled) cs.onPrimaryContainer else cs.onSurface.copy(alpha = 0.38f),
        border = null, gloss = false, text = text, icon = icon,
    )
}

@Composable
private fun PressableButton(
    onClick: () -> Unit, modifier: Modifier, enabled: Boolean,
    container: Color, content: Color, border: BorderStroke?, gloss: Boolean, text: String, icon: ImageVector?,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(PcMotion.INSTANT, easing = PcMotion.Standard), label = "press")
    val shape = MaterialTheme.shapes.medium
    Surface(
        onClick = onClick, enabled = enabled, interactionSource = interaction, shape = shape, color = container, contentColor = content, border = border,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }.heightIn(min = 56.dp),
    ) {
        Row(
            (if (gloss) Modifier.background(Gloss) else Modifier).heightIn(min = 56.dp).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Bottom area for pinned actions: background, top padding and navigation-bar inset. */
@Composable
fun BottomActions(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .readableWidth().padding(horizontal = ScreenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Chrome: top bar, scaffold, snackbar

/** Top bar for every screen. Pass [onBack] on pushed screens; tab roots leave it null and get a large title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcTopAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = if (onBack == null) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로") } },
        actions = { CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { actions() } },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

/**
 * Standard screen: top bar + content + optional bottom area (pinned buttons).
 * [content] receives padding that already includes the top bar; apply it to the scrolling container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { PcTopAppBar(title, onBack, scroll, actions) },
        bottomBar = bottomBar,
        snackbarHost = { if (snackbarHostState != null) PcSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

/** Snackbar in brand style: dark violet ink, r-md, lavender action, and a tiny 틴티 face saying it. */
@Composable
fun PcSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) =
    SnackbarHost(state, modifier) { data ->
        val cs = MaterialTheme.colorScheme
        Snackbar(
            modifier = Modifier.padding(horizontal = 4.dp),
            shape = MaterialTheme.shapes.medium,
            containerColor = cs.inverseSurface,
            contentColor = cs.inverseOnSurface,
            action = data.visuals.actionLabel?.let { label ->
                { TextButton(onClick = { data.performAction() }) { Text(label, color = cs.inversePrimary, style = MaterialTheme.typography.labelLarge) } }
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DropGlyph(Color.Unspecified, Modifier.size(18.dp, 22.dp).tintyEntrance(), brush = BrandBrush, eyes = true)
                Spacer(Modifier.width(12.dp))
                Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

private val BrandBrush = Brush.linearGradient(listOf(PcColors.Peach, PcColors.Lavender))

