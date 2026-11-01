package com.gun.personalcolor.core.ui

import androidx.compose.ui.graphics.toArgb
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
import android.graphics.RenderNode
import android.os.Build
import android.graphics.Shader
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.TintyPainter
import com.gun.personalcolor.core.designsystem.tintyBitmap
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Text-free phone wallpapers from a tone's best colors. Soft, large forms plus a fine film grain
 * (kills gradient banding). The top ~25% stays calm for the clock; nothing busy sits on the dock area.
 */
object WallpaperRenderer {
    /** Live-motion tempo: 1 = the original slow drift. The live wallpaper must read as moving within a second or two. */
    private const val MOTION_SPEED = 3.5f

    /** [supportsDark]: has a dark variant worth offering. [animated]: has a moving version for the live wallpaper. */
    enum class Style(val supportsDark: Boolean, val animated: Boolean = false) {
        AURORA(true, true), GLOW(true, true), WAVE(true, true), PALETTE(false), MINIMAL(false), BOKEH(true, true),
        /** Aurora with the tone's 틴티 standing just above the dock. */
        TINTY(true, true),
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
     * Live-wallpaper extras, owned and updated by the wallpaper engine (one instance, no per-frame allocation).
     * Ages are seconds since the event; < 0 = none.
     */
    class Live {
        var tapX = 0f
        var tapY = 0f
        /** Since the last tap: tone-colored ripple, 틴티 hops and winks. */
        var tapAge = -1f
        /** Since the home screen came back after a while (unlock): 틴티 wakes up and stretches. */
        var wakeAge = -1f
    }

    /**
     * Draws one full frame. [t] (seconds) moves the animated styles; t = 0 is exactly the still image.
     * [dx]/[dy] shift the moving layer for parallax (px; the art overscans a little so edges never show).
     * Allocates only a few shaders per frame, so it is cheap enough to call at 30 fps on a hardware canvas.
     * [night]: slower motion and 틴티 sleeps. [live] (live wallpaper only) adds floating paint drops, a light sweep,
     * touch ripples and 틴티's idle animation and reactions; the still image never has them.
     */
    fun draw(
        c: Canvas, tone: Tone, style: Style, width: Int, height: Int, dark: Boolean = false, date: LocalDate = LocalDate.now(),
        t: Float = 0f, dx: Float = 0f, dy: Float = 0f, night: Boolean = false, live: Live? = null,
    ) {
        val palette = paletteOf(tone)
        fun art(canvas: Canvas, x: Float, y: Float) = Art(canvas, width.toFloat(), height.toFloat(), palette, date.dayOfYear % palette.size,
            dark && style.supportsDark, Random(date.toEpochDay() * 31 + style.ordinal), if (night) t * .5f else t, x, y)
        val w = width.toFloat(); val h = height.toFloat()
        val bg = bgNode
        if (live != null && style == Style.TINTY && bg != null && c.isHardwareAccelerated) {
            // Live 틴티 style, the hero: the blurry background is re-recorded every 3rd frame (10 fps; steps don't show
            // on soft forms) and 틴티 every 2nd (15 fps), both replayed every frame; parallax, drops, hops and ripples
            // stay at 30 fps. Keeps the frame cost low on the wallpaper's background-priority thread.
            val frame = liveFrame++
            val key = "${tone.id}/$dark/$width/$height/$date"
            if (frame % 3 == 0 || key != bgKey) {
                bg.setPosition(0, 0, width, height)
                art(bg.beginRecording(), 0f, 0f).apply { aurora(); if (!this.dark) wash(); grain() }
                bg.endRecording(); bgKey = key
            }
            c.save(); c.translate(dx, dy); c.drawRenderNode(bg); c.restore()
            val a = art(c, dx, dy)
            a.sweep(); a.drops(11)
            drawLiveTinty(c, tone, w, h, if (night) t * .5f else t, dx, night, live, refresh = frame % 2 == 1)
            if (live.tapAge >= 0f) a.ripple(live.tapX, live.tapY, live.tapAge)
            return
        }
        val art = art(c, dx, dy)
        when (style) {
            Style.AURORA -> { art.aurora(); if (live != null) { art.sweep(); art.drops(12) } }
            Style.GLOW -> { art.glow(); if (live != null) art.drops(10) }
            Style.WAVE -> { art.wave(); if (live != null) art.sweep() }
            Style.PALETTE -> art.palette()
            Style.MINIMAL -> art.minimal()
            Style.BOKEH -> art.bokeh()
            Style.TINTY -> {
                art.aurora()
                // Pastel wash so 틴티 never sits on its own body color (BRAND_SYSTEM §6-4); dark keeps the deep aurora.
                if (!art.dark) art.wash()
                if (live != null) { art.sweep(); art.drops(11) }
            }
        }
        art.grain()
        if (style == Style.TINTY) {
            if (live == null) drawTinty(c, tone, w, h, dx, if (night) TintyExpression.Sleep else TintyExpression.Default)
            else drawLiveTinty(c, tone, w, h, if (night) t * .5f else t, dx, night, live, refresh = true)
        }
        if (live != null && live.tapAge >= 0f) art.ripple(live.tapX, live.tapY, live.tapAge)
    }

