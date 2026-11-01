package com.gun.personalcolor.core.domain

import com.gun.personalcolor.core.analysis.Axes
import com.gun.personalcolor.core.analysis.Features
import com.gun.personalcolor.core.analysis.ToneScore
import com.gun.personalcolor.core.model.ResultSource
import com.gun.personalcolor.core.vision.FacePhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A finished diagnosis. [photo] stays in memory only (used by the result and draping screens). */
data class Diagnosis(
    val scores: List<ToneScore>,
    val axes: Axes,
    val source: ResultSource,
    val photo: FacePhoto? = null,
    val features: Features? = null,
    /** Photos averaged into this result (AI only). */
    val photoCount: Int = 1,
    /** 0..1 from the engine (margin + sample quality). Internal only — never shown to users. */
    val confidence: Double = 1.0,
) {
    val top get() = scores.first()
}

/** In-memory holder for the latest diagnosis, shared by the result/drape screens. Cleared on process death. */
@Singleton
class DiagnosisSession @Inject constructor() {
    private val _current = MutableStateFlow<Diagnosis?>(null)
    val current: StateFlow<Diagnosis?> = _current.asStateFlow()
    internal fun set(d: Diagnosis) { _current.value = d }
}
