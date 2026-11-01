package com.gun.personalcolor.core.ui.mood

import com.gun.personalcolor.core.model.Accessory
import com.gun.personalcolor.core.model.Fashion
import com.gun.personalcolor.core.model.Hair
import com.gun.personalcolor.core.model.Makeup
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class MoodCopyTest {
    @Test fun particleRoFollowsTheLastSyllable() {
        assertEquals("으로", MoodCopy.ro("와인"))   // ㄴ batchim
        assertEquals("로", MoodCopy.ro("코랄"))     // ㄹ batchim takes 로
        assertEquals("로", MoodCopy.ro("피치"))     // vowel
        assertEquals("으로", MoodCopy.ro("올리브 그린"))
        assertEquals("로", MoodCopy.ro("Navy"))     // not Hangul
        assertEquals("로", MoodCopy.ro(""))
    }

    @Test fun skyFromConditionAndHour() {
        assertEquals("night", MoodCopy.sky("맑음", 22))
        assertEquals("night", MoodCopy.sky("비", 5))
        assertEquals("sunny", MoodCopy.sky("맑음", 6))
        assertEquals("rain", MoodCopy.sky("이슬비", 12))
        assertEquals("rain", MoodCopy.sky("눈", 12))
        assertEquals("cloudy", MoodCopy.sky("구름 조금", 12))
        assertEquals("cloudy", MoodCopy.sky("흐림", 12))
        assertEquals("sunny", MoodCopy.sky(null, 12))
    }

    @Test fun lineTemplates() {
        assertEquals("오늘의 룩", MoodCopy.line("sunny", "맑음", 20, null))
        assertEquals("햇살 아래, 코랄 한 조각", MoodCopy.line("sunny", "맑음", 20, "코랄"))
        assertEquals("흐린 날엔 와인으로", MoodCopy.line("cloudy", "흐림", 12, "와인"))
        assertEquals("비 오는 날, 와인 한 방울", MoodCopy.line("rain", "소나기", 12, "와인"))
        assertEquals("눈 오는 날, 와인 한 방울", MoodCopy.line("rain", "눈", -2, "와인"))
        assertEquals("코랄로 따뜻하게", MoodCopy.line("cloudy", "흐림", 5, "코랄"))
        // Night follows the day's weather.
        assertEquals("흐린 날엔 피치로", MoodCopy.line("night", "흐림", 12, "피치"))
        assertEquals("햇살 아래, 피치 한 조각", MoodCopy.line("night", null, 12, "피치"))
    }

    @Test fun captionTemplates() {
        val at = LocalDateTime.of(2026, 11, 1, 9, 0)
        assertEquals("Nov 1 · 9° · rainy day look", MoodCopy.caption("rain", "비", 9, at))
        assertEquals("Nov 1 · 9° · snowy day look", MoodCopy.caption("rain", "눈", 9, at))
        assertEquals("Nov 1 · tonight", MoodCopy.caption("night", null, null, at))
        assertEquals("Nov 1 · -3° · cloudy day look", MoodCopy.caption("cloudy", null, -3, at))
        assertEquals("Nov 1 · 20° · sunny day look", MoodCopy.caption("sunny", null, 20, at))
    }

    @Test fun accentIsTheMostColorfulToneColorInTheLook() {
        val tone = tone(NamedColor("피치", "#FFB38A"), NamedColor("와인", "#722F37"), NamedColor("아이보리", "#F3EDE0"))
        val peach = 0xFFFFB38A.toInt(); val wine = 0xFF722F37.toInt(); val ivory = 0xFFF3EDE0.toInt()
        assertEquals("복숭아", MoodCopy.accent(tone, listOf("knit" to peach, "slacks" to wine))) // nickname
        assertEquals("와인", MoodCopy.accent(tone, listOf("knit" to ivory, "slacks" to wine)))
        assertNull(MoodCopy.accent(tone, listOf("knit" to ivory)))                 // too grey
        assertNull(MoodCopy.accent(tone, listOf("knit" to 0xFFFF0000.toInt())))    // not a tone color
        assertNull(MoodCopy.accent(tone, emptyList()))
        // A broken hex in the tone is skipped, not a crash.
        assertEquals("와인", MoodCopy.accent(tone(NamedColor("?", "oops"), NamedColor("와인", "#722F37")), listOf("knit" to wine)))
    }

    private fun tone(vararg best: NamedColor) = Tone(
        id = "t", season = "autumn", nameKo = "", nameEn = "", keywords = emptyList(), summary = "", characteristics = "",
        bestColors = best.toList(), worstColors = emptyList(),
        makeup = Makeup("", emptyList(), "", emptyList(), "", emptyList(), ""), hair = Hair(emptyList(), ""),
        fashion = Fashion("", emptyList()), accessory = Accessory("", ""), avoidTip = "",
    )

    // Every WMO label the weather module emits × temperature × hour: the line and caption never contradict the sky.
    @Test fun copyMatchesEveryWeather() {
        val expected = mapOf(
            "맑음" to ("햇살 아래" to "sunny"), "구름 조금" to ("구름 사이로" to "cloudy"), "흐림" to ("흐린 날" to "cloudy"),
            "안개" to ("흐린 날" to "cloudy"), "이슬비" to ("비 오는 날" to "rainy"), "비" to ("비 오는 날" to "rainy"),
            "소나기" to ("비 오는 날" to "rainy"), "뇌우" to ("비 오는 날" to "rainy"), "눈" to ("눈 오는 날" to "snowy"),
        )
        val at = LocalDateTime.of(2026, 11, 1, 12, 0)
        for ((cond, want) in expected) for (temp in listOf(-5, 5, 6, 15, 30)) for (hour in listOf(9, 14, 23)) {
            val sky = MoodCopy.sky(cond, hour)
            val line = MoodCopy.line(sky, cond, temp, "코랄")
            val caption = MoodCopy.caption(sky, cond, temp, at)
            val wet = want.second in setOf("rainy", "snowy")
            if (wet || temp > 5) assertEquals("$cond $temp° $hour h: $line", true, line.startsWith(want.first))
            else assertEquals("$cond $temp°: $line", "코랄로 따뜻하게", line)
            if (cond != "맑음") assertEquals(false, "햇살" in line)
            if (!wet) assertEquals(false, "오는 날" in line)
            assertEquals("$cond $hour h: $caption", true, caption.endsWith(if (hour >= 22) "tonight" else "${want.second} day look"))
        }
    }
}
