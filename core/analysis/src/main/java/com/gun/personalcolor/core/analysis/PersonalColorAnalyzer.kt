package com.gun.personalcolor.core.analysis

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** Pixel samples per facial region, already taken from the photo by the Android layer. Any list may be empty. */
data class FaceSamples(
    val skin: List<Rgb>,     // cheeks + forehead + chin, a few hundred pixels (union of [skinRegions] when given)
    val sclera: List<Rgb>,   // eye whites (may include some noise: lashes, iris edge)
    val iris: List<Rgb>,
    val hair: List<Rgb>,     // patches above forehead / beside temples — may be background/hat/forehead; treat as unreliable
    val lips: List<Rgb>,
    /** Optional: pixels of a white reference (paper) the user held/tapped. Best white-balance source when present. */
    val whiteRef: List<Rgb> = emptyList(),
    /** Optional per-region split of [skin] ("cheekL", "cheekR", "forehead", "chin"): enables shadow + lighting checks. */
    val skinRegions: Map<String, List<Rgb>> = emptyMap(),
    /** Optional split of [hair] into separate patches, each judged on its own. */
    val hairPatches: List<List<Rgb>> = emptyList(),
)

enum class WhiteBalanceSource { WHITE_REF, SCLERA, NONE }

data class Features(
    val skin: Lab,               // white-balanced robust skin color
    val iris: Lab?, val hair: Lab?, val lips: Lab?,   // null = missing or judged unreliable (not used)
    val whiteBalanceGains: DoubleArray,   // r,g,b multipliers applied in linear light (1,1,1 if none)
    val contrast: Double,        // skin L* minus darkest reliable feature (hair/iris) L*, 0 if unknown
    val whiteBalance: WhiteBalanceSource = WhiteBalanceSource.NONE,
    /** |L*| difference between the two cheeks: side lighting. 0 when unknown. */
    val lightingAsymmetry: Double = 0.0,
    /** Lip chroma high enough that it's probably lipstick: lip color then carries little weight. */
    val lipMakeup: Boolean = false,
    /** 0..1 measurement quality (white-balance source, lighting evenness, missing features). */
    val quality: Double = 1.0,
)

/**
 * Anonymous measurement numbers only (no pixels, no image): for opt-in model-improvement logging.
 * Keys are stable — tools/model/train.py reads them back. Missing features are omitted.
 */
fun Features.measurementVector(): Map<String, Double> = buildMap {
    put("skin_l", skin.l); put("skin_a", skin.a); put("skin_b", skin.b)
    put("skin_h", skin.hue); put("skin_c", skin.chroma)
    hair?.let { put("hair_l", it.l); put("hair_a", it.a); put("hair_b", it.b) }
    iris?.let { put("iris_l", it.l); put("iris_a", it.a); put("iris_b", it.b) }
    lips?.let { put("lip_l", it.l); put("lip_h", ToneModel.signedHue(it)); put("lip_c", it.chroma) }
    put("contrast", contrast)
    put("wb_r", whiteBalanceGains[0]); put("wb_g", whiteBalanceGains[1]); put("wb_b", whiteBalanceGains[2])
    put("wb_src", whiteBalance.ordinal.toDouble())   // 0 paper, 1 sclera, 2 none
    put("asym", lightingAsymmetry)
    put("lip_makeup", if (lipMakeup) 1.0 else 0.0)
    put("quality", quality)
}

enum class QualityIssue { NO_SKIN, TOO_DARK, TOO_BRIGHT, STRONG_COLOR_CAST }

sealed interface AnalysisResult {
    data class Success(
        val axes: Axes,
        val scores: List<ToneScore>,
        val features: Features,
        /**
         * 0..1: estimated chance the season is right, discounted by measurement quality.
         * Below ~0.6 the UI should suggest a draping check or a retake in daylight with white paper.
         */
        val confidence: Double = 1.0,
    ) : AnalysisResult {
        /** "spring" | "summer" | "autumn" | "winter" → summed tone probability, highest first. */
        val seasonScores: Map<String, Double>
            get() = scores.groupBy { it.toneId.substringBefore('_') }
                .mapValues { (_, v) -> v.sumOf { it.probability } }
                .entries.sortedByDescending { it.value }.associate { it.key to it.value }
    }
    data class Rejected(val issue: QualityIssue) : AnalysisResult
}

