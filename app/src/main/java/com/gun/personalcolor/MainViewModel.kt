package com.gun.personalcolor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(repo: UserPreferencesRepository) : ViewModel() {
    /** Null until the first read finishes; the splash screen stays up until then. */
    val prefs: StateFlow<UserPreferences?> = repo.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
