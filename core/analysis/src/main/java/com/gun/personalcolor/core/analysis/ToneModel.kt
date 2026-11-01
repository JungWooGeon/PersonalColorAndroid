package com.gun.personalcolor.core.analysis

import kotlin.math.exp
import kotlin.math.min
import kotlin.math.tanh

/**
 * Tiny MLP (features → tanh hidden layer → 12 tone logits, temperature-calibrated softmax), weights in
 * [ToneModelWeights], trained by tools/model/train.py on synthetic literature-grounded data.
 * Keep [vector] in sync with `features()` in train.py.
 */
internal object ToneModel {
    // Fill values for missing features (same as FILL in train.py); a has* flag tells the model it's missing.
    private const val FILL_HAIR_L = 17.0
    private const val FILL_HAIR_B = 5.0
    private const val FILL_IRIS_L = 22.0
    private const val FILL_CONTRAST = 47.0
    private const val FILL_LIP_H = 33.0
    private const val FILL_LIP_C = 30.0

    /** Hue in (−180, 180]: bluish-pinks stay negative instead of wrapping to ~350°. */
    fun signedHue(c: Lab): Double = c.hue.let { if (it > 180) it - 360 else it }

    fun vector(f: Features): DoubleArray {
        val s = f.skin
        val hair = f.hair
        val iris = f.iris
        val lips = f.lips?.takeIf { !f.lipMakeup }
        val darkest = listOfNotNull(hair?.l, iris?.l).minOrNull()
        return doubleArrayOf(
            s.l, s.a, s.b, s.hue, s.chroma,
            hair?.l ?: FILL_HAIR_L, hair?.b ?: FILL_HAIR_B, if (hair != null) 1.0 else 0.0,
            iris?.l ?: FILL_IRIS_L, if (iris != null) 1.0 else 0.0,
            darkest?.let { s.l - it } ?: FILL_CONTRAST,
            lips?.let(::signedHue) ?: FILL_LIP_H, lips?.chroma ?: FILL_LIP_C, if (lips != null) 1.0 else 0.0,
        )
    }

    fun predict(f: Features): List<ToneScore> {
        val p = probabilities(vector(f))
        return ToneClassifier.TONE_IDS.indices.map { ToneScore(ToneClassifier.TONE_IDS[it], p[it]) }
            .sortedByDescending { it.probability }
    }

    /** Raw feature vector → 12 probabilities in [ToneClassifier.TONE_IDS] order. */
    fun probabilities(x: DoubleArray): DoubleArray {
        val w = ToneModelWeights
        val n = x.size
        val z = DoubleArray(n) { (x[it] - w.MEAN[it]) / w.SD[it] }
        val h = if (w.HIDDEN == 0) z else DoubleArray(w.HIDDEN) { j ->
            var acc = w.B1[j]
            for (i in 0 until n) acc += z[i] * w.W1[i * w.HIDDEN + j]
            tanh(acc)
        }
        val logits = DoubleArray(12) { k ->
            var acc = w.B2[k]
            for (j in h.indices) acc += h[j] * w.W2[j * 12 + k]
            acc / w.TEMPERATURE
        }
        val mx = logits.max()
        val e = DoubleArray(12) { exp(min(0.0, logits[it] - mx)) }
        val sum = e.sum()
        return DoubleArray(12) { e[it] / sum }
    }
}
