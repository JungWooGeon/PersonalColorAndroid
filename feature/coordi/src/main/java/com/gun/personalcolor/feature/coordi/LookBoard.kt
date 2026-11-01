package com.gun.personalcolor.feature.coordi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.ui.clothingBounds
import com.gun.personalcolor.core.ui.drawClothing
import com.gun.personalcolor.core.ui.drawClothingShadow
import com.gun.personalcolor.core.ui.hexColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

// --- Composition (pure: board-width units, the board is 1 wide and LOOK_ASPECT tall) ---

/** Board height / width. */
internal const val LOOK_ASPECT = 1.08f

/** One garment on the board: center, box side and tilt (degrees); higher [z] draws on top. */
internal data class Placement(val slot: OutfitSlot, val cx: Float, val cy: Float, val size: Float, val rotation: Float, val z: Int)

private class Spot(val cx: Float, val cy: Float, val size: Float, val rotation: Float, val z: Int)

/**
 * Flat-lay composition like a lookbook: outer over the top, the bottom below, shoes at the foot, the bag and the small
 * accessory (umbrella / muffler / beanie, label 소품) tucked into the free corners.
 * [bounds] = where the art sits inside its box (0..1); pieces are nudged so that part stays on the board.
 * Labels: 아우터 · 상의 · 하의 · 원피스 · 신발 · 가방 (older "포인트") · 소품.
 */
internal fun lookLayout(slots: List<OutfitSlot>, bounds: (OutfitSlot) -> Rect = { Rect(0.1f, 0.1f, 0.9f, 0.9f) }): List<Placement> {
    val labels = slots.map { it.label }.toSet()
    val outer = "아우터" in labels
    val dress = "원피스" in labels
    val umbrella = slots.any { it.kind == "umbrella" }
    val spots: Map<String, Spot> = when {
        dress && outer -> mapOf(
            "아우터" to Spot(.31f, .36f, .62f, -5f, 3), "원피스" to Spot(.69f, .50f, .70f, 3f, 2),
            "신발" to Spot(.27f, .91f, .36f, -6f, 4), "가방" to Spot(.84f, .93f, .28f, 8f, 5),
            "소품" to if (umbrella) Spot(.20f, .70f, .40f, -28f, 1) else Spot(.17f, .70f, .27f, -10f, 5),
        )
        dress -> mapOf(
            "원피스" to Spot(.40f, .50f, .82f, -3f, 2), "신발" to Spot(.76f, .91f, .40f, -6f, 4), "가방" to Spot(.80f, .21f, .30f, 8f, 5),
            "소품" to Spot(.84f, .58f, .44f, 24f, 0),
        )
        outer -> mapOf(
            "아우터" to Spot(.30f, .35f, .62f, -4f, 3), "상의" to Spot(.71f, .25f, .50f, 5f, 2),
            "하의" to Spot(if (umbrella) .62f else .68f, .70f, .58f, -2f, 1),
            "신발" to Spot(.26f, .91f, .36f, -5f, 4), "가방" to Spot(.86f, .93f, .26f, 8f, 5),
            "소품" to if (umbrella) Spot(.86f, .62f, .40f, 22f, 0) else Spot(.16f, .70f, .27f, -10f, 5),
        )
        else -> mapOf(
            "상의" to Spot(.37f, .30f, .60f, -4f, 2), "하의" to Spot(.67f, .67f, .62f, 3f, 1),
            "신발" to Spot(.26f, .91f, .40f, -5f, 4), "가방" to Spot(.82f, .20f, .30f, 8f, 5),
            "소품" to Spot(.17f, .64f, .40f, -30f, 0),
        )
    }
    val margin = 0.03f
    return slots.map { slot ->
        val spot = spots[if (slot.label == "포인트") "가방" else slot.label] ?: Spot(.5f, .5f, .4f, 0f, 0)
        val size = spot.size
        val rotation = spot.rotation
        // Keep the drawn part (not the empty box) inside the board.
        val b = bounds(slot)
        val left = spot.cx - size / 2
        val top = spot.cy - size / 2
        val dx = (margin - (left + b.left * size)).coerceAtLeast(0f) + (1 - margin - (left + b.right * size)).coerceAtMost(0f)
        val dy = (margin - (top + b.top * size)).coerceAtLeast(0f) + (LOOK_ASPECT - margin - (top + b.bottom * size)).coerceAtMost(0f)
        Placement(slot, spot.cx + dx, spot.cy + dy, size, rotation, spot.z)
    }.sortedBy { it.z }
}

