package com.gun.personalcolor.core.vision

import com.gun.personalcolor.core.analysis.Rgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GarmentColorTest {
    private fun argb(r: Int, g: Int, b: Int, a: Int = 255) = (a shl 24) or (r shl 16) or (g shl 8) or b
    private fun near(e: Rgb, a: Rgb, tol: Int = 8) =
        assertTrue("expected ~$e, got $a", abs(e.r - a.r) <= tol && abs(e.g - a.g) <= tol && abs(e.b - a.b) <= tol)

    @Test fun picksLargestClusterIgnoringShadowsHighlightsAndTransparency() {
        val px = List(600) { argb(200, 40, 40) } +          // red body
            List(200) { argb(30, 60, 160) } +               // blue print
            List(80) { argb(5, 5, 5) } + List(80) { argb(255, 255, 255) } + // shadow, highlight
            List(2000) { argb(0, 255, 0, 0) }               // transparent background
        near(Rgb(200, 40, 40), GarmentColor.dominant(px.toIntArray()))
    }

    @Test fun blackGarmentStaysBlack() {
        val px = List(500) { argb(20 + it % 6, 20, 22) } + List(50) { argb(120, 120, 120) }
        near(Rgb(22, 20, 22), GarmentColor.dominant(px.toIntArray()))
    }

    @Test fun nothingOpaqueIsGrey() = assertEquals(Rgb(128, 128, 128), GarmentColor.dominant(IntArray(10)))
}
