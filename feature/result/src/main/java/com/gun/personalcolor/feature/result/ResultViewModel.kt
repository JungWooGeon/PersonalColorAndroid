package com.gun.personalcolor.feature.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.domain.DiagnosisSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    session: DiagnosisSession,
    val content: ContentRepository,
    private val prefs: UserPreferencesRepository,
) : ViewModel() {
    /** Null after process death (the photo isn't persisted) — the screen then goes back. */
    val diagnosis = session.current
    val myToneId = prefs.preferences.map { it.myToneId }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setMyTone(id: String) = viewModelScope.launch { prefs.setMyTone(id) }
}