    private val bgNode = if (Build.VERSION.SDK_INT >= 29) RenderNode("background").apply { setClipToBounds(false) } else null
    private var bgKey = ""
    private var liveFrame = 0

    /** Still 틴티 centered above the dock (~40% of the width incl. its 15% margin), from a cached bitmap. */
    private fun drawTinty(c: Canvas, tone: Tone, w: Float, h: Float, dx: Float, expression: TintyExpression) {
        val size = (w * .4f).roundToInt().coerceAtLeast(16)
        c.drawBitmap(tintyBitmapFor(tone, size, expression), (w - size) / 2 + dx * .5f, h * .86f - size, tintyPaint)
    }

    /**
     * Live 틴티, drawn as vectors every frame (cheap on a hardware canvas): idle bounce, blinks, ribbon sway, gloss sweep.
     * A tap makes it hop and wink; coming back to the home screen wakes it up (sleepy → stretch); at night it dozes.
     */
    private fun drawLiveTinty(c: Canvas, tone: Tone, w: Float, h: Float, t: Float, dx: Float, night: Boolean, live: Live, refresh: Boolean) {
        val tap = live.tapAge; val wake = live.wakeAge
        val expression = when {
            tap in 0f..1.5f -> TintyExpression.Happy
            wake in 0f..0.8f -> TintyExpression.Sleep
            wake in 0.8f..2.6f -> TintyExpression.Cheer
            night -> TintyExpression.Sleep
            else -> TintyExpression.Default
        }
        val size = w * .4f
        // Hop: 0.5 s parabola, then a quick squash on landing.
        val hopAge = when { tap in 0f..0.7f -> tap; wake in 0.8f..1.5f -> wake - .8f; else -> -1f }
        val height = if (tap >= 0f) size * .24f else size * .16f
        var lift = 0f; var sx = 1f; var sy = 1f
        if (hopAge in 0f..0.5f) {
            val u = hopAge / .5f
            lift = height * 4 * u * (1 - u); sy = 1f + .06f * sin(PI.toFloat() * u); sx = 1f - .04f * sin(PI.toFloat() * u)
        } else if (hopAge in 0.5f..0.7f) {
            val v = sin(PI.toFloat() * (hopAge - .5f) / .2f); sx = 1f + .08f * v; sy = 1f - .09f * v
        }
        val left = (w - size) / 2 + dx * .5f
        val top = h * .86f - size
        c.save()
        c.scale(sx, sy, left + size / 2, top + size * .88f)
        c.translate(0f, -lift)
        drawTintyLayer(c, painterFor(tone, expression), "${tone.id}/$expression/$size", left, top, size, t, refresh)
        c.restore()
    }

