package com.gun.personalcolor.core.analysis

import kotlin.math.exp

/** Each axis roughly in [-1, 1]. warmth: + warm / − cool. value: + light / − deep. clarity: + bright/clear / − muted/soft. */
data class Axes(val warmth: Double, val value: Double, val clarity: Double)

data class ToneScore(val toneId: String, val probability: Double)

/**
 * Nearest-prototype classifier: each of the 12 tones is a point in (warmth, value, clarity);
 * probability = softmax(−squaredDistance / TEMPERATURE).
 */
object ToneClassifier {
    // ponytail: heuristic calibration, tune with labeled data.
    // Higher temperature = flatter distribution. 0.6 gives ~55% top score exactly on a prototype, ~30–45% between them.
    private const val TEMPERATURE = 0.6

    private val PROTOTYPES: List<Pair<String, Axes>> = listOf(
        "spring_light" to Axes(0.5, 0.9, 0.3),
        "spring_true" to Axes(1.0, 0.3, 0.5),
        "spring_bright" to Axes(0.5, 0.2, 1.0),
        "summer_light" to Axes(-0.5, 0.9, 0.0),
        "summer_true" to Axes(-1.0, 0.3, -0.2),
        "summer_mute" to Axes(-0.4, 0.0, -0.9),
        "autumn_mute" to Axes(0.4, 0.0, -0.9),
        "autumn_true" to Axes(1.0, -0.4, -0.3),
        "autumn_deep" to Axes(0.5, -0.9, 0.0),
        "winter_bright" to Axes(-0.5, 0.0, 1.0),
        "winter_true" to Axes(-1.0, -0.2, 0.5),
        "winter_deep" to Axes(-0.5, -0.9, 0.3),
    )

    val TONE_IDS: List<String> = PROTOTYPES.map { it.first }

    fun classify(axes: Axes): List<ToneScore> {
        val d2 = PROTOTYPES.map { (_, p) ->
            sq(axes.warmth - p.warmth) + sq(axes.value - p.value) + sq(axes.clarity - p.clarity)
        }
        val min = d2.min() // subtract min for numerical stability
        val w = d2.map { exp(-(it - min) / TEMPERATURE) }
        val sum = w.sum()
        return TONE_IDS.indices.map { ToneScore(TONE_IDS[it], w[it] / sum) }.sortedByDescending { it.probability }
    }

    private fun sq(x: Double) = x * x
}
