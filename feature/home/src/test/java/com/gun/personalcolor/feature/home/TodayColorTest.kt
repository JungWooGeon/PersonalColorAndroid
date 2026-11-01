package com.gun.personalcolor.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodayColorTest {
    @Test fun emptyPaletteGivesNullInsteadOfCrashing() = assertNull(todayColor(emptyList<String>(), 200))

    @Test fun rotatesDaily() {
        val c = listOf("a", "b", "c")
        assertEquals("a", todayColor(c, 3))
        assertEquals("b", todayColor(c, 4))
        assertEquals("c", todayColor(c, 365))
    }
}