    private var palette: Pair<String, List<Int>>? = null

    /** Parsed once per tone: the live wallpaper asks every frame. */
    @Synchronized
    private fun paletteOf(tone: Tone): List<Int> = palette?.takeIf { it.first == tone.id }?.second
        ?: tone.bestColors.map { hexColor(it.hex).toArgb() }.ifEmpty { listOf(Color.GRAY) }.also { palette = tone.id to it }

    /** 틴티's vector drawing (the heaviest layer), recorded when [refresh] (or the pose changed) and replayed otherwise. */
    private val tintyNode = if (Build.VERSION.SDK_INT >= 29) RenderNode("tinty").apply { setClipToBounds(false) } else null
    private var tintyNodeKey = ""

    private fun drawTintyLayer(c: Canvas, painter: TintyPainter, key: String, left: Float, top: Float, size: Float, t: Float, refresh: Boolean) {
        val node = tintyNode
        if (node == null || !c.isHardwareAccelerated) return painter.draw(c, left, top, size, t)
        if (refresh || key != tintyNodeKey) {
            node.setPosition(0, 0, size.roundToInt(), size.roundToInt())
            val rc = node.beginRecording()
            painter.draw(rc, 0f, 0f, size, t)
            node.endRecording()
            tintyNodeKey = key
        }
        c.save(); c.translate(left, top); c.drawRenderNode(node); c.restore()
    }

    private val painters = HashMap<String, TintyPainter>()

    @Synchronized
    private fun painterFor(tone: Tone, expression: TintyExpression): TintyPainter =
        painters.getOrPut("${tone.id}/$expression") {
            if (painters.size > 12) painters.clear()
            TintyPainter(tone.tintyColors, expression)
        }

    private val tintyPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private var tintyCache: Pair<String, Bitmap>? = null

    @Synchronized
    private fun tintyBitmapFor(tone: Tone, size: Int, expression: TintyExpression): Bitmap {
        val key = "${tone.id}/$size/$expression"
        tintyCache?.let { (k, b) -> if (k == key) return b }
        return tintyBitmap(size, tone.tintyColors, expression).also { tintyCache = key to it }
    }

