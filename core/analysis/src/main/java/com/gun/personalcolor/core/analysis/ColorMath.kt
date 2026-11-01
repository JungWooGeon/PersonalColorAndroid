package com.gun.personalcolor.core.analysis

import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt

/** 0..255 sRGB. */
data class Rgb(val r: Int, val g: Int, val b: Int)

/** CIE L*a*b*, D65. */
data class Lab(val l: Double, val a: Double, val b: Double) {
    val chroma: Double get() = hypot(a, b)
    /** Degrees in [0, 360). */
    val hue: Double get() = (Math.toDegrees(atan2(b, a)) + 360.0) % 360.0
}

object ColorMath {
    // D65 reference white
    private const val XN = 0.95047
    private const val YN = 1.0
    private const val ZN = 1.08883
    private const val EPS = 216.0 / 24389.0
    private const val KAPPA = 24389.0 / 27.0

    fun rgbToLab(c: Rgb): Lab = linearToLab(toLinear(c.r), toLinear(c.g), toLinear(c.b))

    fun labToRgb(c: Lab): Rgb {
        val (r, g, b) = labToLinear(c)
        return Rgb(toSrgb(r), toSrgb(g), toSrgb(b))
    }

    /** sRGB 0..255 → linear light 0..1. */
    internal fun toLinear(v: Int): Double {
        val s = v / 255.0
        return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun toSrgb(v: Double): Int {
        val c = v.coerceIn(0.0, 1.0)
        val s = if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055
        return (s * 255).roundToInt().coerceIn(0, 255)
    }

    internal fun linearToLab(r: Double, g: Double, b: Double): Lab {
        val x = 0.4124564 * r + 0.3575761 * g + 0.1804375 * b
        val y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
        val z = 0.0193339 * r + 0.1191920 * g + 0.9503041 * b
        val fx = f(x / XN)
        val fy = f(y / YN)
        val fz = f(z / ZN)
        return Lab(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }

    internal fun labToLinear(c: Lab): DoubleArray {
        val fy = (c.l + 16) / 116
        val x = XN * fInv(fy + c.a / 500)
        val y = YN * fInv(fy)
        val z = ZN * fInv(fy - c.b / 200)
        return doubleArrayOf(
            3.2404542 * x - 1.5371385 * y - 0.4985314 * z,
            -0.9692660 * x + 1.8760108 * y + 0.0415560 * z,
            0.0556434 * x - 0.2040259 * y + 1.0572252 * z,
        )
    }

    private fun f(t: Double) = if (t > EPS) cbrt(t) else (KAPPA * t + 16) / 116
    private fun fInv(t: Double) = if (t * t * t > EPS) t * t * t else (116 * t - 16) / KAPPA
}
