package com.gun.personalcolor.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.Article
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class HomeState(
    val myTone: Tone? = null,
    /** Rotates daily through my tone's best colors. */
    val todayColor: NamedColor? = null,
    val tones: List<Tone> = emptyList(),
    val articles: List<Article> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(content: ContentRepository, prefs: UserPreferencesRepository) : ViewModel() {
    val state: StateFlow<HomeState> = prefs.preferences.map { p ->
        val my = content.toneOrNull(p.myToneId)
        HomeState(
            myTone = my,
            todayColor = my?.bestColors?.let { it[LocalDate.now().dayOfYear % it.size] },
            tones = content.tones,
            articles = content.articles.take(4),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState(tones = content.tones, articles = content.articles.take(4)))
}
