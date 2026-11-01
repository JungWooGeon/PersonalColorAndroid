package com.gun.personalcolor.feature.quiz

import com.gun.personalcolor.core.model.QuizOption
import com.gun.personalcolor.core.model.QuizQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizScoreTest {
    private val warm = QuizOption("warm", 1.0, 0.5, 0.0)
    private val cool = QuizOption("cool", -1.0, -0.5, 0.0)
    private val unsure = QuizOption("?", 0.0, 0.0, 0.0)
    private val qs = List(4) { QuizQuestion("q$it", "q", options = listOf(warm, cool, unsure)) }

    @Test fun allWarmIsClampedToMaxWarm() {
        val a = score(qs, List(4) { warm })
        assertEquals(1.0, a.warmth, 1e-9)
        assertEquals(0.0, a.clarity, 1e-9)   // no question touches clarity
    }

    @Test fun symmetricAndNeutral() {
        assertEquals(-score(qs, List(4) { warm }).warmth, score(qs, List(4) { cool }).warmth, 1e-9)
        assertEquals(0.0, score(qs, List(4) { unsure }).warmth, 1e-9)
    }

    @Test fun mixedAnswersStayInsideTheBox() {
        val a = score(qs, listOf(warm, warm, cool, unsure))
        assertTrue(a.warmth > 0 && a.warmth < 1)
    }
}
