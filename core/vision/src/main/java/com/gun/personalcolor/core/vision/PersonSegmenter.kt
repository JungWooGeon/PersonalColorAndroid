package com.gun.personalcolor.core.vision

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** On-device person cutout (MediaPipe selfie segmenter). Call off the main thread. */
@Singleton
class PersonSegmenter @Inject constructor(@ApplicationContext private val context: Context) {
    private val segmenter by lazy {
        ImageSegmenter.createFromOptions(
            context,
            ImageSegmenter.ImageSegmenterOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("selfie_segmenter.tflite").build())
                .setRunningMode(RunningMode.IMAGE)
                .setOutputConfidenceMasks(true)
                .setOutputCategoryMask(false)
                .build(),
        )
    }

    // XNNPACK runs SME code (rdsvl, ...) when the kernel lists SME. Apple M4/M5 emulators list sme/sme2 yet trap it:
    // an uncatchable SIGILL. The GPU delegate isn't a way out (no GLES 3.1 there; aborts in post-processing on a
    // Note20), so on SME devices skip the cutout and drape draws without it.
    // ponytail: also skips real SME2 phones, where it likely works; revisit once one can be tested.
    private val smeCpu by lazy { runCatching { hasSme(File("/proc/cpuinfo").readText()) }.getOrDefault(false) }

    /** ALPHA_8 mask at [bitmap]'s size: person opaque, soft edge. Null if segmentation fails or is skipped (SME). */
    @Synchronized
    fun personMask(bitmap: Bitmap): Bitmap? = if (smeCpu) null else runCatching {
        val masks = segmenter.segment(BitmapImageBuilder(bitmap).build()).confidenceMasks().get()
        // The selfie model has a single "person" channel; if a model ever reports [background, person], take the last.
        val mask = masks.last()
        val w = mask.width; val h = mask.height
        val buf = ByteBufferExtractor.extract(mask).order(ByteOrder.nativeOrder()).asFloatBuffer()
        val conf = FloatArray(w * h).also { buf.get(it) }
        val alpha = MaskMath.soften(conf, w, h, radius = (w * 0.012f).roundToInt().coerceAtLeast(1))
        val out = ByteArray(w * h) { (alpha[it] * 255f).roundToInt().toByte() }
        val small = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8).apply { copyPixelsFromBuffer(ByteBuffer.wrap(out)) }
        if (w == bitmap.width && h == bitmap.height) small
        else Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
    }.getOrNull()
}

/** True if any cpuinfo "Features" line lists an SME extension (sme, sme2, sme2p1, smef64f64, ...). */
internal fun hasSme(cpuinfo: String): Boolean = cpuinfo.lineSequence()
    .filter { it.trimStart().startsWith("Features") }
    .any { line -> line.substringAfter(':').split(' ', '\t').any { it.startsWith("sme") } }

/** Pure mask helpers (JVM-testable). */
object MaskMath {
    /** Sharpens the model's soft confidence (0.3..0.7 → 0..1), then box-blurs by [radius] px for a feathered edge. */
    fun soften(conf: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val a = FloatArray(conf.size) { smoothstep(0.3f, 0.7f, conf[it]) }
        return boxBlur(boxBlur(a, w, h, radius, horizontal = true), w, h, radius, horizontal = false)
    }

    fun smoothstep(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }

    /** One separable box-blur pass (edge pixels clamped), O(n) with a running sum. */
    fun boxBlur(src: FloatArray, w: Int, h: Int, r: Int, horizontal: Boolean): FloatArray {
        val out = FloatArray(src.size)
        val len = if (horizontal) w else h
        val lines = if (horizontal) h else w
        val n = 2 * r + 1
        for (line in 0 until lines) {
            fun at(i: Int): Float {
                val c = i.coerceIn(0, len - 1)
                return if (horizontal) src[line * w + c] else src[c * w + line]
            }
            var sum = 0f
            for (i in -r..r) sum += at(i)
            for (i in 0 until len) {
                if (horizontal) out[line * w + i] = sum / n else out[i * w + line] = sum / n
                sum += at(i + r + 1) - at(i - r)
            }
        }
        return out
    }
}
