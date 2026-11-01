package com.gun.personalcolor.feature.coordi

import com.gun.personalcolor.core.model.Accessory
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.model.Fashion
import com.gun.personalcolor.core.model.Hair
import com.gun.personalcolor.core.model.Makeup
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.model.Verdict
import com.gun.personalcolor.core.weather.TodayWeather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OutfitEngineTest {
    private val tone = Tone(
        id = "autumn_true", season = "autumn", nameKo = "가을 웜", nameEn = "Autumn True",
        keywords = emptyList(), summary = "", characteristics = "",
        bestColors = listOf(
            NamedColor("테라코타", "#C0674A"), NamedColor("머스터드", "#D2A23A"), NamedColor("카멜", "#C19A6B"),
            NamedColor("올리브 그린", "#708238"), NamedColor("초콜릿 브라운", "#5A3A2A"), NamedColor("웜 아이보리", "#F3E9D2"),
        ),
        worstColors = listOf(NamedColor("네온 핑크", "#FF4FA3"), NamedColor("아이시 블루", "#D6ECF7")),
        makeup = Makeup("", listOf(NamedColor("브릭", "#A0453A")), "", listOf(NamedColor("브론즈", "#8C6239")), "",
            listOf(NamedColor("코랄", "#E9765B")), ""),
        hair = Hair(emptyList(), ""), fashion = Fashion("", emptyList()), accessory = Accessory("골드", ""), avoidTip = "",
    )
    private val moods = listOf(NamedColor("아이스 블루", "#CFE0EA"), NamedColor("카멜 브라운", "#A9794C"), NamedColor("네온 핑크", "#FF4FA3"))
    private val date = LocalDate.of(2026, 11, 1)

    private fun weather(feels: Double, rain: Int = 0, uv: Double = 2.0) =
        TodayWeather("마포구 망원동", feels, feels, feels - 3, feels + 3, rain, "맑음", 0, uv, 5.0, 0L)

    @Test fun deterministicPerDay() {
        val a = OutfitEngine.build(tone, weather(14.0), date, moods)
        assertEquals(a, OutfitEngine.build(tone, weather(14.0), date, moods))
        val variants = (0..5).map { OutfitEngine.build(tone, weather(14.0), date, moods, variant = it) }.toSet()
        assertTrue("variants should differ", variants.size > 1)
    }

    @Test fun layeringFollowsFeelsLike() {
        assertTrue(OutfitEngine.build(tone, weather(30.0), date, moods).slots.none { it.label == "아우터" })
        val cold = OutfitEngine.build(tone, weather(2.0), date, moods)
        assertTrue(cold.slots.first { it.label == "아우터" }.item.contains("패딩"))
        assertTrue("목도리" in cold.extras)
        assertTrue(OutfitEngine.build(tone, weather(14.0), date, moods).layerLine.contains("자켓·트렌치"))
    }

    @Test fun rainAndUv() {
        val rainy = OutfitEngine.build(tone, weather(15.0, rain = 70), date, moods)
        assertTrue("우산" in rainy.extras)
        assertTrue(rainy.slots.first { it.label == "아우터" }.item.contains("방수"))
        assertTrue(rainy.tip.contains("70%"))
        val sunny = OutfitEngine.build(tone, weather(25.0, uv = 8.0), date, moods)
        assertTrue("선글라스" in sunny.extras)
    }

    @Test fun seasonDefaultsWithoutWeather() {
        val winter = OutfitEngine.build(tone, null, LocalDate.of(2026, 1, 10), moods)
        assertTrue(winter.layerLine.startsWith("1월"))
        assertTrue(winter.slots.any { it.label == "아우터" })
    }

    @Test fun moodClosestToPaletteAvoidsWorst() {
        assertEquals("카멜 브라운", OutfitEngine.closestMood(moods, tone)!!.name)
    }

    @Test fun prefersClosetItems() {
        val closet = listOf(
            ClosetItem(1, "#C19A6B", "카멜 니트", "상의", "autumn_true", Verdict.GOOD, 90),
            ClosetItem(2, "#FF4FA3", "핑크 셔츠", "상의", "autumn_true", Verdict.AVOID, 10),
        )
        // Feels 18 → 니트 band; skip variants that pick a dress instead of top+bottom.
        val outfit = (0..10).map { OutfitEngine.build(tone, weather(18.0), date, moods, closet, it) }.first { o -> o.slots.any { it.label == "상의" } }
        val top = outfit.slots.first { it.label == "상의" }
        assertTrue(top.fromCloset)
        assertEquals("내 옷장의 '카멜 니트'", top.item)
        assertNotEquals("핑크 셔츠", top.color.name)
    }
}
