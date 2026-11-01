package com.gun.personalcolor.core.designsystem

import android.graphics.BlurMaskFilter
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Colors of 틴티. Body gradient runs top-left → bottom-right; [scarf] is the 4-chip palette drape (left → right). */
@Immutable
data class TintyColors(val bodyTop: Color, val bodyBottom: Color, val scarf: List<Color>, val blush: Color) {
    init { require(scarf.size == 4) { "Tinty's scarf has exactly 4 chips" } }

    companion object {
        /** App brand: peach → lavender body, one chip per season (spring coral · summer sky · autumn mustard · winter magenta). */
        val Brand = TintyColors(
            bodyTop = Color(0xFFFFD3BE), bodyBottom = Color(0xFFC9BCF2),
            scarf = listOf(Color(0xFFFF8A6B), Color(0xFF9CC9EC), Color(0xFFD4A017), Color(0xFFB5006E)),
            blush = Color(0xFFF7A3A0),
        )
    }
}

/** Happy = wink. Loading → Thinking, result → Cheer, empty → Surprised, night/off → Sleep. */
enum class TintyExpression { Default, Happy, Surprised, Thinking, Cheer, Sleep }

/**
 * 틴티, the brand character, drawn as vectors (crisp at any size). Square; fits the smaller side of [modifier].
 * Use ≥ 64dp. Idle motion (bounce, blink, ribbon sway, gloss) stops when [animated] is false or system animations are off.
 */
@Composable
fun Tinty(
    modifier: Modifier = Modifier,
    colors: TintyColors = TintyColors.Brand,
    expression: TintyExpression = TintyExpression.Default,
    animated: Boolean = true,
) = TintyCanvas(modifier, colors, expression, animated, working = false)