// --- Styling note ---

/** 틴티's one-liner about today's look. Short, natural, picks a line by [seed]. */
internal fun stylingNote(slots: List<OutfitSlot>, seed: Int): String {
    fun name(s: OutfitSlot) = if (s.fromCloset) s.color.name else s.item
    val main = slots.firstOrNull { it.label == "상의" || it.label == "원피스" }
    val base = slots.firstOrNull { it.label == "하의" } ?: slots.firstOrNull { it.label == "아우터" }
    val outer = slots.firstOrNull { it.label == "아우터" }
    val acc = slots.firstOrNull { it.label == "소품" }
    val point = acc?.takeIf { it.kind != "umbrella" } ?: slots.firstOrNull { it.label == "가방" || it.label == "포인트" }
    val lines = if (acc?.kind == "umbrella") buildList {
        add("비 오는 날엔 ${acc.color.name} 우산 하나로 깔끔하게")
        if (main != null) add("흐린 날일수록 ${main.color.name}${josa(main.color.name, "이", "가")} 얼굴을 살려 줘요")
    } else buildList {
        if (main != null) add("${main.color.name}${josa(main.color.name, "이", "가")} 얼굴 가까이에서 혈색을 살려 줘요")
        if (base != null && point != null) add("${base.color.name}${josa(base.color.name, "으로", "로")} 차분하게 받치고 ${name(point)}${josa(name(point), "으로", "로")} 마무리했어요")
        if (outer != null && main != null && outer.color.name != main.color.name) add("겉은 ${outer.color.name}, 안은 ${main.color.name}. 톤을 맞춰 단정하게 입어요")
        if (acc != null) add("${name(acc)} 하나면 따뜻하고 귀여워요")
    }
    return lines.ifEmpty { listOf("오늘 컬러로 가볍게 맞춰 봤어요") }[Math.floorMod(seed, lines.size.coerceAtLeast(1))]
}

/** Korean particle by the final consonant: josa("카멜", "으로", "로") = "로" (ㄹ takes 로 too). */
internal fun josa(word: String, withFinal: String, withoutFinal: String): String {
    val c = word.trimEnd { !it.isLetterOrDigit() }.lastOrNull() ?: return withoutFinal
    if (c !in '가'..'힣') return withoutFinal
    val jong = (c - '가') % 28
    return if (jong == 0 || (withFinal == "으로" && jong == 8)) withoutFinal else withFinal
}

// --- Pieces (shared by the screen and the share image) ---

/** A closet cut-out, trimmed to its visible pixels, with a pre-blurred alpha mask for its shadow. Opaque photos are "prints". */
internal class LookPhoto(val image: ImageBitmap, val shadow: ImageBitmap?, val print: Boolean, val shadowPad: Int = 0)

