package com.gun.personalcolor.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gun.personalcolor.core.designsystem.Eyebrow
import com.gun.personalcolor.core.designsystem.PcCard
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.designsystem.TintyExpression
import androidx.compose.runtime.remember
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import kotlin.math.floor
import kotlin.math.max

fun hexColor(hex: String) = Color(android.graphics.Color.parseColor(hex))

/** Readable text color on top of [bg]. */
fun contentColorOn(bg: Color) = if (bg.luminance() > 0.45f) Color(0xFF1D1B1A) else Color.White

/** Signature color of a tone: its most vivid mid-lightness best color (never white/black). */
val Tone.accent get() = bestColors.map { hexColor(it.hex) }.maxBy { c ->
    val max = maxOf(c.red, c.green, c.blue); val min = minOf(c.red, c.green, c.blue)
    val l = (max + min) / 2
    val sat = if (max == min) 0f else (max - min) / (1 - kotlin.math.abs(2 * l - 1))
    sat * (1 - kotlin.math.abs(l - 0.55f))
}
val Tone.gradient get() = Brush.linearGradient(bestColors.take(4).map { hexColor(it.hex) })

val SEASON_KO = mapOf("spring" to "봄", "summer" to "여름", "autumn" to "가을", "winter" to "겨울")
val Tone.seasonKo get() = SEASON_KO[season].orEmpty()
val Tone.temperatureKo get() = if (isWarm) "웜톤" else "쿨톤"

/** Round gradient avatar of a tone's best colors. */
@Composable
fun ToneAvatar(tone: Tone, size: Dp = 44.dp, modifier: Modifier = Modifier) =
    Box(modifier.size(size).clip(CircleShape).background(remember(tone) { tone.tintyColors.let { Brush.linearGradient(listOf(it.bodyTop, it.bodyBottom)) } }))

/**
 * Labelled swatches that fill the available width: as many columns as fit (min [minCell]), at least [minColumns].
 * Names stay on one line, shrinking (down to 8sp) to fit the cell.
 */
@Composable
fun SwatchGrid(colors: List<NamedColor>, modifier: Modifier = Modifier, minCell: Dp = 54.dp, minColumns: Int = 4, maxColumns: Int = 8, showNames: Boolean = true) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = 10.dp
        val fit = floor((maxWidth + gap) / (minCell + gap)).toInt().coerceIn(minColumns, max(minColumns, maxColumns))
        // Nudge ±1 column if that avoids a ragged last row (12 → 6×2), keeping cells ≥ 44dp.
        val cols = listOf(fit, fit + 1, fit - 1).firstOrNull { c ->
            c >= minColumns && colors.size % c == 0 && (maxWidth - gap * (c - 1)) / c >= 44.dp
        } ?: fit
        val cell = (maxWidth - gap * (cols - 1)) / cols
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            colors.chunked(cols).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { c ->
                        Column(Modifier.width(cell), horizontalAlignment = Alignment.CenterHorizontally) {
                            Swatch(c.hex, Modifier.fillMaxWidth().aspectRatio(1f))
                            if (showNames) {
                                Spacer(Modifier.height(4.dp))
                                val label = MaterialTheme.typography.labelSmall.copy(textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                // One line (shrinking a little) when it can fit; long names wrap between words instead of being cut.
                                if (fitsOneLine(c.name, cell)) {
                                    BasicText(c.name, style = label, maxLines = 1,
                                        autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = label.fontSize))
                                } else {
                                    Text(splitAtMiddleSpace(c.name), style = label.copy(fontSize = 10.sp, lineHeight = 12.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Rough width of a Korean label at the smallest size (8.5sp per syllable, ~3dp per space).
private fun fitsOneLine(name: String, cell: Dp) = name.count { it != ' ' } * 8.5f + name.count { it == ' ' } * 3f <= cell.value

/** "그레이시 블루" → "그레이시\n블루": break at the space nearest the middle (never inside a word). */
private fun splitAtMiddleSpace(name: String): String {
    val spaces = name.indices.filter { name[it] == ' ' }
    val at = spaces.minByOrNull { kotlin.math.abs(it - name.length / 2) } ?: return name
    return name.substring(0, at) + "\n" + name.substring(at + 1)
}

/** Rounded color square with a hairline border (so whites show on white). */
@Composable
fun Swatch(hex: String, modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)) {
    Box(modifier.clip(shape).background(hexColor(hex)).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape))
}

/** Overlapping color dots. */
@Composable
fun ColorDots(colors: List<NamedColor>, size: Dp = 16.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
        colors.forEach {
            Swatch(it.hex, Modifier.size(size).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape).padding(1.5.dp), CircleShape)
        }
    }
}

/** Thin strip of all best colors. */
@Composable
fun GradientStrip(tone: Tone, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    Row(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2))) {
        tone.bestColors.forEach { Box(Modifier.weight(1f).fillMaxHeight().background(hexColor(it.hex))) }
    }
}

