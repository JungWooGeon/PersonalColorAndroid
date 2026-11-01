package com.gun.personalcolor.core.domain

import com.gun.personalcolor.core.analysis.ToneScore
import com.gun.personalcolor.core.analytics.Analytics
import com.gun.personalcolor.core.data.HistoryRepository
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.ResultSource
import com.gun.personalcolor.core.model.Tone
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow

/** What a draping round compares. [TONE] = two candidate tones head to head. */
enum class RefineAxis { WARM, VALUE, CLARITY, TONE }

enum class RefineChoice { A, B, SAME }

/** One draping comparison: the same photo with cape [a] (from tone [aTone]) vs cape [b] (from [bTone]). */
data class RefineRound(val step: Int, val axis: RefineAxis, val aTone: String, val bTone: String, val a: NamedColor, val b: NamedColor)

data class RefineAnswer(val round: RefineRound, val choice: RefineChoice)

/**
 * Draping confirmation, the way consultants do it: pairwise fabric comparisons, each a piece of evidence that
 * multiplies the photo probabilities. Pure Kotlin; rounds are derived from (photo scores, answers so far),
 * so going back is just dropping the last answer.
 */
object DrapeRefiner {
    // ponytail: heuristic calibration, tune with expert-labelled data.
    /** Likelihood ratio of one decisive pick for a tone that fully has the chosen trait. */
    const val LR = 1.8
    /** A recent self-quiz nudges each tone by up to ×(1 + QUIZ_WEIGHT × its quiz probability). */
    const val QUIZ_WEIGHT = 0.5

    /** Round plan: axis = what's compared, rep = which signature color (0/1), k = rival rank for TONE rounds. */
    private data class Step(val axis: RefineAxis, val rep: Int, val k: Int = 0)
    private val PLAN = listOf(
        Step(RefineAxis.WARM, 0), Step(RefineAxis.WARM, 1), Step(RefineAxis.VALUE, 0), Step(RefineAxis.CLARITY, 0),
        Step(RefineAxis.TONE, 0, 1), Step(RefineAxis.TONE, 1, 1), Step(RefineAxis.TONE, 0, 2), Step(RefineAxis.TONE, 1, 2),
    )
    val TOTAL_STEPS = PLAN.size

    // Light(+1)/deep(−1) and clear(+1)/muted(−1) per tone, mirroring ToneClassifier's prototypes.
    private val VALUE = mapOf(
        "spring_light" to 0.9, "spring_true" to 0.3, "spring_bright" to 0.2, "summer_light" to 0.9, "summer_true" to 0.3, "summer_mute" to 0.0,
        "autumn_mute" to 0.0, "autumn_true" to -0.4, "autumn_deep" to -0.9, "winter_bright" to 0.0, "winter_true" to -0.2, "winter_deep" to -0.9,
    )
    private val CLARITY = mapOf(
        "spring_light" to 0.3, "spring_true" to 0.5, "spring_bright" to 1.0, "summer_light" to 0.0, "summer_true" to -0.2, "summer_mute" to -0.9,
        "autumn_mute" to -0.9, "autumn_true" to -0.3, "autumn_deep" to 0.0, "winter_bright" to 1.0, "winter_true" to 0.5, "winter_deep" to 0.3,
    )

    private fun warm(id: String) = id.startsWith("spring") || id.startsWith("autumn")

    private fun trait(axis: RefineAxis, id: String): Double = when (axis) {
        RefineAxis.WARM -> if (warm(id)) 1.0 else -1.0
        RefineAxis.VALUE -> VALUE[id] ?: 0.0
        RefineAxis.CLARITY -> CLARITY[id] ?: 0.0
        RefineAxis.TONE -> 0.0
    }

