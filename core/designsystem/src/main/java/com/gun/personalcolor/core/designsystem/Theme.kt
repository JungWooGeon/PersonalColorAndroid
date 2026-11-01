package com.gun.personalcolor.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tokens: BRAND_SYSTEM.md §1 ("크림색 캔버스 위에 틴티 한 방울"). Warm-neutral canvas so swatches read true; one violet accent.
// Selection roles (secondary*) reuse the accent so chips, segmented buttons and the nav indicator all speak violet.
private val Light = lightColorScheme(
    primary = Color(0xFF5B4B8A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE4F8),
    onPrimaryContainer = Color(0xFF3A2D66),
    inversePrimary = Color(0xFFCBBEF5),
    secondary = Color(0xFF5B4B8A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAE4F8),
    onSecondaryContainer = Color(0xFF3A2D66),
    tertiary = Color(0xFF276E57),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDF0EA),
    onTertiaryContainer = Color(0xFF1B4D3D),
    background = Color(0xFFFAF8F6),
    onBackground = Color(0xFF1D1B1A),
    surface = Color(0xFFFAF8F6),
    onSurface = Color(0xFF1D1B1A),
    surfaceVariant = Color(0xFFF4F1EE),
    onSurfaceVariant = Color(0xFF66605B),
    surfaceTint = Color(0xFF5B4B8A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF4F1EE),
    surfaceContainerHighest = Color(0xFFEDE8E4),
    inverseSurface = Color(0xFF2B2236),
    inverseOnSurface = Color(0xFFF4F1EE),
    outline = Color(0xFFD5CDC7),
    outlineVariant = Color(0xFFEAE4DF),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF8C1D18),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFCBBEF5),
    onPrimary = Color(0xFF2F2260),
    primaryContainer = Color(0xFF2E2745),
    onPrimaryContainer = Color(0xFFE2DAFB),
    inversePrimary = Color(0xFF5B4B8A),
    secondary = Color(0xFFCBBEF5),
    onSecondary = Color(0xFF2F2260),
    secondaryContainer = Color(0xFF2E2745),
    onSecondaryContainer = Color(0xFFE2DAFB),
    tertiary = Color(0xFF8FD3BC),
    onTertiary = Color(0xFF0F3229),
    tertiaryContainer = Color(0xFF1F3B33),
    onTertiaryContainer = Color(0xFFBFE8DA),
    background = Color(0xFF141312),
    onBackground = Color(0xFFEAE6E3),
    surface = Color(0xFF141312),
    onSurface = Color(0xFFEAE6E3),
    surfaceVariant = Color(0xFF2A2725),
    onSurfaceVariant = Color(0xFFB8B0AA),
    surfaceTint = Color(0xFFCBBEF5),
    surfaceContainerLowest = Color(0xFF1E1C1B),
    surfaceContainerLow = Color(0xFF1E1C1B),
    surfaceContainer = Color(0xFF1E1C1B),
    surfaceContainerHigh = Color(0xFF2A2725),
    surfaceContainerHighest = Color(0xFF353230),
    inverseSurface = Color(0xFFEAE6E3),
    inverseOnSurface = Color(0xFF1D1B1A),
    outline = Color(0xFF4D4845),
    outlineVariant = Color(0xFF3A3633),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF601410),
    onErrorContainer = Color(0xFFF9DEDC),
)

/** Brand tokens with no Material role: ink-3, verdict colors, aurora blobs. Read via [PcTheme.colors]. */
@Immutable
data class PcColors(
    val isDark: Boolean,
    /** Decoration / disabled only — never body text. */
    val ink3: Color,
    val best: Color, val bestBg: Color,
    val okay: Color, val okayBg: Color,
    val worst: Color, val worstBg: Color,
    val auroraPeach: Color, val auroraLavender: Color,
) {
    companion object {
        val Peach = Color(0xFFFFCBB0)
        val Lavender = Color(0xFFCBBEF5)
        /** 틴티 eyes/mouth; fixed in dark mode too. */
        val TintyInk = Color(0xFF2B2236)
        /** One chip per season (scarf, loading dots): spring coral · summer sky · autumn mustard · winter magenta. */
        val Seasons = listOf(Color(0xFFFF8A6B), Color(0xFF9CC9EC), Color(0xFFD4A017), Color(0xFFB5006E))
    }
}

