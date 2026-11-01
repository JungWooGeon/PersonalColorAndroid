package com.gun.personalcolor.core.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class TintyColorsTest {
    @Test fun mixEndpointsAndMidpoint() {
        val a = Color(0xFF000000); val b = Color(0xFFFFFFFF)
        assertEquals(a, mix(a, b, 0f))
        assertEquals(b, mix(a, b, 1f))
        assertEquals(0.5f, mix(a, b, 0.5f).red, 0.01f)
    }

    @Test fun builtInPalettesHaveFourChips() {
        assertEquals(4, TintyColors.Brand.scarf.size)
        assertEquals(4, TintyColors.Clear.scarf.size)
        assertEquals(4, PcColors.Seasons.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun wrongChipCountIsRejected() {
        TintyColors(Color.White, Color.White, listOf(Color.White), Color.White)
    }

}
