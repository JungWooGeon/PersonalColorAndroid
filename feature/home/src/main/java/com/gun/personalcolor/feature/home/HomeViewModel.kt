package com.gun.personalcolor.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.Article
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.weather.TodayWeather
import com.gun.personalcolor.core.weather.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class HomeState(
    val myTone: Tone? = null,
    /** Rotates daily through my tone's best colors. */
    val todayColor: NamedColor? = null,
    val tones: List<Tone> = emptyList(),
    val articles: List<Article> = emptyList(),
    /** 틴티's lines, first one fits the weather / time of day. */
    val lines: List<String> = emptyList(),
    val hour: Int = 12,
    /** False until preferences are read (avoids flashing the first-run hero). */
    val loaded: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(content: ContentRepository, prefs: UserPreferencesRepository, weather: WeatherRepository) : ViewModel() {
    // Weather only if location was granted earlier (오늘의 코디); home never asks. Null until/unless it arrives.
    private val todayWeather = flow<TodayWeather?> { emit(weather.today()) }.onStart { emit(null) }

    val state: StateFlow<HomeState> = combine(prefs.preferences, todayWeather) { p, w ->
        val my = content.toneOrNull(p.myToneId)
        val today = my?.bestColors?.let { it[LocalDate.now().dayOfYear % it.size] }
        val hour = LocalTime.now().hour
        HomeState(
            myTone = my, todayColor = today, tones = content.tones, articles = content.articles.take(4),
            lines = hostLines(hour, w, my, today?.name), hour = hour, loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState(tones = content.tones, articles = content.articles.take(4)))
}
