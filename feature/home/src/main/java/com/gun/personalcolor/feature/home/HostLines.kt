package com.gun.personalcolor.feature.home

import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.weather.TodayWeather
import com.gun.personalcolor.core.weather.WeatherCodes

/**
 * What 틴티 says on home. The first line fits the moment (weather, then time of day); a tap moves to the next.
 * Each line stays short enough for one bubble (~20 chars).
 */
internal fun hostLines(hour: Int, weather: TodayWeather?, tone: Tone?, todayColor: String?): List<String> {
    if (tone == null || todayColor == null) return listOf("내 색, 같이 찾아봐요", "사진 한 장이면 돼요", "앗, 간지러워요")
    val c = todayColor
    val first = when {
        weather != null && weather.icon in WET -> "비 오는 날, $c 어때요?"
        weather?.icon == WeatherCodes.SNOW -> "눈 오는 날, $c 어때요?"
        weather != null && weather.feelsLike <= 5 -> "쌀쌀해요. $c 니트 어때요?"
        weather != null && weather.feelsLike >= 27 -> "더운 날엔 ${c}${ro(c)} 가볍게"
        else -> when (hour) {
            in 5..10 -> "오늘은 ${c}${ro(c)} 시작해요"
            in 11..16 -> "$c 한 가지만 더해 봐요"
            in 17..21 -> "저녁 약속엔 $c 어때요?"
            else -> "오늘도 수고했어요"
        }
    }
    val other = tone.bestColors.map { it.name }.firstOrNull { it != c }
    return listOfNotNull(first, other?.let { "${it}도 잘 어울려요" }, "옷 사기 전엔 컬러 체크해요", "앗, 간지러워요")
}

private val WET = setOf(WeatherCodes.RAIN, WeatherCodes.SHOWER, WeatherCodes.DRIZZLE, WeatherCodes.THUNDER)

/** "로" or "으로" after [word] (ㄹ batchim and vowels take 로). */
internal fun ro(word: String): String {
    val last = word.lastOrNull() ?: return "로"
    if (last !in '가'..'힣') return "로"
    val jong = (last - '가') % 28
    return if (jong == 0 || jong == 8) "로" else "으로"
}
