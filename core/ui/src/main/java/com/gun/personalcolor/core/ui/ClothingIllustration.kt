package com.gun.personalcolor.core.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import java.util.concurrent.ConcurrentHashMap

/** Flat-lay garment illustration for a [CLOTHING_KINDS] key, filled with [color]. Square-ish; unknown kinds draw a round chip. */
@Composable
fun ClothingIllustration(kind: String, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawClothing(kind, color) }
}

/** Offscreen render (widgets, notifications): same drawing as [ClothingIllustration]. */
fun renderClothingBitmap(kind: String, color: Color, sizePx: Int, density: Float): Bitmap {
    val image = ImageBitmap(sizePx, sizePx)
    CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(image), Size(sizePx.toFloat(), sizePx.toFloat())) {
        drawClothing(kind, color)
    }
    return image.asAndroidBitmap()
}

/** Draws the garment centered in this scope's size, in a 100×100 design space. No composition needed. */
fun DrawScope.drawClothing(kind: String, color: Color) {
    val art = cache.getOrPut(kind) { buildArt(kind) }
    val lum = color.luminance()
    val dark = lum < 0.12f
    // Light colors get a darker outline; very dark ones a lighter one so they still read on dark surfaces.
    val outline = if (dark) lerp(color, Color.White, 0.35f) else lerp(color, Color.Black, if (lum > 0.75f) 0.35f else 0.25f)
    val detail = if (dark) lerp(color, Color.White, 0.2f) else lerp(color, Color.Black, if (lum > 0.75f) 0.2f else 0.16f)
    fun tint(t: Tint) = when (t) {
        Tint.Base -> color
        Tint.Inner -> if (dark) lerp(color, Color.White, 0.12f) else lerp(color, Color.Black, 0.12f)
        Tint.Light -> if (lum > 0.8f) lerp(color, Color.Black, 0.07f) else Color(0xFFF6F3EE)
        Tint.Outline -> outline
    }
    val s = size.minDimension / 100f
    val outlineStroke = Stroke(1.6f, join = StrokeJoin.Round, cap = StrokeCap.Round)
    val detailStroke = Stroke(1.1f, join = StrokeJoin.Round, cap = StrokeCap.Round)
    translate((size.width - 100 * s) / 2, (size.height - 100 * s) / 2) {
        scale(s, s, pivot = Offset.Zero) {
            // Soft drop shadow: two offset passes.
            art.ops.forEach { op ->
                if (op is Op.Fill) {
                    translate(0.6f, 2.6f) { drawPath(op.path, Color.Black.copy(alpha = 0.07f)) }
                    translate(0.3f, 1.3f) { drawPath(op.path, Color.Black.copy(alpha = 0.07f)) }
                }
            }
            art.ops.forEach { op ->
                when (op) {
                    is Op.Fill -> { drawPath(op.path, tint(op.tint)); drawPath(op.path, outline, style = outlineStroke) }
                    is Op.Line -> drawPath(op.path, if (op.strong) outline else detail, style = detailStroke)
                    is Op.Thick -> drawPath(op.path, tint(op.tint), style = Stroke(op.width, join = StrokeJoin.Round, cap = StrokeCap.Round))
                    is Op.Dot -> drawCircle(detail, op.r, op.c)
                }
            }
        }
    }
}

// --- Art building (100×100 design space, x right, y down) ---

private enum class Tint { Base, Inner, Light, Outline }

private sealed interface Op {
    class Fill(val path: Path, val tint: Tint) : Op
    class Line(val path: Path, val strong: Boolean) : Op
    class Thick(val path: Path, val width: Float, val tint: Tint) : Op
    class Dot(val c: Offset, val r: Float) : Op
}

private val cache = ConcurrentHashMap<String, Art>()

