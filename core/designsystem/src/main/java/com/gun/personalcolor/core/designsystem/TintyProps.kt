package com.gun.personalcolor.core.designsystem

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser

// 틴티's props, in the character's 512 box (held by the right hand, see PROP_HAND). Same material as the body:
// no outlines, a soft violet drop shadow, a top-light gradient and one white highlight. Colors come from the palette,
// so every prop follows the tone 틴티 is drawn in.

/** Right hand (x, y, degrees) while holding each prop. */
internal val PROP_HAND: Map<TintyProp, FloatArray> = mapOf(
    TintyProp.ColorCard to floatArrayOf(404f, 358f, -28f),
    TintyProp.Palette to floatArrayOf(414f, 340f, -24f),
    TintyProp.Magnifier to floatArrayOf(398f, 352f, -30f),
    TintyProp.Eyedropper to floatArrayOf(446f, 252f, 32f),
    TintyProp.Hanger to floatArrayOf(440f, 218f, 8f),
    TintyProp.Book to floatArrayOf(414f, 350f, -20f),
    TintyProp.Fabric to floatArrayOf(414f, 232f, 18f),
    TintyProp.Cards to floatArrayOf(408f, 352f, -24f),
    TintyProp.Lock to floatArrayOf(410f, 352f, -20f),
)

/** Bounds of each prop alone (for [TintyPropArt]), a little padded. */
internal val PROP_BOX: Map<TintyProp, Rect> = mapOf(
    TintyProp.ColorCard to Rect(368f, 196f, 522f, 378f),
    TintyProp.Palette to Rect(374f, 236f, 514f, 364f),
    TintyProp.Magnifier to Rect(372f, 192f, 512f, 372f),
    TintyProp.Eyedropper to Rect(384f, 186f, 512f, 346f),
    TintyProp.Hanger to Rect(372f, 176f, 508f, 348f),
    TintyProp.Book to Rect(386f, 226f, 506f, 374f),
    TintyProp.Fabric to Rect(388f, 200f, 508f, 368f),
    TintyProp.Cards to Rect(384f, 218f, 512f, 364f),
    TintyProp.Crown to Rect(180f, 60f, 296f, 156f),
    TintyProp.Lock to Rect(392f, 240f, 500f, 360f),
)

