package com.gun.personalcolor.feature.my

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.HistoryRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.HistoryEntry
import com.gun.personalcolor.core.model.Tone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
            entries = visibleEntries(e, tones.keys),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyUiState())

    // A failed disk write leaves the list as it was; the user can retry.
    fun delete(timeMillis: Long) { viewModelScope.launch { runCatching { history.delete(timeMillis) }.onFailure(::logWriteFailure) } }
    fun clear() { viewModelScope.launch { runCatching { history.clear() }.onFailure(::logWriteFailure) } }

    private fun logWriteFailure(e: Throwable) {
        if (e is CancellationException) throw e
        Log.w("MyViewModel", "history write failed", e)
    }
}

/**
 * Newest first, entries of removed tones dropped, one per timestamp: the history list keys rows by time and
 * LazyColumn crashes on a duplicate key.
 */
internal fun visibleEntries(entries: List<HistoryEntry>, toneIds: Set<String>): List<HistoryEntry> =
    entries.filter { it.toneId in toneIds }.distinctBy { it.timeMillis }.sortedByDescending { it.timeMillis }
