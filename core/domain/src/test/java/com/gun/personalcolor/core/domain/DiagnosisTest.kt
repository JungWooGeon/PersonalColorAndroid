package com.gun.personalcolor.core.domain

import com.gun.personalcolor.core.analysis.Axes
import com.gun.personalcolor.core.analysis.ToneClassifier
import com.gun.personalcolor.core.analysis.ToneScore
import com.gun.personalcolor.core.model.ResultSource
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosisTest {
    private val axes = Axes(0.5, 0.9, 0.3)

    @Test fun topIsFirstScore() {
        assertEquals("winter_deep", Diagnosis(listOf(ToneScore("winter_deep", 0.6)), axes, ResultSource.QUIZ).top.toneId)
    }

    @Test fun emptyScoresFallBackToNearestPrototype() {
        val top = Diagnosis(emptyList(), axes, ResultSource.QUIZ).top
        assertEquals(ToneClassifier.classify(axes).first().toneId, top.toneId)
        assertEquals("spring_light", top.toneId)
    }
}