object PersonalColorAnalyzer {
    // ---- ponytail: calibration from tools/model/RESEARCH.md (literature + heuristics); retune with labeled data. ----
    // All L*/a*/b*/hue values are CIE Lab (D65).

    // Sample sizes / robustness
    private const val MIN_SKIN_PIXELS = 30
    private const val MIN_REGION_PIXELS = 8
    private const val MIN_FEATURE_PIXELS = 8
    private const val MIN_SCLERA_PIXELS = 12
    private const val MIN_WHITE_REF_PIXELS = 12
    private const val TRIM = 0.2                 // hair/iris/lips: drop darkest & brightest 20% by L*

    // Skin outlier rejection, relative to each region's median: shadows, speculars, blush/makeup, stray pixels.
    private const val SKIN_SHADOW_L = 8.0
    private const val SKIN_SPECULAR_L = 8.0
    private const val SKIN_BLUSH_C = 6.0
    private const val SKIN_MAX_DAB = 10.0        // max a*b* distance from region median
    private const val SHADOWED_REGION_L = 12.0   // whole region this much darker than the brightest one → in shadow, skip

    // Quality gates (on white-balanced skin L*)
    private const val TOO_DARK_L = 20.0
    private const val TOO_BRIGHT_L = 90.0

    // White balance. Paper ≈ slightly bluish white; sclera mapped to a slightly warm white (eye whites are naturally yellowish).
    private val SCLERA_TARGET = Lab(85.0, 1.5, 5.0)
    private const val SCLERA_GAIN_MIN = 0.6     // linear-light gains, green anchored at 1
    private const val SCLERA_GAIN_MAX = 1.7
    private const val SCLERA_CAST_REJECT = 2.4   // raw max/min gain beyond this = cast too strong to correct
    private const val PAPER_GAIN_MIN = 0.45      // paper is trusted more: wider correction range
    private const val PAPER_GAIN_MAX = 2.2
    private const val PAPER_CAST_REJECT = 3.5
    private const val PAPER_CLIPPED = 0.97       // linear channel this bright = blown out, colour unknown
    private const val PAPER_TOO_DARK = 0.05

    // Lighting evenness (cheek-to-cheek L* difference): starts costing quality at 6, max penalty at 20.
    private const val ASYM_OK = 6.0
    private const val ASYM_BAD = 20.0

    // Lips with chroma above this are probably lipstick/tint.
    private const val LIP_MAKEUP_C = 42.0

    // Rule-based axes (for the explanation UI). term = (x − CENTER) / SCALE clamped to [-1, 1].
    // Centers ≈ Korean population means, scales ≈ spread between tone groups (RESEARCH.md §5).
    private const val SKIN_HUE_CENTER = 56.0     // h°: ~51 pink/cool … ~61 yellow/warm
    private const val SKIN_HUE_SCALE = 5.0
    private const val SKIN_BA_CENTER = 5.0       // b* − a*: ~3 cool … ~8 warm
    private const val SKIN_BA_SCALE = 3.0
    private const val LIP_HUE_CENTER = 33.0      // bluish-pink ~25° … coral ~43° (bare lips)
    private const val LIP_HUE_SCALE = 10.0
    private const val W_HUE = 0.6
    private const val W_BA = 0.25
    private const val W_LIP = 0.15
    private const val SKIN_L_CENTER = 64.5
    private const val SKIN_L_SCALE = 3.5
    private const val FEATURE_L_CENTER = 16.0    // darkest of hair/iris
    private const val FEATURE_L_SCALE = 6.0
    private const val W_FEATURE_VALUE = 0.4
    private const val SKIN_CHROMA_CENTER = 18.5
    private const val SKIN_CHROMA_SCALE = 2.0
    private const val CONTRAST_CENTER = 47.0
    private const val CONTRAST_SCALE = 5.0
    private const val W_CONTRAST = 0.5

    // Feature reliability: hair/iris must be clearly darker than skin and not skin-coloured; hair must also be
    // one consistent color (otherwise it's probably background, a hat, or forehead).
    private const val HAIR_MIN_DARKER_L = 10.0
    private const val HAIR_MIN_DE_FROM_SKIN = 15.0
    private const val HAIR_MAX_SPREAD = 12.0     // median ΔE76 from the patch's median color
    private const val IRIS_MIN_DARKER_L = 10.0

