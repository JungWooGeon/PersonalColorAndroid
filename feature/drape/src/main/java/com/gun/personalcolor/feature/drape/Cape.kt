package com.gun.personalcolor.feature.drape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.gun.personalcolor.core.vision.FacePhoto
import kotlin.math.min
import kotlin.math.roundToInt

// MediaPipe face-oval landmarks from one jaw angle (288) round the chin (152) to the other (58).
private val JAW = listOf(288, 397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136, 172, 58)

/** Photo-space landmark [i] → canvas point inside [r]. */
internal fun FacePhoto.point(i: Int, r: Rect) = landmarks[i].let { Offset(r.left + it.x * r.width, r.top + it.y * r.height) }

/**
 * Fabric drape the way a consultant holds it: wrapped around the neck and shoulders just BELOW the chin, never over
 * the face. The top edge is a smooth neckline (lowest under the chin, falling toward the shoulders), with a soft
 * shadow under the chin, a top-to-bottom light falloff and a few gentle folds so it reads as cloth, not flat paint.
 */
internal fun DrawScope.drawCape(photo: FacePhoto, r: Rect, color: Color, toCanvas: Boolean = false) {
    if (photo.landmarks.size <= 152 || r.isEmpty) return
    val xs = photo.landmarks.map { r.left + it.x * r.width }
    val ys = photo.landmarks.map { r.top + it.y * r.height }
    val faceW = xs.max() - xs.min()
    val faceH = ys.max() - ys.min()
    val cx = (xs.min() + xs.max()) / 2
    val chinY = photo.point(152, r).y
    // Cloth stays inside the visible photo (fit mode: the photo rect; face-crop mode: the canvas).
    val left = if (toCanvas) 0f else maxOf(r.left, 0f)
    val right = if (toCanvas) size.width else minOf(r.right, size.width)
    val bottom = if (toCanvas) size.height else minOf(r.bottom, size.height)

    fun edgeY(x: Float): Float {
        // Flat just under the chin across the face width, then rising smoothly over the shoulders (outside the face)
        // so the clothes underneath don't show — like a cape tucked up around the neck.
        val t = kotlin.math.abs((x - cx) / faceW)
        val rise = ((t - 0.45f) / 0.6f).coerceIn(0f, 1f).let { (1 - kotlin.math.cos(it * Math.PI.toFloat())) / 2 }
        return chinY + faceH * (0.03f - 0.16f * rise)
    }
    val path = Path().apply {
        moveTo(left, edgeY(left))
        val steps = 48
        for (i in 1..steps) { val x = left + (right - left) * i / steps; lineTo(x, edgeY(x)) }
        lineTo(right, bottom); lineTo(left, bottom); close()
    }
    val top = chinY + faceH * 0.05f
    // Base cloth.
    drawPath(path, color)
    clipPath(path) {
        val h = (bottom - top).coerceAtLeast(1f)
        // Light from above: brighter near the neckline, deeper toward the bottom.
        drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.10f), 0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.22f), startY = top, endY = top + h))
        // Soft vertical folds.
        val folds = listOf(-0.95f to 0.16f, -0.35f to 0.10f, 0.3f to 0.12f, 0.9f to 0.15f)
        folds.forEach { (off, a) ->
            val fx = cx + off * faceW
            drawRect(Brush.horizontalGradient(
                0f to Color.Transparent, 0.5f to Color.Black.copy(alpha = a), 1f to Color.Transparent,
                startX = fx - faceW * 0.22f, endX = fx + faceW * 0.22f,
            ))
        }
        // Shadow the chin casts on the cloth.
        drawOval(
            Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent), center = Offset(cx, top), radius = faceW * 0.55f),
            topLeft = Offset(cx - faceW * 0.55f, top - faceW * 0.2f), size = Size(faceW * 1.1f, faceW * 0.55f),
        )
    }
}

