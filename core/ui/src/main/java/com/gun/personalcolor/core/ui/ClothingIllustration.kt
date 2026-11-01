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
import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
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

/**
 * Draws the garment centered in this scope's size, in a 100×100 design space. No composition needed.
 * Every fill gets a soft top-left light / bottom-right shade so big renders read as fabric, not flat clip-art;
 * outlines thin out as the art grows (icon-weight at ~50dp, illustration-weight at board size).
 */
fun DrawScope.drawClothing(kind: String, color: Color) {
    val ops = opsOf(kind)
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
    val weight = (72f / (100f * s / density)).coerceIn(0.62f, 1f)
    val outlineStroke = Stroke(1.6f * weight, join = StrokeJoin.Round, cap = StrokeCap.Round)
    val detailStroke = Stroke(1.1f * weight.coerceAtLeast(0.75f), join = StrokeJoin.Round, cap = StrokeCap.Round)
    val sheen = if (dark) SHEEN_DARK else SHEEN
    translate((size.width - 100 * s) / 2, (size.height - 100 * s) / 2) {
        scale(s, s, pivot = Offset.Zero) {
            // Soft drop shadow: two offset passes.
            ops.forEach { op ->
                if (op is Op.Fill) {
                    translate(0.6f, 2.6f) { drawPath(op.path, Color.Black.copy(alpha = 0.07f)) }
                    translate(0.3f, 1.3f) { drawPath(op.path, Color.Black.copy(alpha = 0.07f)) }
                }
            }
            var lastFill: Path? = null
            ops.forEach { op ->
                when (op) {
                    is Op.Fill -> {
                        lastFill = op.path
                        drawPath(op.path, tint(op.tint))
                        clipPath(op.path) {
                            drawRect(sheen, Offset(-10f, -10f), Size(120f, 120f))
                            drawPath(op.path, Color.Black.copy(alpha = if (dark) 0.10f else 0.05f), style = RIM)
                        }
                        drawPath(op.path, outline, style = outlineStroke)
                    }
                    is Op.Shade -> clipPath(lastFill ?: op.path) {
                        drawIntoCanvas { c ->
                            c.drawPath(op.path, softPaint(
                                if (op.light) Color.White.copy(alpha = if (dark) 0.07f else 0.16f) else Color.Black.copy(alpha = if (dark) 0.28f else 0.09f),
                            ))
                        }
                    }
                    is Op.Line -> drawPath(op.path, if (op.strong) outline else detail, style = detailStroke)
                    is Op.Thick -> drawPath(op.path, tint(op.tint), style = Stroke(op.width, join = StrokeJoin.Round, cap = StrokeCap.Round))
                    is Op.Dot -> drawCircle(detail, op.r, op.c)
                }
            }
        }
    }
}

/**
 * The garment's silhouette as a soft blurred shadow (flat-lay contact shadow), placed like [drawClothing].
 * [blur] is in design units (100 = full width). Blur needs API 28+ with hardware drawing; older devices get a hard edge.
 */
fun DrawScope.drawClothingShadow(kind: String, color: Color = Color.Black, blur: Float = 4f) {
    val s = size.minDimension / 100f
    val paint = Paint().apply { this.color = color }
    paint.asFrameworkPaint().maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL) // local units: the blur follows the canvas scale
    translate((size.width - 100 * s) / 2, (size.height - 100 * s) / 2) {
        drawIntoCanvas { c ->
            // One layer so overlapping parts don't stack darker.
            c.saveLayer(Rect(-20f * s, -20f * s, 120f * s, 120f * s), Paint().apply { alpha = color.alpha })
            paint.color = color.copy(alpha = 1f)
            c.scale(s, s)
            opsOf(kind).forEach { op ->
                when (op) {
                    is Op.Fill -> c.drawPath(op.path, paint)
                    is Op.Thick -> c.drawPath(op.path, Paint().apply {
                        this.color = paint.color; style = PaintingStyle.Stroke; strokeWidth = op.width
                        asFrameworkPaint().maskFilter = paint.asFrameworkPaint().maskFilter
                    })
                    else -> Unit
                }
            }
            c.restore()
        }
    }
}

