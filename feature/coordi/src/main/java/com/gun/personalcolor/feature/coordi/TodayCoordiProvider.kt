package com.gun.personalcolor.feature.coordi

import android.content.Context
import android.util.Log
import com.gun.personalcolor.core.data.ClosetRepository
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.weather.TodayWeather
import com.gun.personalcolor.core.weather.WeatherRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** Short summary for the home card and the daily notification. */
data class CoordiSummary(
    val toneName: String,
    val todayColorName: String,
    val todayColorHex: String,
    /** "망원동 18°C 맑음"; null without weather. */
    val weatherLine: String?,
    /** "카멜 트렌치코트 + 아이보리 니트" */
    val outfitHeadline: String,
)

@Singleton
class TodayCoordiProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferencesRepository,
    private val content: ContentRepository,
    private val closet: ClosetRepository,
    private val weather: WeatherRepository,
) {
    @Serializable private class MoodMonth(val month: Int, val colors: List<NamedColor>)
    @Serializable private class MoodFile(val months: List<MoodMonth>)

    private val moods: Map<Int, List<NamedColor>> by lazy {
        runCatching {
            val text = context.assets.open("coordi_season_moods.json").bufferedReader().use { it.readText() }
            Json { ignoreUnknownKeys = true }.decodeFromString<MoodFile>(text).months.associate { it.month to it.colors }
        }.getOrElse { emptyMap() }
    }

    fun outfit(tone: Tone, weather: TodayWeather?, closet: List<ClosetItem>, variant: Int, date: LocalDate = LocalDate.now()): Outfit =
        OutfitEngine.build(tone, weather, date, moods[date.monthValue].orEmpty(), closet, variant)

    /** Null when no tone is set. Never throws. */
    suspend fun summary(): CoordiSummary? = try {
        content.toneOrNull(prefs.preferences.first().myToneId)?.let { tone ->
            val w = weather.today()
            val date = LocalDate.now()
            val color = tone.bestColors[date.dayOfYear % tone.bestColors.size]
            CoordiSummary(
                toneName = tone.nameKo,
                todayColorName = color.name,
                todayColorHex = color.hex,
                weatherLine = w?.let(::weatherLine),
                outfitHeadline = outfit(tone, w, closet.items.first(), 0, date).headline,
            )
        }
    } catch (e: Exception) {
        Log.w("Coordi", "summary failed", e); null
    }

    companion object {
        fun weatherLine(w: TodayWeather) = listOfNotNull(w.shortPlace, "${w.temp.roundToInt()}°C", w.condition).joinToString(" ")
    }
}