    private val scleraTargetRatio = ColorMath.labToLinear(SCLERA_TARGET).let { t -> DoubleArray(3) { t[it] / t[1] } }
    // Office/printer paper has optical brighteners: slightly bluish, not neutral (RESEARCH.md §6).
    private val PAPER_TARGET = Lab(94.0, 0.5, -3.0)
    private val paperTargetRatio = ColorMath.labToLinear(PAPER_TARGET).let { t -> DoubleArray(3) { t[it] / t[1] } }

    fun analyze(samples: FaceSamples): AnalysisResult {
        if (samples.skin.size < MIN_SKIN_PIXELS) return AnalysisResult.Rejected(QualityIssue.NO_SKIN)
        val (gains, wbSource) = whiteBalance(samples) ?: return AnalysisResult.Rejected(QualityIssue.STRONG_COLOR_CAST)

        val regions = samples.skinRegions.ifEmpty { mapOf("all" to samples.skin) }
            .mapValues { (_, px) -> cleanSkin(px, gains) }
            .filterValues { it.size >= MIN_REGION_PIXELS }
        if (regions.isEmpty()) return AnalysisResult.Rejected(QualityIssue.NO_SKIN)
        val regionL = regions.mapValues { (_, labs) -> median(labs.map { it.l }) }
        val brightest = regionL.values.max()
        val lit = regions.filterKeys { regionL.getValue(it) > brightest - SHADOWED_REGION_L }.values.flatten()
        val skin = medianLab(lit)
        if (skin.l < TOO_DARK_L) return AnalysisResult.Rejected(QualityIssue.TOO_DARK)
        if (skin.l > TOO_BRIGHT_L) return AnalysisResult.Rejected(QualityIssue.TOO_BRIGHT)
        val asymmetry = if ("cheekL" in regionL && "cheekR" in regionL) abs(regionL.getValue("cheekL") - regionL.getValue("cheekR")) else 0.0

        val hair = hair(samples, gains, skin)
        val iris = robust(samples.iris, gains)
            ?.takeIf { it.n >= MIN_FEATURE_PIXELS && it.lab.l < skin.l - IRIS_MIN_DARKER_L }?.lab
        val lips = robust(samples.lips, gains)?.takeIf { it.n >= MIN_FEATURE_PIXELS }?.lab
        val lipMakeup = lips != null && lips.chroma > LIP_MAKEUP_C

        val darkestFeatureL = listOfNotNull(hair?.l, iris?.l).minOrNull()
        val contrast = darkestFeatureL?.let { skin.l - it } ?: 0.0

        var quality = when (wbSource) { WhiteBalanceSource.WHITE_REF -> 1.0; WhiteBalanceSource.SCLERA -> 0.9; WhiteBalanceSource.NONE -> 0.7 }
        quality *= 1 - 0.5 * ((asymmetry - ASYM_OK) / (ASYM_BAD - ASYM_OK)).coerceIn(0.0, 1.0)
        if (hair == null) quality *= 0.9
        if (iris == null) quality *= 0.95

        val features = Features(skin, iris, hair, lips, gains, contrast, wbSource, asymmetry, lipMakeup, quality)
        val scores = ToneModel.predict(features)
        val seasonTop = scores.groupBy { it.toneId.substringBefore('_') }.values.maxOf { g -> g.sumOf { it.probability } }
        return AnalysisResult.Success(ruleAxes(features), scores, features, confidence = quality * seasonTop)
    }

