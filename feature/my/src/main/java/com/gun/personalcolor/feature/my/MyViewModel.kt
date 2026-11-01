package com.gun.personalcolor.feature.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.HistoryRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.HistoryEntry
import com.gun.personalcolor.core.model.Tone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyUiState(
    val loading: Boolean = true,
    val tones: Map<String, Tone> = emptyMap(),
    val myTone: Tone? = null,
    val favorites: List<Tone> = emptyList(),
    /** Newest first; entries whose tone no longer exists are dropped. */
    val entries: List<HistoryEntry> = emptyList(),
)

/** Shared by the 마이 tab and the history screen. */
@HiltViewModel
class MyViewModel @Inject constructor(
    content: ContentRepository,
    prefs: UserPreferencesRepository,
    private val history: HistoryRepository,
) : ViewModel() {
    val state = combine(prefs.preferences, history.entries) { p, e ->
        val tones = content.tones.associateBy { it.id }
        MyUiState(
            loading = false,
            tones = tones,
            myTone = tones[p.myToneId],
            favorites = content.tones.filter { it.id in p.favoriteToneIds },
            entries = e.filter { it.toneId in tones }.sortedByDescending { it.timeMillis },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyUiState())

    fun delete(timeMillis: Long) { viewModelScope.launch { history.delete(timeMillis) } }
    fun clear() { viewModelScope.launch { history.clear() } }
}