    private val grainShader: Shader by lazy { BitmapShader(grainTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }

    /** Soft round light with a gaussian-like falloff (alpha only; drawn tinted), so no edge ever shows. */
    private val blobSprite: Bitmap by lazy {
        val n = 256
        val stops = floatArrayOf(1f, .83f, .47f, .18f, 0f) // at 0, .25, .5, .75, 1 of the radius
        val px = IntArray(n * n) { i ->
            val d = (hypot(i % n - n / 2 + .5f, i / n - n / 2 + .5f) / (n / 2)).coerceAtMost(1f) * 4
            val k = d.toInt().coerceAtMost(3)
            val a = stops[k] + (stops[k + 1] - stops[k]) * (d - k)
            Color.argb((a * 255).roundToInt(), 0, 0, 0)
        }
        Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888).extractAlpha()
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
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
        val rect = android.graphics.RectF()

        fun f(min: Float, max: Float) = min + rnd.nextFloat() * (max - min)

        /** Slow swing, 0 at t = 0 (so the still frame is unchanged), roughly -1..1 over [period] seconds. */
        fun wob(period: Float, phase: Float) = sin(2 * PI.toFloat() * t * MOTION_SPEED / period + phase) - sin(phase)

        /** Mid-tone colors for flowing hues (no near-white/black, which would look dirty in a blob). */
        val ring = colors.filter { lum(it) in .08f..0.9f && sat(it) > .3f }.ifEmpty { colors.filter { lum(it) in .08f..0.9f } }.ifEmpty { colors }

        /** [base] at t = 0, then flowing through the tone's colors, one step every ~9 s. */
        fun flow(base: Int, offset: Float): Int {
            val start = ring.indexOf(base).takeIf { it >= 0 } ?: return base
            val u = t * MOTION_SPEED / 32f + offset
            val k = u.toInt()
            val f = (u - k).let { it * it * (3 - 2 * it) }
            return mix(ring[(start + k) % ring.size], ring[(start + k + 1) % ring.size], f)
        }

        /** The parallax-shifted layer. */
        inline fun moving(block: () -> Unit) {
            c.save(); c.translate(dx, dy); block(); c.restore()
        }

        /** Full-screen fill, overscanned so a parallax shift of the recorded background never shows an edge. */
        fun fill(shader: Shader) {
            p.shader = shader; c.drawRect(-w * .1f, -h * .05f, w * 1.1f, h * 1.05f, p); p.shader = null
        }

        /** Pastel veil over the 틴티 aurora. */
        fun wash() {
            p.color = Color.argb(125, 255, 255, 255); c.drawRect(-w * .1f, -h * .05f, w * 1.1f, h * 1.05f, p)
        }

        fun vertical(y0: Float, y1: Float, vararg cs: Int, pos: FloatArray? = null) =
            LinearGradient(0f, y0, 0f, y1, cs, pos, Shader.TileMode.CLAMP)

        /** Soft round light with a gaussian-like falloff, so no edge ever shows. */
        fun blob(x: Float, y: Float, r: Float, color: Int, alpha: Float = 1f) {
            // A cached alpha-only sprite tinted by the paint color: no shader per blob, so it's cheap at 30 fps.
            p.color = withAlpha(color, alpha)
            rect.set(x - r, y - r, x + r, y + r)
            c.drawBitmap(blobSprite, null, rect, p)
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
                listOf(others[0], main, others[1], others[2], main).forEachIndexed { i, base ->
                    // Companions drift through the tone's colors; today's color stays put.
                    val col = if (i == 1 || i == 4) base else flow(base, 0f)
                    val x = w * (if (i % 2 == 0) f(-.15f, .35f) else f(.65f, 1.15f)) + w * .24f * wob(17f + i * 5, i * 2.4f)
                    val y = h * (.32f + i * .16f + f(-.05f, .05f)) + h * .1f * wob(23f + i * 4, i * 1.3f + 1f)
                    val r = w * (if (base == main) f(.95f, 1.2f) else f(.7f, .95f)) * (1 + .12f * wob(11f + i * 3, i * .7f))
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
                blob(cx + w * f(-.3f, .3f) + w * .3f * wob(19f, 0f), cy + h * f(.1f, .18f) + h * .08f * wob(13f, 2f), w * .8f, flow(others[0], .5f), .45f)
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
                val bob = base + h * .022f * wob(9f + i * 2, i * 1.9f)
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
                    // Rises slowly through the band below the clock and comes back in at the bottom (faded at both ends).
                    val y0 = h * (.3f + .55f * (rnd.nextFloat() + rnd.nextFloat()) / 2) - h * .012f * near * MOTION_SPEED * t
                    val y = h * .25f + (y0 - h * .25f).mod(h * .85f)
                    val edge = ((y - h * .25f) / (h * .12f)).coerceIn(0f, 1f) * ((h * 1.1f - y) / (h * .12f)).coerceIn(0f, 1f)
                    val a = f(.25f, .55f) * (1 + .2f * wob(7f + i % 4 * 2, i * 2f)) * edge
                    p.shader = RadialGradient(x, y, r, intArrayOf(withAlpha(color, a * .8f), withAlpha(color, a), withAlpha(color, 0f)),
                        floatArrayOf(0f, f(.55f, .85f), 1f), Shader.TileMode.CLAMP)
                    c.drawCircle(x, y, r, p)
                }
                p.shader = null; p.xfermode = null
            }
        }

        fun grain() = fill(grainShader)

