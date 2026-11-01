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

private val BIG_POINTS = setOf("bag", "shoulder_bag", "crossbody", "backpack", "muffler", "scarf")

/**
 * Flat-lay composition like a lookbook: outer over the top, the bottom below, shoes at the foot, the point item tucked in.
 * [bounds] = where the art sits inside its box (0..1); pieces are nudged so that part stays on the board.
 */
internal fun lookLayout(slots: List<OutfitSlot>, bounds: (OutfitSlot) -> Rect = { Rect(0.1f, 0.1f, 0.9f, 0.9f) }): List<Placement> {
    val labels = slots.map { it.label }.toSet()
    val outer = "아우터" in labels
    val spots: Map<String, Spot> = when {
        "원피스" in labels && outer -> mapOf(
            "아우터" to Spot(.32f, .37f, .66f, -5f, 3), "원피스" to Spot(.67f, .50f, .76f, 3f, 2),
            "신발" to Spot(.26f, .90f, .40f, -6f, 4), "포인트" to Spot(.86f, .92f, .28f, 8f, 5),
        )
        "원피스" in labels -> mapOf(
            "원피스" to Spot(.40f, .52f, .86f, -3f, 2), "신발" to Spot(.76f, .90f, .42f, -6f, 4), "포인트" to Spot(.80f, .22f, .30f, 8f, 5),
        )
        outer -> mapOf(
            "아우터" to Spot(.32f, .37f, .68f, -4f, 3), "상의" to Spot(.70f, .28f, .54f, 5f, 2), "하의" to Spot(.67f, .71f, .62f, -2f, 1),
            "신발" to Spot(.27f, .90f, .40f, -5f, 4), "포인트" to Spot(.89f, .91f, .26f, 10f, 5),
        )
        else -> mapOf(
            "상의" to Spot(.36f, .30f, .62f, -4f, 2), "하의" to Spot(.66f, .67f, .64f, 3f, 1),
            "신발" to Spot(.27f, .90f, .42f, -5f, 4), "포인트" to Spot(.81f, .19f, .30f, 8f, 5),
        )
    }
    val margin = 0.03f
    return slots.map { slot ->
        val spot = spots[slot.label] ?: Spot(.5f, .5f, .4f, 0f, 0)
        var size = spot.size
        var rotation = spot.rotation
        if (slot.label == "포인트") when (slot.kind) {
            "umbrella" -> { size *= 1.45f; rotation -= 40f }
            in BIG_POINTS -> size *= 1.25f
        }
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
    val point = slots.firstOrNull { it.label == "포인트" }
    val shoes = slots.firstOrNull { it.label == "신발" }
    val lines = if (point?.kind == "umbrella") buildList {
        val c = point.color.name
        add("비 오는 날엔 $c 우산 하나로 기분까지 환하게!")
        if (shoes?.color?.name == c) add("레인부츠와 우산을 $c${josa(c, "으로", "로")} 맞춰 비 오는 날도 산뜻하게")
        if (main != null) add("흐린 날일수록 ${main.color.name}${josa(main.color.name, "이", "가")} 얼굴을 살려 줘요")
    } else buildList {
        if (main != null) add("${main.color.name}${josa(main.color.name, "이", "가")} 얼굴 가까이에서 혈색을 살려 줘요")
        if (base != null && point != null) add("${base.color.name}${josa(base.color.name, "으로", "로")} 차분하게 받치고 ${name(point)}${josa(name(point), "으로", "로")} 포인트를 줬어요")
        if (outer != null && main != null && outer.color.name != main.color.name) add("겉은 ${outer.color.name}, 안은 ${main.color.name}. 톤을 맞춰 단정하게 입어요")
        if (point != null) add("${name(point)} 하나면 심심하지 않아요")
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
        slot.kind != null -> {
            translate(drop.x, drop.y) { drawClothingShadow(slot.kind, shadow, blur = 3.2f) }
            drawClothing(slot.kind, hexColor(slot.color.hex))
        }
        else -> {
            val r = side * 0.3f
            drawCircle(shadow, r, center + drop)
            drawCircle(hexColor(slot.color.hex), r)
        }
    }
}

/** Where the drawn part sits in the piece box (0..1), for [lookLayout]. */
internal fun pieceBounds(slot: OutfitSlot, photo: LookPhoto?): Rect = when {
    photo != null -> Rect(0.07f, 0.07f, 0.93f, 0.93f)
    slot.kind != null -> clothingBounds(slot.kind).let { Rect(it.left / 100, it.top / 100, it.right / 100, it.bottom / 100) }
    else -> Rect(0.2f, 0.2f, 0.8f, 0.8f)
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

// --- Screen board ---

/**
 * Today's look as a styled flat-lay on tone-tinted paper. A new outfit settles in piece by piece
 * (old pieces lift away, new ones drop in, staggered by layer).
 */
@Composable
internal fun LookBoard(slots: List<OutfitSlot>, tinty: TintyColors, modifier: Modifier = Modifier) {
    val dark = PcTheme.colors.isDark
    val surface = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
    val palette = remember(tinty, dark, surface) { lookPalette(tinty, dark, surface) }
    BoxWithConstraints(
        modifier.fillMaxWidth().aspectRatio(1 / LOOK_ASPECT).clip(androidx.compose.material3.MaterialTheme.shapes.extraLarge)
            .drawBehind { drawLookBackdrop(palette) },
    ) {
        val w = maxWidth
        AnimatedContent(
            slots,
            transitionSpec = { fadeIn(tween(1)) togetherWith fadeOut(tween(260, delayMillis = 140)) using SizeTransform(clip = false) },
            label = "look",
        ) { shown ->
            val photos = shown.associateWith { rememberLookPhoto(it.imageFile.takeIf { _ -> it.fromCloset }) }
            if (photos.values.any { it.loading }) return@AnimatedContent
            val placements = remember(shown, photos.values.map { it.photo }) { lookLayout(shown) { pieceBounds(it, photos[it]?.photo) } }
            Box(Modifier.fillMaxSize()) {
                placements.forEachIndexed { i, p ->
                    // Old pieces lift away together; new ones drop in after them, lowest layer first.
                    val t by transition.animateFloat(
                        transitionSpec = {
                            if (targetState == EnterExitState.Visible) tween(420, delayMillis = 180 + i * 70, easing = EaseOutBack)
                            else tween(200, delayMillis = i * 25, easing = FastOutSlowInEasing)
                        },
                        label = "piece$i",
                    ) { if (it == EnterExitState.Visible) 1f else 0f }
                    val side = w * p.size
                    Canvas(
                        Modifier.offset(w * p.cx - side / 2, w * p.cy - side / 2).size(side).zIndex(p.z.toFloat())
                            .graphicsLayer {
                                alpha = t.coerceIn(0f, 1f)
                                translationY = (1 - t) * -side.toPx() * 0.08f
                                scaleX = 0.94f + 0.06f * t; scaleY = scaleX
                                rotationZ = p.rotation + (1 - t) * 4f
                            },
                    ) { drawPiece(p.slot, photos[p.slot]?.photo, palette.shadow) }
                }
            }
        }
    }
}

private class PhotoState(val loading: Boolean, val photo: LookPhoto?)

@Composable
private fun rememberLookPhoto(file: String?): PhotoState {
    val context = LocalContext.current
    val state by produceState(PhotoState(file != null, null), file) {
        if (file == null) return@produceState
        value = PhotoState(false, withContext(Dispatchers.IO) { loadLookPhoto(context, file) })
    }
    return state
}

/** Decodes a closet photo (≤ ~720px), trims transparent margins and pre-blurs its silhouette for the shadow. */
internal fun loadLookPhoto(context: Context, file: String, maxSide: Int = 720): LookPhoto? = runCatching {
    val f = File(context.filesDir, "closet/$file")
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val raw = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val trimmed = trimTransparent(raw)
    val opaque = !trimmed.hasAlpha() || trimmed.getPixel(0, 0) ushr 24 == 0xFF && trimmed.getPixel(trimmed.width - 1, trimmed.height - 1) ushr 24 == 0xFF
    val offset = IntArray(2)
    val shadow = if (opaque) null else trimmed.extractAlpha(
        android.graphics.Paint().apply { maskFilter = BlurMaskFilter(maxOf(trimmed.width, trimmed.height) * 0.025f, BlurMaskFilter.Blur.NORMAL) },
        offset,
    )
    LookPhoto(trimmed.asImageBitmap(), shadow?.asImageBitmap(), print = opaque, shadowPad = -offset[0])
}.getOrNull()

/** Crops fully transparent rows/columns around a cut-out so it scales like the illustrations. */
private fun trimTransparent(b: Bitmap): Bitmap {
    if (!b.hasAlpha()) return b
    val w = b.width
    val h = b.height
    val px = IntArray(w * h).also { b.getPixels(it, 0, w, 0, 0, w, h) }
    var l = w; var t = h; var r = -1; var btm = -1
    for (y in 0 until h) for (x in 0 until w) if (px[y * w + x] ushr 24 > 16) {
        if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > btm) btm = y
    }
    if (r < l || btm < t) return b
    return Bitmap.createBitmap(b, l, t, r - l + 1, btm - t + 1)
}

// --- Share image ---

/** "오늘의 코디" share image: the same flat-lay on a 1080×1350 card with the headline and a color legend. */
internal fun renderLookShare(
    slots: List<OutfitSlot>, photos: Map<OutfitSlot, LookPhoto?>, palette: LookPalette, title: String, headline: String, brand: String,
): Bitmap {
    val width = 1080
    val height = 1350
    val image = ImageBitmap(width, height)
    val board = 1080f - 2 * 64f
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(image), Size(width.toFloat(), height.toFloat())) {
        drawLookBackdrop(palette)
        translate(64f, 190f) {
            lookLayout(slots) { pieceBounds(it, photos[it]) }.forEach { p ->
                val side = board * p.size
                translate(board * p.cx - side / 2, board * p.cy - side / 2) {
                    rotate(p.rotation, Offset(side / 2, side / 2)) {
                        withSize(side) { drawPiece(p.slot, photos[p.slot], palette.shadow) }
                    }
                }
            }
        }
    }
    val bmp = image.asAndroidBitmap()
    val c = android.graphics.Canvas(bmp)
    val ink = if (palette.dark) 0xFFF4F0F7.toInt() else 0xFF2B2236.toInt()
    val muted = if (palette.dark) 0xB3F4F0F7.toInt() else 0x992B2236.toInt()
    val bold = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.color = muted; paint.textSize = 34f; paint.letterSpacing = 0.12f; paint.typeface = bold
    c.drawText(title, 72f, 104f, paint)
    paint.color = ink; paint.textSize = 52f; paint.letterSpacing = 0f
    c.drawText(ellipsize(headline, paint, 1080f - 144f), 72f, 166f, paint)
    // Color legend chips along the bottom.
    var x = 72f
    paint.textSize = 30f; paint.typeface = android.graphics.Typeface.DEFAULT
    slots.forEach { s ->
        val label = s.color.name
        val tw = paint.measureText(label)
        if (x + 44f + tw > 1080f - 72f) return@forEach
        paint.color = hexColor(s.color.hex).let { android.graphics.Color.argb(255, (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt()) }
        c.drawCircle(x + 14f, 1268f, 14f, paint)
        paint.color = muted; paint.style = android.graphics.Paint.Style.STROKE; paint.strokeWidth = 1.5f
        c.drawCircle(x + 14f, 1268f, 14f, paint); paint.style = android.graphics.Paint.Style.FILL
        paint.color = ink
        c.drawText(label, x + 38f, 1279f, paint)
        x += 38f + tw + 34f
    }
    paint.color = muted; paint.textSize = 28f; paint.typeface = bold
    c.drawText(brand, 1080f - 72f - paint.measureText(brand), 104f, paint)
    return bmp
}

private fun ellipsize(text: String, paint: android.graphics.Paint, max: Float): String {
    if (paint.measureText(text) <= max) return text
    var t = text
    while (t.isNotEmpty() && paint.measureText("$t…") > max) t = t.dropLast(1)
    return "$t…"
}

/** Runs [block] with this scope's size set to a [side]×[side] square at the current origin. */
private inline fun DrawScope.withSize(side: Float, block: DrawScope.() -> Unit) {
    val old = drawContext.size
    drawContext.size = Size(side, side)
    block()
    drawContext.size = old
}
