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
import android.util.Log
import kotlinx.coroutines.CancellationException

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
    private val tones = flow { emit(content.safeTones()) }

    val state: StateFlow<ClosetUiState> = combine(tones, closet.items, prefs.preferences) { tones, items, p ->
        // Duplicate ids would crash the keyed grid; keep the newest (first) of each.
        ClosetUiState(false, items.distinctBy { it.id }.map { entryFor(it, tones, p.myToneId) })
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClosetUiState())

    fun update(item: ClosetItem) = safeLaunch { closet.update(item) }
    fun delete(id: Long) = safeLaunch { closet.delete(id) }
    fun clear() = safeLaunch { closet.clear() }

    private fun safeLaunch(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { Log.w("Closet", "closet write failed", e) }
        }
    }
    fun imageFile(name: String) = closet.imageFile(name)
}

internal fun entryFor(saved: ClosetItem, tones: List<Tone>, myToneId: String?): ClosetEntry {
    // A corrupt hex would crash every Swatch that draws it; show it as grey instead.
    val item = safeHex(saved.hex).let { if (it == saved.hex) saved else saved.copy(hex = it) }
    val myTone = tones.firstOrNull { it.id == myToneId }
    val ref = myTone ?: tones.firstOrNull { it.id == item.toneId }
    val v = ref?.let { runCatching { ColorMatcher.evaluate(hexToRgb(item.hex), it, tones) }.getOrNull() }
    return if (v != null && myTone != null && myTone.id != item.toneId) ClosetEntry(item, v.level, v.score, true, ref, v)
    else ClosetEntry(item, MatchLevel.of(item.verdict), item.score, false, ref, v)
}

/** Tones from the bundled content, or none if it can't be read (screens then show their no-tone state). */
internal fun ContentRepository.safeTones(): List<Tone> =
    runCatching { tones }.onFailure { Log.e("ColorMatch", "tones unreadable", it) }.getOrDefault(emptyList())
