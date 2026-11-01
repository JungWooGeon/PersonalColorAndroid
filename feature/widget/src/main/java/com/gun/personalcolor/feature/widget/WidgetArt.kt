package com.gun.personalcolor.feature.widget

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.ui.clothingBounds
import android.graphics.Shader
import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.drawTintyOn
import com.gun.personalcolor.core.designsystem.tintyBitmap
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The widget's illustration layer, drawn off the main thread into one transparent bitmap per exact widget size:
 * today's color as a glossy pool of paint along the bottom, 틴티 standing in it, and (4x2) the soft shadow of the
 * lookbook card that Glance draws on top. The paper behind it and all text stay in Glance, so light/dark follow the
 * system without re-rendering.
 */
internal object WidgetArt {
    /** Where things sit, in px of the bitmap. Shared with the Glance layout through [Scene]. */
    class Scene(
        val w: Float, val h: Float, val density: Float,
        /** Pool surface (mean line). */
        val surface: Float,
        /** 틴티's square and its horizontal center (also the paint drop's, when 틴티 is hidden). */
        val tintySize: Float, val tintyX: Float,
        /** False while our live wallpaper shows 틴티 (one 틴티 per screen): a falling drop of today's color instead. */
        val character: Boolean = true,
        /** 4x2 lookbook card, for its shadow. */
        val card: RectF? = null,
        /** Mid size: companion color dots along the bottom-left of the pool (center y, radius, start x, gap). */
        val dots: List<Int> = emptyList(), val dotY: Float = 0f, val dotR: Float = 0f, val dotX: Float = 0f,
    )

