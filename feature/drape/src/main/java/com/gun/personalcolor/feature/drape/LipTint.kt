package com.gun.personalcolor.feature.drape

import com.gun.personalcolor.core.analysis.Lab
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Pure lipstick color math in Lab. The lipstick replaces the lip's colour but keeps its own lightness detail
 * (creases, shading, highlights), so it reads as pigment on a lip rather than a flat layer.
 */
internal object LipTint {
    /**
     * Lip statistics: median Lab, the lightness where highlights start (top ~8%) and peak,
     * and [light] = how bright the surrounding skin is vs a well-lit face (dim photo → < 1).
     */
    data class Stats(val l: Double, val a: Double, val b: Double, val hi: Double, val peak: Double, val light: Double)

    private const val REF_SKIN_L = 68.0

    fun stats(lip: List<Lab>, skinL: Double?): Stats {
        val ls = lip.map { it.l }.sorted()
        fun pct(p: Double) = ls[((ls.size - 1) * p).roundToInt()]
        return Stats(
            l = pct(0.5), a = lip.map { it.a }.sorted()[lip.size / 2], b = lip.map { it.b }.sorted()[lip.size / 2],
            hi = pct(0.92), peak = maxOf(pct(0.995), pct(0.92) + 1),
            light = skinL?.let { (it / REF_SKIN_L).coerceIn(0.6, 1.1) } ?: 1.0,
        )
    }

    /** 1 for lip-coloured pixels, fading to 0 for ones far from the lip (teeth, shadowed skin) that slipped into the polygon. */
    fun colorWeight(px: Lab, s: Stats): Double {
        val d = sqrt(sq((px.l - s.l) * 0.5) + sq(px.a - s.a) + sq(px.b - s.b))
        return 1 - smooth(14.0, 28.0, d)
    }

    /** The pixel [px] wearing lipstick [target]. */
    fun tint(px: Lab, s: Stats, target: Lab, glossy: Boolean): Lab {
        val detail = px.l - s.l
        // Lipstick lightness, dimmed like the skin around it, and only partly replacing the lip's own lightness.
        val base = s.l + (target.l * s.light - s.l) * 0.6
        val chroma = s.light.coerceIn(0.75, 1.0)
        var l: Double
        var a = target.a * chroma + (px.a - s.a) * 0.3
        var b = target.b * chroma + (px.b - s.b) * 0.3
        if (glossy) {
            // Keep the texture a touch crisper and turn existing highlights into a soft white sheen.
            l = base + detail * 1.05
            val t = smooth(s.hi, s.peak, px.l)
            l += t * (100 - l) * 0.45
            a *= 1 - 0.6 * t; b *= 1 - 0.6 * t
        } else {
            // Matte: flatter texture, highlights pressed down.
            l = base + detail * 0.85 - maxOf(0.0, px.l - s.hi) * 0.6
        }
        return Lab(l.coerceIn(0.0, 100.0), a, b)
    }

    /** Alpha (0..255) of the tinted pixel over the photo. */
    fun alpha(mask: Double, intensity: Double): Int = (mask * intensity * 255).roundToInt().coerceIn(0, 255)

    fun smooth(e0: Double, e1: Double, x: Double): Double {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0.0, 1.0)
        return t * t * (3 - 2 * t)
    }

    private fun sq(x: Double) = x * x
}
