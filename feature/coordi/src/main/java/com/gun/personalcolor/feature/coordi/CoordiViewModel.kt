package com.gun.personalcolor.feature.coordi

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.data.ClosetRepository
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.COORDI_TRIAL_DAYS
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.weather.TodayWeather
import com.gun.personalcolor.core.weather.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

internal data class WeatherState(val loading: Boolean = true, val weather: TodayWeather? = null, val hasPermission: Boolean = false)

internal data class CoordiUiState(
    val loading: Boolean = true,
    val tone: Tone? = null,
    val weather: WeatherState = WeatherState(),
    val outfit: Outfit? = null,
    /** Days left in the free trial; null once it's over. */
    val trialDaysLeft: Int? = null,
)

@HiltViewModel
internal class CoordiViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferencesRepository,
    content: ContentRepository,
    closet: ClosetRepository,
    private val weatherRepo: WeatherRepository,
    provider: TodayCoordiProvider,
) : ViewModel() {
    private val weather = MutableStateFlow(WeatherState())
    private val variant = MutableStateFlow(0)

    val state: StateFlow<CoordiUiState> = combine(prefs.preferences, closet.items, weather, variant) { p, items, w, v ->
        val tone = content.toneOrNull(p.myToneId)
        val now = System.currentTimeMillis()
        val start = p.coordiTrialStartMillis ?: now // not saved yet on the very first emission
        val trialLeft = (COORDI_TRIAL_DAYS - (now - start) / DAY_MS).toInt().takeIf { it > 0 }
        CoordiUiState(
            loading = tone != null && w.loading,
            tone = tone,
            weather = w,
            // Wait for the weather so the outfit doesn't change under the user's eyes.
            // Never crash the screen on bad content: no outfit shows the loading card instead.
            outfit = if (tone != null && !w.loading) runCatching { provider.outfit(tone, w.weather, items, v) }.getOrNull() else null,
            trialDaysLeft = trialLeft,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoordiUiState())

    init {
        viewModelScope.launch { runCatching { prefs.startCoordiTrialIfNeeded(System.currentTimeMillis()) } }
        refreshWeather()
    }

    private var fetch: Job? = null
    private var fetchedAt = 0L

    /** Calls while a fetch for the same permission state is running are dropped (grant callback + resume fire together). */
    fun refreshWeather() {
        val permitted = WeatherRepository.hasPermission(context)
        if (fetch?.isActive == true && weather.value.hasPermission == permitted) return
        val justGranted = permitted && !weather.value.hasPermission
        fetch?.cancel()
        fetch = viewModelScope.launch {
            weather.value = WeatherState(loading = true, hasPermission = permitted)
            var w = if (permitted) load() else null
            // Right after the grant there's often no location fix yet: give it one more try before the season fallback.
            if (w == null && justGranted) { delay(GRANT_RETRY_DELAY_MS); w = load() }
            fetchedAt = System.currentTimeMillis()
            weather.value = WeatherState(loading = false, weather = w, hasPermission = permitted)
        }
    }

    // A failed or slow fetch (or a revoked permission mid-call) falls back to the season-based outfit.
    private suspend fun load() = runCatching { withTimeoutOrNull(WEATHER_TIMEOUT_MS) { weatherRepo.today() } }.getOrNull()

    /** Re-check on resume: the user may have granted location in system settings, or the last try fell back to the season. */
    fun onResume() {
        if (shouldRefreshOnResume(WeatherRepository.hasPermission(context), weather.value, System.currentTimeMillis() - fetchedAt)) refreshWeather()
    }

    fun nextOutfit() { variant.value++ }

    private companion object {
        const val DAY_MS = 24 * 60 * 60_000L
        const val WEATHER_TIMEOUT_MS = 8_000L
        const val GRANT_RETRY_DELAY_MS = 1_500L
    }
}

private const val RESUME_RETRY_MS = 30_000L

/** Permission changed, or permitted but the last result was the season fallback and it's been [RESUME_RETRY_MS] (debounce). */
internal fun shouldRefreshOnResume(permitted: Boolean, w: WeatherState, sinceLastFetchMs: Long): Boolean = when {
    permitted != w.hasPermission -> true
    w.loading -> false
    else -> permitted && w.weather == null && sinceLastFetchMs >= RESUME_RETRY_MS
}
