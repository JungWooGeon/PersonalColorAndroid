package com.gun.personalcolor.core.vision

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import dagger.hilt.android.qualifiers.ApplicationContext
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

    /** ALPHA_8 mask at [bitmap]'s size: person opaque, soft edge. Null if segmentation fails. */
    @Synchronized
    fun personMask(bitmap: Bitmap): Bitmap? = runCatching {
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
