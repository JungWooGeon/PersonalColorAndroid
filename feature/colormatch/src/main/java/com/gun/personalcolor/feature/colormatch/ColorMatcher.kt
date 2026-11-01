package com.gun.personalcolor.feature.colormatch

import com.gun.personalcolor.core.analysis.ColorMath
import com.gun.personalcolor.core.analysis.Lab
import com.gun.personalcolor.core.analysis.Rgb
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.model.Verdict
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class MatchLevel(val label: String, val verdict: Verdict) {
    GOOD("잘 어울려요", Verdict.GOOD), NEUTRAL("무난해요", Verdict.OK), AVOID("피하는 게 좋아요", Verdict.AVOID);

    companion object { fun of(v: Verdict) = entries.first { it.verdict == v } }
}

data class ToneMatch(val tone: Tone, val deltaE: Double)

data class ColorVerdict(
    val level: MatchLevel,
    val score: Int,                    // 0..100
    val reason: String,
    val nearestBest: List<NamedColor>, // 3 closest best colors of the reference tone
    val topTones: List<ToneMatch>,     // 3 tones whose best colors are closest
    val nameHint: String? = null,      // matched good/worst color name, for labels like "카멜 상의"
)

object ColorMatcher {
    // ponytail: heuristic thresholds (CIEDE2000 units), tuned by eye on the 12-tone palettes, not on user data.
    const val GOOD_MAX = 10.0        // within this of a best color → good
    const val VERY_CLOSE = 5.0       // "아주 비슷해요"
    const val AVOID_MAX = 12.0       // within this of a worst color (and closer than any best) → avoid
    const val NEUTRAL_CHROMA = 8.0   // below this chroma the color counts as white/grey/black/beige-ish
    const val CHROMA_HINT = 20.0     // chroma gap to the tone's palette worth mentioning
    const val LIGHTNESS_HINT = 22.0  // lightness gap to the tone's palette worth mentioning
    private const val SCORE_SPAN = 35.0 // ΔE where the "closeness" part of the score reaches 0
    private const val WORST_SPAN = 25.0 // ΔE below which closeness to a worst color lowers the score

    fun evaluate(rgb: Rgb, tone: Tone, allTones: List<Tone>): ColorVerdict {
        val lab = ColorMath.rgbToLab(rgb)
        val good = goodColors(tone).map { (label, c) -> Triple(label, c, deltaE2000(lab, labOf(c.hex))) }
        // A tone with no palette (bad content) can't be judged; treat it as neutral rather than crash.
        val nearGood = good.minByOrNull { it.third }
        val nearWorst = tone.worstColors.minByOrNull { deltaE2000(lab, labOf(it.hex)) }
        val dGood = nearGood?.third ?: Double.MAX_VALUE
        val dWorst = nearWorst?.let { deltaE2000(lab, labOf(it.hex)) } ?: Double.MAX_VALUE

        val level = when {
            nearGood != null && dGood <= GOOD_MAX && dGood < dWorst -> MatchLevel.GOOD
            nearWorst != null && dWorst < dGood && dWorst <= AVOID_MAX -> MatchLevel.AVOID
            else -> MatchLevel.NEUTRAL
        }
        val reason = when (level) {
            MatchLevel.GOOD -> nearGood?.let { (label, c) -> "$label '${c.name}'${gwa(c.name)} ${if (dGood <= VERY_CLOSE) "아주 " else ""}비슷해요" }.orEmpty()
            MatchLevel.AVOID -> nearWorst?.let { "WORST 컬러 '${it.name}'에 가까워요" }.orEmpty()
            MatchLevel.NEUTRAL -> neutralHint(lab, tone)
        }

        val closeness = (1 - dGood / SCORE_SPAN).coerceIn(0.0, 1.0)
        val penalty = if (dWorst < WORST_SPAN) (WORST_SPAN - dWorst) / WORST_SPAN * 0.4 else 0.0
        val raw = ((closeness - penalty) * 100).toInt()
        // Keep the bar consistent with the badge.
        val score = when (level) {
            MatchLevel.GOOD -> raw.coerceIn(70, 100)
            MatchLevel.NEUTRAL -> raw.coerceIn(40, 69)
            MatchLevel.AVOID -> raw.coerceIn(0, 39)
        }

        val nearestBest = tone.bestColors.sortedBy { deltaE2000(lab, labOf(it.hex)) }.take(3)
        val topTones = allTones
            .filter { it.bestColors.isNotEmpty() }
            .map { t -> ToneMatch(t, t.bestColors.minOf { deltaE2000(lab, labOf(it.hex)) }) }
            .sortedBy { it.deltaE }.take(3)
        val nameHint = when (level) {
            MatchLevel.GOOD -> nearGood?.second?.name
            MatchLevel.AVOID -> nearWorst?.name
            // Neutral: name it after the closest named color in any tone's palette (e.g. "네이비"), not a hex code.
            MatchLevel.NEUTRAL -> allTones.flatMap { it.bestColors + it.worstColors }
                .minByOrNull { deltaE2000(lab, labOf(it.hex)) }?.name
        }
        return ColorVerdict(level, score, reason, nearestBest, topTones, nameHint)
    }

