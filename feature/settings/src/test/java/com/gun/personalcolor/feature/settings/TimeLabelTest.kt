package com.gun.personalcolor.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeLabelTest {
    @Test fun twelveHourClock() {
        assertEquals("오전 12:00", timeLabel(0, 0))
        assertEquals("오전 8:05", timeLabel(8, 5))
        assertEquals("오후 12:30", timeLabel(12, 30))
        assertEquals("오후 11:59", timeLabel(23, 59))
    }
}
