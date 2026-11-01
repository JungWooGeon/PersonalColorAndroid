package com.gun.personalcolor.feature.coordi

import androidx.compose.ui.geometry.Rect
import com.gun.personalcolor.core.model.NamedColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LookBoardTest {
    private fun slot(label: String, kind: String?) = OutfitSlot(label, NamedColor("카멜", "#C19A6B"), "카멜 $label", kind = kind)

    private val combos = listOf(
        listOf(slot("아우터", "trench"), slot("상의", "knit"), slot("하의", "slacks"), slot("신발", "loafers"), slot("포인트", "scarf")),
        listOf(slot("상의", "tee"), slot("하의", "wide"), slot("신발", "shoes"), slot("포인트", "bag")),
        listOf(slot("원피스", "dress"), slot("신발", "sandals"), slot("포인트", "earrings")),
        listOf(slot("아우터", "coat"), slot("원피스", "knit_dress"), slot("신발", "long_boots"), slot("포인트", "umbrella")),
        listOf(slot("아우터", "windbreaker"), slot("상의", "longtee"), slot("하의", "jeans"), slot("신발", "rain_boots"), slot("포인트", "umbrella")),
        listOf(slot("상의", "tee"), slot("하의", "pants"), slot("신발", "shoes"), slot("포인트", null)),
    )

    @Test fun everyComboKeepsTheDrawnPartOnTheBoard() {
        // Art fills ~76% of its box (tops) up to ~92% (umbrella); the worst case is checked.
        val art = Rect(0.04f, 0.04f, 0.96f, 0.96f)
        combos.forEach { slots ->
            val placed = lookLayout(slots) { art }
            assertEquals(slots.size, placed.size)
            placed.forEach { p ->
                val l = p.cx - p.size / 2
                val t = p.cy - p.size / 2
                assertTrue("${p.slot.label} left", l + art.left * p.size >= 0.029f)
                assertTrue("${p.slot.label} right", l + art.right * p.size <= 0.971f)
                assertTrue("${p.slot.label} top", t + art.top * p.size >= 0.029f)
                assertTrue("${p.slot.label} bottom", t + art.bottom * p.size <= LOOK_ASPECT - 0.029f)
            }
        }
    }

    @Test fun outerSitsOverTheTopAndShoesOverTheBottom() {
        val z = lookLayout(combos[0]).associate { it.slot.label to it.z }
        assertTrue(z.getValue("아우터") > z.getValue("상의"))
        assertTrue(z.getValue("상의") > z.getValue("하의"))
        assertTrue(z.getValue("신발") > z.getValue("하의"))
        // Drawn in z order.
        assertEquals(lookLayout(combos[0]).map { it.z }, lookLayout(combos[0]).map { it.z }.sorted())
    }

    @Test fun josaFollowsTheFinalConsonant() {
        assertEquals("로", josa("카멜", "으로", "로"))
        assertEquals("으로", josa("브라운", "으로", "로"))
        assertEquals("로", josa("아이보리", "으로", "로"))
        assertEquals("이", josa("올리브 그린", "이", "가"))
        assertEquals("가", josa("네이비", "이", "가"))
        assertEquals("가", josa("'니트'", "이", "가"))
    }

    @Test fun stylingNoteNamesTheUmbrellaOnRainyDays() {
        assertTrue(stylingNote(combos[4], 3).contains("우산"))
        combos.forEach { slots -> repeat(5) { assertTrue(stylingNote(slots, it).isNotBlank()) } }
    }
}