/**
 * Consultant-style view: head-and-shoulders crop of the diagnosis photo, the person cut out with [mask] onto a neutral
 * grey [background], and a cape in [cloth] from just under the chin down. No mask (segmentation failed) → no cutout,
 * but the cape still hides the clothes. [original] shows the plain photo. [extra] draws on top (e.g. lipstick).
 */
@Composable
internal fun DrapedPhoto(
    photo: FacePhoto,
    mask: ImageBitmap?,
    background: Color,
    cloth: Color?,
    original: Boolean,
    modifier: Modifier,
    extra: DrawScope.(Rect) -> Unit = {},
) {
    val image = remember(photo) { photo.bitmap.asImageBitmap() }
    // Offscreen layer so the mask (DstIn) and background (DstOver) blend only within this canvas.
    Canvas(modifier.clipToBounds().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        // With a cutout, frame by the face (empty areas become background + cloth); without, stay inside the photo.
        val r = drawFaceFilled(image, photo, fixedFrame = mask != null && !original)
        if (original) return@Canvas
        if (mask != null) {
            drawImage(mask, dstOffset = IntOffset(r.left.roundToInt(), r.top.roundToInt()),
                dstSize = IntSize(r.width.roundToInt(), r.height.roundToInt()), blendMode = BlendMode.DstIn)
            drawRect(background, blendMode = BlendMode.DstOver)
        }
        if (cloth != null) drawCape(photo, r, cloth, toCanvas = mask != null)
        extra(r)
    }
}

/**
 * Zooms so the face (forehead to a bit below the chin, where the cape shows) fills the canvas, cropping the rest.
 * Returns the full image's rect in canvas space so landmark mapping keeps working.
 */
internal fun DrawScope.drawFaceFilled(image: ImageBitmap, photo: FacePhoto, fixedFrame: Boolean = false): Rect {
    if (photo.landmarks.isEmpty()) return Rect.Zero
    val xs = photo.landmarks.map { it.x * image.width }; val ys = photo.landmarks.map { it.y * image.height }
    val faceW = xs.max() - xs.min(); val faceH = ys.max() - ys.min()
    // Zero-size canvas (mid-layout) or a degenerate face box would make the scale NaN/∞ and roundToInt() throw.
    if (size.width <= 0f || size.height <= 0f || faceW <= 0f || faceH <= 0f) return Rect.Zero
    // Wanted view: whole head with a little room above, shoulders below. Then grow it to the canvas aspect
    // and keep it inside the photo, so the card is always fully covered by the photo (no empty bands).
    val aspect = size.width / size.height
    var h = faceH * 2.0f
    var w = h * aspect
    if (w < faceW * 1.8f) { w = faceW * 1.8f; h = w / aspect }
    if (fixedFrame) {
        // Same framing for every photo: face in the upper part, room for the cloth below, even for tight selfies.
        val cxF = (xs.min() + xs.max()) / 2
        val l = cxF - w / 2; val t = ys.min() - faceH * 0.35f
        val scale = size.width / w
        val r = Rect(-l * scale, -t * scale, (image.width - l) * scale, (image.height - t) * scale)
        drawImage(image, dstOffset = IntOffset(r.left.roundToInt(), r.top.roundToInt()), dstSize = IntSize(r.width.roundToInt(), r.height.roundToInt()))
        return r
    }
    val scaleDown = minOf(1f, image.width / w, image.height / h)
    w *= scaleDown; h *= scaleDown
    val cx = (xs.min() + xs.max()) / 2
    val cy = ys.min() - faceH * 0.35f + h / 2           // face sits in the upper part, cloth below
    val l = (cx - w / 2).coerceIn(0f, image.width - w)
    val t = (cy - h / 2).coerceIn(0f, image.height - h)
    val scale = size.width / w
    val r = Rect(-l * scale, -t * scale, (image.width - l) * scale, (image.height - t) * scale)
    drawImage(image, dstOffset = IntOffset(r.left.roundToInt(), r.top.roundToInt()), dstSize = IntSize(r.width.roundToInt(), r.height.roundToInt()))
    return r
}
