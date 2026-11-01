package com.gun.personalcolor.feature.drape

import com.gun.personalcolor.core.analysis.Axes
import com.gun.personalcolor.core.analysis.ToneScore
import com.gun.personalcolor.core.domain.Diagnosis
import com.gun.personalcolor.core.domain.DrapeRefiner
import com.gun.personalcolor.core.domain.RefineAnswer
import com.gun.personalcolor.core.domain.RefineAxis
import com.gun.personalcolor.core.domain.RefineChoice
import com.gun.personalcolor.core.domain.RefineRound
import com.gun.personalcolor.core.model.Accessory
import com.gun.personalcolor.core.model.Fashion
import com.gun.personalcolor.core.model.Hair
import com.gun.personalcolor.core.model.Makeup
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.ResultSource
import com.gun.personalcolor.core.model.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RefineTest {
    private val ids = listOf("spring_light", "spring_true", "spring_bright", "summer_light", "summer_true", "summer_mute",
        "autumn_mute", "autumn_true", "autumn_deep", "winter_bright", "winter_true", "winter_deep")
    private val tones = ids.map { id ->
        Tone(id, id.substringBefore('_'), id, id, emptyList(), "", "", listOf(NamedColor("$id 1", "#111111"), NamedColor("$id 2", "#222222")),
            emptyList(), Makeup("", emptyList(), "", emptyList(), "", emptyList(), ""), Hair(emptyList(), ""), Fashion("", emptyList()), Accessory("", ""), "")
    }
    private val base = mapOf("autumn_mute" to 0.23, "summer_mute" to 0.20, "autumn_true" to 0.15, "autumn_deep" to 0.10).let { top ->
        ids.map { ToneScore(it, top[it] ?: 0.04) }.sortedByDescending { it.probability }
    }

    /** Runs the whole flow with [pick] answering each round. */
    private fun run(pick: (RefineRound) -> RefineChoice): List<RefineAnswer> {
        val answers = mutableListOf<RefineAnswer>()
        while (true) {
            val r = DrapeRefiner.nextRound(base, answers, tones) ?: return answers
            answers += RefineAnswer(r, pick(r))
        }
    }

    // Someone who really is 가을 뮤트: warm over cool, muted over clear, 가을 뮤트 over any rival.
    private fun autumnMute(r: RefineRound): RefineChoice {
        fun side(good: (String) -> Boolean) = when {
            good(r.aTone) && !good(r.bTone) -> RefineChoice.A
            good(r.bTone) && !good(r.aTone) -> RefineChoice.B
            else -> RefineChoice.SAME
        }
        return when (r.axis) {
            RefineAxis.WARM -> side { it.startsWith("spring") || it.startsWith("autumn") }
            RefineAxis.CLARITY -> side { it.endsWith("mute") }
            RefineAxis.VALUE -> RefineChoice.SAME
            RefineAxis.TONE -> side { it == "autumn_mute" }
        }
    }

    @Test fun consistentPicksConvergeOnTheTone() {
        val answers = run(::autumnMute)
        assertTrue(answers.size in 6..DrapeRefiner.TOTAL_STEPS)
        val post = DrapeRefiner.posterior(base, answers)
        assertEquals("autumn_mute", post[0].toneId)
        assertTrue("got ${post[0].probability}", post[0].probability > 0.5)
        assertEquals(1.0, post.sumOf { it.probability }, 1e-9)
    }

    @Test fun picksCanOverturnThePhoto() {
        // Always choosing the cool side / 여름 뮤트 moves the top tone off 가을 뮤트.
        val answers = run { r ->
            when (r.axis) {
                RefineAxis.WARM -> if (r.aTone.startsWith("summer") || r.aTone.startsWith("winter")) RefineChoice.A else RefineChoice.B
                RefineAxis.TONE -> if (r.aTone == "summer_mute") RefineChoice.A else if (r.bTone == "summer_mute") RefineChoice.B else RefineChoice.SAME
                else -> RefineChoice.SAME
            }
        }
        assertEquals("summer_mute", DrapeRefiner.posterior(base, answers)[0].toneId)
    }

    @Test fun allSimilarKeepsThePhotoScores() {
        val post = DrapeRefiner.posterior(base, run { RefineChoice.SAME })
        assertEquals(base.map { it.toneId }, post.map { it.toneId })
        base.zip(post).forEach { (a, b) -> assertEquals(a.probability, b.probability, 1e-9) }
    }

    @Test fun roundsCompareDifferentTonesAndStepsIncrease() {
        val answers = run(::autumnMute)
        answers.zipWithNext().forEach { (x, y) -> assertTrue(y.round.step > x.round.step) }
        answers.forEach { assertTrue(it.round.aTone != it.round.bTone) }
        assertNull(DrapeRefiner.nextRound(base, answers, tones))
    }

    @Test fun quizNudgesButDoesNotDominate() {
        val post = DrapeRefiner.posterior(base, emptyList(), quiz = listOf(ToneScore("summer_mute", 0.6)))
        assertEquals("summer_mute", post[0].toneId) // 0.20 × 1.3 = 0.26 > 0.23
        val far = DrapeRefiner.posterior(base, emptyList(), quiz = listOf(ToneScore("winter_deep", 0.9)))
        assertEquals("autumn_mute", far[0].toneId)
    }
}
