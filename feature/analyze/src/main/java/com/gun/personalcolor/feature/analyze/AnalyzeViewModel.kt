package com.gun.personalcolor.feature.analyze

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.domain.AnalyzePhotoUseCase
import com.gun.personalcolor.core.domain.PhotoFailure
import com.gun.personalcolor.core.vision.PhotoInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AnalyzeState {
    /** Tips + camera/album buttons. */
    data object Idle : AnalyzeState
    data object Camera : AnalyzeState
    /** Guided capture saved at [uri]; asking for an optional white-paper point. [auto] = auto capture. */
    data class WhiteRef(val uri: Uri, val auto: Boolean) : AnalyzeState
    data object Working : AnalyzeState
    data class Failed(val failure: PhotoFailure) : AnalyzeState
}

@HiltViewModel
class AnalyzeViewModel @Inject constructor(
    private val analyzePhoto: AnalyzePhotoUseCase,
    prefs: UserPreferencesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<AnalyzeState>(AnalyzeState.Idle)
    val state = _state.asStateFlow()

    val showGuide = prefs.preferences.map { it.showPhotoGuide }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun openCamera() { _state.value = AnalyzeState.Camera }

    fun captured(uri: Uri, auto: Boolean) { _state.value = AnalyzeState.WhiteRef(uri, auto) }

    /** Loading caption shown while working: real progress, but each step stays up long enough to read. [FOUND] at the end. */
    private val _step = MutableStateFlow(0)
    val step = _step.asStateFlow()

    fun analyze(inputs: List<PhotoInput>, onSuccess: () -> Unit) {
        if (inputs.isEmpty()) return
        if (_state.value == AnalyzeState.Working) return
        _state.value = AnalyzeState.Working
        _step.value = 0
        viewModelScope.launch {
            val real = MutableStateFlow(0)
            // Never ahead of the real work, never faster than STEP_MS per caption.
            val captions = launch { for (i in 1..3) { delay(STEP_MS); real.first { it >= i }; _step.value = i } }
            val failure = analyzePhoto.analyze(inputs) { s -> real.update { maxOf(it, s) } }
            if (failure == null) {
                real.value = 3; captions.join()
                delay(HOLD_MS); _step.value = FOUND; delay(FOUND_MS)
            } else captions.cancel()
            _state.value = if (failure == null) AnalyzeState.Idle else AnalyzeState.Failed(failure)
            if (failure == null) onSuccess()
        }
    }

    companion object {
        const val FOUND = 4
        private const val STEP_MS = 560L
        private const val HOLD_MS = 380L
        private const val FOUND_MS = 760L
    }

    /** One step back: white ref → camera, camera/failure → tips. */
    fun back() {
        _state.value = when (_state.value) {
            is AnalyzeState.WhiteRef -> AnalyzeState.Camera
            AnalyzeState.Working -> return
            else -> AnalyzeState.Idle
        }
    }
}
