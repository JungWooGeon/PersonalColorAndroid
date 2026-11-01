package com.gun.personalcolor.feature.drape

import com.gun.personalcolor.core.analysis.Lab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LipTintTest {
    private val lip = (0 until 100).map { Lab(45.0 + it * 0.2, 25.0, 12.0) } // L 45..64.8
    private val s = LipTint.stats(lip, skinL = 68.0)
    private val red = Lab(40.0, 60.0, 35.0)

    @Test fun keepsLightnessDetail() {
        val dark = LipTint.tint(Lab(s.l - 5, 25.0, 12.0), s, red, glossy = false)
        val mid = LipTint.tint(Lab(s.l, 25.0, 12.0), s, red, glossy = false)
        assertEquals(5 * 0.85, mid.l - dark.l, 1e-6)
    }

    @Test fun takesLipstickHueAndMovesLightnessPartly() {
        val t = LipTint.tint(Lab(s.l, s.a, s.b), s, red, glossy = false)
        assertEquals(red.a, t.a, 1e-6); assertEquals(red.b, t.b, 1e-6)
        assertTrue(t.l < s.l && t.l > red.l)
    }

    @Test fun glossyBrightensHighlightsMatteDoesNot() {
        val hl = Lab(s.peak, 25.0, 12.0)
        assertTrue(LipTint.tint(hl, s, red, glossy = true).l > LipTint.tint(hl, s, red, glossy = false).l + 5)
    }

    @Test fun dimPhotoDarkensLipstick() {
        val dim = LipTint.stats(lip, skinL = 45.0)
        assertTrue(LipTint.tint(Lab(s.l, 25.0, 12.0), dim, red, false).l < LipTint.tint(Lab(s.l, 25.0, 12.0), s, red, false).l)
    }

    @Test fun zeroMaskOrIntensityIsIdentity() {
        assertEquals(0, LipTint.alpha(0.0, 1.0))
        assertEquals(0, LipTint.alpha(1.0, 0.0))
        assertEquals(255, LipTint.alpha(1.0, 1.0))
    }

    @Test fun teethGetLowWeight() {
        assertEquals(1.0, LipTint.colorWeight(Lab(s.l, s.a, s.b), s), 1e-9)
        assertTrue(LipTint.colorWeight(Lab(85.0, 0.0, 8.0), s) < 0.3)
    }
}