    /**
     * Several photos of the same person: quality-weighted mean of tone probabilities and axes
     * (lighting noise partly cancels out). Features/confidence come from the best photo, confidence nudged up.
     */
    fun combine(results: List<AnalysisResult.Success>): AnalysisResult.Success {
        require(results.isNotEmpty())
        if (results.size == 1) return results[0]
        val w = results.map { max(it.features.quality, 1e-3) }
        val total = w.sum()
        val probs = ToneClassifier.TONE_IDS.associateWith { id ->
            results.indices.sumOf { i -> w[i] * results[i].scores.first { it.toneId == id }.probability } / total
        }
        fun avg(f: (Axes) -> Double) = results.indices.sumOf { i -> w[i] * f(results[i].axes) } / total
        val best = results.maxBy { it.features.quality }
        val scores = probs.map { ToneScore(it.key, it.value) }.sortedByDescending { it.probability }
        val seasonTop = scores.groupBy { it.toneId.substringBefore('_') }.values.maxOf { g -> g.sumOf { it.probability } }
        val quality = (best.features.quality + 0.05 * (results.size - 1)).coerceAtMost(1.0)
        return AnalysisResult.Success(Axes(avg { it.warmth }, avg { it.value }, avg { it.clarity }), scores, best.features, quality * seasonTop)
    }

    /** Explainable warmth / value / clarity axes (the model decides the tone; these drive the UI's sliders). */
    fun ruleAxes(f: Features): Axes {
        val skin = f.skin
        val hueTerm = term(skin.hue, SKIN_HUE_CENTER, SKIN_HUE_SCALE)
        val baTerm = term(skin.b - skin.a, SKIN_BA_CENTER, SKIN_BA_SCALE)
        val warmth = if (f.lips != null && !f.lipMakeup) {
            W_HUE * hueTerm + W_BA * baTerm + W_LIP * term(ToneModel.signedHue(f.lips), LIP_HUE_CENTER, LIP_HUE_SCALE)
        } else {
            (W_HUE * hueTerm + W_BA * baTerm) / (W_HUE + W_BA)
        }
        val darkestFeatureL = listOfNotNull(f.hair?.l, f.iris?.l).minOrNull()
        val skinValue = term(skin.l, SKIN_L_CENTER, SKIN_L_SCALE)
        val value = if (darkestFeatureL == null) skinValue
        else (1 - W_FEATURE_VALUE) * skinValue + W_FEATURE_VALUE * term(darkestFeatureL, FEATURE_L_CENTER, FEATURE_L_SCALE)
        val chromaTerm = term(skin.chroma, SKIN_CHROMA_CENTER, SKIN_CHROMA_SCALE)
        val clarity = if (darkestFeatureL == null) chromaTerm
        else (1 - W_CONTRAST) * chromaTerm + W_CONTRAST * term(f.contrast, CONTRAST_CENTER, CONTRAST_SCALE)
        return Axes(warmth.coerceIn(-1.0, 1.0), value.coerceIn(-1.0, 1.0), clarity.coerceIn(-1.0, 1.0))
    }

    /** Priority: white paper > sclera > none. Null when the cast is too strong to trust. */
    private fun whiteBalance(s: FaceSamples): Pair<DoubleArray, WhiteBalanceSource>? {
        paperNeutral(s.whiteRef)?.let { m ->
            val g = gains(m, paperTargetRatio, PAPER_GAIN_MIN, PAPER_GAIN_MAX, PAPER_CAST_REJECT) ?: return null
            return g to WhiteBalanceSource.WHITE_REF
        }
        val m = scleraNeutral(s.sclera) ?: return doubleArrayOf(1.0, 1.0, 1.0) to WhiteBalanceSource.NONE
        val g = gains(m, scleraTargetRatio, SCLERA_GAIN_MIN, SCLERA_GAIN_MAX, SCLERA_CAST_REJECT) ?: return null
        return g to WhiteBalanceSource.SCLERA
    }

    /** Linear-light gains (green anchored at 1) that map measured neutral [m] to the target channel ratios. */
    private fun gains(m: DoubleArray, target: DoubleArray, min: Double, max: Double, reject: Double): DoubleArray? {
        val raw = DoubleArray(3) { c -> target[c] / (m[c] / m[1]) }
        if (raw.max() / raw.min() > reject) return null
        return DoubleArray(3) { raw[it].coerceIn(min, max) }
    }

    /** Median linear color of the paper, or null if there's no usable paper (too few pixels, blown out, too dark). */
    private fun paperNeutral(px: List<Rgb>): DoubleArray? {
        if (px.size < MIN_WHITE_REF_PIXELS) return null
        // Brightest half: drops fingers, shadow, the disk spilling off the paper's edge.
        val m = linearMedian(px.map(::linear).sortedByDescending { it.sum() }.take(px.size / 2))
        return m.takeIf { it.max() <= PAPER_CLIPPED && it.min() >= PAPER_TOO_DARK }
    }