internal fun DrawScope.drawProp(prop: TintyProp, p: TintyPalette, bp: BlurPaints, color: Color) {
    bp.drop.color = p.deep.copy(alpha = 0.26f).toArgb()
    when (prop) {
        TintyProp.ColorCard -> rotate(10f, Offset(444f, 286f)) { paintChip(bp, Offset(444f, 286f), 118f, 154f, color) }
        TintyProp.Palette -> rotate(-16f, Offset(446f, 300f)) {
            val board = Path().apply {
                addOval(Rect(384f, 252f, 508f, 348f))
                addOval(Rect(406f, 314f, 426f, 334f))
                fillType = PathFillType.EvenOdd
            }
            shadowed(board, bp, WOOD)
            DABS.forEachIndexed { i, d -> blob(Offset(446f + d.x, 300f + d.y), 13f, p.scarf[i]) }
        }
        TintyProp.Magnifier -> {
            drawLine(p.deep, Offset(400f, 350f), Offset(424f, 300f), 18f, StrokeCap.Round)
            shadowCircle(bp, Offset(452f, 250f), 50f)
            drawCircle(Color.White.copy(alpha = 0.55f), 42f, Offset(452f, 250f))
            drawCircle(color.copy(alpha = 0.18f), 42f, Offset(452f, 250f))
            drawArc(Color.White, 200f, 60f, false, Offset(422f, 220f), Size(60f, 60f), style = Stroke(7f, cap = StrokeCap.Round))
            drawCircle(p.deep, 46f, Offset(452f, 250f), style = Stroke(12f))
            drawCircle(Color.White.copy(alpha = 0.3f), 46f, Offset(452f, 250f), style = Stroke(3f))
        }
        TintyProp.Eyedropper -> rotate(34f, Offset(450f, 262f)) {
            // Glass tube with paint inside, collar, rubber bulb; a drop forming at the tip.
            val tube = rr(440f, 254f, 20f, 70f, 9f)
            shadowed(tube, bp, Color.White.copy(alpha = 0.9f))
            drawRoundRect(color, Offset(443f, 288f), Size(14f, 34f), CornerRadius(7f))
            drawRoundRect(p.deep, Offset(434f, 246f), Size(32f, 14f), CornerRadius(6f))
            shadowed(rr(436f, 196f, 28f, 54f, 14f), bp, p.scarf[3])
            drawRoundRect(gloss(196f, 54f), Offset(436f, 196f), Size(28f, 54f), CornerRadius(14f))
            translate(450f, 346f) { drawPath(Geo.drop, color, alpha = 1f); }
        }
        TintyProp.Hanger -> {
            val tee = parse("M394 254 L420 246 Q440 262 460 246 L486 254 L502 284 L484 292 L480 344 L400 344 L396 292 L378 284 Z")
            shadowed(tee, bp, color)
            drawPath(tee, drape(246f, 344f))
            drawPath(parse("M440 214 L384 254 L496 254 Z"), p.deep, style = Stroke(7f, join = StrokeJoin.Round))
            drawPath(parse("M440 216 L440 200 C440 184 460 182 462 196"), p.deep, style = Stroke(7f, cap = StrokeCap.Round))
        }
        TintyProp.Book -> rotate(-12f, Offset(446f, 300f)) {
            shadowed(rr(404f, 246f, 88f, 112f, 10f), bp, Color.White)
            drawRect(PAGES, Offset(484f, 252f), Size(6f, 100f))
            drawRoundRect(color, Offset(404f, 246f), Size(80f, 112f), CornerRadius(10f))
            drawRoundRect(drape(246f, 358f), Offset(404f, 246f), Size(80f, 112f), CornerRadius(10f))
            drawRect(Color.Black.copy(alpha = 0.12f), Offset(412f, 246f), Size(5f, 112f))
            drawRoundRect(Color.White.copy(alpha = 0.75f), Offset(426f, 272f), Size(44f, 24f), CornerRadius(6f))
            drawPath(parse("M460 352 L460 380 L468 372 L476 380 L476 352 Z"), p.scarf[3])
        }
        TintyProp.Fabric -> {
            val cloth = parse("M410 228 L494 220 Q504 286 492 354 Q472 344 456 358 Q436 346 418 358 Q404 294 410 228 Z")
            shadowed(cloth, bp, color)
            drawPath(cloth, drape(220f, 358f))
            drawPath(parse("M440 234 Q444 290 436 346"), Color.Black.copy(alpha = 0.12f), style = Stroke(9f, cap = StrokeCap.Round))
            drawPath(parse("M470 230 Q478 290 474 344"), Color.White.copy(alpha = 0.3f), style = Stroke(7f, cap = StrokeCap.Round))
        }
        TintyProp.Cards -> {
            rotate(16f, Offset(464f, 286f)) {
                shadowed(rr(424f, 232f, 80f, 108f, 12f), bp, Color.White)
                drawCircle(p.scarf[1], 16f, Offset(464f, 276f))
                drawRoundRect(CHIP_LINE, Offset(444f, 308f), Size(40f, 6f), CornerRadius(3f))
            }
            rotate(-6f, Offset(436f, 300f)) {
                shadowed(rr(396f, 246f, 80f, 108f, 12f), bp, Color.White)
                drawRoundRect(color.copy(alpha = 0.22f), Offset(404f, 254f), Size(64f, 92f), CornerRadius(8f))
                translate(436f, 296f) {
                    drawPath(Question, p.deep, style = Stroke(9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawCircle(p.deep, 6f, Offset(0f, 24f))
                }
            }
        }
        TintyProp.Crown -> {
            rotate(-14f, Offset(238f, 112f)) {
                val crown = parse("M200 130 L200 98 L219 114 L238 86 L257 114 L276 98 L276 130 Z")
                shadowed(crown, bp, GOLD)
                drawPath(crown, GOLD, style = Stroke(8f, join = StrokeJoin.Round))
                drawPath(crown, GoldShade)
                drawCircle(p.scarf[3], 6f, Offset(238f, 118f)); drawCircle(p.scarf[1], 4.5f, Offset(216f, 121f)); drawCircle(p.scarf[0], 4.5f, Offset(260f, 121f))
                drawCircle(Color.White, 3f, Offset(238f, 86f)); drawCircle(Color.White, 3f, Offset(200f, 98f)); drawCircle(Color.White, 3f, Offset(276f, 98f))
            }
            star(190f, 76f, 12f, GOLD); star(292f, 136f, 8f, p.scarf[1])
        }
        TintyProp.Lock -> {
            drawArc(CHIP_LINE, 180f, 180f, false, Offset(424f, 262f), Size(48f, 56f), style = Stroke(12f, cap = StrokeCap.Round))
            drawLine(CHIP_LINE, Offset(424f, 290f), Offset(424f, 300f), 12f, StrokeCap.Round)
            drawLine(CHIP_LINE, Offset(472f, 290f), Offset(472f, 300f), 12f, StrokeCap.Round)
            shadowed(rr(410f, 294f, 76f, 60f, 14f), bp, color)
            drawRoundRect(gloss(294f, 60f), Offset(410f, 294f), Size(76f, 60f), CornerRadius(14f))
            drawCircle(Color.White, 8f, Offset(448f, 318f)); drawRoundRect(Color.White, Offset(445f, 318f), Size(6f, 18f), CornerRadius(3f))
        }
        TintyProp.None -> Unit
    }
}

/** A paint-chip card: big color square, two label lines. */
private fun DrawScope.paintChip(bp: BlurPaints, c: Offset, w: Float, h: Float, color: Color) {
    shadowed(rr(c.x - w / 2, c.y - h / 2, w, h, 12f), bp, Color.White)
    drawRoundRect(color, Offset(c.x - w / 2 + 9f, c.y - h / 2 + 9f), Size(w - 18f, h * 0.64f), CornerRadius(7f))
    drawRoundRect(gloss(c.y - h / 2 + 9f, h * 0.64f), Offset(c.x - w / 2 + 9f, c.y - h / 2 + 9f), Size(w - 18f, h * 0.64f), CornerRadius(7f))
    drawRoundRect(CHIP_LINE, Offset(c.x - w / 2 + 12f, c.y + h * 0.24f), Size(w * 0.5f, 6f), CornerRadius(3f))
    drawRoundRect(CHIP_LINE.copy(alpha = 0.6f), Offset(c.x - w / 2 + 12f, c.y + h * 0.24f + 13f), Size(w * 0.3f, 6f), CornerRadius(3f))
}

private fun DrawScope.shadowed(path: Path, bp: BlurPaints, fill: Color) {
    translate(0f, 4f) { drawContext.canvas.nativeCanvas.drawPath(path.asAndroidPath(), bp.drop) }
    drawPath(path, fill)
}

private fun DrawScope.shadowCircle(bp: BlurPaints, c: Offset, r: Float) =
    drawContext.canvas.nativeCanvas.drawCircle(c.x, c.y + 4f, r, bp.drop)

/** Paint dab: color with a small top-left highlight. */
private fun DrawScope.blob(c: Offset, r: Float, color: Color) {
    drawCircle(color, r, c)
    drawCircle(Color.White.copy(alpha = 0.45f), r * 0.32f, c + Offset(-r * 0.35f, -r * 0.35f))
}

private fun rr(x: Float, y: Float, w: Float, h: Float, r: Float) =
    Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(x, y, x + w, y + h, CornerRadius(r))) }

private fun parse(d: String): Path = PathParser().parsePathString(d).toPath()

private val DABS = listOf(Offset(-30f, -14f), Offset(-4f, -30f), Offset(26f, -22f), Offset(36f, 6f))
private val WOOD = Color(0xFFF7E6CC)
private val PAGES = Color(0xFFEDE6DD)
private val GOLD = Color(0xFFF2C14E)
// Gradients need explicit 512-box coordinates (a brush otherwise spans the whole canvas).
private fun gloss(top: Float, h: Float) = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.35f), 0.5f to Color.White.copy(alpha = 0f), startY = top, endY = top + h)
private fun drape(top: Float, bottom: Float) = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.25f), 0.45f to Color.White.copy(alpha = 0f), 1f to Color.Black.copy(alpha = 0.16f), startY = top, endY = bottom,
)
private val GoldShade = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.35f), 1f to Color(0x33B8741A), startY = 86f, endY = 130f)
private val Question = parse("M-13 -14 C-13 -32 13 -32 13 -14 C13 -2 0 0 0 12")
