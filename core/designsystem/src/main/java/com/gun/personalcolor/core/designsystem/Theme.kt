package com.gun.personalcolor.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Warm-neutral canvas so swatches read true; one violet accent.
private val Light = lightColorScheme(
    primary = Color(0xFF5B4B8A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE4F8),
    onPrimaryContainer = Color(0xFF261A4D),
    secondary = Color(0xFFB4675A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF9E6E1),
    onSecondaryContainer = Color(0xFF45201A),
    tertiary = Color(0xFF3F7A6B),
    tertiaryContainer = Color(0xFFDDF0EA),
    onTertiaryContainer = Color(0xFF0F3229),
    background = Color(0xFFFAF8F6),
    onBackground = Color(0xFF1D1B1A),
    surface = Color(0xFFFAF8F6),
    onSurface = Color(0xFF1D1B1A),
    surfaceVariant = Color(0xFFF0ECE8),
    onSurfaceVariant = Color(0xFF66605B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF4F1EE),
    surfaceContainerHighest = Color(0xFFEDE8E4),
    outline = Color(0xFFD5CDC7),
    outlineVariant = Color(0xFFEAE4DF),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFCBBEF5),
    onPrimary = Color(0xFF2F2260),
    primaryContainer = Color(0xFF463873),
    onPrimaryContainer = Color(0xFFEAE4F8),
    secondary = Color(0xFFF0B4A8),
    onSecondary = Color(0xFF55271F),
    secondaryContainer = Color(0xFF6E3A31),
    onSecondaryContainer = Color(0xFFF9E6E1),
    tertiary = Color(0xFF9ED3C3),
    tertiaryContainer = Color(0xFF235346),
    onTertiaryContainer = Color(0xFFDDF0EA),
    background = Color(0xFF141312),
    onBackground = Color(0xFFEAE6E3),
    surface = Color(0xFF141312),
    onSurface = Color(0xFFEAE6E3),
    surfaceVariant = Color(0xFF2A2725),
    onSurfaceVariant = Color(0xFFB8B0AA),
    surfaceContainerLowest = Color(0xFF0F0E0D),
    surfaceContainerLow = Color(0xFF1C1A19),
    surfaceContainer = Color(0xFF201E1C),
    surfaceContainerHigh = Color(0xFF2A2826),
    surfaceContainerHighest = Color(0xFF353230),
    outline = Color(0xFF4D4845),
    outlineVariant = Color(0xFF3A3633),
)

// Korean wraps per syllable by default ("…어울리는 톤이에\n요."). Phrase breaking keeps words whole (API 33+; no-op below).
private val KoreanLineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)
private val base = Typography().let { t ->
    // Phrase breaking only kicks in for a Korean text locale, so set it explicitly (devices may be in another language).
    fun TextStyle.kr() = copy(lineBreak = KoreanLineBreak, localeList = LocaleList("ko-KR"))
    Typography(
        t.displayLarge.kr(), t.displayMedium.kr(), t.displaySmall.kr(),
        t.headlineLarge.kr(), t.headlineMedium.kr(), t.headlineSmall.kr(),
        t.titleLarge.kr(), t.titleMedium.kr(), t.titleSmall.kr(),
        t.bodyLarge.kr(), t.bodyMedium.kr(), t.bodySmall.kr(),
        t.labelLarge.kr(), t.labelMedium.kr(), t.labelSmall.kr(),
    )
}
private fun TextStyle.tight() = copy(letterSpacing = (-0.3).sp)
private val Type = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold).tight(),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp).tight(),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp).tight(),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp).tight(),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp).tight(),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 22.sp),
    bodySmall = base.bodySmall.copy(lineHeight = 18.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PcTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, typography = Type, shapes = AppShapes, content = content)

/** Secondary body text. */
val Muted: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