private class Art {
    val ops = mutableListOf<Op>()
    fun fill(tint: Tint = Tint.Base, block: Path.() -> Unit) { ops += Op.Fill(Path().apply(block), tint) }
    fun line(strong: Boolean = false, block: Path.() -> Unit) { ops += Op.Line(Path().apply(block), strong) }
    fun thick(width: Float, tint: Tint, block: Path.() -> Unit) { ops += Op.Thick(Path().apply(block), width, tint) }
    fun dot(x: Float, y: Float, r: Float = 1.3f) { ops += Op.Dot(Offset(x, y), r) }
    /** Short parallel lines between segment a1→a2 and b1→b2 (ribbing, quilting). */
    fun ribs(a1: Offset, a2: Offset, b1: Offset, b2: Offset, n: Int) = line {
        for (i in 1..n) {
            val t = i / (n + 1f)
            val a = androidx.compose.ui.geometry.lerp(a1, a2, t)
            val b = androidx.compose.ui.geometry.lerp(b1, b2, t)
            moveTo(a.x, a.y); lineTo(b.x, b.y)
        }
    }
}

/** Polyline through x,y pairs. */
private fun Path.poly(vararg p: Float, close: Boolean = true) {
    moveTo(p[0], p[1])
    for (i in 2 until p.size step 2) lineTo(p[i], p[i + 1])
    if (close) close()
}

/** Closed symmetric shape from its left half: points from top-center down the left side to bottom-center. */
private fun Path.sym(vararg p: Float) {
    poly(*p, close = false)
    for (i in p.size - 2 downTo 0 step 2) lineTo(100 - p[i], p[i + 1])
    close()
}

/** An open polyline and its mirror image. */
private fun Path.mirrored(vararg p: Float) {
    poly(*p, close = false)
    poly(*FloatArray(p.size) { if (it % 2 == 0) 100 - p[it] else p[it] }, close = false)
}

/** A closed polygon and its mirror image. */
private fun Path.mirroredPoly(vararg p: Float) {
    poly(*p)
    poly(*FloatArray(p.size) { if (it % 2 == 0) 100 - p[it] else p[it] })
}

/** Sampled quadratic curve (start included) for use inside point lists. */
private fun q(x0: Float, y0: Float, cx: Float, cy: Float, x1: Float, y1: Float, n: Int = 6): FloatArray {
    val out = FloatArray((n + 1) * 2)
    for (i in 0..n) {
        val t = i / n.toFloat(); val u = 1 - t
        out[i * 2] = u * u * x0 + 2 * u * t * cx + t * t * x1
        out[i * 2 + 1] = u * u * y0 + 2 * u * t * cy + t * t * y1
    }
    return out
}

private fun o(x: Float, y: Float) = Offset(x, y)

