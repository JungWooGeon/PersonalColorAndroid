package com.gun.personalcolor.feature.quiz

import androidx.lifecycle.SavedStateHandle
import com.gun.personalcolor.core.model.QuizOption
import com.gun.personalcolor.core.model.QuizQuestion
import org.junit.Assert.assertEquals
import org.junit.Test

class QuizSaveTest {
    private val a = QuizOption("a", 1.0, 0.0, 0.0)
    private val b = QuizOption("b", -1.0, 0.0, 0.0)
    private val qs = List(4) { QuizQuestion("q$it", "q", options = listOf(a, b)) }

    @Test fun roundTripsThroughSavedStateHandle() {
        val handle = SavedStateHandle()
        handle["k"] = encodeAnswers(qs, listOf(b, a))
        assertEquals(listOf(b, a), decodeAnswers(qs, handle["k"]))
    }

    @Test fun emptyAndBrokenStateRestartSafely() {
        assertEquals(emptyList<QuizOption>(), decodeAnswers(qs, null))
        assertEquals(listOf(a), decodeAnswers(qs, intArrayOf(0, 7, 1)))
    }

    @Test fun finishedQuizRestoresToLastQuestion() {
        assertEquals(3, decodeAnswers(qs, intArrayOf(0, 1, 0, 1)).size)
    }
}