    /** Best colors first, then makeup and hair colors as "also good". */
    private fun goodColors(t: Tone): List<Pair<String, NamedColor>> =
        t.bestColors.map { "BEST 컬러" to it } +
            t.makeup.lipColors.map { "추천 립 컬러" to it } +
            t.makeup.eyeColors.map { "추천 아이 컬러" to it } +
            t.makeup.blushColors.map { "추천 블러셔" to it } +
            t.hair.colors.map { "추천 헤어 컬러" to it }

    private fun neutralHint(lab: Lab, tone: Tone): String {
        val palette = tone.bestColors.map { labOf(it.hex) }
        val meanL = palette.map { it.l }.average()
        val meanC = palette.map { it.chroma }.average()
        return when {
            lab.l - meanL > LIGHTNESS_HINT -> "이 톤엔 조금 더 깊이 있는 색이 좋아요"
            meanL - lab.l > LIGHTNESS_HINT -> "이 톤엔 조금 더 밝은 색이 좋아요"
            lab.chroma < NEUTRAL_CHROMA -> "무채색이라 어디에 매치해도 무난해요"
            lab.chroma - meanC > CHROMA_HINT -> "이 톤엔 조금 더 차분한 색이 좋아요"
            meanC - lab.chroma > CHROMA_HINT -> "이 톤엔 조금 더 선명한 색이 좋아요"
            else -> "BEST 컬러와 조금 비슷해요"
        }
    }

    private val labCache = HashMap<String, Lab>()
    @Synchronized private fun labOf(hex: String): Lab = labCache.getOrPut(hex) { ColorMath.rgbToLab(hexToRgb(hex)) }

    /** 와/과 by the final consonant of the last Hangul syllable. */
    private fun gwa(word: String): String {
        val c = word.lastOrNull { it.isLetterOrDigit() } ?: return "와"
        return if (c in '가'..'힣' && (c - '가') % 28 != 0) "과" else "와"
    }
}

/** Grey for anything unparsable (corrupt saved data must never crash a screen). */
fun hexToRgb(hex: String): Rgb {
    val v = hex.trim().removePrefix("#").toLongOrNull(16)?.toInt() ?: 0x808080
    return Rgb((v shr 16) and 0xFF, (v shr 8) and 0xFF, v and 0xFF)
}

private val HEX = Regex("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?")

/** [hex] if `Color.parseColor` accepts it as #RRGGBB / #AARRGGBB, else grey. */
internal fun safeHex(hex: String): String = if (HEX.matches(hex)) hex else "#808080"

fun Rgb.toHex(): String = "#%02X%02X%02X".format(r, g, b)

/** CIEDE2000 color difference (kL = kC = kH = 1), Sharma, Wu & Dalal 2005. */
fun deltaE2000(x: Lab, y: Lab): Double {
    val c7 = ((hypot(x.a, x.b) + hypot(y.a, y.b)) / 2).pow(7)
    val g = 0.5 * (1 - sqrt(c7 / (c7 + 25.0.pow(7))))
    val a1 = (1 + g) * x.a
    val a2 = (1 + g) * y.a
    val c1 = hypot(a1, x.b)
    val c2 = hypot(a2, y.b)
    fun hue(a: Double, b: Double) = if (a == 0.0 && b == 0.0) 0.0 else (Math.toDegrees(atan2(b, a)) + 360) % 360
    val h1 = hue(a1, x.b)
    val h2 = hue(a2, y.b)

    val dL = y.l - x.l
    val dC = c2 - c1
    val dh = when {
        c1 * c2 == 0.0 -> 0.0
        abs(h2 - h1) <= 180 -> h2 - h1
        h2 - h1 > 180 -> h2 - h1 - 360
        else -> h2 - h1 + 360
    }
    val dH = 2 * sqrt(c1 * c2) * sin(Math.toRadians(dh / 2))

    val lBar = (x.l + y.l) / 2
    val cBar = (c1 + c2) / 2
    val hBar = when {
        c1 * c2 == 0.0 -> h1 + h2
        abs(h1 - h2) <= 180 -> (h1 + h2) / 2
        h1 + h2 < 360 -> (h1 + h2 + 360) / 2
        else -> (h1 + h2 - 360) / 2
    }
    val t = 1 - 0.17 * cos(Math.toRadians(hBar - 30)) + 0.24 * cos(Math.toRadians(2 * hBar)) +
        0.32 * cos(Math.toRadians(3 * hBar + 6)) - 0.20 * cos(Math.toRadians(4 * hBar - 63))
    val dTheta = 30 * exp(-((hBar - 275) / 25).pow(2))
    val cBar7 = cBar.pow(7)
    val rc = 2 * sqrt(cBar7 / (cBar7 + 25.0.pow(7)))
    val sl = 1 + 0.015 * (lBar - 50).pow(2) / sqrt(20 + (lBar - 50).pow(2))
    val sc = 1 + 0.045 * cBar
    val sh = 1 + 0.015 * cBar * t
    val rt = -sin(Math.toRadians(2 * dTheta)) * rc
    return sqrt((dL / sl).pow(2) + (dC / sc).pow(2) + (dH / sh).pow(2) + rt * (dC / sc) * (dH / sh))
}
