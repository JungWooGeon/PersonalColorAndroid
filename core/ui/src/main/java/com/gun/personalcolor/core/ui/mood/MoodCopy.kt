package com.gun.personalcolor.core.ui.mood

import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.hexColor
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The mood scenes' words: weather → sky, the short Korean line and the English caption (same templates as the widgets). */
object MoodCopy {
    /** Condition ("이슬비", "구름 조금", "맑음") + hour → sunny|cloudy|rain|night. Night is 22–6 h whatever the weather. */
    fun sky(condition: String?, hour: Int): String {
        if (hour >= 22 || hour < 6) return "night"
        val c = condition.orEmpty()
        return when {
            listOf("비", "눈", "소나기", "진눈깨비", "뇌우").any { it in c } -> "rain"
            listOf("흐", "구름", "안개", "황사", "박무", "연무").any { it in c } -> "cloudy"
            else -> "sunny"
        }
    }

    /** 으로/로: 로 after a vowel or ㄹ (와인으로, 코랄로, 피치로). */
    fun ro(word: String): String {
        val ch = word.lastOrNull() ?: return "로"
        if (ch !in '가'..'힣') return "로"
        val jong = (ch - '가') % 28
        return if (jong == 0 || jong == 8) "로" else "으로"
    }

    /** The line follows the weather, also at night (the look is still today's; only the scene is dark). */
    fun line(sky: String, condition: String?, temp: Int?, color: String?): String {
        color ?: return "오늘의 룩"
        val day = if (sky == "night") sky(condition, 12) else sky
        return when {
            day == "rain" -> "${if (condition.orEmpty().contains("눈")) "눈" else "비"} 오는 날, $color 한 방울"
            temp != null && temp <= 5 -> "$color${ro(color)} 따뜻하게"
            day == "cloudy" && condition.orEmpty().contains("구름 조금") -> "구름 사이로, $color 한 조각"
            day == "cloudy" -> "흐린 날엔 $color${ro(color)}"
            else -> "햇살 아래, $color 한 조각"
        }
    }

    /** "Nov 1 · 9° · rainy day look", "Nov 1 · 9° · tonight". */
    fun caption(sky: String, condition: String?, temp: Int?, now: LocalDateTime = LocalDateTime.now()): String {
        val word = when (sky) {
            "night" -> "tonight"
            "rain" -> if (condition.orEmpty().contains("눈")) "snowy day look" else "rainy day look"
            "cloudy" -> "cloudy day look"
            else -> "sunny day look"
        }
        return listOfNotNull(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH).format(now), temp?.let { "$it°" }, word).joinToString(" · ")
    }

    /** The look's accent: its most colorful piece whose color is one of the tone's colors (softer nickname where one exists). */
    fun accent(tone: Tone, pieces: List<Pair<String, Int>>): String? =
        pieces.mapNotNull { (_, c) -> tone.bestColors.firstOrNull { hexColor(it.hex).toArgb() == c }?.let { it to c } }
            .maxByOrNull { chroma(it.second) }?.takeIf { chroma(it.second) > 40 }?.first?.name
            ?.let { NICK[it] ?: it }

    private val NICK = mapOf("피치" to "복숭아")

    private fun chroma(c: Int): Int {
        val r = c shr 16 and 0xFF; val g = c shr 8 and 0xFF; val b = c and 0xFF
        return maxOf(r, g, b) - minOf(r, g, b)
    }
}
