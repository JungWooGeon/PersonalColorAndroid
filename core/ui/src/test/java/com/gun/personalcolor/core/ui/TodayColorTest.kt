package com.gun.personalcolor.core.ui

import com.gun.personalcolor.core.model.NamedColor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TodayColorTest {
    private val colors = List(12) { NamedColor("c$it", "#000000") }

    @Test fun picksByDayOfYear() {
        assertEquals("c1", todayOf(colors, LocalDate.of(2026, 1, 1)).name)   // day 1
        assertEquals("c0", todayOf(colors, LocalDate.of(2026, 1, 12)).name)  // day 12
    }

    @Test fun noColorsIsGreyNotACrash() = assertEquals("#9E9E9E", todayOf(emptyList(), LocalDate.of(2026, 11, 1)).hex)
}