        /** A soft diagonal band of light crossing the screen every ~9 s (live only). */
        fun sweep() {
            val u = (t * MOTION_SPEED / 32f + .55f) % 1f * 1.6f - .3f // -0.3..1.3 of the diagonal
            val x = w * u * 1.6f - w * .3f
            val y = h * u
            val band = w * .45f
            fill(LinearGradient(x - band, y - band * .5f, x + band, y + band * .5f,
                intArrayOf(0, withAlpha(Color.WHITE, if (dark) .07f else .2f), 0), null, Shader.TileMode.CLAMP))
        }

        /**
         * Glossy paint drops rising from the bottom and fading out before the clock area (live only). Nearer drops are
         * bigger, faster and shift more with parallax.
         */
        fun drops(n: Int) {
            val span = h * .8f
            repeat(n) { i ->
                val near = f(0f, 1f)
                val r = w * (.008f + .02f * near)
                val travel = (f(0f, 1f) * span + t * h * (.035f + .045f * near)) % span
                val y = h * 1.02f - travel
                val x = w * f(.04f, .96f) + w * .03f * sin(2 * PI.toFloat() * t / f(3.5f, 6f) + i) + dx * (.6f + near)
                val life = travel / span
                val a = f(.45f, .8f) * (life / .12f).coerceAtMost(1f) * ((1f - life) / .4f).coerceIn(0f, 1f)
                if (a <= 0f) return@repeat
                val col = flow(ring[(i * 5 + today) % ring.size], i * .3f) // mid-tones only: no grey/black dots
                val body = shade(col, if (dark) .1f else .32f) // pastel on the light wash
                blob(x, y, r * 2.8f, body, a * .4f)
                p.color = withAlpha(body, a); c.drawCircle(x, y, r, p)
                p.color = withAlpha(Color.WHITE, a * .8f)
                c.drawCircle(x - r * .35f, y - r * .4f, r * .28f, p)
            }
        }

        /** Rings of the tone's colors spreading from a tap. */
        fun ripple(x: Float, y: Float, age: Float) {
            val u0 = (age / .6f).coerceIn(0f, 1f)
            blob(x, y, w * (.08f + .22f * u0), main, .45f * (1 - u0))
            p.style = Paint.Style.STROKE
            listOf(main, others[0], others[1]).forEachIndexed { k, col ->
                val u = (age - k * .13f) / 1.1f
                if (u !in 0f..1f) return@forEachIndexed
                val e = 1 - (1 - u) * (1 - u) * (1 - u)
                p.strokeWidth = w * .012f * (1 - u) + 1.5f
                p.color = withAlpha(shade(col, if (dark) .2f else .3f), .65f * (1 - u))
                c.drawCircle(x, y, w * (.05f + .3f * e), p)
            }
            p.style = Paint.Style.FILL
        }
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(x: Int, y: Int) = (x + (y - x) * t).roundToInt()
        return Color.rgb(ch(Color.red(a), Color.red(b)), ch(Color.green(a), Color.green(b)), ch(Color.blue(a), Color.blue(b)))
    }

    private fun sat(c: Int): Float {
        val mx = maxOf(Color.red(c), Color.green(c), Color.blue(c)); val mn = minOf(Color.red(c), Color.green(c), Color.blue(c))
        return if (mx == 0) 0f else (mx - mn) / mx.toFloat()
    }

    private fun lum(c: Int) = (.2126f * Color.red(c) + .7152f * Color.green(c) + .0722f * Color.blue(c)) / 255f

    /** t > 0 toward white, t < 0 toward black. */
    private fun shade(c: Int, t: Float) = if (t >= 0) mix(c, Color.WHITE, t) else mix(c, Color.BLACK, -t)

    private fun withAlpha(c: Int, a: Float) = (c and 0xFFFFFF) or ((a * 255).roundToInt().coerceIn(0, 255) shl 24)
}