    /** Photo scores updated with every answer (and an optional recent quiz result), renormalized, best first. */
    fun posterior(base: List<ToneScore>, answers: List<RefineAnswer>, quiz: List<ToneScore>? = null): List<ToneScore> {
        val p = base.associate { it.toneId to it.probability }.toMutableMap()
        for ((r, c) in answers) {
            if (c == RefineChoice.SAME) continue
            val sign = if (c == RefineChoice.A) 1.0 else -1.0
            if (r.axis == RefineAxis.TONE) {
                p.computeIfPresent(r.aTone) { _, v -> v * LR.pow(sign) }
                p.computeIfPresent(r.bTone) { _, v -> v * LR.pow(-sign) }
            } else {
                // Tone with A's trait value ×LR, with B's ÷LR, linear in between.
                val ta = trait(r.axis, r.aTone); val tb = trait(r.axis, r.bTone)
                val mid = (ta + tb) / 2; val half = (ta - tb) / 2
                if (half == 0.0) continue
                for (id in p.keys) p[id] = p.getValue(id) * LR.pow(sign * ((trait(r.axis, id) - mid) / half).coerceIn(-1.0, 1.0))
            }
        }
        quiz?.forEach { q -> p.computeIfPresent(q.toneId) { _, v -> v * (1 + QUIZ_WEIGHT * q.probability) } }
        val sum = p.values.sum().takeIf { it > 0 } ?: return base
        return p.map { (id, v) -> ToneScore(id, v / sum) }.sortedByDescending { it.probability }
    }

    /** The next comparison after [answers], or null when done. Steps without a meaningful pair are skipped. */
    fun nextRound(base: List<ToneScore>, answers: List<RefineAnswer>, tones: List<Tone>): RefineRound? {
        val byId = tones.associateBy { it.id }
        val ranked = posterior(base, answers).filter { it.toneId in byId }
        var step = (answers.lastOrNull()?.round?.step ?: -1) + 1
        while (step < PLAN.size) {
            val s = PLAN[step]
            pair(s, ranked)?.let { (a, b) ->
                // Alternate sides so "always pick the left one" doesn't bias the result.
                val (x, y) = if (step % 2 == 0) a to b else b to a
                val cx = byId.getValue(x).bestColors; val cy = byId.getValue(y).bestColors
                if (cx.isNotEmpty() && cy.isNotEmpty()) return RefineRound(step, s.axis, x, y, cx[s.rep % cx.size], cy[s.rep % cy.size])
            }
            step++
        }
        return null
    }

    /** (tone showing the "+" side, tone showing the "−" side) for an axis step, or (leader, rival) for a TONE step. */
    private fun pair(s: Step, ranked: List<ToneScore>): Pair<String, String>? {
        val ids = ranked.map { it.toneId }
        if (ids.size < 2) return null
        val top4 = ids.take(4)
        return when (s.axis) {
            RefineAxis.TONE -> ranked.getOrNull(s.k)?.takeIf { s.k == 1 || it.probability >= 0.05 }?.let { ids[0] to it.toneId }
            RefineAxis.WARM -> {
                val w = top4.firstOrNull(::warm) ?: ids.firstOrNull(::warm)
                val c = top4.firstOrNull { !warm(it) } ?: ids.firstOrNull { !warm(it) }
                if (w != null && c != null) w to c else null
            }
            else -> {
                // Extremes among the top candidates; if they barely differ, use the leader's temperature family instead.
                fun extremes(list: List<String>) = list.maxBy { trait(s.axis, it) } to list.minBy { trait(s.axis, it) }
                val (hi, lo) = extremes(top4)
                if (trait(s.axis, hi) - trait(s.axis, lo) >= 0.6) hi to lo
                else extremes(ids.filter { warm(it) == warm(ids[0]) }).takeIf { (h, l) -> abs(trait(s.axis, h) - trait(s.axis, l)) >= 0.6 }
            }
        }
    }
}

/** Folds draping answers (and a self-quiz from the last 30 days, if any) into the photo diagnosis and publishes it. */
class RefineDiagnosisUseCase @Inject constructor(
    private val history: HistoryRepository,
    private val publish: PublishDiagnosisUseCase,
    private val analytics: Analytics,
) {
    suspend operator fun invoke(base: Diagnosis, answers: List<RefineAnswer>): Diagnosis {
        val since = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val quiz = history.entries.first().firstOrNull { it.source == ResultSource.QUIZ && it.timeMillis >= since }
            ?.top?.map { ToneScore(it.toneId, it.probability) }
        // ponytail: axes stay the photo's; recompute from scores if a screen ever shows refined axes.
        val refined = base.copy(scores = DrapeRefiner.posterior(base.scores, answers, quiz), source = ResultSource.AI)
        publish(refined)
        analytics.log("refine_done", "from" to base.top.toneId, "to" to refined.top.toneId, "answers" to answers.size.toString())
        return refined
    }
}
