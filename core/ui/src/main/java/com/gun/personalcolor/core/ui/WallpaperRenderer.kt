package com.gun.personalcolor.core.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Text-free phone wallpapers from a tone's best colors. Soft, large forms plus a fine film grain
 * (kills gradient banding). The top ~25% stays calm for the clock; nothing busy sits on the dock area.
 */
object WallpaperRenderer {
    /** Live-motion tempo: 1 = original very slow drift; ~2.5 makes the movement clearly visible but still calm. */
    private const val MOTION_SPEED = 2.5f

    /** [supportsDark]: has a dark variant worth offering. [animated]: has a moving version for the live wallpaper. */
    enum class Style(val supportsDark: Boolean, val animated: Boolean = false) {
        AURORA(true, true), GLOW(true, true), WAVE(true, true), PALETTE(false), MINIMAL(false), BOKEH(true, true)
    }

    /** Same rule as the home screen and the widget. */
    fun todayColor(tone: Tone, date: LocalDate = LocalDate.now()): NamedColor =
        tone.bestColors[date.dayOfYear % tone.bestColors.size]

    /** Composition is seeded by [date], so every day gets a slightly different take on the same style. */
    fun render(tone: Tone, style: Style, width: Int, height: Int, dark: Boolean = false, date: LocalDate = LocalDate.now()): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp), tone, style, width, height, dark, date)
        return bmp
    }

    /**
     * Draws one full frame. [t] (seconds) moves the animated styles; t = 0 is exactly the still image.
     * [dx]/[dy] shift the moving layer for parallax (px; the art overscans a little so edges never show).
     * Allocates only a few shaders per frame, so it is cheap enough to call at 30 fps on a hardware canvas.
     */
    fun draw(
        c: Canvas, tone: Tone, style: Style, width: Int, height: Int, dark: Boolean = false, date: LocalDate = LocalDate.now(),
        t: Float = 0f, dx: Float = 0f, dy: Float = 0f,
    ) {
        val palette = tone.bestColors.map { Color.parseColor(it.hex) }
        val art = Art(c, width.toFloat(), height.toFloat(), palette, date.dayOfYear % palette.size,
            dark && style.supportsDark, Random(date.toEpochDay() * 31 + style.ordinal), t, dx, dy)
        when (style) {
            Style.AURORA -> art.aurora()
            Style.GLOW -> art.glow()
            Style.WAVE -> art.wave()
            Style.PALETTE -> art.palette()
            Style.MINIMAL -> art.minimal()
            Style.BOKEH -> art.bokeh()
        }
        art.grain()
    }

    /** 256px tile of ~2% monochrome noise, repeated over the whole image. */
    private val grainTile: Bitmap by lazy {
        val n = 256
        val r = java.util.Random(7)
        val px = IntArray(n * n) {
            val g = r.nextGaussian()
            val a = (abs(g) * 7).toInt().coerceAtMost(24)
            if (g > 0) Color.argb(a, 255, 255, 255) else Color.argb(a, 0, 0, 0)
        }
        Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888)
    }

    private class Art(
        val c: Canvas, val w: Float, val h: Float, val colors: List<Int>, val today: Int, val dark: Boolean, val rnd: Random,
        val t: Float, val dx: Float, val dy: Float,
    ) {
        val main = colors[today]
        /** Three companion colors, picked by date. */
        val others = (colors - main).shuffled(rnd).ifEmpty { listOf(main) }.let { l -> List(3) { l[it % l.size] } }
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

        fun f(min: Float, max: Float) = min + rnd.nextFloat() * (max - min)

        /** Slow swing, 0 at t = 0 (so the still frame is unchanged), roughly -1..1 over [period] seconds. */
        fun wob(period: Float, phase: Float) = sin(2 * PI.toFloat() * t * MOTION_SPEED / period + phase) - sin(phase)

        /** The parallax-shifted layer. */
        inline fun moving(block: () -> Unit) {
            c.save(); c.translate(dx, dy); block(); c.restore()
        }

        fun fill(shader: Shader) {
            p.shader = shader; c.drawRect(0f, 0f, w, h, p); p.shader = null
        }

        fun vertical(y0: Float, y1: Float, vararg cs: Int, pos: FloatArray? = null) =
            LinearGradient(0f, y0, 0f, y1, cs, pos, Shader.TileMode.CLAMP)

        /** Soft round light with a gaussian-like falloff, so no edge ever shows. */
        fun blob(x: Float, y: Float, r: Float, color: Int, alpha: Float = 1f) {
            val a = floatArrayOf(1f, .83f, .47f, .18f, 0f)
            p.shader = RadialGradient(x, y, r, IntArray(5) { withAlpha(color, a[it] * alpha) },
                floatArrayOf(0f, .25f, .5f, .75f, 1f), Shader.TileMode.CLAMP)
            c.drawCircle(x, y, r, p); p.shader = null
        }

        /** Fades the clock area into [color]. */
        fun calmTop(color: Int, alpha: Float) =
            fill(vertical(0f, h * .4f, withAlpha(color, alpha), withAlpha(color, 0f)))

        fun vignette(strength: Float) {
            val edge = if (dark) Color.BLACK else shade(main, -.5f)
            p.shader = RadialGradient(w / 2, h / 2, hypot(w, h) / 2 * 1.05f,
                intArrayOf(withAlpha(edge, 0f), withAlpha(edge, 0f), withAlpha(edge, strength)),
                floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w, h, p); p.shader = null
        }

        fun aurora() {
            val top = if (dark) shade(main, -.88f) else shade(main, .72f)
            fill(vertical(0f, h, top, if (dark) shade(main, -.8f) else shade(main, .5f)))
            // Companions first, today's color on top and biggest.
            moving {
                listOf(others[0], main, others[1], others[2], main).forEachIndexed { i, col ->
                    val x = w * (if (i % 2 == 0) f(-.15f, .35f) else f(.65f, 1.15f)) + w * .16f * wob(17f + i * 5, i * 2.4f)
                    val y = h * (.32f + i * .16f + f(-.05f, .05f)) + h * .08f * wob(23f + i * 4, i * 1.3f + 1f)
                    val r = w * (if (col == main) f(.95f, 1.2f) else f(.7f, .95f)) * (1 + .09f * wob(11f + i * 3, i * .7f))
                    blob(x, y, r, if (dark) shade(col, -.15f) else shade(col, .12f), if (dark) .8f else .9f)
                }
            }
            calmTop(top, .6f)
        }

        fun glow() {
            fill(vertical(0f, h, if (dark) shade(main, -.92f) else shade(main, .9f), if (dark) shade(main, -.84f) else shade(main, .78f)))
            val cx = w * (.5f + f(-.12f, .12f))
            val cy = h * f(.54f, .62f)
            val breathe = wob(8f, 0f) // one slow breath every 8 s
            moving {
                blob(cx + w * f(-.3f, .3f) + w * .18f * wob(19f, 0f), cy + h * f(.1f, .18f) + h * .05f * wob(13f, 2f), w * .8f, others[0], .4f)
                blob(cx, cy, w * f(1f, 1.2f) * (1 + .12f * breathe), main)
                blob(cx, cy, w * .35f * (1 + .2f * breathe), shade(main, if (dark) .25f else .4f), .5f + .1f * breathe) // soft hot core
            }
            vignette(if (dark) .55f else .14f)
        }

        fun wave() {
            fill(vertical(0f, h, if (dark) shade(main, -.9f) else shade(main, .84f), if (dark) shade(main, -.78f) else shade(main, .62f)))
            val tints = listOf(others[0], main, others[1], main, others[2])
            moving { tints.forEachIndexed { i, tint ->
                val depth = i / (tints.size - 1f) // 0 = back, 1 = front
                val col = mix(main, tint, .35f) // today's color always leads
                val color = if (dark) shade(col, -.78f + depth * .55f) else shade(col, .58f - depth * .78f)
                val base = h * (.44f + depth * .38f)
                val amp = h * f(.018f, .04f)
                val len = w * f(.9f, 1.7f)
                // Drifts sideways, alternating direction per layer; front layers a bit faster.
                val ph = f(0f, 2 * PI.toFloat()) + t * MOTION_SPEED * 2 * PI.toFloat() / (22f - i * 2.5f) * (if (i % 2 == 0) 1 else -1)
                val bob = base + h * .014f * wob(9f + i * 2, i * 1.9f)
                val (x0, x1, bottom) = Triple(-w * .1f, w * 1.1f, h * 1.1f) // overscan for parallax
                val path = Path().apply {
                    moveTo(x0, bottom)
                    val step = w / 96
                    var x = x0
                    while (x <= x1 + step) {
                        lineTo(x, bob + amp * sin(2 * PI.toFloat() * x / len + ph) + amp * .4f * sin(2 * PI.toFloat() * x / (len * .53f) + ph * 1.7f))
                        x += step
                    }
                    lineTo(x1, bottom); close()
                }
                p.shader = vertical(base - amp, h, color, shade(color, -.18f))
                p.setShadowLayer(48 * w / 1080, 0f, -10 * w / 1080, withAlpha(Color.BLACK, if (dark) .45f else .16f))
                c.drawPath(path, p)
                p.clearShadowLayer(); p.shader = null
            } }
        }

        fun palette() {
            val n = colors.size
            val start = today - n / 2 + n // today's color sits in the middle
            val e = .22f / n // soft blend between bands
            val cs = IntArray(2 * n) { colors[(start + it / 2) % n] }
            val pos = FloatArray(2 * n) { if (it % 2 == 0) (it / 2f / n + e) else ((it / 2 + 1f) / n - e) }
            pos[0] = 0f; pos[2 * n - 1] = 1f
            fill(LinearGradient(0f, 0f, w, 0f, cs, pos, Shader.TileMode.CLAMP))
            fill(vertical(0f, h, withAlpha(Color.WHITE, .4f), withAlpha(Color.WHITE, 0f), withAlpha(Color.BLACK, 0f), withAlpha(Color.BLACK, .16f),
                pos = floatArrayOf(0f, .38f, .7f, 1f)))
        }

        fun minimal() {
            fill(vertical(0f, h, shade(main, .14f), main, shade(main, -.12f)))
            blob(w * if (rnd.nextBoolean()) -.1f else 1.1f, -h * .05f, w * 1.3f, Color.WHITE, .18f)
        }

        fun bokeh() {
            fill(vertical(0f, h, if (dark) shade(main, -.9f) else shade(main, .8f), if (dark) shade(main, -.78f) else shade(main, .56f)))
            moving {
                blob(w * f(0f, 1f) + w * .14f * wob(29f, 0f), h * f(.5f, .8f), w, main, .5f)
                blob(w * f(0f, 1f) + w * .14f * wob(23f, 2f), h * f(.6f, .9f), w * .8f, others[0], .35f)
                if (dark) p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                // Big, faint circles first (far), small crisp ones last (near). Kept out of the clock area.
                // Each floats on its own slow loop; near ones travel further.
                List(26) { w * f(.03f, .13f) }.sortedDescending().forEachIndexed { i, r ->
                    val near = .6f + i / 25f
                    val col = if (rnd.nextFloat() < .5f) main else others[rnd.nextInt(3)]
                    val color = if (dark) col else shade(col, .1f)
                    val x = w * f(-.05f, 1.05f) + w * .06f * near * wob(13f + i % 7 * 2, i * 1.1f)
                    val y = h * (.3f + .55f * (rnd.nextFloat() + rnd.nextFloat()) / 2) + h * .05f * near * wob(17f + i % 5 * 3, i * .9f)
                    val a = f(.25f, .55f) * (1 + .2f * wob(7f + i % 4 * 2, i * 2f))
                    p.shader = RadialGradient(x, y, r, intArrayOf(withAlpha(color, a * .8f), withAlpha(color, a), withAlpha(color, 0f)),
                        floatArrayOf(0f, f(.55f, .85f), 1f), Shader.TileMode.CLAMP)
                    c.drawCircle(x, y, r, p)
                }
                p.shader = null; p.xfermode = null
            }
        }

        fun grain() = fill(BitmapShader(grainTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT))
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(x: Int, y: Int) = (x + (y - x) * t).roundToInt()
        return Color.rgb(ch(Color.red(a), Color.red(b)), ch(Color.green(a), Color.green(b)), ch(Color.blue(a), Color.blue(b)))
    }

    /** t > 0 toward white, t < 0 toward black. */
    private fun shade(c: Int, t: Float) = if (t >= 0) mix(c, Color.WHITE, t) else mix(c, Color.BLACK, -t)

    private fun withAlpha(c: Int, a: Float) = (c and 0xFFFFFF) or ((a * 255).roundToInt().coerceIn(0, 255) shl 24)
}