/** Screen-level wait: Tinty thinking (faster bounce, hopping color chips) with an optional muted line under it. */
@Composable
fun TintyLoading(
    modifier: Modifier = Modifier,
    message: String? = null,
    colors: TintyColors = TintyColors.Brand,
    size: Dp = 132.dp,
) {
    Column(
        modifier.semantics(mergeDescendants = true) { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TintyCanvas(Modifier.size(size), colors, TintyExpression.Thinking, animated = true, working = true)
        if (message != null) Text(message, style = Muted, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Implementation. Geometry is a straight port of the character spec (_source/tinty.py), all in a 512×512 box.
// Paths/brushes are built once; a frame only changes transforms and a few floats (no per-frame allocation).

private const val CYCLE = 5.6f // s; every motion period divides it (0.8 / 1.4 / 2.8 / 5.6)
private val INK = Color(0xFF2B2236)

@Composable
private fun TintyCanvas(modifier: Modifier, colors: TintyColors, expression: TintyExpression, animated: Boolean, working: Boolean) {
    val resolver = LocalContext.current.contentResolver
    val systemAnimOff = remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val palette = remember(colors, expression) { TintyPalette(colors, expression == TintyExpression.Sleep) }
    val paints = remember { BlurPaints() }
    val time: State<Float>? = if (animated && !systemAnimOff) {
        rememberInfiniteTransition("tinty").animateFloat(
            0f, CYCLE, infiniteRepeatable(tween((CYCLE * 1000).toInt(), easing = LinearEasing)), label = "t",
        )
    } else null
    // The state is read inside the draw lambda, so animation only re-draws (no recomposition / relayout).
    Canvas(modifier) { drawTinty(palette, paints, expression, time?.value ?: -1f, working) }
}

private fun DrawScope.drawTinty(p: TintyPalette, bp: BlurPaints, e: TintyExpression, t: Float, working: Boolean) {
    val moving = t >= 0f
    val ph = if (moving) 2f * PI.toFloat() * t / (if (working) 0.8f else 1.4f) else 0f
    val up = if (moving) 0.5f - 0.5f * cos(ph) else 0f
    val sq = if (moving) 0.5f + 0.5f * cos(ph) else 0f
    val lift = (if (working) 0.045f else 0.028f) * 512f
    // Blinks at 1.2s, then a double blink at 4.0/4.32s, 0.16s each.
    var blink = 1f
    if (moving) for (b0 in BLINKS) { val d = t - b0; if (d >= 0f && d < 0.16f) blink = 1f - 0.92f * sin(PI.toFloat() * d / 0.16f) }
    val sway = if (moving) 6f * sin(ph + 1f) else 0f
    val gloss = when {
        !moving -> -1f
        working -> ((t % 2.8f) / 2.8f - 0.5f) / 0.5f
        else -> (t - 2.0f) / 0.9f
    }

    val s = min(size.width, size.height) / 512f
    translate((size.width - 512f * s) / 2f, (size.height - 512f * s) / 2f) {
        scale(s, s, pivot = Offset.Zero) {
            // Ground shadow shrinks/fades as Tinty lifts off.
            scale(1f - 0.22f * up, 1f - 0.22f * up, pivot = Offset(256f, 476f)) {
                bp.ground.color = p.deep.copy(alpha = 0.22f * (1f - 0.36f * up)).toArgb()
                drawContext.canvas.nativeCanvas.drawOval(146f, 464f, 366f, 488f, bp.ground)
            }
            translate(0f, -lift * up) {
                scale(1f + 0.028f * sq, 1f - 0.032f * sq, pivot = Offset(256f, 450.56f)) {
                    drawBody(p, bp, gloss)
                    drawFace(p, bp, e, blink)
                    drawScarf(p, bp, sway)
                    drawHands(p, bp, e)
                    drawExtras(p, bp, e, t, working)
                }
            }
        }
    }
}

private val BLINKS = floatArrayOf(1.2f, 4.0f, 4.32f)
private val BOX = Size(512f, 512f)

private fun DrawScope.drawBody(p: TintyPalette, bp: BlurPaints, gloss: Float) {
    drawPath(Geo.body, p.body)
    clipPath(Geo.body) {
        drawRect(Geo.highlight, size = BOX)
        drawRect(p.shade, size = BOX)
        val nc = drawContext.canvas.nativeCanvas
        bp.softStroke.color = WHITE40; bp.softStroke.strokeWidth = 9f
        nc.drawPath(Geo.rimLight.asAndroidPath(), bp.softStroke)
        bp.softStroke.color = p.deep.copy(alpha = 0.35f).toArgb(); bp.softStroke.strokeWidth = 16f
        nc.drawPath(Geo.rimShade.asAndroidPath(), bp.softStroke)
        if (gloss in 0f..1f) rotate(20f, Offset(256f, 256f)) {
            translate(-260f + 900f * gloss, -100f) { drawRect(Geo.sheen, size = Size(90f, 700f)) }
        }
    }
    rotate(30f, Offset(184f, 222f)) {
        bp.soft.color = WHITE50
        drawContext.canvas.nativeCanvas.drawOval(168f, 182f, 200f, 262f, bp.soft)
    }
    drawCircle(Color.White, 7.5f, Offset(216f, 170f), alpha = 0.9f)
    drawCircle(Color.White, 3.5f, Offset(292f, 92f), alpha = 0.7f)
}

private const val EX = 256f
private const val EY = 316f
private const val GAP = 38f

private fun DrawScope.drawFace(p: TintyPalette, bp: BlurPaints, e: TintyExpression, blink: Float) {
    bp.soft.color = p.blush.copy(alpha = 0.62f).toArgb()
    val nc = drawContext.canvas.nativeCanvas
    for (sx in SIDES) nc.drawOval(EX + sx * 64f - 21f, EY + 32f - 11f, EX + sx * 64f + 21f, EY + 32f + 11f, bp.soft)
    when (e) {
        TintyExpression.Default -> {
            eye(EX - GAP, EY, blink = blink); eye(EX + GAP, EY, blink = blink)
            drawPath(Geo.smile, INK, style = Geo.line55)
        }
        TintyExpression.Happy -> {
            arc(EX - GAP, EY, up = true)
            drawPath(Geo.winkChevron, INK, style = Geo.line7)
            openMouth()
        }
        TintyExpression.Surprised -> {
            eye(EX - GAP - 3f, EY - 4f, 16f, 21f, blink = blink); eye(EX + GAP + 3f, EY - 4f, 16f, 21f, blink = blink)
            drawOval(INK, Offset(EX - 9f, EY + 36f - 11f), Size(18f, 22f))
        }
        TintyExpression.Thinking -> {
            eye(EX - GAP + 4f, EY - 3f, lx = 3f, ly = -3f, blink = blink); eye(EX + GAP + 4f, EY - 3f, lx = 3f, ly = -3f, blink = blink)
            drawPath(Geo.thinkMouth, INK, style = Geo.line55)
        }
        TintyExpression.Cheer -> {
            arc(EX - GAP, EY, up = true); arc(EX + GAP, EY, up = true)
            openMouth()
        }
        TintyExpression.Sleep -> {
            arc(EX - GAP, EY + 2f, up = false); arc(EX + GAP, EY + 2f, up = false)
            drawOval(INK, Offset(EX - 5f, EY + 32f - 4f), Size(10f, 8f), alpha = 0.85f)
        }
    }
}

private val SIDES = floatArrayOf(-1f, 1f)

private fun DrawScope.eye(x: Float, y: Float, rx: Float = 14f, ry: Float = 19f, lx: Float = 0f, ly: Float = 0f, blink: Float) =
    scale(1f, blink, pivot = Offset(x, y)) {
        drawOval(INK, Offset(x - rx, y - ry), Size(2 * rx, 2 * ry))
        drawCircle(Color.White, rx * 0.37f, Offset(x + 4f + lx, y - 7f + ly))
        drawCircle(Color.White, 2.2f, Offset(x - 4f + lx, y + 7f + ly), alpha = 0.65f)
    }

private fun DrawScope.arc(x: Float, y: Float, up: Boolean) =
    translate(x, y) { drawPath(if (up) Geo.arcUp else Geo.arcDown, INK, style = Geo.line7) }

private fun DrawScope.openMouth() {
    drawPath(Geo.openMouth, INK)
    drawOval(TONGUE, Offset(EX - 8f, EY + 40f - 5f), Size(16f, 10f))
}

private fun DrawScope.drawScarf(p: TintyPalette, bp: BlurPaints, sway: Float) {
    // Drop shadow of the whole drape (band, tails, knot), offset 3 down.
    bp.drop.color = p.deep.copy(alpha = 0.28f).toArgb()
    val nc = drawContext.canvas.nativeCanvas
    translate(0f, 3f) {
        nc.drawPath(Geo.band.asAndroidPath(), bp.drop)
        rotate(sway, TAIL_PIVOT) {
            nc.drawPath(Geo.ribbonA.asAndroidPath(), bp.drop); nc.drawPath(Geo.ribbonB.asAndroidPath(), bp.drop)
        }
        nc.drawRoundRect(322f, 400f, 360f, 434f, 13f, 13f, bp.drop)
    }
    clipPath(Geo.band) {
        for (i in 0 until 4) {
            val x0 = SEG_X[i]; val x1 = SEG_X[i + 1]
            drawRect(p.scarf[i], Offset(x0, 370f), Size(x1 - x0 + 0.6f, 120f))
        }
        drawRect(Geo.bandShade, Offset(0f, 370f), Size(512f, 120f))
        drawRect(Geo.bandSides, Offset(100f, 370f), Size(312f, 120f))
        for (i in 1..3) drawRect(Color.White, Offset(SEG_X[i] - 1f, 370f), Size(2f, 120f), alpha = 0.55f)
        drawPath(Geo.bandHighlight, Color.White, alpha = 0.5f, style = Geo.line25)
    }
    rotate(sway, TAIL_PIVOT) {
        drawPath(Geo.ribbonA, p.scarf[3]); drawPath(Geo.ribbonA, Geo.ribbonAShade, alpha = 0.8f)
        drawPath(Geo.ribbonB, p.scarf[0]); drawPath(Geo.ribbonB, Geo.ribbonBShade, alpha = 0.8f)
    }
    drawRoundRect(p.scarf[1], Offset(322f, 400f), Size(38f, 34f), CornerRadius(13f))
    drawRoundRect(Geo.knotShade, Offset(322f, 400f), Size(38f, 34f), CornerRadius(13f))
    drawPath(Geo.knotSmile, Color.Black, alpha = 0.15f, style = Geo.line3)
}

private val TAIL_PIVOT = Offset(340f, 420f)
// Chip edges: cylinder foreshortening (256 + 152·sin a), outer chips stretched to the band ends.
private val SEG_X = FloatArray(5) { i -> 256f + 152f * sin(-PI.toFloat() / 2 + PI.toFloat() * i / 4) }.also { it[0] = 90f; it[4] = 422f }

private fun DrawScope.drawHands(p: TintyPalette, bp: BlurPaints, e: TintyExpression) {
    val hands = when (e) {
        TintyExpression.Cheer -> HANDS_CHEER
        TintyExpression.Thinking -> HANDS_THINK
        else -> HANDS_DEFAULT
    }
    bp.drop.color = p.deep.copy(alpha = 0.28f).toArgb()
    for (h in 0 until hands.size / 3) translate(hands[h * 3], hands[h * 3 + 1] + 3f) {
        rotate(hands[h * 3 + 2], Offset.Zero) { drawContext.canvas.nativeCanvas.drawOval(-16f, -22f, 16f, 22f, bp.drop) }
    }
    for (h in 0 until hands.size / 3) translate(hands[h * 3], hands[h * 3 + 1]) {
        rotate(hands[h * 3 + 2], Offset.Zero) {
            drawOval(p.hand, Offset(-16f, -22f), Size(32f, 44f))
            drawOval(Color.White, Offset(-9f, -16f), Size(12f, 16f), alpha = 0.22f)
        }
    }
}

// (x, y, degrees) per hand
private val HANDS_DEFAULT = floatArrayOf(114f, 394f, 18f, 398f, 394f, -18f)
private val HANDS_CHEER = floatArrayOf(124f, 262f, -40f, 388f, 262f, 40f)
private val HANDS_THINK = floatArrayOf(114f, 394f, 18f, 334f, 356f, -20f)

private fun DrawScope.drawExtras(p: TintyPalette, bp: BlurPaints, e: TintyExpression, t: Float, working: Boolean) {
    when (e) {
        TintyExpression.Happy -> { star(410f, 206f, 19f, p.scarf[1]); star(440f, 250f, 9f, p.scarf[0], 0.85f) }
        TintyExpression.Surprised -> {
            drawLine(INK, Offset(376f, 126f), Offset(392f, 102f), 7f, StrokeCap.Round, alpha = 0.75f)
            drawLine(INK, Offset(402f, 152f), Offset(428f, 142f), 7f, StrokeCap.Round, alpha = 0.75f)
            drawLine(INK, Offset(348f, 104f), Offset(350f, 80f), 7f, StrokeCap.Round, alpha = 0.75f)
        }
        TintyExpression.Thinking -> for (i in 0 until 3) {
            val x = CHIPS[i * 3]; val y = CHIPS[i * 3 + 1]
            // While working, the chips hop one after another.
            val hop = if (working && t >= 0f) 14f * max(0f, sin(2f * PI.toFloat() * t / 1.4f - i * 0.9f)) else 0f
            translate(0f, -hop) { rotate(CHIPS[i * 3 + 2], Offset(x, y)) {
                bp.drop.color = p.deep.copy(alpha = 0.28f).toArgb()
                drawContext.canvas.nativeCanvas.drawRoundRect(x - 16f, y - 18f, x + 16f, y + 24f, 6f, 6f, bp.drop)
                drawRoundRect(Color.White, Offset(x - 16f, y - 21f), Size(32f, 42f), CornerRadius(6f))
                drawRoundRect(p.scarf[i], Offset(x - 12f, y - 17f), Size(24f, 24f), CornerRadius(4f))
                drawRoundRect(CHIP_LINE, Offset(x - 12f, y + 11f), Size(14f, 3f), CornerRadius(1.5f))
            } }
        }
        TintyExpression.Cheer -> {
            star(90f, 160f, 20f, p.scarf[0]); star(424f, 140f, 24f, p.scarf[1])
            star(452f, 212f, 11f, p.scarf[2]); star(64f, 222f, 10f, p.scarf[3])
        }
        TintyExpression.Sleep -> {
            // "Z z z" (font sizes 44/30/20 at the spec's baselines), drawn as strokes so no font is needed.
            zee(346f, 146f, 30f, p); zee(390f, 106f, 16f, p); zee(419f, 76f, 10.5f, p)
        }
        TintyExpression.Default -> Unit
    }
}

private val CHIPS = floatArrayOf(380f, 132f, 10f, 424f, 186f, 20f, 440f, 252f, 30f)
private val CHIP_LINE = Color(0xFFC9C2CF)
private val TONGUE = Color(0xFFF27D8C)
private val WHITE40 = Color.White.copy(alpha = 0.4f).toArgb()
private val WHITE50 = Color.White.copy(alpha = 0.5f).toArgb()

private fun DrawScope.star(x: Float, y: Float, r: Float, c: Color, alpha: Float = 1f) =
    translate(x, y) { scale(r, r, pivot = Offset.Zero) { drawPath(Geo.star, c, alpha) } }

private fun DrawScope.zee(x: Float, baseline: Float, h: Float, p: TintyPalette) =
    translate(x, baseline - h) { scale(h, h, pivot = Offset.Zero) { drawPath(Geo.zee, p.zee, alpha = 0.7f, style = Geo.zeeStroke) } }

/** Color-dependent brushes, rebuilt only when colors/expression change. */
private class TintyPalette(c: TintyColors, sleep: Boolean) {
    private val c1 = if (sleep) mix(c.bodyTop, Color(0xFFB9B3D6), 0.3f) else c.bodyTop
    private val c2 = if (sleep) mix(c.bodyBottom, Color(0xFF8C86B8), 0.3f) else c.bodyBottom
    val deep = mix(c2, Color(0xFF1B1530), 0.45f)
    val scarf = c.scarf
    val blush = c.blush
    val body = boxGradient(listOf(c1, c2), Geo.body.getBounds(), 0.15f, 0.05f, 0.8f, 1f)
    val shade = Brush.radialGradient(0f to deep.copy(alpha = 0.45f), 0.7f to deep.copy(alpha = 0f), center = Offset(368.64f, 537.6f), radius = 384f)
    private val arm = mix(c1, c2, 0.72f)
    val hand = boxGradient(listOf(mix(arm, Color.White, 0.12f), mix(arm, deep, 0.3f)), Rect(-16f, -22f, 16f, 22f), 0f, 0f, 1f, 1f)
    val zee = mix(c2, INK, 0.4f)
}

/** Native paints with a Gaussian blur (SVG feGaussianBlur / feDropShadow). Blur is GPU-supported on API 28+, crisp below. */
private class BlurPaints {
    private fun blur(sigma: Float) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter((sigma - 0.5f) / 0.57735f, BlurMaskFilter.Blur.NORMAL) // Android: sigma = 0.57735·r + 0.5
    }
    val soft = blur(2.5f)
    val softStroke = blur(2.5f).apply { style = android.graphics.Paint.Style.STROKE; strokeCap = android.graphics.Paint.Cap.ROUND }
    val drop = blur(3f)
    val ground = blur(6f)
}

/** Static geometry (512 box), parsed once. */
private object Geo {
    private fun p(d: String): Path = PathParser().parsePathString(d).toPath()

    val body = p("M250 106 C266 84 286 70 308 66 C318 65 320 74 312 80 C300 90 296 100 300 114 " +
        "C326 174 404 226 404 322 C404 404 338 452 256 452 C174 452 108 404 108 322 C108 226 212 162 250 106 Z")
    val band = p("M106 380 Q256 440 406 380 C414 392 412 410 402 418 Q256 478 110 418 C100 410 98 392 106 380 Z")
    val ribbonA = p("M346 424 C362 438 380 454 392 476 L378 474 L376 490 C362 466 346 450 330 436 Z")
    val ribbonB = p("M330 430 C330 450 326 468 316 488 L330 484 L338 496 C348 474 352 452 352 430 Z")
    val rimLight = p("M120 352 C140 430 210 456 290 448")
    val rimShade = p("M110 428 Q256 486 402 428")
    val bandHighlight = p("M106 383 Q256 443 406 383")
    val knotSmile = p("M332 412 Q341 418 350 412")
    val smile = p("M246 342 Q256 351 266 342")
    val openMouth = p("M241 340 Q256 338 271 340 Q269 362 256 362 Q243 362 241 340 Z")
    val winkChevron = p("M282 309 L304 317 L282 325")
    val thinkMouth = p("M250 347 Q258 341 266 346 Q272 350 276 344")
    val arcUp = p("M-13 5 Q0 -13 13 5")
    val arcDown = p("M-13 -5 Q0 13 13 -5")
    val star = p("M0 -1 Q.28 -.28 1 0 Q.28 .28 0 1 Q-.28 .28 -1 0 Q-.28 -.28 0 -1 Z")
    val zee = p("M0 0 L.8 0 L0 1 L.8 1")

    val line3 = Stroke(3f, cap = StrokeCap.Round)
    val line25 = Stroke(2.5f)
    val line55 = Stroke(5.5f, cap = StrokeCap.Round)
    val line7 = Stroke(7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val zeeStroke = Stroke(0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round)

    val highlight = Brush.radialGradient(0f to Color.White.copy(alpha = 0.5f), 0.6f to Color.White.copy(alpha = 0f), center = Offset(174.08f, 153.6f), radius = 281.6f)
    val sheen = Brush.horizontalGradient(0f to Color.White.copy(alpha = 0f), 0.5f to Color.White.copy(alpha = 0.55f), 1f to Color.White.copy(alpha = 0f), startX = 0f, endX = 90f)
    private fun drape(top: Float, bottom: Float) = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.3f), 0.4f to Color.White.copy(alpha = 0f), 1f to Color.Black.copy(alpha = 0.2f), startY = top, endY = bottom,
    )
    val bandShade = drape(370f, 490f)
    val ribbonAShade = drape(424f, 490f)
    val ribbonBShade = drape(430f, 496f)
    val knotShade = drape(400f, 434f)
    val bandSides = Brush.horizontalGradient(
        0f to Color.Black.copy(alpha = 0.22f), 0.22f to Color.Black.copy(alpha = 0f), 0.78f to Color.Black.copy(alpha = 0f), 1f to Color.Black.copy(alpha = 0.22f),
        startX = 100f, endX = 412f,
    )
}

/** sRGB component mix, same as the spec's generator (not Compose's Oklab lerp). */
internal fun mix(a: Color, b: Color, t: Float) =
    Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t)

/**
 * SVG objectBoundingBox linear gradient (x1,y1 → x2,y2 in 0..1 of [box]) as a user-space gradient.
 * Stretching the box tilts the iso-lines, so the end point is the one whose normal matches the stretched direction.
 */
private fun boxGradient(colors: List<Color>, box: Rect, x1: Float, y1: Float, x2: Float, y2: Float): Brush {
    val dx = x2 - x1; val dy = y2 - y1; val d2 = dx * dx + dy * dy
    val gx = dx / box.width / d2; val gy = dy / box.height / d2; val g2 = gx * gx + gy * gy
    val start = Offset(box.left + x1 * box.width, box.top + y1 * box.height)
    return Brush.linearGradient(colors, start, start + Offset(gx / g2, gy / g2))
}