/** Where the garment sits in its 100×100 design box (union of its fills and straps), for layout. */
fun clothingBounds(kind: String): Rect = boundsCache.getOrPut(kind) {
    opsOf(kind).mapNotNull { op ->
        when (op) {
            is Op.Fill -> op.path.getBounds()
            is Op.Thick -> op.path.getBounds().inflate(op.width / 2)
            else -> null
        }
    }.reduceOrNull { a, b -> Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)) }
        ?: Rect(0f, 0f, 100f, 100f)
}

private val boundsCache = ConcurrentHashMap<String, Rect>()

// Light from the top-left: a lifted shoulder, a shaded hem.
private val SHEEN = Brush.linearGradient(
    0f to Color.White.copy(alpha = 0.22f), 0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.09f),
    start = Offset(18f, 4f), end = Offset(82f, 100f),
)
private val SHEEN_DARK = Brush.linearGradient(
    0f to Color.White.copy(alpha = 0.13f), 0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.18f),
    start = Offset(18f, 4f), end = Offset(82f, 100f),
)
/** Inner rim: drawn clipped to the fill, so half of it shades the edge inward. */
private val RIM = Stroke(5f, join = StrokeJoin.Round)
/** Fold shade / sheen: blurred 1.6 design units (API 28+ on hardware canvases; a hard edge before that). */
private fun softPaint(color: Color) = Paint().also { it.color = color; it.asFrameworkPaint().maskFilter = BlurMaskFilter(1.6f, BlurMaskFilter.Blur.NORMAL) }

// --- Art (100×100 design space, x right, y down) ---

private enum class Tint { Base, Inner, Light, Outline }

private sealed interface Op {
    class Fill(val path: Path, val tint: Tint) : Op
    class Shade(val path: Path, val light: Boolean) : Op
    class Line(val path: Path, val strong: Boolean) : Op
    class Thick(val path: Path, val width: Float, val tint: Tint) : Op
    class Dot(val c: Offset, val r: Float) : Op
}

private val cache = ConcurrentHashMap<String, List<Op>>()

private fun opsOf(kind: String) = cache.getOrPut(kind) {
    ART[kind]?.map(::parseLayer) ?: listOf(Op.Fill(Path().apply { addOval(Rect(20f, 20f, 80f, 80f)) }, Tint.Base))
}

/** Every kind's layers (ClothingArt*.kt). Unknown kinds draw a round chip. */
private val ART: Map<String, List<String>> by lazy { BASIC_ART + OUTER_DRESS_ART + TOP_BOTTOM_ART + SHOE_ART + ACCESSORY_ART }

/**
 * Layer string = code + SVG path data (100×100 space), drawn in order:
 * F base fill · I inner (shade) fill · W light fill (soles, lenses…) · O outline-color fill — all fills get the outline stroke
 * and the automatic light/shade;
 * G soft fold shadow · H soft sheen highlight (blurred, no outline, clipped to the fill right before them: folds, creases);
 * L detail line · S strong (outline-color) line · D "x y r" dot · T<width><B|I|W|O> thick stroke (straps, handles).
 */
private fun parseLayer(layer: String): Op {
    val code = layer.substringBefore(' ')
    val d = layer.substringAfter(' ')
    fun path() = PathParser().parsePathString(d).toPath()
    fun tint(c: Char) = when (c) { 'I' -> Tint.Inner; 'W' -> Tint.Light; 'O' -> Tint.Outline; else -> Tint.Base }
    return when (code[0]) {
        'F', 'I', 'W', 'O' -> Op.Fill(path(), tint(code[0]))
        'G' -> Op.Shade(path(), light = false)
        'H' -> Op.Shade(path(), light = true)
        'L' -> Op.Line(path(), false)
        'S' -> Op.Line(path(), true)
        'D' -> d.trim().split(Regex("\\s+")).map(String::toFloat).let { (x, y, r) -> Op.Dot(Offset(x, y), r) }
        'T' -> Op.Thick(path(), code.drop(1).dropLast(1).toFloat(), tint(code.last()))
        else -> error("Unknown clothing layer code: $layer")
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
