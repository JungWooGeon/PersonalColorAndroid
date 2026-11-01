package com.gun.personalcolor.core.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskMathTest {
    @Test fun softenKeepsInteriorAndFeathersEdge() {
        val w = 20; val h = 4
        // Left half person (0.9), right half background (0.1).
        val conf = FloatArray(w * h) { if (it % w < 10) 0.9f else 0.1f }
        val a = MaskMath.soften(conf, w, h, radius = 2)
        assertEquals(1f, a[0], 1e-4f)          // deep inside: fully opaque
        assertEquals(0f, a[w - 1], 1e-4f)      // deep outside: fully transparent
        assertTrue(a[9] in 0.01f..0.99f)       // edge: partial
        assertTrue(a[8] > a[9] && a[9] > a[10]) // monotonic falloff
    }

    @Test fun boxBlurPreservesUniform() {
        val a = MaskMath.boxBlur(FloatArray(12) { 0.5f }, 4, 3, 1, horizontal = false)
        a.forEach { assertEquals(0.5f, it, 1e-6f) }
    }
}