/** Small pill label, e.g. "#화사한". Text color follows [color] (primaryContainer → onPrimaryContainer). */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    Surface(modifier, shape = RoundedCornerShape(50), color = color) {
        Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

/**
 * Pastel wash of a tone (≥70% lifted toward white) for hero cards that carry 틴티 (BRAND_SYSTEM §6-4).
 * Dark mode keeps the same hue as a low-alpha tint over the card surface.
 */
@Composable
fun Tone.softBrush(): Brush {
    val dark = PcTheme.colors.isDark
    return remember(this, dark) {
        val c = tintyColors
        if (dark) Brush.linearGradient(listOf(c.bodyTop.copy(alpha = 0.20f), c.bodyBottom.copy(alpha = 0.14f)))
        else Brush.linearGradient(listOf(c.bodyTop.lift(0.72f), c.bodyBottom.lift(0.74f)))
    }
}

private fun Color.lift(t: Float) = Color(red + (1 - red) * t, green + (1 - green) * t, blue + (1 - blue) * t)

/**
 * Tone hero: pastel card with the tone's 틴티, season line, name and color strip; keyword tags and summary below.
 * [expression] null hides 틴티 (when another 틴티 is already on screen).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToneHeader(tone: Tone, modifier: Modifier = Modifier, expression: TintyExpression? = TintyExpression.Default, animated: Boolean = true) {
    Column(modifier) {
        Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(tone.softBrush())) {
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 8.dp, top = 20.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("${tone.seasonKo} · ${tone.temperatureKo} · ${tone.nameEn}")
                    Spacer(Modifier.height(4.dp))
                    Text(tone.nameKo, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(12.dp))
                    GradientStrip(tone, Modifier.padding(end = 8.dp), height = 8.dp)
                }
                if (expression != null) Tinty(Modifier.size(112.dp), remember(tone) { tone.tintyColors }, expression, animated)
            }
        }
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tone.keywords.forEach { Tag("#$it") }
        }
        Spacer(Modifier.height(12.dp))
        Text(tone.summary, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Compact list card for a tone: its 틴티, names, summary, color strip. */
@Composable
fun ToneCard(tone: Tone, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    PcCard(modifier, onClick = onClick) {
        Column(Modifier.padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tinty(Modifier.size(64.dp), remember(tone) { tone.tintyColors }, animated = false)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(tone.nameKo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${tone.temperatureKo} · ${tone.nameEn}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                trailing?.invoke()
            }
            Text(tone.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            Spacer(Modifier.height(12.dp))
            GradientStrip(tone, Modifier.padding(start = 4.dp), height = 8.dp)
        }
    }
}

// 틴티 per tone (same picks as the character spec): body = 2 representative best colors, scarf = 4 contrasting ones.
private val TINTY_PICK = mapOf(
    "spring_light" to listOf("피치", "웜 핑크", "라이트 민트", "버터 옐로", "아쿠아 블루", "라이트 코랄"),
    "spring_true" to listOf("골든 옐로", "코랄", "터콰이즈", "애플 그린", "웜 레드", "오렌지"),
    "spring_bright" to listOf("레몬 옐로", "비비드 코랄", "클리어 블루", "아쿠아 민트", "비비드 그린", "클리어 레드"),
    "summer_light" to listOf("파우더 핑크", "라벤더", "베이비 블루", "파스텔 아쿠아", "로즈 핑크", "페리윙클"),
    "summer_true" to listOf("소프트 로즈", "라벤더", "미디엄 블루", "스카이 블루", "블루 그린", "라즈베리"),
    "summer_mute" to listOf("더스티 로즈", "그레이시 블루", "블루 세이지", "모브", "슬레이트 블루", "소프트 플럼"),
    "autumn_mute" to listOf("살몬 베이지", "세이지 그린", "소프트 머스터드", "소프트 카멜", "올리브 그린", "소프트 테라코타"),
    "autumn_true" to listOf("머스터드", "펌킨 오렌지", "올리브 그린", "러스트", "카멜", "테라코타"),
    "autumn_deep" to listOf("브론즈", "와인 레드", "딥 틸", "딥 올리브", "다크 체리 레드", "토바코 브라운"),
    "winter_bright" to listOf("핫핑크", "바이올렛", "쿨 터콰이즈", "코발트 블루", "에메랄드 그린", "퓨어 화이트"),
    "winter_true" to listOf("마젠타", "로열 블루", "크림슨", "퓨어 화이트", "미드나잇 네이비", "파인 그린"),
    "winter_deep" to listOf("로열 퍼플", "딥 사파이어", "쿨 레드", "퓨어 화이트", "와인", "에그플랜트"),
)

/** 틴티 dressed in this tone. Deep body colors are lifted toward white just enough that the eyes stay readable. */
val Tone.tintyColors: TintyColors get() {
    val byName = bestColors.associate { it.name to hexColor(it.hex) }
    // If tones.json renames colors, fall back to the first 6 mid-lightness best colors.
    val picked = TINTY_PICK[id]?.mapNotNull { byName[it] }?.takeIf { it.size == 6 }
        ?: bestColors.map { hexColor(it.hex) }.filter { it.luminance() in 0.03f..0.92f }.take(6)
    if (picked.size < 6) return TintyColors.Brand
    return TintyColors(
        bodyTop = picked[0].liftTo(0.16f),
        bodyBottom = picked[1].liftTo(0.09f),
        scarf = picked.subList(2, 6),
        blush = makeup.blushColors.firstOrNull()?.let { hexColor(it.hex) } ?: Color(0xFFF4A3A0),
    )
}

private fun Color.liftTo(minLuminance: Float): Color {
    var t = 0f
    fun at(t: Float) = Color(red + (1 - red) * t, green + (1 - green) * t, blue + (1 - blue) * t)
    while (at(t).luminance() < minLuminance && t < 0.9f) t += 0.02f
    return at(t)
}
