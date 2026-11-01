package com.gun.personalcolor.feature.drape

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.gun.personalcolor.core.analysis.ColorMath
import com.gun.personalcolor.core.analysis.Lab
import com.gun.personalcolor.core.analysis.Rgb
import com.gun.personalcolor.core.ui.hexColor
import com.gun.personalcolor.core.vision.FacePhoto
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

// MediaPipe face-mesh lip contours: outer edge of the lips and the mouth opening.
private val LIP_OUTER = listOf(61, 185, 40, 39, 37, 0, 267, 269, 270, 409, 291, 375, 321, 405, 314, 17, 84, 181, 91, 146)
private val LIP_INNER = listOf(78, 191, 80, 81, 82, 13, 312, 311, 310, 415, 308, 324, 318, 402, 317, 14, 87, 178, 88, 95)

/** Rendered lipstick for the lip box ([left], [top], photo pixels): tinted pixels with alpha = mask × intensity. */
internal class LipPatch(val image: ImageBitmap, private val left: Int, private val top: Int, private val photoW: Int, private val photoH: Int) {
    /** Draws over the photo that [drawFaceFilled] placed at [r]. */
    fun draw(scope: DrawScope, r: Rect) {
        val sx = r.width / photoW; val sy = r.height / photoH
        scope.drawImage(
            image,
            dstOffset = IntOffset((r.left + left * sx).roundToInt(), (r.top + top * sy).roundToInt()),
            dstSize = IntSize((image.width * sx).roundToInt(), (image.height * sy).roundToInt()),
        )
    }
}

/**
 * Per-photo lip analysis (mask + Lab of every pixel in the lip box), done once; [render] is then cheap enough to
 * run on every slider step. Work happens only in the lip bounding box, at photo resolution.
 */
internal class LipMakeup private constructor(
    private val photo: FacePhoto,
    private val left: Int, private val top: Int, private val w: Int, private val h: Int,
    private val mask: FloatArray,
    private val labs: Array<Lab>,
    private val stats: LipTint.Stats,
) {
    fun render(hex: String, intensity: Float, glossy: Boolean): LipPatch {
        val target = hexColor(hex).toArgb().let { ColorMath.rgbToLab(Rgb(it shr 16 and 0xFF, it shr 8 and 0xFF, it and 0xFF)) }
        val out = IntArray(w * h)
        for (i in out.indices) {
            val a = LipTint.alpha(mask[i].toDouble(), intensity.toDouble())
            if (a == 0) continue
            val c = ColorMath.labToRgb(LipTint.tint(labs[i], stats, target, glossy))
            out[i] = (a shl 24) or (c.r shl 16) or (c.g shl 8) or c.b
        }
        val bmp = Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
        return LipPatch(bmp.asImageBitmap(), left, top, photo.bitmap.width, photo.bitmap.height)
    }

    companion object {
        /** null when the mouth is too small or not found. Call off the main thread. */
        fun of(photo: FacePhoto): LipMakeup? {
            val bmp = photo.bitmap
            if (photo.landmarks.size <= LIP_OUTER.max()) return null // not a full face mesh
            fun p(i: Int) = photo.landmarks[i].let { PointF(it.x * bmp.width, it.y * bmp.height) }
            val outer = LIP_OUTER.map(::p); val inner = LIP_INNER.map(::p)
            val c1 = p(61); val c2 = p(291)
            val mouthW = hypot(c2.x - c1.x, c2.y - c1.y)
            if (mouthW < 8f) return null
            val margin = mouthW * 0.15f
            val left = (outer.minOf { it.x } - margin).toInt().coerceAtLeast(0)
            val top = (outer.minOf { it.y } - margin).toInt().coerceAtLeast(0)
            val w = ceil(outer.maxOf { it.x } + margin).toInt().coerceAtMost(bmp.width) - left
            val h = ceil(outer.maxOf { it.y } + margin).toInt().coerceAtMost(bmp.height) - top
            if (w <= 2 || h <= 2) return null

            // Lip polygon (outer minus the mouth opening), anti-aliased.
            val polyBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(polyBmp).apply {
                translate(-left.toFloat(), -top.toFloat())
                drawPath(Path().apply { smoothLoop(outer); smoothLoop(inner); fillType = Path.FillType.EVEN_ODD }, Paint(Paint.ANTI_ALIAS_FLAG))
            }
            val polyPx = IntArray(w * h).also { polyBmp.getPixels(it, 0, w, 0, 0, w, h) }
            polyBmp.recycle()
            val poly = FloatArray(w * h) { (polyPx[it] ushr 24) / 255f }

            val px = IntArray(w * h).also { bmp.getPixels(it, 0, w, left, top, w, h) }
            val labs = Array(w * h) { i -> px[i].let { ColorMath.rgbToLab(Rgb(it shr 16 and 0xFF, it shr 8 and 0xFF, it and 0xFF)) } }
            val lip = labs.filterIndexed { i, _ -> poly[i] > 0.5f }
            if (lip.size < 30) return null
            val skin = labs.filterIndexed { i, _ -> poly[i] == 0f }.map { it.l }.sorted()
            val stats = LipTint.stats(lip, skin.getOrNull(skin.size / 2))

            // Soft edge (~2.5% of mouth width), pulled slightly inward so colour doesn't spill onto skin,
            // tapered at the mouth corners, and weakened on pixels that don't look like lip (teeth, shadow).
            val blurred = boxBlur(boxBlur(poly, w, h, (mouthW * 0.0125f).roundToInt().coerceAtLeast(1)), w, h, (mouthW * 0.0125f).roundToInt().coerceAtLeast(1))
            val mask = FloatArray(w * h) { i ->
                if (blurred[i] <= 0f) return@FloatArray 0f
                val x = left + i % w + 0.5f; val y = top + i / w + 0.5f
                val corner = minOf(hypot(x - c1.x, y - c1.y), hypot(x - c2.x, y - c2.y)) / mouthW
                (LipTint.smooth(0.15, 0.95, blurred[i].toDouble()) * LipTint.smooth(0.0, 0.08, corner.toDouble()) *
                    LipTint.colorWeight(labs[i], stats)).toFloat()
            }
            return LipMakeup(photo, left, top, w, h, mask, labs, stats)
        }

        /** Separable box blur with clamped edges; two passes ≈ Gaussian. */
        private fun boxBlur(src: FloatArray, w: Int, h: Int, r: Int): FloatArray {
            val tmp = FloatArray(src.size); val out = FloatArray(src.size); val n = 2 * r + 1
            for (y in 0 until h) for (x in 0 until w) {
                var s = 0f; for (k in -r..r) s += src[y * w + (x + k).coerceIn(0, w - 1)]; tmp[y * w + x] = s / n
            }
            for (y in 0 until h) for (x in 0 until w) {
                var s = 0f; for (k in -r..r) s += tmp[(y + k).coerceIn(0, h - 1) * w + x]; out[y * w + x] = s / n
            }
            return out
        }

        /** Closed curve through the midpoints of [p], using each landmark as a control point, so the contour has no corners. */
        private fun Path.smoothLoop(p: List<PointF>) {
            fun mid(i: Int) = p[i % p.size].let { a -> p[(i + 1) % p.size].let { b -> PointF((a.x + b.x) / 2, (a.y + b.y) / 2) } }
            moveTo(mid(0).x, mid(0).y)
            for (i in 1..p.size) { val c = p[i % p.size]; val e = mid(i); quadTo(c.x, c.y, e.x, e.y) }
            close()
        }
    }
}