private fun buildArt(kind: String): Art = Art().apply {
    when (kind) {
        "tee" -> {
            fill { sym(*q(50f, 24f, 42f, 24f, 40f, 16f), 28f, 19.5f, 12f, 33f, 19.5f, 43.5f, 30f, 37f, 30f, 86f, 50f, 86f) }
            line { mirrored(*q(50f, 28f, 40f, 27.5f, 38.6f, 17.8f)) }
            line { mirrored(14.6f, 30.8f, 22.1f, 41.3f) }
            line { mirrored(50f, 82.5f, 30f, 82.5f) }
        }
        "sleeveless" -> {
            fill { sym(*q(50f, 27f, 42f, 26f, 38.5f, 13f), *q(32.5f, 13f, 33f, 31f, 27f, 34f), 28f, 86f, 50f, 86f) }
            line { mirrored(*q(50f, 30.5f, 40f, 29.5f, 36f, 13.6f)) }
            line { mirrored(*q(35.5f, 13.5f, 36.5f, 32f, 27.5f, 37.5f)) }
            line { mirrored(50f, 82.5f, 28f, 82.5f) }
        }
        "shirt" -> {
            fill { sym(50f, 21f, 43f, 14.5f, 29f, 18f, 13f, 68f, 22.5f, 71.5f, 31f, 39f, 31f, 85f, *q(31f, 85f, 38f, 89f, 50f, 88f)) }
            line { mirrored(14.4f, 63.5f, 23.7f, 66.8f) }
            line { moveTo(52.5f, 25f); lineTo(52.5f, 87.5f) }
            line { poly(36f, 40f, 43f, 40f, 43f, 48f, 39.5f, 50f, 36f, 48f) }
            listOf(34f, 46f, 58f, 70f, 81f).forEach { dot(50.2f, it, 1.1f) }
            fill(Tint.Inner) { poly(44f, 13f, 56f, 13f, 54f, 16.5f, 46f, 16.5f) }
            fill { mirroredPoly(44f, 13f, 37.5f, 16.5f, 42f, 30f, 50f, 22f) }
        }
        "knit" -> {
            fill { sym(*q(50f, 23f, 42f, 23f, 40.5f, 15f), 28f, 19f, 13.5f, 70f, 23.5f, 73f, 30.5f, 38f, 30f, 88f, 50f, 88f) }
            line { mirrored(*q(50f, 27f, 40f, 26.5f, 38.6f, 17.5f)) }
            line { mirrored(50f, 80f, 30.1f, 80f) }
            ribs(o(31f, 81.5f), o(69f, 81.5f), o(31f, 87f), o(69f, 87f), 10)
            line { mirrored(15.2f, 64f, 24.7f, 67f) }
            ribs(o(15.2f, 64f), o(24.7f, 67f), o(13.5f, 70f), o(23.5f, 73f), 3)
            ribs(o(84.8f, 64f), o(75.3f, 67f), o(86.5f, 70f), o(76.5f, 73f), 3)
        }
        "sweatshirt" -> {
            fill { sym(*q(50f, 23f, 42f, 23f, 40.5f, 15f), 28f, 19f, 13.5f, 70f, 23.5f, 73f, 30.5f, 38f, 30.5f, 80f, 32f, 88f, 50f, 88f) }
            line { mirrored(*q(50f, 27f, 40f, 26.5f, 38.6f, 17.5f)) }
            line { mirrored(39.8f, 17.4f, 30.6f, 37.5f) }
            line { mirrored(50f, 80f, 30.5f, 80f) }
            line { mirrored(15.2f, 64f, 24.7f, 67f) }
            line { poly(47.5f, 27.3f, 50f, 31f, 52.5f, 27.3f, close = false) }
        }
        "cardigan" -> {
            fill { sym(50f, 18f, 40.5f, 15f, 28f, 19f, 13.5f, 70f, 23.5f, 73f, 30.5f, 38f, 30f, 88f, 50f, 88f) }
            fill(Tint.Inner) { poly(41.5f, 15.5f, 50f, 18.5f, 58.5f, 15.5f, 50f, 42f) }
            line { moveTo(50f, 42f); lineTo(50f, 88f) }
            line { mirrored(38.4f, 16.4f, 47.2f, 45f, 47.2f, 87.5f) }
            listOf(50f, 60f, 70f).forEach { dot(50f, it, 1.4f) }
            line { mirrored(47.2f, 81f, 30.1f, 81f) }
            ribs(o(31f, 82.5f), o(46f, 82.5f), o(31f, 87.2f), o(46f, 87.2f), 4)
            ribs(o(54f, 82.5f), o(69f, 82.5f), o(54f, 87.2f), o(69f, 87.2f), 4)
            line { mirrored(15.2f, 64f, 24.7f, 67f) }
            ribs(o(15.2f, 64f), o(24.7f, 67f), o(13.5f, 70f), o(23.5f, 73f), 3)
            ribs(o(84.8f, 64f), o(75.3f, 67f), o(86.5f, 70f), o(76.5f, 73f), 3)
        }
        "jacket" -> {
            fill { sym(50f, 18f, 42f, 15f, 29f, 19f, 15f, 70f, 24.5f, 73f, 31.5f, 40f, 31f, 84f, 50f, 84f) }
            fill(Tint.Inner) { poly(43.5f, 16f, 50f, 19f, 56.5f, 16f, 50f, 52f) }
            fill { mirroredPoly(43.5f, 16f, 36f, 24f, 39f, 27f, 35.5f, 31f, 50f, 53f) }
            line { moveTo(50f, 53f); lineTo(50f, 84f) }
            dot(50f, 60f, 1.5f); dot(50f, 71f, 1.5f)
            line { mirrored(34f, 66f, 44f, 66f) }
            line { moveTo(57f, 40f); lineTo(64f, 40f) }
            line { mirrored(16.4f, 65f, 25.6f, 68f) }
        }
        "coat" -> {
            fill { sym(50f, 18f, 42f, 15f, 28f, 19f, 14f, 72f, 24f, 75f, 31f, 40f, 29.5f, 96f, 50f, 96f) }
            fill(Tint.Inner) { poly(43.5f, 16f, 50f, 19f, 56.5f, 16f, 50f, 46f) }
            fill { mirroredPoly(43.5f, 16f, 34f, 25f, 38f, 28.5f, 33f, 33f, 50f, 47f) }
            line { moveTo(50f, 47f); lineTo(50f, 96f) }
            listOf(56f, 68f).forEach { dot(44f, it, 1.5f); dot(56f, it, 1.5f) }
            line { mirrored(33f, 76f, 42f, 76f) }
            line { mirrored(15.4f, 67f, 25f, 70f) }
        }
        "trench" -> {
            fill { sym(50f, 18f, 42f, 15f, 28f, 19f, 14f, 72f, 24f, 75f, 31f, 40f, 29.5f, 96f, 50f, 96f) }
            fill(Tint.Inner) { poly(43.5f, 16f, 50f, 19f, 56.5f, 16f, 50f, 44f) }
            fill { mirroredPoly(43.5f, 16f, 34f, 25f, 38f, 28.5f, 33f, 33f, 50f, 45f) }
            fill { mirroredPoly(30.5f, 19f, 40f, 16f, 40.5f, 18.5f, 31f, 21.5f) }
            line { moveTo(50f, 45f); lineTo(50f, 96f) }
            dot(44f, 50f, 1.4f); dot(56f, 50f, 1.4f); dot(44f, 70f, 1.4f); dot(56f, 70f, 1.4f)
            fill { poly(30.7f, 56f, 69.3f, 56f, 69.4f, 61.5f, 30.6f, 61.5f) }
            fill { poly(45.5f, 54.8f, 54.5f, 54.8f, 54.5f, 62.7f, 45.5f, 62.7f) }
            line { poly(47.8f, 57f, 52.2f, 57f, 52.2f, 60.5f, 47.8f, 60.5f) }
            fill { poly(55f, 61.5f, 59f, 61.5f, 61f, 72f, 57f, 72f) }
            line { mirrored(15.4f, 67f, 25f, 70f) }
            line { mirrored(14.9f, 69f, 24.5f, 72f) }
        }
        "padding" -> {
            fill { sym(50f, 16f, 41f, 14f, 27f, 20f, 12f, 70f, 24f, 74f, 29f, 40f, 28f, 86f, *q(28f, 86f, 38f, 88.5f, 50f, 88.5f)) }
            line {
                listOf(30f, 42f, 54f, 66f, 78f).forEach { y ->
                    val x = if (y < 40f) 31f else 29f - (y - 40f) / 46f
                    moveTo(x, y); quadraticTo(50f, y + 2.5f, 100 - x, y)
                }
            }
            ribs(o(27f, 20f), o(12f, 70f), o(29f, 40f), o(24f, 74f), 3)
            ribs(o(73f, 20f), o(88f, 70f), o(71f, 40f), o(76f, 74f), 3)
            line { moveTo(50f, 17f); lineTo(50f, 88.5f) }
            fill { addRoundRect(RoundRect(39f, 9f, 61f, 18f, CornerRadius(3f))) }
            line(strong = true) { poly(48.6f, 20f, 51.4f, 20f, 51.4f, 25f, 48.6f, 25f) }
        }
        "pants" -> {
            fill { sym(50f, 12f, 30f, 12f, 26.5f, 90f, 45f, 90f, 50f, 33f) }
            line { mirrored(50f, 18.5f, 29.8f, 18.5f) }
            line { poly(53.5f, 18.5f, 53.5f, 28f, 50f, 32.5f, close = false) }
            line { mirrored(37f, 21f, 36f, 89f) }
            line { mirrored(31f, 19f, 37.5f, 30f) }
            line { mirrored(36f, 12f, 36f, 18.5f) }
        }
        "jeans" -> {
            fill { sym(50f, 12f, 30f, 12f, 28f, 90f, 45.5f, 90f, 50f, 34f) }
            line { mirrored(50f, 18.5f, 29.8f, 18.5f) }
            line { poly(53.5f, 18.5f, 53.5f, 29f, 50f, 33f, close = false) }
            line { mirrored(*q(29.8f, 25f, 38f, 25f, 40f, 18.5f)) }
            line { mirrored(36f, 12f, 36f, 18.5f) }
            line { moveTo(50f, 12f); lineTo(50f, 18.5f) }
            line { mirrored(28.2f, 85.5f, 45.6f, 85.5f) }
            dot(30.7f, 25.5f, 0.9f); dot(69.3f, 25.5f, 0.9f)
        }
        "skirt" -> {
            fill { sym(50f, 16f, 37f, 16f, 23f, 84f, *q(23f, 84f, 36f, 88.5f, 50f, 88.5f)) }
            line { mirrored(50f, 22.5f, 35.8f, 22.5f) }
            line { mirrored(42f, 30f, 35f, 86.5f) }
            line { moveTo(50f, 32f); lineTo(50f, 88f) }
        }
        "shorts" -> {
            fill { sym(50f, 22f, 30f, 22f, 25f, 72f, 46f, 74f, 50f, 50f) }
            line { mirrored(50f, 28.5f, 29.7f, 28.5f) }
            line { mirrored(25.4f, 67f, 46.6f, 69f) }
            line { poly(53.5f, 28.5f, 53.5f, 40f, 50f, 44f, close = false) }
            line { mirrored(31f, 28.5f, 36.5f, 38f) }
        }
        "dress" -> {
            fill {
                sym(*q(50f, 22f, 44f, 22f, 41f, 14.5f), 31f, 16.5f, 25.5f, 28f, 31.5f, 31.5f, 34f, 41f, 36.5f, 50f,
                    21f, 92f, *q(21f, 92f, 35f, 96f, 50f, 96f))
            }
            line { mirrored(*q(50f, 25.5f, 42f, 25.5f, 39.6f, 16f)) }
            line { mirrored(50f, 50f, 36.5f, 50f) }
            line { mirrored(42.5f, 54f, 34f, 94f) }
            line { moveTo(50f, 55f); lineTo(50f, 95.5f) }
        }
        "scarf" -> {
            fill { addRoundRect(RoundRect(24f, 12f, 76f, 30f, CornerRadius(9f))) }
            line { moveTo(31f, 18f); quadraticTo(50f, 23f, 69f, 18f) }
            fill { poly(28f, 24f, 47f, 24f, 45f, 86f, 27f, 86f) }
            fill { poly(53f, 26f, 71f, 26f, 76f, 82f, 58f, 84f) }
            line { poly(27.4f, 74f, 45.4f, 74f, close = false); poly(27.3f, 78f, 45.3f, 78f, close = false) }
            line { poly(57f, 72f, 74.9f, 70f, close = false); poly(57.4f, 76f, 75.3f, 74f, close = false) }
            line(strong = true) {
                for (i in 0..5) { val x = 29f + i * 3.2f; moveTo(x, 86f); lineTo(x, 91f) }
                for (i in 0..5) { val x = 59.5f + i * 3.2f; val y = 83.8f - i * 0.36f; moveTo(x, y); lineTo(x + 0.4f, y + 5f) }
            }
        }
        "bag" -> {
            val handle: Path.() -> Unit = { moveTo(35f, 44f); cubicTo(35f, 12f, 65f, 12f, 65f, 44f) }
            thick(7f, Tint.Outline, handle)
            thick(4.2f, Tint.Base, handle)
            fill { moveTo(20f, 40f); lineTo(80f, 40f); lineTo(85f, 84f); quadraticTo(85.5f, 90f, 79f, 90f); lineTo(21f, 90f); quadraticTo(14.5f, 90f, 15f, 84f); close() }
            line { moveTo(19.4f, 46f); lineTo(80.6f, 46f) }
            line { mirroredPoly(31f, 46f, 39f, 46f, 39f, 54f, 31f, 54f) }
        }
        "hat" -> {
            fill { moveTo(20f, 62f); cubicTo(20f, 22f, 80f, 22f, 80f, 62f); close() }
            line { moveTo(50f, 32.5f); lineTo(50f, 64f); mirrored(*q(50f, 32.5f, 36f, 40f, 33f, 63.5f)) }
            fill { addOval(androidx.compose.ui.geometry.Rect(47.4f, 28.4f, 52.6f, 33.6f)) }
            fill { moveTo(12f, 62f); quadraticTo(50f, 88f, 88f, 62f); quadraticTo(50f, 70f, 12f, 62f); close() }
            line { val c = q(17f, 64.6f, 50f, 83.5f, 83f, 64.6f, 10); poly(*c, close = false) }
        }
        "shoes" -> { shoe(4f, -17f, Tint.Inner); shoe(-2f, -3f, Tint.Base) }
        "socks" -> { sock(5f, -6f, Tint.Inner); sock(-12f, 3f, Tint.Base) }
        "umbrella" -> {
            thick(3.4f, Tint.Outline) { moveTo(50f, 48f); lineTo(50f, 84f); quadraticTo(50f, 92f, 43.5f, 91f); quadraticTo(39f, 90f, 39f, 85.5f) }
            fill {
                moveTo(8f, 50f); cubicTo(10f, 14f, 90f, 14f, 92f, 50f)
                quadraticTo(81.5f, 43f, 71f, 50f); quadraticTo(60.5f, 43f, 50f, 50f)
                quadraticTo(39.5f, 43f, 29f, 50f); quadraticTo(18.5f, 43f, 8f, 50f); close()
            }
            line { mirrored(*q(50f, 23.5f, 37f, 28f, 29f, 50f)); moveTo(50f, 23.5f); lineTo(50f, 50f) }
            thick(2.4f, Tint.Outline) { moveTo(50f, 23f); lineTo(50f, 16f) }
        }
        else -> fill { addOval(androidx.compose.ui.geometry.Rect(20f, 20f, 80f, 80f)) }
    }
}

