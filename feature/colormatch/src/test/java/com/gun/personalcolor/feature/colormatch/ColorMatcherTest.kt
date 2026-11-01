package com.gun.personalcolor.feature.colormatch

import com.gun.personalcolor.core.analysis.Lab
import com.gun.personalcolor.core.model.Accessory
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.model.Verdict
import com.gun.personalcolor.core.model.Fashion
import com.gun.personalcolor.core.model.Hair
import com.gun.personalcolor.core.model.Makeup
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMatcherTest {
    // Sharma, Wu & Dalal (2005) test data: pairs 1, 7, 13, 17, 25, 33.
    @Test fun ciede2000MatchesPublishedPairs() {
        listOf(
            Triple(Lab(50.0, 2.6772, -79.7751), Lab(50.0, 0.0, -82.7485), 2.0425),
            Triple(Lab(50.0, 0.0, 0.0), Lab(50.0, -1.0, 2.0), 2.3669),
            Triple(Lab(50.0, 2.4900, -0.0010), Lab(50.0, -2.4900, 0.0009), 7.1792),
            Triple(Lab(50.0, 2.5, 0.0), Lab(73.0, 25.0, -18.0), 27.1492),
            Triple(Lab(60.2574, -34.0099, 36.2677), Lab(60.4626, -34.1751, 39.4387), 1.2644),
            Triple(Lab(2.0776, 0.0795, -1.1350), Lab(0.9033, -0.0636, -0.5514), 0.9082),
        ).forEach { (a, b, expected) ->
            assertEquals(expected, deltaE2000(a, b), 1e-4)
            assertEquals(expected, deltaE2000(b, a), 1e-4) // symmetric
        }
    }

    private fun tone(id: String, best: List<String>, worst: List<String>) = Tone(
        id, "autumn", id, id, emptyList(), "", "",
        best.mapIndexed { i, h -> NamedColor("베스트$i", h) },
        worst.mapIndexed { i, h -> NamedColor("워스트$i", h) },
        Makeup("", emptyList(), "", emptyList(), "", emptyList(), ""),
        Hair(emptyList(), ""), Fashion("", emptyList()), Accessory("", ""), "",
    )

    private val autumn = tone("autumn", listOf("#C0603A", "#8A7650", "#6B4E3D"), listOf("#FF2D95", "#000000"))
    private val winter = tone("winter", listOf("#E0218A", "#0047AB", "#000000"), listOf("#C19A6B"))
    private val all = listOf(autumn, winter)

    @Test fun nearBestIsGood() {
        val v = ColorMatcher.evaluate(hexToRgb("#C2623C"), autumn, all)
        assertEquals(MatchLevel.GOOD, v.level)
        assertTrue(v.score >= 70)
        assertEquals("#C0603A", v.nearestBest.first().hex)
        assertEquals("autumn", v.topTones.first().tone.id)
    }

    @Test fun nearWorstIsAvoid() {
        val v = ColorMatcher.evaluate(hexToRgb("#FF3399"), autumn, all)
        assertEquals(MatchLevel.AVOID, v.level)
        assertTrue(v.score < 40)
        assertEquals("winter", v.topTones.first().tone.id)
    }

    @Test fun farFromEverythingIsNeutral() {
        val v = ColorMatcher.evaluate(hexToRgb("#7FA0B0"), autumn, all)
        assertEquals(MatchLevel.NEUTRAL, v.level)
        assertTrue(v.score in 40..69)
    }

    @Test fun closetKeepsStoredVerdictForSameToneAndReJudgesForNewTone() {
        val item = ClosetItem(1, "#C2623C", "베스트0 상의", "상의", "autumn", Verdict.OK, 50)
        val same = entryFor(item, all, "autumn")
        assertEquals(MatchLevel.NEUTRAL, same.level) // stored value kept
        assertEquals(50, same.score)
        assertTrue(!same.reEvaluated)
        val other = entryFor(item, all, "winter")
        assertTrue(other.reEvaluated)
        assertEquals("winter", other.refTone?.id)
        val expected = ColorMatcher.evaluate(hexToRgb(item.hex), winter, all)
        assertEquals(expected.level, other.level)
        assertEquals(expected.score, other.score)
    }

    @Test fun goodVerdictNamesTheMatchedColor() =
        assertEquals("베스트0", ColorMatcher.evaluate(hexToRgb("#C2623C"), autumn, all).nameHint)

    @Test fun hexRoundTrip() = assertEquals("#C0603A", hexToRgb("#c0603a").toHex())
}
