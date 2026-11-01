package com.gun.personalcolor.core.vision

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.gun.personalcolor.core.analysis.Rgb
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** On-device garment cut-out (ML Kit subject segmentation). Nothing leaves the device. Call from a background dispatcher. */
@Singleton
class GarmentCutter @Inject constructor(@ApplicationContext private val context: Context) {
    private val segmenter by lazy {
        SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundBitmap().build())
    }

    /** Foreground on transparent, cropped to the subject + padding, max side [maxSide]. Null if nothing was found. */
    suspend fun cutout(bitmap: Bitmap, maxSide: Int = 1024): Bitmap? {
        // First use: Play services downloads the segmentation model on demand. Make sure it's installed (waits up to
        // ~45 s, usually a few seconds), then segment; one retry covers a just-finished install.
        ensureModel()
        val fg = segment(bitmap) ?: run {
            Thread.sleep(1_500) // ponytail: blocks one background thread once; fine for a single user action
            segment(bitmap)
        } ?: return null
        return runCatching { crop(fg, maxSide) }.getOrNull()
    }

    /** True when the model is ready. Requests the download if needed and waits for it (bounded). */
    suspend fun ensureModel(): Boolean {
        val client = ModuleInstall.getClient(context)
        val available = suspendCoroutine { c ->
            client.areModulesAvailable(segmenter).addOnSuccessListener { c.resume(it.areModulesAvailable()) }
                .addOnFailureListener { c.resume(false) }
        }
        if (available) return true
        val installed = suspendCoroutine { c ->
            client.installModules(ModuleInstallRequest.newBuilder().addApi(segmenter).build())
                .addOnSuccessListener { c.resume(true) }.addOnFailureListener { c.resume(false) }
        }
        if (!installed) return false
        repeat(30) {   // install call returns before the module is usable; poll briefly
            val ok = suspendCoroutine { c ->
                client.areModulesAvailable(segmenter).addOnSuccessListener { r -> c.resume(r.areModulesAvailable()) }
                    .addOnFailureListener { c.resume(false) }
            }
            if (ok) return true
            Thread.sleep(1_500)
        }
        return false
    }

    private suspend fun segment(bitmap: Bitmap): Bitmap? = suspendCoroutine { c ->
        runCatching {
            segmenter.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { c.resume(it.foregroundBitmap) }
                .addOnFailureListener { c.resume(null) }
        }.onFailure { c.resume(null) }
    }

    private fun crop(fg: Bitmap, maxSide: Int): Bitmap? {
        val w = fg.width; val h = fg.height
        val px = IntArray(w * h).also { fg.getPixels(it, 0, w, 0, 0, w, h) }
        var x0 = w; var y0 = h; var x1 = -1; var y1 = -1
        for (y in 0 until h) for (x in 0 until w) if ((px[y * w + x] ushr 24) > 24) {
            if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y
        }
        if (x1 < 0) return null
        val pad = (max(x1 - x0, y1 - y0) * 0.04f).roundToInt()
        x0 = max(0, x0 - pad); y0 = max(0, y0 - pad); x1 = min(w - 1, x1 + pad); y1 = min(h - 1, y1 + pad)
        val cropped = Bitmap.createBitmap(fg, x0, y0, x1 - x0 + 1, y1 - y0 + 1)
        val scale = maxSide.toFloat() / max(cropped.width, cropped.height)
        return if (scale >= 1f) cropped
        else Bitmap.createScaledBitmap(cropped, (cropped.width * scale).roundToInt().coerceAtLeast(1), (cropped.height * scale).roundToInt().coerceAtLeast(1), true)
    }

    /** Main garment color of a cut-out (or any photo): see [GarmentColor.dominant]. */
    fun dominantColor(cutout: Bitmap): Rgb {
        // Downsample to ≤ ~40k pixels; plenty for a color estimate.
        val step = max(1, (kotlin.math.sqrt(cutout.width * cutout.height / 40_000.0)).roundToInt())
        val small = if (step == 1) cutout
        else Bitmap.createScaledBitmap(cutout, max(1, cutout.width / step), max(1, cutout.height / step), false)
        val px = IntArray(small.width * small.height).also { small.getPixels(it, 0, small.width, 0, 0, small.width, small.height) }
        return GarmentColor.dominant(px)
    }
}

/** Pure color helpers (JVM-testable). Pixels are ARGB ints. */
object GarmentColor {
    /**
     * Opaque pixels only; drops the darkest and brightest 10% (shadows, highlights), then k-means (k = 3) in RGB
     * and returns the mean of the largest cluster. Grey if there is nothing opaque.
     */
    fun dominant(argb: IntArray): Rgb {
        val opaque = argb.filter { (it ushr 24) >= 200 }
        if (opaque.isEmpty()) return Rgb(128, 128, 128)
        val sorted = opaque.sortedBy { luma(it) }
        val cut = sorted.size / 10
        val px = sorted.subList(cut, sorted.size - cut).ifEmpty { sorted }

        // Seeds at the 1/6, 1/2, 5/6 luminance quantiles: deterministic and well spread.
        val centers = Array(3) { i -> px[(px.size * (2 * i + 1) / 6).coerceAtMost(px.size - 1)].let { doubleArrayOf(r(it).toDouble(), g(it).toDouble(), b(it).toDouble()) } }
        val label = IntArray(px.size)
        repeat(12) {
            px.forEachIndexed { i, p -> label[i] = centers.indices.minBy { dist2(p, centers[it]) } }
            for (k in centers.indices) {
                var n = 0; var rs = 0.0; var gs = 0.0; var bs = 0.0
                px.forEachIndexed { i, p -> if (label[i] == k) { n++; rs += r(p); gs += g(p); bs += b(p) } }
                if (n > 0) centers[k] = doubleArrayOf(rs / n, gs / n, bs / n)
            }
        }
        val counts = IntArray(3).also { c -> label.forEach { c[it]++ } }
        val c = centers[counts.indices.maxBy { counts[it] }]
        return Rgb(c[0].roundToInt(), c[1].roundToInt(), c[2].roundToInt())
    }

    private fun r(p: Int) = (p shr 16) and 0xFF
    private fun g(p: Int) = (p shr 8) and 0xFF
    private fun b(p: Int) = p and 0xFF
    private fun luma(p: Int) = 0.299 * r(p) + 0.587 * g(p) + 0.114 * b(p)
    private fun dist2(p: Int, c: DoubleArray): Double {
        val dr = r(p) - c[0]; val dg = g(p) - c[1]; val db = b(p) - c[2]
        return dr * dr + dg * dg + db * db
    }
}
