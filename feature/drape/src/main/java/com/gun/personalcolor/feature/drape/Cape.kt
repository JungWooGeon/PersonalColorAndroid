package com.gun.personalcolor.feature.drape

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

/** Draws [image] fitted (letterboxed) into the canvas and returns where it landed. */
internal fun DrawScope.drawFitted(image: ImageBitmap): Rect {
    val scale = min(size.width / image.width, size.height / image.height)
    val w = image.width * scale; val h = image.height * scale
    val left = (size.width - w) / 2; val top = (size.height - h) / 2
    drawImage(image, dstOffset = IntOffset(left.roundToInt(), top.roundToInt()), dstSize = IntSize(w.roundToInt(), h.roundToInt()))
    return Rect(left, top, left + w, top + h)
}

/** Photo-space landmark [i] → canvas point inside [r]. */
internal fun FacePhoto.point(i: Int, r: Rect) = landmarks[i].let { Offset(r.left + it.x * r.width, r.top + it.y * r.height) }

/** Fabric "cape" from the jawline down to the bottom of the photo, like a consultant's drape. */
internal fun DrawScope.drawCape(photo: FacePhoto, r: Rect, color: Color) {
    fun pt(i: Int) = photo.point(i, r)
    val path = Path().apply {
        moveTo(r.right, pt(288).y)
        JAW.forEach { lineTo(pt(it).x, pt(it).y) }
        lineTo(r.left, pt(58).y)
        val bottom = maxOf(r.bottom, size.height)
        lineTo(r.left, bottom)
        lineTo(r.right, bottom)
        close()
    }
    drawPath(path, color)
}

/** The diagnosis photo with an optional cape; used by the try-on and the 정밀 진단 comparisons. */
@Composable
internal fun CapePhoto(photo: FacePhoto, color: Color?, modifier: Modifier, faceCrop: Boolean = false) {
    val image = remember(photo) { photo.bitmap.asImageBitmap() }
    Canvas(modifier.clipToBounds()) {
        val r = if (faceCrop) drawFaceFilled(image, photo) else drawFitted(image)
        if (color != null) drawCape(photo, r, color)
    }
}

/**
 * Zooms so the face (forehead to a bit below the chin, where the cape shows) fills the canvas, cropping the rest.
 * Returns the full image's rect in canvas space so landmark mapping keeps working.
 */
internal fun DrawScope.drawFaceFilled(image: ImageBitmap, photo: FacePhoto): Rect {
    val xs = photo.landmarks.map { it.x * image.width }; val ys = photo.landmarks.map { it.y * image.height }
    val faceW = xs.max() - xs.min(); val faceH = ys.max() - ys.min()
    val cropW = faceW * 1.9f
    val cropTop = ys.min() - faceH * 0.25f; val cropBottom = ys.max() + faceH * 0.55f
    val cropH = cropBottom - cropTop
    val scale = maxOf(size.width / cropW, size.height / cropH)
    val cx = (xs.min() + xs.max()) / 2; val cy = (cropTop + cropBottom) / 2
    val left = size.width / 2 - cx * scale; val top = size.height / 2 - cy * scale
    val r = Rect(left, top, left + image.width * scale, top + image.height * scale)
    drawImage(image, dstOffset = IntOffset(r.left.roundToInt(), r.top.roundToInt()), dstSize = IntSize(r.width.roundToInt(), r.height.roundToInt()))
    return r
}
