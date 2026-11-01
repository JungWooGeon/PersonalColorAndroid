package com.gun.personalcolor.feature.tones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.Tone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.IOException
import android.util.Log

data class TonesUiState(
    val tones: List<Tone> = emptyList(),
    val myToneId: String? = null,
    val favorites: Set<String> = emptySet(),
)

/** Shared by the tone list, detail and compare screens (each gets its own instance). */
@HiltViewModel
class TonesViewModel @Inject constructor(
    content: ContentRepository,
    private val prefs: UserPreferencesRepository,
) : ViewModel() {
    val state = prefs.preferences
        .map { TonesUiState(content.tones, it.myToneId, it.favoriteToneIds) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TonesUiState(content.tones))

    fun toggleFavorite(id: String) = write { prefs.toggleFavorite(id) }
    fun setMyTone(id: String) = write { prefs.setMyTone(id) }

    /** DataStore writes throw IOException on disk errors; drop the tap instead of crashing. */
    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { try { block() } catch (e: IOException) { Log.w("Tones", "write failed", e) } }
    }
}

/** Same value/clarity, other temperature — the tone people most often confuse with this one. */
internal val OPPOSITE = mapOf(
    "spring_light" to "summer_light", "summer_light" to "spring_light",
    "spring_true" to "summer_true", "summer_true" to "spring_true",
    "spring_bright" to "winter_bright", "winter_bright" to "spring_bright",
    "summer_mute" to "autumn_mute", "autumn_mute" to "summer_mute",
    "autumn_true" to "winter_true", "winter_true" to "autumn_true",
    "autumn_deep" to "winter_deep", "winter_deep" to "autumn_deep",
)

/** Same-season siblings plus the opposite-temperature neighbor. */
internal fun similarTones(tone: Tone, all: List<Tone>): List<Tone> =
    all.filter { it.season == tone.season && it.id != tone.id } + all.filter { it.id == OPPOSITE[tone.id] }