private fun Art.shoe(dx: Float, dy: Float, tint: Tint) {
    val d = Offset(dx, dy)
    fun Path.at() = apply { translate(d) }
    fill(tint) { poly(17f, 48f, 31f, 46f, 36f, 50f, 46f, 50f, 60f, 54f, 78f, 59f, 87f, 64f, 89f, 69f, 16f, 69f); at() }
    fill(Tint.Light) { poly(14f, 67f, 90f, 67f, 90.5f, 71f, 88f, 74f, 16f, 74f, 13.5f, 71f); at() }
    line {
        poly(47f, 50.8f, 50f, 58f, close = false); poly(52f, 52f, 55f, 59.5f, close = false); poly(57f, 53.5f, 60f, 61f, close = false)
        poly(*q(75f, 58.5f, 72f, 64f, 77f, 67f), close = false)
        poly(21f, 48.5f, 22.5f, 67f, close = false)
        at()
    }
}

private fun Art.sock(dx: Float, dy: Float, tint: Tint) {
    val d = Offset(dx, dy)
    fill(tint) {
        moveTo(30f, 12f); lineTo(52f, 12f); lineTo(52f, 55f); lineTo(75f, 60f)
        quadraticTo(88f, 63f, 87f, 73f); quadraticTo(86f, 83f, 75f, 83f); lineTo(45f, 84f)
        quadraticTo(30f, 85f, 30f, 68f); close(); translate(d)
    }
    line {
        moveTo(30f, 21f); lineTo(52f, 21f)
        for (x in listOf(34f, 38f, 42f, 46f)) { moveTo(x, 13.5f); lineTo(x, 19.5f) }
        poly(*q(74f, 60.5f, 70f, 71.5f, 74.5f, 83f), close = false)
        poly(*q(30f, 63f, 42f, 63.5f, 45f, 83.8f), close = false)
        translate(d)
    }
}

@Preview(widthDp = 360, heightDp = 1500)
@Composable
private fun ClothingIllustrationPreview() = PcTheme { ClothingGrid() }

@Preview(widthDp = 360, heightDp = 1500)
@Composable
private fun ClothingIllustrationDarkPreview() = PcTheme(darkTheme = true) { ClothingGrid() }

@Composable
private fun ClothingGrid() {
    val colors = listOf(Color(0xFFF3E9D2), Color(0xFFC19A6B), Color(0xFF708238), Color(0xFF1F2A44), Color(0xFF1A1A1A))
    Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CLOTHING_KINDS.forEach { (kind, label) ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label.first, Modifier.size(64.dp, 56.dp), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                colors.forEach { ClothingIllustration(kind, it, Modifier.size(52.dp)) }
            }
        }
    }
}
