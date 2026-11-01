package com.gun.personalcolor.feature.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwatchEdgeTest {
    @Test fun outlinesOnlyNearBackground() {
        assertTrue(blendsIn(0.01f, 0.02f))   // black on dark
        assertTrue(blendsIn(0.97f, 1f))      // white on light
        assertFalse(blendsIn(0.4f, 0.02f))
    }
}