/** Draws one piece into a square scope: soft contact shadow, then the garment (art, cut-out photo or color chip). */
internal fun DrawScope.drawPiece(slot: OutfitSlot, photo: LookPhoto?, shadow: Color) {
    val side = size.minDimension
    val drop = Offset(side * 0.012f, side * 0.03f)
    when {
        photo != null -> {
            val img = photo.image
            val scale = side * (if (photo.print) 0.74f else 0.80f) / maxOf(img.width, img.height)
            val w = img.width * scale
            val h = img.height * scale
            val topLeft = Offset((size.width - w) / 2, (size.height - h) / 2)
            if (photo.print) {
                // A small print on the table: white border, soft shadow.
                val pad = side * 0.035f
                val frame = Rect(topLeft.x - pad, topLeft.y - pad, topLeft.x + w + pad, topLeft.y + h + pad)
                drawIntoCanvas { c ->
                    val p = Paint().apply { color = shadow }
                    p.asFrameworkPaint().maskFilter = BlurMaskFilter(side * 0.03f, BlurMaskFilter.Blur.NORMAL)
                    c.drawRect(frame.translate(drop), p)
                }
                drawRoundRect(Color(0xFFFBFAF7), frame.topLeft, frame.size, CornerRadius(side * 0.012f))
            } else if (photo.shadow != null) {
                // The blurred mask is larger than the photo by shadowPad on each side.
                val pad = photo.shadowPad * scale
                drawImage(photo.shadow, dstOffset = androidx.compose.ui.unit.IntOffset((topLeft.x + drop.x - pad).toInt(), (topLeft.y + drop.y - pad).toInt()),
                    dstSize = IntSize((w + 2 * pad).toInt(), (h + 2 * pad).toInt()), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(shadow))
            }
            drawImage(img, dstOffset = androidx.compose.ui.unit.IntOffset(topLeft.x.toInt(), topLeft.y.toInt()), dstSize = IntSize(w.toInt(), h.toInt()))
        }
        else -> {
            // Never a bare chip: an unknown kind is drawn as a small bag in its color.
            val kind = slot.kind ?: FALLBACK_KIND
            translate(drop.x, drop.y) { drawClothingShadow(kind, shadow, blur = 3.2f) }
            drawClothing(kind, hexColor(slot.color.hex))
        }
    }
}

private const val FALLBACK_KIND = "crossbody"

/** Where the drawn part sits in the piece box (0..1), for [lookLayout]. */
internal fun pieceBounds(slot: OutfitSlot, photo: LookPhoto?): Rect = when {
    photo != null -> Rect(0.07f, 0.07f, 0.93f, 0.93f)
    else -> clothingBounds(slot.kind ?: FALLBACK_KIND).let { Rect(it.left / 100, it.top / 100, it.right / 100, it.bottom / 100) }
}

// --- Backdrop ---

internal class LookPalette(val top: Color, val bottom: Color, val shadow: Color, val grain: Color, val dark: Boolean)

internal fun lookPalette(tinty: TintyColors, dark: Boolean, surface: Color): LookPalette = if (dark) {
    LookPalette(lerp(surface, tinty.bodyTop, 0.10f), lerp(surface, tinty.bodyBottom, 0.08f), Color.Black.copy(alpha = 0.55f), Color.White.copy(alpha = 0.035f), true)
} else {
    // Tone-tinted paper: almost white, warm or cool by the tone, with a slightly deeper shadow tint.
    LookPalette(lerp(tinty.bodyTop, Color.White, 0.86f), lerp(tinty.bodyBottom, Color.White, 0.84f),
        lerp(tinty.bodyBottom, Color(0xFF1E1528), 0.75f).copy(alpha = 0.26f), Color(0xFF3A2A40).copy(alpha = 0.05f), false)
}

private val GRAIN: List<Offset> = Random(7).let { r -> List(1600) { Offset(r.nextFloat(), r.nextFloat()) } }

/** Paper backdrop: tone gradient, a soft window light from the top-left, a faint vignette and paper grain. */
internal fun DrawScope.drawLookBackdrop(p: LookPalette) {
    drawRect(Brush.linearGradient(listOf(p.top, p.bottom), Offset.Zero, Offset(size.width, size.height)))
    drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = if (p.dark) 0.05f else 0.55f), Color.Transparent),
        Offset(size.width * 0.22f, size.height * 0.12f), size.width * 0.85f))
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = if (p.dark) 0.18f else 0.045f)),
        Offset(size.width * 0.45f, size.height * 0.42f), size.maxDimension * 0.85f))
    drawPoints(GRAIN.map { Offset(it.x * size.width, it.y * size.height) }, PointMode.Points, p.grain, strokeWidth = size.width / 520f)
}
