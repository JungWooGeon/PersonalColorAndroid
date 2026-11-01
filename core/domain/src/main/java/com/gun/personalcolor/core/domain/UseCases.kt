package com.gun.personalcolor.core.domain

import android.net.Uri
import com.gun.personalcolor.core.analysis.AnalysisResult
import com.gun.personalcolor.core.analysis.PersonalColorAnalyzer
import com.gun.personalcolor.core.analysis.Axes
import com.gun.personalcolor.core.analysis.QualityIssue
import com.gun.personalcolor.core.analysis.ToneClassifier
import com.gun.personalcolor.core.analytics.Analytics
import com.gun.personalcolor.core.data.HistoryRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.HistoryEntry
import com.gun.personalcolor.core.model.ResultSource
import com.gun.personalcolor.core.model.TonePercent
import com.gun.personalcolor.core.vision.FaceAnalyzer
import com.gun.personalcolor.core.vision.FaceOutcome
import com.gun.personalcolor.core.vision.PhotoInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface PhotoFailure {
    data object BadImage : PhotoFailure
    data object NoFace : PhotoFailure
    data object ManyFaces : PhotoFailure
    data class Quality(val issue: QualityIssue) : PhotoFailure
}

/** Saves a diagnosis as "current" and, if the user allows it, to history. */
class PublishDiagnosisUseCase @Inject constructor(
    private val session: DiagnosisSession,
    private val history: HistoryRepository,
    private val prefs: UserPreferencesRepository,
    private val analytics: Analytics,
) {
    suspend operator fun invoke(d: Diagnosis) {
        session.set(d)
        analytics.log("result", "tone" to d.top.toneId, "source" to d.source.name.lowercase())
        if (prefs.preferences.first().saveHistory) {
            history.add(HistoryEntry(d.top.toneId, d.source, System.currentTimeMillis(), d.scores.take(3).map { TonePercent(it.toneId, it.probability) }))
        }
    }
}

/**
 * Photos → face landmarks → color engine. With several photos of the same person, the axes are averaged
 * (lighting noise cancels out) and re-classified. Publishes the diagnosis and returns null, or returns why
 * none of the photos could be used (the first photo's problem).
 */
class AnalyzePhotoUseCase @Inject constructor(
    private val faces: FaceAnalyzer,
    private val publish: PublishDiagnosisUseCase,
    private val analytics: Analytics,
) {
    suspend operator fun invoke(uris: List<Uri>): PhotoFailure? = analyze(uris.map { PhotoInput(it) })

    /** Same, with optional white-paper reference points (guided camera). */
    suspend fun analyze(inputs: List<PhotoInput>): PhotoFailure? {
        val outcomes = withContext(Dispatchers.Default) {
            inputs.map { input -> runCatching { faces.analyze(input) }.getOrElse { analytics.error(it); FaceOutcome.BadImage } }
        }
        val ok = outcomes.mapNotNull { o -> (o as? FaceOutcome.Done)?.let { d -> (d.result as? AnalysisResult.Success)?.let { d.photo to it } } }
        if (ok.isEmpty()) {
            val failure = failureOf(outcomes.first())
            analytics.log("analysis_failed", "reason" to failure.toString())
            return failure
        }
        // Quality-weighted combination of all usable photos (the model decides; axes are for the explanation UI).
        val combined = if (ok.size == 1) ok.first().second else PersonalColorAnalyzer.combine(ok.map { it.second })
        val photo = ok.first().first
        analytics.log("analysis_photos", "count" to ok.size.toString())
        publish(Diagnosis(combined.scores, combined.axes, ResultSource.AI, photo, combined.features,
            photoCount = ok.size, confidence = combined.confidence))
        return null
    }

    private fun failureOf(o: FaceOutcome): PhotoFailure = when (o) {
        FaceOutcome.BadImage -> PhotoFailure.BadImage
        FaceOutcome.NoFace -> PhotoFailure.NoFace
        FaceOutcome.ManyFaces -> PhotoFailure.ManyFaces
        is FaceOutcome.Done -> (o.result as? AnalysisResult.Rejected)?.let { PhotoFailure.Quality(it.issue) } ?: PhotoFailure.BadImage
    }
}

/** Self-diagnosis answers already reduced to axes → published diagnosis. */
class QuizDiagnosisUseCase @Inject constructor(private val publish: PublishDiagnosisUseCase) {
    suspend operator fun invoke(axes: Axes): Diagnosis =
        Diagnosis(ToneClassifier.classify(axes), axes, ResultSource.QUIZ).also { publish(it) }
}
