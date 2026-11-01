package com.gun.personalcolor.feature.colormatch

import com.gun.personalcolor.core.analysis.Rgb
import com.gun.personalcolor.core.model.Accessory
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.model.Fashion
import com.gun.personalcolor.core.model.Hair
import com.gun.personalcolor.core.model.Makeup
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.model.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression tests for corrupt / unusual data that used to crash the color check and closet. */
class ClosetSafetyTest {
    private fun tone(id: String, best: List<String>, worst: List<String>) = Tone(
        id, "autumn", id, id, emptyList(), "", "",
        best.mapIndexed { i, h -> NamedColor("베스트$i", h) },
        worst.mapIndexed { i, h -> NamedColor("워스트$i", h) },
        Makeup("", emptyList(), "", emptyList(), "", emptyList(), ""),
        Hair(emptyList(), ""), Fashion("", emptyList()), Accessory("", ""), "",
    )

    private val autumn = tone("autumn", listOf("#C0603A", "#8A7650"), listOf("#FF2D95"))

    @Test fun badHexFallsBackToGreyInsteadOfThrowing() {
        listOf("", "#", "#ZZZZZZ", "red", "not a color").forEach { assertEquals(Rgb(128, 128, 128), hexToRgb(it)) }
        assertEquals("#C0603A", hexToRgb("#80C0603A").toHex()) // #AARRGGBB keeps RGB, as before
    }

    @Test fun safeHexOnlyPassesWhatParseColorAccepts() {
        assertEquals("#C0603A", safeHex("#C0603A"))
        assertEquals("#80c0603a", safeHex("#80c0603a"))
        listOf("", "C0603A", "#C0603", "#GGGGGG", "red", "#C0603A ").forEach { assertEquals("#808080", safeHex(it)) }
    }

    @Test fun closetEntryWithBadHexIsShownGrey() {
        val e = entryFor(ClosetItem(1, "oops", "옷", "상의", "autumn", Verdict.GOOD, 80), listOf(autumn), "autumn")
        assertEquals("#808080", e.item.hex)
        assertEquals(MatchLevel.GOOD, e.level) // stored verdict kept
    }

    @Test fun closetEntryWithNullKindAndUnknownToneDoesNotThrow() {
        val e = entryFor(ClosetItem(2, "#C0603A", "옷", "기타", "gone", Verdict.OK, 50, kind = null), listOf(autumn), null)
        assertNull(e.refTone)
        assertNull(e.verdict)
        assertEquals(MatchLevel.NEUTRAL, e.level)
    }

    @Test fun toneWithoutPaletteIsNeutralNotACrash() {
        val empty = tone("empty", emptyList(), emptyList())
        val v = ColorMatcher.evaluate(Rgb(200, 100, 50), empty, listOf(empty, autumn))
        assertEquals(MatchLevel.NEUTRAL, v.level)
        assertTrue(v.nearestBest.isEmpty())
        assertEquals(listOf("autumn"), v.topTones.map { it.tone.id }) // palette-less tones are skipped
    }

    @Test fun scaledSideNeverHitsZero() {
        assertEquals(1, scaledSide(3, 0.01f))       // a thin panorama
        assertEquals(800, scaledSide(1600, 0.5f))
    }
}