private val LightBrand = PcColors(
    isDark = false, ink3 = Color(0xFF9A918B),
    best = Color(0xFF276E57), bestBg = Color(0xFFDDF0EA),
    okay = Color(0xFF7A5E14), okayBg = Color(0xFFF6EDCF),
    worst = Color(0xFFA33F33), worstBg = Color(0xFFF9E3DE),
    auroraPeach = Color(0xFFFFE3D4), auroraLavender = Color(0xFFE6DFFB),
)
private val DarkBrand = PcColors(
    isDark = true, ink3 = Color(0xFF857D77),
    best = Color(0xFF8FD3BC), bestBg = Color(0xFF1F3B33),
    okay = Color(0xFFE3C77A), okayBg = Color(0xFF3A321C),
    worst = Color(0xFFF0A89C), worstBg = Color(0xFF45231E),
    auroraPeach = Color(0x24FFCBB0), auroraLavender = Color(0x29CBBEF5),
)

private val LocalPcColors = staticCompositionLocalOf { LightBrand }

// Korean wraps per syllable by default ("…어울리는 톤이에\n요."). Phrase breaking keeps words whole (API 33+; no-op below).
private val KoreanLineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)
// Phrase breaking only kicks in for a Korean text locale, so set it explicitly (devices may be in another language).
private fun TextStyle.kr() = copy(lineBreak = KoreanLineBreak, localeList = LocaleList("ko-KR"))
private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) =
    TextStyle(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking.sp).kr()

// BRAND_SYSTEM §2. Titles tighten by ~-0.02em; at most three weights per screen (400 · 600 · 800/700).
private val Type = Typography(
    displayLarge = style(48, 56, FontWeight.ExtraBold, -1.2),
    displayMedium = style(40, 48, FontWeight.ExtraBold, -1.0),
    displaySmall = style(36, 44, FontWeight.ExtraBold, -0.9),
    headlineLarge = style(30, 38, FontWeight.ExtraBold, -0.7),
    headlineMedium = style(26, 34, FontWeight.Bold, -0.6),
    headlineSmall = style(22, 30, FontWeight.Bold, -0.5),
    titleLarge = style(20, 28, FontWeight.Bold, -0.4),
    titleMedium = style(17, 24, FontWeight.SemiBold, -0.2),
    titleSmall = style(15, 22, FontWeight.SemiBold, -0.1),
    bodyLarge = style(16, 26, FontWeight.Normal),
    bodyMedium = style(14, 22, FontWeight.Normal),
    bodySmall = style(13, 18, FontWeight.Medium),
    labelLarge = style(14, 20, FontWeight.Bold),
    labelMedium = style(12, 16, FontWeight.SemiBold),
    labelSmall = style(11, 16, FontWeight.Medium),
)

// BRAND_SYSTEM §3: r-xs 8 · r-sm 12 · r-md 16 (buttons, inputs) · r-lg 24 (cards) · r-xl 32 (hero cards, sheet top).
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** "Drop corner": like 틴티's tip, only the top-right is less round. 1–2 per screen (icon tile, bubble, one hero card). */
val DropShape = RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
/** [DropShape] scaled for 36–44dp icon tiles. */
val DropShapeSmall = RoundedCornerShape(topStart = 14.dp, topEnd = 5.dp, bottomEnd = 14.dp, bottomStart = 14.dp)

/** BRAND_SYSTEM §7 motion tokens. */
object PcMotion {
    const val INSTANT = 100
    const val FAST = 160
    const val BASE = 240
    const val SLOW = 400
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Out = CubicBezierEasing(0f, 0f, 0f, 1f)
    val In = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    /** Small overshoot: 틴티 entrance, BEST badge, "saved". */
    val Pop = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
}

@Composable
fun PcTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalPcColors provides if (darkTheme) DarkBrand else LightBrand) {
        MaterialTheme(colorScheme = if (darkTheme) Dark else Light, typography = Type, shapes = AppShapes, content = content)
    }

object PcTheme {
    val colors: PcColors @Composable @ReadOnlyComposable get() = LocalPcColors.current
}

/** Secondary body text. */
val Muted: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
