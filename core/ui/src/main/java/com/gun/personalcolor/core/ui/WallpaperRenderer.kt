package com.gun.personalcolor.core.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import java.time.LocalDate
import java.util.Random

/**
 * Phone wallpapers from a tone's best colors. The top ~20% stays calm for the clock and
 * nothing important goes in the bottom ~10% (gesture bar / dock).
 */
object WallpaperRenderer {
    enum class Style { TODAY, PALETTE_CARD, SEASON, GRADIENT, STRIPES, GRID, DOTS }

    /** Same rule as the home screen and the widget. */
    fun todayColor(tone: Tone, date: LocalDate = LocalDate.now()): NamedColor =
        tone.bestColors[date.dayOfYear % tone.bestColors.size]

    /** [todayColor] defaults to today's color of [tone]; used by TODAY (and as the lead color of SEASON). */
    fun render(
        tone: Tone, style: Style, width: Int, height: Int, dark: Boolean, showName: Boolean = true,
        todayColor: NamedColor? = null,
    ): Bitmap {
        val w = width.toFloat(); val h = height.toFloat()
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = if (dark) 0xFF161416.toInt() else 0xFFFAF8F6.toInt()
        val colors = tone.bestColors.map { Color.parseColor(it.hex) }
            .map { if (dark) blend(it, Color.BLACK, 0.18f) else it }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(bg)
        val today = todayColor ?: todayColor(tone)
        val todayInt = Color.parseColor(today.hex).let { if (dark) blend(it, Color.BLACK, 0.18f) else it }
        var fadeTo: Int? = bg // color the calm clock area fades into; null = the style handles it
        var namePill = showName

        when (style) {
            Style.TODAY -> {
                // Soft field of today's color, lighter (or deeper in dark) toward the top.
                val top = blend(todayInt, bg, if (dark) 0.55f else 0.45f)
                val bottom = blend(todayInt, Color.BLACK, if (dark) 0.25f else 0.06f)
                p.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(top, todayInt, bottom), floatArrayOf(0.05f, 0.6f, 1f), Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, w, h, p)
                // Subtle palette accents: a few wide, faint glows from neighbouring colors.
                val others = colors.filter { it != todayInt }
                listOf(0.05f to 0.55f, 0.95f to 0.68f, 0.35f to 0.98f).forEachIndexed { i, (fx, fy) ->
                    val col = others[(i * 3 + 1) % others.size]
                    p.shader = RadialGradient(fx * w, fy * h, w * 0.6f, intArrayOf(alpha(col, 80), alpha(col, 0)), null, Shader.TileMode.CLAMP)
                    c.drawRect(0f, 0f, w, h, p)
                }
                p.shader = null
                val ink = inkOn(todayInt)
                drawCentered(c, p, today.name, h * 0.83f, w * 0.045f, alpha(ink, 225), bold = true)
                if (showName) {
                    val d = LocalDate.now()
                    drawCentered(c, p, "${d.monthValue}월 ${d.dayOfMonth}일 · 오늘의 컬러", h * 0.83f + w * 0.065f, w * 0.03f, alpha(ink, 150))
                }
                fadeTo = null; namePill = false
            }
            Style.PALETTE_CARD -> {
                // Paint-chip card: tone name on top, the 12 best colors in a 3×4 grid with names.
                p.shader = RadialGradient(w * 0.5f, h * 0.55f, h * 0.6f, intArrayOf(blend(bg, colors[0], 0.22f), bg), null, Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, w, h, p); p.shader = null
                val m = w * 0.1f; val top = h * 0.25f; val bottom = h * 0.87f
                val card = RectF(m, top, w - m, bottom)
                val r = w * 0.045f
                p.color = if (dark) 0xFF242125.toInt() else Color.WHITE
                p.setShadowLayer(w * 0.04f, 0f, w * 0.012f, if (dark) 0x66000000 else 0x22000000)
                c.drawRoundRect(card, r, r, p)
                p.clearShadowLayer()
                val pad = w * 0.06f
                val ink = if (dark) 0xF0FFFFFF.toInt() else 0xFF1D1B1A.toInt()
                val muted = alpha(ink, 140)
                p.typeface = Typeface.DEFAULT_BOLD; p.textSize = w * 0.05f; p.color = ink
                c.drawText(ellipsize(p, tone.nameKo, card.width() - 2 * pad), card.left + pad, card.top + pad + p.textSize * 0.9f, p)
                p.typeface = Typeface.DEFAULT; p.textSize = w * 0.026f; p.color = muted
                c.drawText(ellipsize(p, tone.nameEn.uppercase(), card.width() - 2 * pad), card.left + pad, card.top + pad + w * 0.095f, p)
                val gridTop = card.top + pad + w * 0.15f
                val gridBottom = card.bottom - pad
                val cols = 3; val rows = 4; val gap = w * 0.03f
                val label = if (showName) w * 0.045f else 0f
                val cw = (card.width() - 2 * pad - gap * (cols - 1)) / cols
                val ch = (gridBottom - gridTop - gap * (rows - 1)) / rows - label
                tone.bestColors.take(cols * rows).forEachIndexed { i, nc ->
                    val x = card.left + pad + (i % cols) * (cw + gap)
                    val y = gridTop + (i / cols) * (ch + label + gap)
                    p.color = colors[i]
                    c.drawRoundRect(RectF(x, y, x + cw, y + ch), w * 0.015f, w * 0.015f, p)
                    if (showName) {
                        p.textSize = w * 0.022f; p.color = muted
                        c.drawText(ellipsize(p, nc.name, cw), x, y + ch + w * 0.032f, p)
                    }
                }
                fadeTo = null; namePill = false
            }
            Style.SEASON -> {
                val base = blend(bg, todayInt, if (dark) 0.16f else 0.14f)
                c.drawColor(base); fadeTo = base
                val lead = listOf(todayInt) + colors.filter { it != todayInt }
                drawSeason(c, p, tone.season, lead, w, h, Random(tone.id.hashCode().toLong()))
            }
            Style.STRIPES -> {
                // Six soft vertical bands with short blended edges.
                val band = colors.filterIndexed { i, _ -> i % 2 == 0 }.take(6)
                val n = band.size; val e = 0.012f
                val cs = band.flatMap { listOf(it, it) }.toIntArray()
                val pos = (0 until n).flatMap { i -> listOf(if (i == 0) 0f else i / n.toFloat() + e, if (i == n - 1) 1f else (i + 1) / n.toFloat() - e) }.toFloatArray()
                p.shader = LinearGradient(0f, 0f, w, 0f, cs, pos, Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, w, h, p)
            }
            Style.GRID -> {
                val cols = 3; val rows = 4
                val m = w * 0.09f; val gap = w * 0.035f
                val top = h * 0.26f; val bottom = h * 0.86f
                val cw = (w - 2 * m - gap * (cols - 1)) / cols
                val ch = (bottom - top - gap * (rows - 1)) / rows
                val r = minOf(cw, ch) * 0.22f
                colors.take(cols * rows).forEachIndexed { i, col ->
                    val x = m + (i % cols) * (cw + gap); val y = top + (i / cols) * (ch + gap)
                    p.color = col
                    c.drawRoundRect(RectF(x, y, x + cw, y + ch), r, r, p)
                }
            }
            Style.GRADIENT -> {
                // Large radial blobs fading to transparent read as a blurred mesh gradient.
                p.color = blend(bg, colors[0], 0.35f); c.drawRect(0f, 0f, w, h, p)
                val spots = listOf(0.15f to 0.30f, 0.90f to 0.42f, 0.25f to 0.62f, 0.80f to 0.78f, 0.45f to 0.95f, 0.55f to 0.50f)
                spots.forEachIndexed { i, (fx, fy) ->
                    val col = colors[(i * 2) % colors.size]
                    p.shader = RadialGradient(fx * w, fy * h, w * 0.75f, intArrayOf(col, alpha(col, 0)), null, Shader.TileMode.CLAMP)
                    c.drawRect(0f, 0f, w, h, p)
                }
                p.shader = null
            }
            Style.DOTS -> {
                val spots = listOf(
                    Triple(0.22f, 0.36f, 0.30f), Triple(0.78f, 0.42f, 0.26f), Triple(0.50f, 0.56f, 0.34f),
                    Triple(0.18f, 0.72f, 0.28f), Triple(0.82f, 0.74f, 0.32f), Triple(0.45f, 0.90f, 0.30f),
                )
                spots.forEachIndexed { i, (fx, fy, fr) ->
                    p.color = alpha(colors[(i * 2 + 1) % colors.size], 225)
                    c.drawCircle(fx * w, fy * h, fr * w, p)
                }
            }
        }

