package com.gun.personalcolor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetAccessTest {
    private val day = 24 * 60 * 60_000L
    private val now = 100 * day

    @Test fun trialNotStartedIsOpen() = assertTrue(coordiOpen(null, now, unlocked = false))
    @Test fun trialDaySevenIsOpen() = assertTrue(coordiOpen(now - 7 * day + 1, now, unlocked = false))
    @Test fun trialOverIsLocked() = assertFalse(coordiOpen(now - 7 * day, now, unlocked = false))
    @Test fun trialOverButUnlockedIsOpen() = assertTrue(coordiOpen(now - 30 * day, now, unlocked = true))
}