    /** Median linear color of the clean eye white, or null if there's too little / it's pitch black. */
    private fun scleraNeutral(px: List<Rgb>): DoubleArray? {
        if (px.size < MIN_SCLERA_PIXELS) return null
        val lin = px.map(::linear)
        // Brighter half drops lashes / iris edge / lid shadow. Of that, the half closest to its median chromaticity
        // drops red vessels. (Picking the least *saturated* half instead would bias the estimate toward grey under a cast.)
        val bright = lin.sortedByDescending { it.sum() }.take(lin.size / 2).filter { it[1] >= 1e-3 }
        if (bright.isEmpty()) return null
        val m0 = linearMedian(bright)
        if (m0[1] < 1e-3) return null
        val neutral = bright.sortedBy { sq(it[0] / it[1] - m0[0] / m0[1]) + sq(it[2] / it[1] - m0[2] / m0[1]) }
            .take(max(bright.size / 2, 1))
        return linearMedian(neutral).takeIf { m -> m.all { it >= 1e-3 } }
    }

    /** Skin pixels of one region with shadows, speculars, blush/makeup patches and strays removed. */
    private fun cleanSkin(px: List<Rgb>, gains: DoubleArray): List<Lab> {
        if (px.isEmpty()) return emptyList()
        val labs = px.map { toLab(it, gains) }
        val m = medianLab(labs)
        val mc = m.chroma
        return labs.filter {
            it.l >= m.l - SKIN_SHADOW_L && it.l <= m.l + SKIN_SPECULAR_L &&
                it.chroma <= mc + SKIN_BLUSH_C && sqrt(sq(it.a - m.a) + sq(it.b - m.b)) <= SKIN_MAX_DAB
        }
    }

    /** Best hair estimate from the patches that look like hair (dark, uniform, not skin-coloured); null if none do. */
    private fun hair(s: FaceSamples, gains: DoubleArray, skin: Lab): Lab? {
        val good = s.hairPatches.ifEmpty { listOf(s.hair) }.mapNotNull { robust(it, gains) }.filter {
            it.n >= MIN_FEATURE_PIXELS && it.lab.l < skin.l - HAIR_MIN_DARKER_L &&
                it.spread < HAIR_MAX_SPREAD && deltaE(it.lab, skin) > HAIR_MIN_DE_FROM_SKIN
        }
        return if (good.isEmpty()) null else medianLab(good.map { it.lab })
    }

    private class Region(val lab: Lab, val spread: Double, val n: Int)

    /** Trim by L* then component-wise median; spread = median ΔE76 from that median. */
    private fun robust(px: List<Rgb>, gains: DoubleArray): Region? {
        if (px.isEmpty()) return null
        val labs = px.map { toLab(it, gains) }.sortedBy { it.l }
        val cut = (labs.size * TRIM).toInt()
        val core = labs.subList(cut, labs.size - cut)
        val m = medianLab(core)
        return Region(m, median(core.map { deltaE(it, m) }), px.size)
    }

    private fun linear(c: Rgb) = doubleArrayOf(ColorMath.toLinear(c.r), ColorMath.toLinear(c.g), ColorMath.toLinear(c.b))
    private fun linearMedian(xs: List<DoubleArray>) = DoubleArray(3) { c -> median(xs.map { it[c] }) }
    private fun toLab(c: Rgb, g: DoubleArray) =
        ColorMath.linearToLab(ColorMath.toLinear(c.r) * g[0], ColorMath.toLinear(c.g) * g[1], ColorMath.toLinear(c.b) * g[2])

    private fun medianLab(xs: List<Lab>) = Lab(median(xs.map { it.l }), median(xs.map { it.a }), median(xs.map { it.b }))
    private fun median(xs: List<Double>): Double {
        val s = xs.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2
    }

    private fun deltaE(x: Lab, y: Lab) = sqrt(sq(x.l - y.l) + sq(x.a - y.a) + sq(x.b - y.b))
    private fun term(x: Double, center: Double, scale: Double) = ((x - center) / scale).coerceIn(-1.0, 1.0)
    private fun sq(x: Double) = x * x
}