        // Calm clock area: fade the art into the background toward the top.
        fadeTo?.let { f ->
            p.shader = LinearGradient(0f, h * 0.10f, 0f, h * 0.28f, f, alpha(f, 0), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w, h * 0.28f, p)
            p.shader = null
        }

        if (namePill) {
            val size = w * 0.026f
            p.typeface = Typeface.DEFAULT_BOLD; p.textSize = size
            val tw = p.measureText(tone.nameKo); val ph = size * 1.9f
            val l = (w - tw) / 2 - size; val t = h * 0.89f - ph
            p.color = alpha(bg, 170)
            c.drawRoundRect(RectF(l, t, l + tw + 2 * size, t + ph), ph / 2, ph / 2, p)
            p.color = if (dark) 0xCCFFFFFF.toInt() else 0xB31D1B1A.toInt()
            c.drawText(tone.nameKo, l + size, t + ph / 2 + size * 0.36f, p)
        }
        return bmp
    }

    /** Soft abstract motif per season: spring petals, summer waves, autumn leaves, winter ridges. */
    private fun drawSeason(c: Canvas, p: Paint, season: String, colors: List<Int>, w: Float, h: Float, rnd: Random) {
        fun col(i: Int) = colors[i % colors.size]
        fun r(from: Float, to: Float) = from + rnd.nextFloat() * (to - from)
        when (season) {
            "spring" -> {
                // Five-petal blossoms + a few loose petals.
                repeat(7) { i ->
                    val cx = r(0.1f, 0.9f) * w; val cy = r(0.32f, 0.86f) * h; val size = r(0.09f, 0.17f) * w
                    val rot = r(0f, 72f)
                    p.color = alpha(col(i), 175)
                    for (k in 0 until 5) {
                        c.save(); c.rotate(rot + k * 72f, cx, cy)
                        c.drawOval(RectF(cx - size * 0.3f, cy - size, cx + size * 0.3f, cy - size * 0.08f), p)
                        c.restore()
                    }
                    p.color = alpha(col(i + 5), 220)
                    c.drawCircle(cx, cy, size * 0.16f, p)
                }
                repeat(10) { i ->
                    val cx = r(0.05f, 0.95f) * w; val cy = r(0.3f, 0.9f) * h; val size = r(0.025f, 0.045f) * w
                    p.color = alpha(col(i + 2), 140)
                    c.save(); c.rotate(r(0f, 360f), cx, cy)
                    c.drawOval(RectF(cx - size * 0.4f, cy - size, cx + size * 0.4f, cy + size), p)
                    c.restore()
                }
            }
            "summer" -> {
                // Layered soft waves rising from the lower half.
                val bands = 5
                for (i in 0 until bands) {
                    val y = h * (0.46f + i * 0.09f); val amp = h * 0.022f; val len = w / r(1.2f, 1.8f)
                    val phase = r(0f, len)
                    val path = Path().apply {
                        moveTo(-phase, y)
                        var x = -phase; var up = true
                        while (x < w) {
                            quadTo(x + len / 4, y + if (up) -amp else amp, x + len / 2, y)
                            x += len / 2; up = !up
                        }
                        lineTo(w, h); lineTo(0f, h); close()
                    }
                    p.color = alpha(col(i * 2), 150 + i * 20)
                    c.drawPath(path, p)
                }
            }
            "autumn" -> {
                // Pointed leaves with a thin vein, gently scattered.
                repeat(11) { i ->
                    val cx = r(0.08f, 0.92f) * w; val cy = r(0.32f, 0.88f) * h; val len = r(0.12f, 0.24f) * w
                    val leaf = Path().apply {
                        moveTo(cx, cy - len / 2)
                        quadTo(cx + len * 0.42f, cy, cx, cy + len / 2)
                        quadTo(cx - len * 0.42f, cy, cx, cy - len / 2)
                        close()
                    }
                    c.save(); c.rotate(r(-60f, 60f) + if (i % 2 == 0) 180f else 0f, cx, cy)
                    p.style = Paint.Style.FILL; p.color = alpha(col(i), 200)
                    c.drawPath(leaf, p)
                    p.style = Paint.Style.STROKE; p.strokeWidth = w * 0.004f; p.color = alpha(Color.WHITE, 90)
                    c.drawLine(cx, cy - len * 0.38f, cx, cy + len * 0.45f, p)
                    c.restore()
                }
                p.style = Paint.Style.FILL
            }
            else -> {
                // Winter: layered crisp ridges with a few faint flakes above.
                repeat(14) { i ->
                    p.color = alpha(col(i + 3), 90)
                    c.drawCircle(r(0.05f, 0.95f) * w, r(0.3f, 0.55f) * h, r(0.006f, 0.014f) * w, p)
                }
                for (i in 0 until 4) {
                    val base = h * (0.6f + i * 0.08f)
                    val path = Path().apply {
                        moveTo(0f, base - r(0f, 0.05f) * h)
                        var x = 0f
                        while (x < w) {
                            x += r(0.15f, 0.3f) * w
                            lineTo(x, base - r(0.02f, 0.11f) * h)
                        }
                        lineTo(w, h); lineTo(0f, h); close()
                    }
                    p.color = alpha(col(i * 2), 170 + i * 20)
                    c.drawPath(path, p)
                }
            }
        }
    }

    private fun drawCentered(c: Canvas, p: Paint, text: String, y: Float, size: Float, color: Int, bold: Boolean = false) {
        p.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        p.textSize = size; p.color = color
        val t = ellipsize(p, text, c.width * 0.8f)
        c.drawText(t, (c.width - p.measureText(t)) / 2, y, p)
    }

    private fun ellipsize(p: Paint, text: String, max: Float): String {
        if (p.measureText(text) <= max) return text
        var end = text.length
        while (end > 0 && p.measureText(text, 0, end) + p.measureText("…") > max) end--
        return text.substring(0, end) + "…"
    }

    /** Dark ink on light colors, white on dark. */
    private fun inkOn(c: Int): Int {
        val l = (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255
        return if (l > 0.6) 0xFF1D1B1A.toInt() else Color.WHITE
    }

    private fun alpha(c: Int, a: Int) = (c and 0x00FFFFFF) or (a shl 24)

    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(x: Int, y: Int) = (x + (y - x) * t).toInt()
        return Color.argb(ch(Color.alpha(a), Color.alpha(b)), ch(Color.red(a), Color.red(b)), ch(Color.green(a), Color.green(b)), ch(Color.blue(a), Color.blue(b)))
    }
}
