package com.gun.personalcolor.feature.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameBudgetTest {
    /** 1080×2340 phone: the budget RemoteViews allows for one update. */
    private val phone = 1080L * 2340 * 4 * 3 / 2

    @Test fun phoneWidgetBreathesWithAllThreePhases() =
        assertEquals(3 to 1f, frameBudget(900, 630, phone))

    @Test fun bigWidgetFallsBackToTheStill() {
        // 4 phone screens of pixels per 3 frames would exceed the budget; one still fits.
        assertEquals(1 to 1f, frameBudget(1080, 1600, phone))
    }

    @Test fun stillOverTheBudgetRendersSmallerAndFits() {
        val (count, scale) = frameBudget(2560, 2560, phone)
        assertEquals(1, count)
        assertTrue(scale in 0.1f..0.99f)
        val bytes = (2560 * scale).toLong() * (2560 * scale).toLong() * 4
        assertTrue(bytes <= phone)
    }

    @Test fun degenerateSizesNeverThrow() {
        assertEquals(1 to 1f, frameBudget(0, 0, phone))
        assertEquals(1 to 1f, frameBudget(100, 100, 0))
    }
}
