package com.gun.personalcolor.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.gun.personalcolor.core.analysis.AnalysisResult
import com.gun.personalcolor.core.analysis.FaceSamples
import com.gun.personalcolor.core.analysis.PersonalColorAnalyzer
import com.gun.personalcolor.core.analysis.Rgb
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/** One photo to analyze. [whiteRef]: optional point (normalized 0..1 image coords) on a white paper in the photo. */
data class PhotoInput(val uri: Uri, val whiteRef: Offset? = null)

/** Photo + the 478 face landmarks in normalized (0..1) image coordinates. */
class FacePhoto(val bitmap: Bitmap, val landmarks: List<Offset>)

sealed interface FaceOutcome {
    data class Done(val photo: FacePhoto, val result: AnalysisResult) : FaceOutcome
    data object NoFace : FaceOutcome
    data object ManyFaces : FaceOutcome
    data object BadImage : FaceOutcome
}

/**
 * Runs entirely on device: the photo never leaves the phone. Call off the main thread.
 * The MediaPipe model (~4MB) loads on first use and stays for the app's lifetime.
 */
@Singleton
class FaceAnalyzer @Inject constructor(@ApplicationContext private val context: Context) {
    private val landmarker by lazy {
        FaceLandmarker.createFromOptions(
            context,
            FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_landmarker.task").build())
                .setRunningMode(RunningMode.IMAGE)
                .setNumFaces(2)
                .build(),
        )
    }

    @Synchronized
    fun analyze(input: PhotoInput): FaceOutcome {
        val bitmap = BitmapLoader.load(context, input.uri) ?: return FaceOutcome.BadImage
        val faces = landmarker.detect(BitmapImageBuilder(bitmap).build()).faceLandmarks()
        if (faces.isEmpty()) return FaceOutcome.NoFace
        if (faces.size > 1) return FaceOutcome.ManyFaces
        val points = faces[0].map { Offset(it.x(), it.y()) }
        val photo = FacePhoto(bitmap, points)
        return FaceOutcome.Done(photo, PersonalColorAnalyzer.analyze(Sampler(bitmap, points).samples(input.whiteRef)))
    }
}

object BitmapLoader {
    /** Decodes at most ~[maxSide]px on the long side, upright according to EXIF. Null if unreadable. */
    fun load(context: Context, uri: Uri, maxSide: Int = 1280): Bitmap? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val raw = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        } ?: return null
        val degrees = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        if (degrees == 0) raw
        else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
    }.getOrNull()
}

/**
 * Picks pixels from facial regions using MediaPipe face-mesh landmark indices.
 * Regions are kept separate (cheeks / forehead / chin, hair patches) so the engine can reject shadowed or
 * mis-placed patches and judge lighting from left/right consistency. Reads the bitmap once into an IntArray
 * (getPixel per sample is a JNI call each), so sampling stays well under ~20 ms at 1280 px.
 */
private class Sampler(bmp: Bitmap, lm: List<Offset>) {
    private val w = bmp.width
    private val h = bmp.height
    private val argb = IntArray(w * h).also { bmp.getPixels(it, 0, w, 0, 0, w, h) }
    private val p = lm.map { Offset(it.x * w, it.y * h) }
    private val faceW = dist(p[234], p[454])
    private val faceH = dist(p[10], p[152])
    private val up = (p[10] - p[152]).let { it / it.getDistance() }

    fun samples(whiteRef: Offset? = null): FaceSamples {
        // Lower cheeks (below the cheekbone, away from under-eye shadow and the nose), forehead centre, chin.
        val regions = mapOf(
            "cheekL" to disk(centroid(50, 205, 187), faceW * 0.055f),
            "cheekR" to disk(centroid(280, 425, 411), faceW * 0.055f),
            "forehead" to disk(centroid(151, 108, 337), faceW * 0.06f),
            "chin" to disk(centroid(199, 175), faceW * 0.04f),
        )
        val hairPatches = hairPatches()
        return FaceSamples(
            skin = regions.values.flatten(),
            sclera = sclera(468, 469, 471, 33, 133) + sclera(473, 474, 476, 263, 362),
            iris = irisRing(468, 469, 471) + irisRing(473, 474, 476),
            hair = hairPatches.flatten(),
            lips = disk(mid(p[14], p[17]), dist(p[14], p[17]) * 0.3f) + disk(mid(p[0], p[13]), dist(p[0], p[13]) * 0.3f),
            whiteRef = whiteRef?.let { disk(Offset(it.x * w, it.y * h), faceW * 0.04f) }.orEmpty(),
            skinRegions = regions,
            hairPatches = hairPatches,
        )
    }

    private fun irisRadius(a: Int, b: Int) = dist(p[a], p[b]) / 2f

    /** Eye white: points on the line from the iris edge toward each eye corner. */
    private fun sclera(center: Int, ea: Int, eb: Int, cornerA: Int, cornerB: Int): List<Rgb> {
        val c = p[center]
        val r = irisRadius(ea, eb)
        return listOf(p[cornerA], p[cornerB]).flatMap { corner ->
            val d = dist(c, corner)
            if (d <= r * 1.4f) return@flatMap emptyList()
            (0..8).flatMap { i ->
                val t = (r * 1.3f + (d * 0.8f - r * 1.3f) * i / 8f) / d
                disk(c + (corner - c) * t, max(1f, r * 0.12f))
            }
        }
    }

    /** Iris ring, skipping the pupil and the dark limbal edge. */
    private fun irisRing(center: Int, ea: Int, eb: Int): List<Rgb> {
        val c = p[center]
        val r = irisRadius(ea, eb)
        return pixels(c, r * 0.8f) { d -> d >= r * 0.4f }
    }

    /**
     * Several small patches: above the hairline (two heights, since foreheads differ) and beside the temples.
     * Any of them may be forehead, background or a hat — the engine judges each patch separately.
     */
    private fun hairPatches(): List<List<Rgb>> {
        val r = faceH * 0.035f
        val top = listOf(10, 109, 338).flatMap { id -> listOf(0.08f, 0.16f).map { k -> p[id] + up * (faceH * k) } }
        val right = (p[454] - p[234]).let { it / it.getDistance() }
        val sides = listOf(p[127] - right * (faceW * 0.06f), p[356] + right * (faceW * 0.06f)).map { it + up * (faceH * 0.1f) }
        return (top + sides).map { disk(it, r) }.filter { it.isNotEmpty() }
    }

    private fun centroid(vararg ids: Int) = ids.fold(Offset.Zero) { acc, i -> acc + p[i] } / ids.size.toFloat()
    private fun disk(c: Offset, r: Float) = pixels(c, r) { true }

    private inline fun pixels(c: Offset, r: Float, keep: (Float) -> Boolean): List<Rgb> {
        val out = ArrayList<Rgb>()
        val step = max(1, (r / 6f).roundToInt())   // ~150 px per disk max
        val ri = r.roundToInt()
        for (dy in -ri..ri step step) for (dx in -ri..ri step step) {
            val d = hypot(dx.toFloat(), dy.toFloat())
            if (d > r || !keep(d)) continue
            val x = (c.x + dx).roundToInt()
            val y = (c.y + dy).roundToInt()
            if (x !in 0 until w || y !in 0 until h) continue
            val px = argb[y * w + x]
            out += Rgb((px shr 16) and 0xFF, (px shr 8) and 0xFF, px and 0xFF)
        }
        return out
    }

    private fun dist(a: Offset, b: Offset) = (a - b).getDistance()
    private fun mid(a: Offset, b: Offset) = (a + b) / 2f
}
