package com.gun.personalcolor.core.ads

import com.gun.personalcolor.core.ads.AdManager.Companion.BACK_TO_BACK_GAP_MS
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPacingTest {
    private var now = 1_000_000L
    private val pacing = AdPacing { now }

    @Test fun firstEverInterstitialIsDue() = assertTrue(pacing.interstitialDue())

    @Test fun backToBackGapSkipsInterstitial() {
        pacing.onFullScreenShown()
        assertFalse(pacing.interstitialDue())
        now += BACK_TO_BACK_GAP_MS
        assertFalse(pacing.interstitialDue()) // exactly at the gap: still too soon
        now += 1
        assertTrue(pacing.interstitialDue())
    }

    @Test fun lockedUntilRewarded() {
        assertFalse(pacing.isUnlocked(AdManager.KEY_PHOTO))
        assertFalse(pacing.consumeUnlock(AdManager.KEY_PHOTO))
    }

    @Test fun unlockWindowExpires() {
        val untilMidnight = 3 * 60 * 60_000L
        pacing.unlock(AdManager.KEY_COORDI, untilMidnight)
        now += untilMidnight - 1
        assertTrue(pacing.isUnlocked(AdManager.KEY_COORDI))
        now += 1
        assertFalse(pacing.isUnlocked(AdManager.KEY_COORDI))
    }

    @Test fun unlocksArePerKey() {
        pacing.unlock("lip", AdManager.SESSION_UNLOCK_MS)
        assertTrue(pacing.isUnlocked("lip"))
        assertFalse(pacing.isUnlocked(AdManager.KEY_PHOTO))
    }

    @Test fun sessionUnlockDoesNotOverflow() {
        pacing.unlock("lip", AdManager.SESSION_UNLOCK_MS)
        now += 365L * 24 * 60 * 60_000
        assertTrue(pacing.isUnlocked("lip"))
    }

    @Test fun consumeReturnsTrueOnlyOnceAfterReward() {
        pacing.unlock(AdManager.KEY_PHOTO, AdManager.PHOTO_UNLOCK_MS)
        assertTrue(pacing.consumeUnlock(AdManager.KEY_PHOTO))
        assertFalse(pacing.isUnlocked(AdManager.KEY_PHOTO))
        assertFalse(pacing.consumeUnlock(AdManager.KEY_PHOTO))
    }
}
