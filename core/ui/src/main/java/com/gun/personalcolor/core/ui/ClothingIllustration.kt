package com.gun.personalcolor.core.ui

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.gun.personalcolor.core.model.CLOTHING_KINDS

/*
 * Garment drawing for a [CLOTHING_KINDS] key, filled with a color: the painted art from [GarmentArt], centered in a
 * 100×100 design box. Every kind has painted art; a kind without it (unknown key, a missing asset, or GarmentArt not
 * initialized, as in @Preview) draws a plain round chip in the color instead.
 */

/** Draws the garment centered in this scope's size, in a 100×100 design space. No composition needed. */
fun DrawScope.drawClothing(kind: String, color: Color) {
    GarmentArt.bitmap(kind, color)?.let { bmp -> drawPainted(bmp, kind); return }
    val s = size.minDimension / 100f
    val c = center
    drawCircle(color, CHIP_R * s, c)
    val outline = if (color.luminance() < 0.12f) lerp(color, Color.White, 0.35f) else lerp(color, Color.Black, 0.25f)
    drawCircle(outline, CHIP_R * s, c, style = Stroke(1.6f * s))
}

/**
 * The garment's silhouette as a soft blurred shadow (flat-lay contact shadow), placed like [drawClothing].
 * [blur] is in design units (100 = full width). Blur needs API 28+ with hardware drawing; older devices get a hard edge.
 */
fun DrawScope.drawClothingShadow(kind: String, color: Color = Color.Black, blur: Float = 4f) {
    val s = size.minDimension / 100f
    val p = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        if (blur > 0f) maskFilter = BlurMaskFilter(blur * s, BlurMaskFilter.Blur.NORMAL)
    }
    val bmp = GarmentArt.bitmap(kind, Color.Black)
    val r = paintedRect(kind)
    if (bmp == null || r == null) {
        p.color = color.toArgb()
        drawIntoCanvas { it.nativeCanvas.drawCircle(center.x, center.y, CHIP_R * s, p) }
        return
    }
    p.colorFilter = android.graphics.PorterDuffColorFilter(color.copy(alpha = 1f).toArgb(), android.graphics.PorterDuff.Mode.SRC_IN)
    p.alpha = (color.alpha * 255).toInt()
    drawIntoCanvas { it.nativeCanvas.drawBitmap(bmp, null, dstOf(r), p) }
}

/** Where the garment sits in its 100×100 design box, for layout. */
fun clothingBounds(kind: String): Rect = paintedRect(kind) ?: Rect(Offset(50f, 50f), CHIP_R)

private const val CHIP_R = 30f

/** Painted art fits [PAINTED_FILL] of the 100×100 design box, centered, keeping its aspect. */
private const val PAINTED_FILL = 94f

private fun paintedRect(kind: String): Rect? {
    val aspect = GarmentArt.aspect(kind)?.takeIf { it.isFinite() && it > 0f } ?: return null
    val w = if (aspect >= 1f) PAINTED_FILL else PAINTED_FILL * aspect
    val h = if (aspect >= 1f) PAINTED_FILL / aspect else PAINTED_FILL
    return Rect(50f - w / 2, 50f - h / 2, 50f + w / 2, 50f + h / 2)
}

private fun DrawScope.dstOf(r: Rect): android.graphics.RectF {
    val s = size.minDimension / 100f
    val ox = (size.width - 100 * s) / 2
    val oy = (size.height - 100 * s) / 2
    return android.graphics.RectF(ox + r.left * s, oy + r.top * s, ox + r.right * s, oy + r.bottom * s)
}

private fun DrawScope.drawPainted(bmp: Bitmap, kind: String) {
    val r = paintedRect(kind) ?: return
    drawIntoCanvas { it.nativeCanvas.drawBitmap(bmp, null, dstOf(r), PAINTED_PAINT) }
}

private val PAINTED_PAINT = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG)
