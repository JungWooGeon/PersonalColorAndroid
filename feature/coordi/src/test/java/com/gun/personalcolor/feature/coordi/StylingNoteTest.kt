package com.gun.personalcolor.feature.coordi

import com.gun.personalcolor.core.model.NamedColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StylingNoteTest {
    private fun slot(label: String, kind: String?) = OutfitSlot(label, NamedColor("카멜", "#C19A6B"), "카멜 $label", kind = kind)

    private val combos = listOf(
        listOf(slot("아우터", "trench"), slot("상의", "knit"), slot("하의", "slacks"), slot("신발", "loafers"), slot("가방", "shoulder_bag")),
        listOf(slot("상의", "tee"), slot("하의", "wide"), slot("신발", "shoes"), slot("가방", "bag")),
        listOf(slot("원피스", "dress"), slot("신발", "sandals"), slot("가방", "mini_bag")),
        listOf(slot("아우터", "coat"), slot("원피스", "knit_dress"), slot("신발", "long_boots"), slot("가방", "hobo"), slot("소품", "umbrella")),
        listOf(slot("아우터", "windbreaker"), slot("상의", "longtee"), slot("하의", "jeans"), slot("신발", "rain_boots"), slot("가방", "bag"), slot("소품", "umbrella")),
        listOf(slot("상의", "tee"), slot("하의", "pants"), slot("신발", "shoes"), slot("포인트", null)),
        listOf(slot("아우터", "coat"), slot("상의", "knit"), slot("하의", "long_skirt"), slot("신발", "long_boots"), slot("가방", "shoulder_bag"), slot("소품", "muffler")),
    )

    @Test fun josaFollowsTheFinalConsonant() {
        assertEquals("로", josa("카멜", "으로", "로"))
        assertEquals("으로", josa("브라운", "으로", "로"))
        assertEquals("로", josa("아이보리", "으로", "로"))
        assertEquals("이", josa("올리브 그린", "이", "가"))
        assertEquals("가", josa("네이비", "이", "가"))
        assertEquals("가", josa("'니트'", "이", "가"))
        assertEquals("가", josa("", "이", "가"))
        assertEquals("로", josa("Navy", "으로", "로"))
    }

    @Test fun stylingNoteNamesTheUmbrellaOnRainyDays() {
        assertTrue((0..3).any { stylingNote(combos[4], it).contains("우산") })
        combos.forEach { slots -> repeat(5) { assertTrue(stylingNote(slots, it).isNotBlank()) } }
    }

    @Test fun stylingNoteNeverCrashesOnOddInput() {
        assertTrue(stylingNote(emptyList(), 0).isNotBlank())
        assertTrue(stylingNote(combos[0], Int.MIN_VALUE).isNotBlank())
        assertTrue(stylingNote(listOf(slot("신발", "shoes")), -7).isNotBlank())
    }

    // Regression: "흐린 날일수록…" on a sunny day. The umbrella can mean rain later or snow, so no line names the sky.
    @Test fun stylingNoteNeverNamesTheWeather() {
        val sky = listOf("흐린", "맑은", "비 오는", "눈 오는", "햇살", "구름")
        combos.forEach { slots -> repeat(6) { seed ->
            val note = stylingNote(slots, seed)
            assertTrue(note, sky.none { it in note })
        } }
    }

    @Test fun neutralTopNeverBringsOutTheFace() {
        val grey = listOf(OutfitSlot("원피스", NamedColor("그레이", "#A3A6AD"), "그레이 니트 원피스", kind = "knit_dress"),
            OutfitSlot("가방", NamedColor("코발트 블루", "#0047AB"), "코발트 블루 숄더백", kind = "shoulder_bag"))
        repeat(6) { assertTrue(stylingNote(grey, it), "살려" !in stylingNote(grey, it)) }
    }
}