    fun render(s: Scene, color: Int, tinty: TintyColors, mood: TintyExpression): Bitmap {
        val bmp = Bitmap.createBitmap(s.w.roundToInt().coerceAtLeast(1), s.h.roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        val d = s.density

        // A breath of today's color over the paper, strongest at the top-left where the name sits.
        p.shader = RadialGradient(0f, 0f, maxOf(s.w, s.h) * .9f, withAlpha(color, .16f), withAlpha(color, 0f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, s.w, s.h, p); p.shader = null

        s.card?.let { r ->
            p.color = withAlpha(Color.rgb(0x5B, 0x4B, 0x8A), .22f)
            p.maskFilter = BlurMaskFilter(10 * d, BlurMaskFilter.Blur.NORMAL)
            c.drawRoundRect(r.left, r.top + 4 * d, r.right, r.bottom + 4 * d, 16 * d, 16 * d, p)
            p.maskFilter = null
            p.color = Color.BLACK // shaders below take the paint's alpha
        }

        val size = s.tintySize
        val amp = s.h * .022f
        val len = s.w * .85f
        fun y(x: Float) = s.surface + amp * sin(2 * PI.toFloat() * (x - s.tintyX) / len + PI.toFloat() / 2) - amp
        // When 틴티's body is close to today's color (purple on purple), the pool steps a little darker or lighter
        // and 틴티 gets a soft light rim, so it never melts into the paint.
        val body = mix(tinty.bodyTop.toArgb(), tinty.bodyBottom.toArgb(), .5f)
        val close = s.character && dist(body, color) < 80f
        val pool = if (!close) color else shade(color, if (lum(color) > .45f) -.22f else -.3f)

        if (s.character) {
            // 틴티 first; the pool drawn over it hides everything below the surface, so it stands *in* the paint.
            val tLeft = s.tintyX - size / 2
            val tTop = s.surface - size * .885f // scarf just above the paint
            if (close) {
                val art = tintyBitmap(size.roundToInt().coerceAtLeast(1), tinty, mood)
                val off = IntArray(2)
                val glow = art.extractAlpha(Paint().apply { maskFilter = BlurMaskFilter(5 * d, BlurMaskFilter.Blur.OUTER) }, off)
                p.color = withAlpha(Color.WHITE, .9f)
                c.drawBitmap(glow, tLeft + off[0], tTop + off[1], p)
                glow.recycle()
                c.drawBitmap(art, tLeft, tTop, null)
                art.recycle()
            } else drawTintyOn(c, tLeft, tTop, size, tinty, mood)
        } else paintDrop(c, p, s.tintyX, y(s.tintyX), size, color, d)

        val poolPath = Path().apply {
            moveTo(0f, s.h); lineTo(0f, y(0f))
            var x = 0f
            while (x < s.w) { x += 4 * d; lineTo(x, y(x)) }
            lineTo(s.w, s.h); close()
        }
        val top = s.surface - amp * 2
        p.shader = LinearGradient(0f, top, 0f, s.h, pool, shade(pool, -.16f), Shader.TileMode.CLAMP)
        c.drawPath(poolPath, p); p.shader = null
        // Gloss across the surface, then a crisp lip so pale colors (ivory) still read on white paper.
        p.shader = LinearGradient(0f, top, 0f, top + 12 * d, withAlpha(Color.WHITE, .3f), withAlpha(Color.WHITE, 0f), Shader.TileMode.CLAMP)
        c.drawPath(poolPath, p); p.shader = null
        val lip = Path().apply {
            moveTo(0f, y(0f)); var x = 0f
            while (x < s.w) { x += 4 * d; lineTo(x, y(x)) }
        }
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 1.2f * d; p.color = withAlpha(shade(pool, -.28f), if (lum(pool) > .8f) .55f else .25f)
        c.drawPath(lip, p)
        p.strokeWidth = 2f * d; p.color = withAlpha(Color.WHITE, .7f)
        val hl = Path().apply {
            val x0 = s.w * .06f; val x1 = minOf(s.w * .4f, s.tintyX - size * .5f)
            moveTo(x0, y(x0) + 3 * d); var x = x0
            while (x < x1) { x += 4 * d; lineTo(x, y(x) + 3 * d) }
        }
        c.drawPath(hl, p)

        // Ripple rings where 틴티 meets the paint.
        val sy = y(s.tintyX)
        for (k in 0..1) {
            val rw = size * (.36f + .16f * k); val rh = size * (.045f + .02f * k)
            p.strokeWidth = (1.2f - .4f * k) * d; p.color = withAlpha(Color.WHITE, .5f - .26f * k)
            c.drawOval(s.tintyX - rw, sy - rh + 2 * d, s.tintyX + rw, sy + rh + 2 * d, p)
        }
        p.style = Paint.Style.FILL

        // Two paint drops jumping off the surface.
        drop(c, p, s.tintyX - size * .5f, sy - size * .2f, size * .045f, color)
        drop(c, p, s.tintyX - size * .62f, sy - size * .07f, size * .028f, color)
        if (!s.character) {
            drop(c, p, s.tintyX + size * .42f, sy - size * .14f, size * .036f, color)
            drop(c, p, s.tintyX + size * .26f, sy - size * .26f, size * .024f, color)
        }

        s.dots.forEachIndexed { i, dot ->
            val x = s.dotX + s.dotR + i * (s.dotR * 2 + 6 * d)
            p.color = withAlpha(Color.BLACK, .12f)
            p.maskFilter = BlurMaskFilter(2 * d, BlurMaskFilter.Blur.NORMAL)
            c.drawCircle(x, s.dotY + 1.5f * d, s.dotR + 1.5f * d, p); p.maskFilter = null
            p.color = Color.WHITE; c.drawCircle(x, s.dotY, s.dotR + 2 * d, p)
            p.color = dot; c.drawCircle(x, s.dotY, s.dotR, p)
            p.color = withAlpha(Color.WHITE, .45f); c.drawCircle(x - s.dotR * .35f, s.dotY - s.dotR * .38f, s.dotR * .26f, p)
        }
        return bmp
    }

    /**
     * A big glossy drop of today's color about to land in the pool: the character-free hero, same footprint as 틴티.
     * [surface] = the pool line under it.
     */
    private fun paintDrop(c: Canvas, p: Paint, cx: Float, surface: Float, size: Float, color: Int, d: Float) {
        val r = size * .24f
        val cy = surface - size * .16f - r // hovering just above the paint
        val tip = cy - r * 2.05f
        val path = Path().apply {
            moveTo(cx, tip)
            cubicTo(cx + r * .2f, tip + r * .75f, cx + r, cy - r * .6f, cx + r, cy)
            cubicTo(cx + r, cy + r * .56f, cx + r * .56f, cy + r, cx, cy + r)
            cubicTo(cx - r * .56f, cy + r, cx - r, cy + r * .56f, cx - r, cy)
            cubicTo(cx - r, cy - r * .6f, cx - r * .2f, tip + r * .75f, cx, tip)
            close()
        }
        // Soft shadow on the paint below, then the drop with a light rim so it reads on its own color.
        p.color = withAlpha(shade(color, -.5f), .28f)
        p.maskFilter = BlurMaskFilter(4 * d, BlurMaskFilter.Blur.NORMAL)
        c.drawOval(cx - r * .8f, surface - r * .08f, cx + r * .8f, surface + r * .22f, p)
        p.maskFilter = null
        p.color = Color.BLACK // the gradient below takes the paint's alpha
        p.shader = LinearGradient(cx - r, tip, cx + r, cy + r, shade(color, .16f), shade(color, -.14f), Shader.TileMode.CLAMP)
        c.drawPath(path, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f * d; p.color = withAlpha(Color.WHITE, .85f)
        c.drawPath(path, p); p.style = Paint.Style.FILL
        // Gloss: a long highlight on the left shoulder and a sparkle.
        p.color = withAlpha(Color.WHITE, .55f)
        c.save(); c.rotate(28f, cx - r * .45f, cy - r * .35f)
        c.drawOval(cx - r * .6f, cy - r * .95f, cx - r * .3f, cy + r * .1f, p); c.restore()
        p.color = withAlpha(Color.WHITE, .9f); c.drawCircle(cx + r * .38f, cy - r * .5f, r * .09f, p)
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(x: Int, y: Int) = (x + (y - x) * t).roundToInt()
        return Color.rgb(ch(Color.red(a), Color.red(b)), ch(Color.green(a), Color.green(b)), ch(Color.blue(a), Color.blue(b)))
    }

    private fun dist(a: Int, b: Int): Float {
        val dr = Color.red(a) - Color.red(b); val dg = Color.green(a) - Color.green(b); val db = Color.blue(a) - Color.blue(b)
        return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toFloat())
    }

    private fun drop(c: Canvas, p: Paint, x: Float, y: Float, r: Float, color: Int) {
        p.shader = RadialGradient(x - r * .3f, y - r * .3f, r * 1.2f, shade(color, .25f), shade(color, -.08f), Shader.TileMode.CLAMP)
        c.drawCircle(x, y, r, p); p.shader = null
        p.color = withAlpha(Color.WHITE, .8f); c.drawCircle(x - r * .35f, y - r * .35f, r * .3f, p)
    }

    /**
     * Today's outfit as a flat-lay filling a [w]×[h] px board, overlapped like the app's lookbook: the outer big on the
     * left, the top tucked up right, the bottom below it, shoes at the foot; each slightly tilted with a soft shadow.
     * Garment art is cropped to its real bounds so nothing floats in empty padding.
     * [frosted] = the locked teaser: generic pieces in the tone's colors, blurred (never the real outfit).
     */
    fun lookbook(pieces: List<Pair<String, Bitmap>>, w: Int, h: Int, density: Float, frosted: Boolean): Bitmap {
        val out = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val W = w.toFloat(); val H = h.toFloat()
        val category = { kind: String -> CLOTHING_KINDS[kind]?.second }
        val hasOuter = pieces.any { category(it.first) == "아우터" }
        // (center x as a share of the width, center y and size as shares of the height, tilt, z)
        val boardy = W < H * 1.5f // tall-ish card (4x2 on 4-column homes): stack like the app's LookBoard
        fun spot(cat: String?): FloatArray = if (boardy) when {
            cat == "아우터" -> floatArrayOf(.3f, .4f, .72f, -4f, 3f)
            cat == "상의" && hasOuter -> floatArrayOf(.74f, .28f, .56f, 5f, 2f)
            cat == "상의" -> floatArrayOf(.36f, .32f, .64f, -4f, 2f)
            cat == "하의" -> floatArrayOf(.7f, .66f, .66f, -2f, 1f)
            cat == "원피스" -> floatArrayOf(if (hasOuter) .68f else .45f, .52f, .86f, 3f, 2f)
            cat == "신발" -> floatArrayOf(.26f, .86f, .32f, -5f, 4f)
            else -> floatArrayOf(.86f, .9f, .26f, 8f, 5f)
        } else when {
            cat == "아우터" -> floatArrayOf(.3f, .5f, 1f, -5f, 3f)
            cat == "상의" && hasOuter -> floatArrayOf(.6f, .36f, .76f, 5f, 2f)
            cat == "상의" -> floatArrayOf(.33f, .44f, .92f, -4f, 2f)
            cat == "하의" && hasOuter -> floatArrayOf(.8f, .56f, .96f, -2f, 1f)
            cat == "하의" -> floatArrayOf(.68f, .54f, 1f, 3f, 1f)
            cat == "원피스" -> floatArrayOf(if (hasOuter) .66f else .5f, .52f, 1f, 3f, 2f)
            cat == "신발" -> floatArrayOf(.5f, .86f, .34f, -5f, 4f)
            else -> floatArrayOf(.88f, .2f, .3f, 8f, 5f)
        }
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.rgb(0x2B, 0x22, 0x36), .22f)
            maskFilter = BlurMaskFilter(5 * density, BlurMaskFilter.Blur.NORMAL)
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        pieces.map { it to spot(category(it.first)) }.sortedBy { it.second[4] }.forEach { (piece, sp) ->
            val (kind, b) = piece
            val bounds = clothingBounds(kind)
            val k = b.width / 100f
            val src = Rect((bounds.left * k).toInt(), (bounds.top * k).toInt(), (bounds.right * k).toInt(), (bounds.bottom * k).toInt())
            val side = minOf(H * sp[2] * .92f, W * .9f, H * .98f) // never wider than the board (coerceIn below)
            val scale = side / maxOf(src.width(), src.height()).coerceAtLeast(1)
            val dw = src.width() * scale; val dh = src.height() * scale
            val cx = (W * sp[0]).coerceIn(dw / 2, W - dw / 2); val cy = (H * sp[1]).coerceIn(dh / 2, H - dh / 2)
            val dst = RectF(cx - dw / 2, cy - dh / 2, cx + dw / 2, cy + dh / 2)
            c.save(); c.rotate(sp[3], cx, cy)
            val alpha = b.extractAlpha()
            c.drawBitmap(alpha, src, RectF(dst.left, dst.top + 3 * density, dst.right, dst.bottom + 3 * density), shadow)
            alpha.recycle()
            c.drawBitmap(b, src, dst, paint)
            c.restore()
        }
        if (!frosted) return out
        // Soft focus: halve down to 1/16, then double back up; each bilinear step smooths the last (a cheap, even blur).
        var s = out
        repeat(4) { s = Bitmap.createScaledBitmap(s, (s.width / 2).coerceAtLeast(1), (s.height / 2).coerceAtLeast(1), true) }
        repeat(4) { s = Bitmap.createScaledBitmap(s, s.width * 2, s.height * 2, true) }
        return Bitmap.createScaledBitmap(s, out.width, out.height, true)
    }

    fun withAlpha(c: Int, a: Float) = (c and 0xFFFFFF) or ((a * 255).roundToInt().coerceIn(0, 255) shl 24)

    fun lum(c: Int) = (.2126f * Color.red(c) + .7152f * Color.green(c) + .0722f * Color.blue(c)) / 255f

    /** t > 0 toward white, t < 0 toward black. */
    fun shade(c: Int, t: Float): Int {
        val target = if (t >= 0) 255 else 0
        val k = kotlin.math.abs(t)
        fun ch(x: Int) = (x + (target - x) * k).roundToInt()
        return Color.rgb(ch(Color.red(c)), ch(Color.green(c)), ch(Color.blue(c)))
    }
}
