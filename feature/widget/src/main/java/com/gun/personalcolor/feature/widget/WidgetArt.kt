package com.gun.personalcolor.feature.widget

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.drawTintyOn
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
        /** 틴티's square and its horizontal center. */
        val tintySize: Float, val tintyX: Float,
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

        // 틴티 first; the pool drawn over it hides everything below the surface, so it stands *in* the paint.
        val size = s.tintySize
        val tLeft = s.tintyX - size / 2
        val tTop = s.surface - size * .885f // scarf just above the paint
        drawTintyOn(c, tLeft, tTop, size, tinty, mood)

        val amp = s.h * .022f
        val len = s.w * .85f
        fun y(x: Float) = s.surface + amp * sin(2 * PI.toFloat() * (x - s.tintyX) / len + PI.toFloat() / 2) - amp
        val pool = Path().apply {
            moveTo(0f, s.h); lineTo(0f, y(0f))
            var x = 0f
            while (x < s.w) { x += 4 * d; lineTo(x, y(x)) }
            lineTo(s.w, s.h); close()
        }
        val top = s.surface - amp * 2
        p.shader = LinearGradient(0f, top, 0f, s.h, color, shade(color, -.16f), Shader.TileMode.CLAMP)
        c.drawPath(pool, p); p.shader = null
        // Gloss across the surface, then a crisp lip so pale colors (ivory) still read on white paper.
        p.shader = LinearGradient(0f, top, 0f, top + 12 * d, withAlpha(Color.WHITE, .3f), withAlpha(Color.WHITE, 0f), Shader.TileMode.CLAMP)
        c.drawPath(pool, p); p.shader = null
        val lip = Path().apply {
            moveTo(0f, y(0f)); var x = 0f
            while (x < s.w) { x += 4 * d; lineTo(x, y(x)) }
        }
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 1.2f * d; p.color = withAlpha(shade(color, -.28f), if (lum(color) > .8f) .55f else .25f)
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

    private fun drop(c: Canvas, p: Paint, x: Float, y: Float, r: Float, color: Int) {
        p.shader = RadialGradient(x - r * .3f, y - r * .3f, r * 1.2f, shade(color, .25f), shade(color, -.08f), Shader.TileMode.CLAMP)
        c.drawCircle(x, y, r, p); p.shader = null
        p.color = withAlpha(Color.WHITE, .8f); c.drawCircle(x - r * .35f, y - r * .35f, r * .3f, p)
    }

    /**
     * Today's outfit as a flat-lay: the pieces overlap a little, the middle one forward, each with a soft shadow.
     * [frosted] = the locked teaser: generic pieces in the tone's colors, heavily blurred (never the real outfit).
     */
    fun lookbook(pieces: List<Bitmap>, cell: Int, density: Float, frosted: Boolean): Bitmap {
        val n = pieces.size.coerceAtLeast(1)
        val step = cell * .74f
        val out = Bitmap.createBitmap((cell + step * (n - 1)).roundToInt(), cell, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.rgb(0x2B, 0x22, 0x36), .2f)
            maskFilter = BlurMaskFilter(5 * density, BlurMaskFilter.Blur.NORMAL)
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        // Draw the sides first so the middle piece sits on top.
        val order = if (n == 3) listOf(0, 2, 1) else pieces.indices.toList()
        for (i in order) {
            val b = pieces[i]
            val scale = if (n == 3 && i == 1) 1f else .92f
            val sz = cell * scale
            val left = i * step + (cell - sz) / 2; val top = (cell - sz) / 2
            val dst = RectF(left, top, left + sz, top + sz)
            val alpha = b.extractAlpha()
            c.drawBitmap(alpha, null, RectF(dst.left, dst.top + 3 * density, dst.right, dst.bottom + 3 * density), shadow)
            alpha.recycle()
            c.drawBitmap(b, null, dst, paint)
        }
        if (!frosted) return out
        // Soft focus: halve down to 1/16, then double back up; each bilinear step smooths the last (a cheap, even blur).
        var b = out
        repeat(4) { b = Bitmap.createScaledBitmap(b, (b.width / 2).coerceAtLeast(1), (b.height / 2).coerceAtLeast(1), true) }
        repeat(4) { b = Bitmap.createScaledBitmap(b, b.width * 2, b.height * 2, true) }
        return Bitmap.createScaledBitmap(b, out.width, out.height, true)
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
