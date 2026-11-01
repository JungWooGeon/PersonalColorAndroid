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

    fun analyze(inputs: List<PhotoInput>, onSuccess: () -> Unit) {
        if (inputs.isEmpty()) return
        if (_state.value == AnalyzeState.Working) return
        _state.value = AnalyzeState.Working
        viewModelScope.launch {
            val failure = analyzePhoto.analyze(inputs)
            _state.value = if (failure == null) AnalyzeState.Idle else AnalyzeState.Failed(failure)
            if (failure == null) onSuccess()
        }
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
