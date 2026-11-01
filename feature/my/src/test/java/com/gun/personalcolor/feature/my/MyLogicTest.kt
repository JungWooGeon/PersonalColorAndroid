package com.gun.personalcolor.feature.my

import com.gun.personalcolor.core.model.HistoryEntry
import com.gun.personalcolor.core.model.ResultSource
import com.gun.personalcolor.core.ui.WallpaperRenderer.Style
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyLogicTest {
    private fun e(tone: String, time: Long) = HistoryEntry(tone, ResultSource.QUIZ, time, emptyList())

    @Test fun visibleEntries_newestFirst_dropsUnknownTones_andDuplicateTimes() {
        val out = visibleEntries(listOf(e("a", 1), e("gone", 5), e("a", 3), e("b", 3), e("b", 2)), setOf("a", "b"))
        assertEquals(listOf(3L, 2L, 1L), out.map { it.timeMillis })
    }

    @Test fun visibleEntries_empty() = assertTrue(visibleEntries(emptyList(), setOf("a")).isEmpty())

    @Test fun visibleEntries_many() {
        val many = (1..5000L).map { e("a", it) }
        assertEquals(5000L, visibleEntries(many, setOf("a")).first().timeMillis)
    }

    @Test fun styleCodec_roundTrips() {
        Style.entries.forEach { s ->
            listOf(false, true).forEach { dark ->
                assertEquals(s to (dark && s.supportsDark), decodeStyle(encodeStyle(s, dark)))
            }
        }
    }

    @Test fun styleCodec_badInputFallsBackToTinty() {
        assertEquals(Style.TINTY to false, decodeStyle(""))
        assertEquals(Style.TINTY to false, decodeStyle("GRADIENT")) // old default that no longer exists
        assertEquals(Style.TINTY to true, decodeStyle("NOPE_DARK"))
    }

    @Test fun portrait_neverZero_andShortSideFirst() {
        assertEquals(1080 to 2400, portrait(2400, 1080))
        assertEquals(1 to 1, portrait(0, 0))
        assertEquals(1 to 500, portrait(-3, 500))
    }

    @Test fun liveModeShowsOnlyAnimatedStyles_tintyFirst() {
        val live = WallpaperUiState(live = true).styles
        assertEquals(Style.TINTY, live.first())
        assertTrue(live.all { it.animated })
        assertFalse(Style.PALETTE in live)
        assertEquals(Style.entries.size, WallpaperUiState().styles.size)
    }

    @Test fun liveChoice_light_onlyFalseForRealDarkVariant() {
        assertTrue(LiveChoice(Style.TINTY, dark = false, toneId = null).light)
        assertFalse(LiveChoice(Style.TINTY, dark = true, toneId = null).light)
        Style.entries.filter { !it.supportsDark }.forEach { assertTrue(LiveChoice(it, dark = true, toneId = null).light) }
    }
}
