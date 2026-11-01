package com.gun.personalcolor.feature.quiz

import kotlinx.coroutines.CancellationException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.analysis.Axes
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.domain.QuizDiagnosisUseCase
import com.gun.personalcolor.core.model.QuizOption
import com.gun.personalcolor.core.model.QuizQuestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class QuizViewModel @Inject constructor(
    content: ContentRepository,
    private val diagnose: QuizDiagnosisUseCase,
) : ViewModel() {
    val questions: List<QuizQuestion> = content.quiz
    private val _answers = MutableStateFlow<List<QuizOption>>(emptyList())
    val answers = _answers.asStateFlow()

    /** True once the result is published; the screen navigates (with its *current* nav/activity, rotation-safe) and calls [resultShown]. */
    private val _done = MutableStateFlow(false)
    val done = _done.asStateFlow()

    fun answer(option: QuizOption) {
        if (_answers.value.size >= questions.size) return
        val now = _answers.value + option
        _answers.value = now
        if (now.size == questions.size) viewModelScope.launch {
            try {
                diagnose(score(questions, now))
                _done.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Couldn't publish: let the last answer be picked again instead of a spinner forever.
                Log.w("QuizViewModel", "quiz diagnosis failed", e)
                _answers.value = now.dropLast(1)
            }
        }
    }

    fun resultShown() { _done.value = false }

    /** Returns false when there is nothing to undo (caller leaves the screen). */
    fun undo(): Boolean {
        if (_answers.value.isEmpty()) return false
        _answers.value = _answers.value.dropLast(1)
        return true
    }
}

// ponytail: heuristic gain — real answers are mixed, so raw sums rarely reach the extremes. Tune with feedback data.
private const val QUIZ_GAIN = 1.6

/** Sum each axis, divide by the largest magnitude that axis could reach, amplify and clamp to [-1, 1]. */
internal fun score(questions: List<QuizQuestion>, answers: List<QuizOption>): Axes {
    fun axis(pick: (QuizOption) -> Double): Double {
        val max = questions.sumOf { q -> q.options.maxOf { abs(pick(it)) } }
        return if (max == 0.0) 0.0 else (QUIZ_GAIN * answers.sumOf(pick) / max).coerceIn(-1.0, 1.0)
    }
    return Axes(axis { it.warmth }, axis { it.value }, axis { it.clarity })
}
