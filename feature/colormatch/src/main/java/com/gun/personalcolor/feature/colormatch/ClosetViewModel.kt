package com.gun.personalcolor.feature.colormatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.data.ClosetRepository
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.model.Tone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A saved item plus its verdict for the tone it's judged against now. */
data class ClosetEntry(
    val item: ClosetItem,
    val level: MatchLevel,
    val score: Int,
    val reEvaluated: Boolean,   // judged against the current my tone instead of the saved one
    val refTone: Tone?,
    val verdict: ColorVerdict?, // reason + top tones for refTone
)

data class ClosetUiState(val loading: Boolean = true, val entries: List<ClosetEntry> = emptyList())

@HiltViewModel
class ClosetViewModel @Inject constructor(
    content: ContentRepository,
    prefs: UserPreferencesRepository,
    private val closet: ClosetRepository,
) : ViewModel() {
    private val tones = flow { emit(content.tones) }

    val state: StateFlow<ClosetUiState> = combine(tones, closet.items, prefs.preferences) { tones, items, p ->
        ClosetUiState(false, items.map { entryFor(it, tones, p.myToneId) })
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClosetUiState())

    fun update(item: ClosetItem) { viewModelScope.launch { closet.update(item) } }
    fun delete(id: Long) { viewModelScope.launch { closet.delete(id) } }
    fun clear() { viewModelScope.launch { closet.clear() } }
}

internal fun entryFor(item: ClosetItem, tones: List<Tone>, myToneId: String?): ClosetEntry {
    val myTone = tones.firstOrNull { it.id == myToneId }
    val ref = myTone ?: tones.firstOrNull { it.id == item.toneId }
    val v = ref?.let { runCatching { ColorMatcher.evaluate(hexToRgb(item.hex), it, tones) }.getOrNull() }
    val re = v != null && myTone != null && myTone.id != item.toneId
    return if (re) ClosetEntry(item, v!!.level, v.score, true, ref, v)
    else ClosetEntry(item, MatchLevel.of(item.verdict), item.score, false, ref, v)
}
