package com.gun.personalcolor.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone

/** A shareable 1080×1920 "나의 퍼스널컬러 리포트" image (light theme). Never contains the user's photo. */
object ReportRenderer {
    private const val W = 1080
    private const val H = 1920
    private const val M = 72f
    private const val CW = W - 2 * M
    private const val BG = 0xFFFAF8F6.toInt()
    private const val INK = 0xFF1D1B1A.toInt()
    private const val MUTED = 0xFF6F6863.toInt()
    private const val FAINT = 0xFF9A918B.toInt()
    private const val ACCENT = 0xFF5B4B8A.toInt()
    private const val ACCENT_BG = 0xFFECE8F4.toInt()
    private const val LINE = 0x1F1D1B1A

    @Suppress("UNUSED_PARAMETER") // kept for future localized strings/resources
    fun render(context: Context, tone: Tone): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(BG)

        // Header: gradient card from the best colors with a small white pill.
        val header = RectF(M, 56f, W - M, 296f)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(header.left, header.top, header.right, header.bottom,
                tone.bestColors.take(5).map { Color.parseColor(it.hex) }.toIntArray(), null, Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(header, 48f, 48f, fill)
        pill(c, "PERSONAL COLOR REPORT", header.left + 36f, header.top + 36f, 24f, Color.WHITE, INK)

        var y = header.bottom + 70f
        c.drawText("나의 퍼스널컬러", M, y, paint(30f, false, MUTED))
        y += 104f
        val name = fit(tone.nameKo, 96f, 48f, true, CW)
        c.drawText(name.second, M, y, paint(name.first, true, INK))
        y += 54f
        val sub = "${tone.seasonKo} · ${tone.temperatureKo} · ${tone.nameEn}"
        c.drawText(ellipsize(sub, paint(32f, false, MUTED), CW), M, y, paint(32f, false, MUTED))
        y += 28f
        var x = M
        for (k in tone.keywords) {
            val w = pillWidth("#$k", 26f)
            if (x + w > W - M) break
            pill(c, "#$k", x, y, 26f, ACCENT_BG, ACCENT)
            x += w + 12f
        }
        y += 52f

        y = title(c, "베스트 컬러", y)
        y = grid(c, tone.bestColors.take(12), y, swatchH = 104f) + 10f
        y = title(c, "피하면 좋은 컬러", y)
        y = grid(c, tone.worstColors.take(6), y, swatchH = 72f) + 10f

        // Two columns: makeup | hair + accessory.
        val colW = (CW - 48f) / 2
        val right = M + colW + 48f
        val top = y
        var ly = title(c, "립 · 아이 · 블러셔", top, M)
        for ((label, colors) in listOf("립" to tone.makeup.lipColors, "아이" to tone.makeup.eyeColors, "블러셔" to tone.makeup.blushColors)) {
            c.drawText(label, M, ly + 36f, paint(26f, true, INK))
            dots(c, colors, M + 110f, ly, colW - 110f)
            ly += 64f
        }
        var ry = title(c, "헤어 컬러", top, right)
        dots(c, tone.hair.colors, right, ry, colW)
        ry = title(c, "액세서리", ry + 52f, right)
        val gold = tone.accessory.metal.contains("골드")
        val metal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(right, ry, right + 52f, ry + 52f,
                if (gold) intArrayOf(0xFFF3DC8E.toInt(), 0xFFB8902F.toInt()) else intArrayOf(0xFFF1F2F4.toInt(), 0xFF9EA4AC.toInt()),
                null, Shader.TileMode.CLAMP)
        }
        c.drawCircle(right + 26f, ry + 26f, 26f, metal)
        c.drawText(ellipsize(tone.accessory.metal, paint(30f, true, INK), colW - 68f), right + 68f, ry + 37f, paint(30f, true, INK))
        ry += 64f
        ry += layout(c, tone.accessory.tip, right, ry, colW, 24f, MUTED, maxLines = 2)
        y = maxOf(ly, ry)

        y = title(c, "코디 추천", y)
        val bullet = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT }
        for (s in tone.fashion.styling.take(3)) {
            c.drawCircle(M + 8f, y + 24f, 6f, bullet)
            c.drawText(ellipsize(s, paint(30f, false, INK), CW - 32f), M + 32f, y + 34f, paint(30f, false, INK))
            y += 48f
        }

        val footer = paint(26f, true, FAINT).apply { textAlign = Paint.Align.CENTER }
        c.drawText("나의 컬러는? - 퍼스널컬러", W / 2f, H - 44f, footer)
        return bmp
    }

    private fun paint(size: Float, bold: Boolean, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun ellipsize(s: String, p: TextPaint, maxW: Float) = TextUtils.ellipsize(s, p, maxW, TextUtils.TruncateAt.END).toString()

    /** Largest size in [min, start] (2px steps) at which [s] fits [maxW]; ellipsized at [min] if it still doesn't. */
    private fun fit(s: String, start: Float, min: Float, bold: Boolean, maxW: Float): Pair<Float, String> {
        var size = start
        while (size > min && paint(size, bold, INK).measureText(s) > maxW) size -= 2f
        return size to ellipsize(s, paint(size, bold, INK), maxW)
    }

    /** Section title with a small accent bar; returns the y where content starts. */
    private fun title(c: Canvas, text: String, y: Float, x: Float = M): Float {
        c.drawRoundRect(RectF(x, y + 34f, x + 8f, y + 66f), 4f, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT })
        c.drawText(text, x + 22f, y + 62f, paint(32f, true, INK))
        return y + 86f
    }

    private fun swatch(c: Canvas, r: RectF, hex: String, radius: Float) {
        c.drawRoundRect(r, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(hex) })
        // Hairline so whites/ivories stay visible on the light background.
        c.drawRoundRect(r, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = LINE })
    }

    /** 6-column grid of swatches with auto-shrinking names below; returns the bottom y. */
    private fun grid(c: Canvas, colors: List<NamedColor>, top: Float, swatchH: Float): Float {
        val cols = 6; val gap = 16f
        val cell = (CW - gap * (cols - 1)) / cols
        val rowH = swatchH + 44f
        colors.forEachIndexed { i, col ->
            val x = M + (i % cols) * (cell + gap)
            val y = top + (i / cols) * (rowH + 10f)
            swatch(c, RectF(x, y, x + cell, y + swatchH), col.hex, 22f)
            val (size, text) = fit(col.name, 24f, 16f, false, cell)
            c.drawText(text, x + cell / 2, y + swatchH + 32f, paint(size, false, MUTED).apply { textAlign = Paint.Align.CENTER })
        }
        val rows = (colors.size + cols - 1) / cols
        return top + rows * rowH + (rows - 1).coerceAtLeast(0) * 10f
    }

    /** One row of 52px circles starting at (x, top), as many as fit [maxW]. */
    private fun dots(c: Canvas, colors: List<NamedColor>, x: Float, top: Float, maxW: Float) {
        val d = 52f; val gap = 12f
        val n = minOf(colors.size, ((maxW + gap) / (d + gap)).toInt())
        colors.take(n).forEachIndexed { i, col ->
            val l = x + i * (d + gap)
            swatch(c, RectF(l, top, l + d, top + d), col.hex, d / 2)
        }
    }

    private fun pillWidth(text: String, size: Float) = paint(size, true, INK).measureText(text) + size * 1.8f

    private fun pill(c: Canvas, text: String, x: Float, y: Float, size: Float, bg: Int, fg: Int) {
        val h = size * 2f
        val w = pillWidth(text, size)
        c.drawRoundRect(RectF(x, y, x + w, y + h), h / 2, h / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg })
        c.drawText(text, x + size * 0.9f, y + h / 2 + size * 0.36f, paint(size, true, fg))
    }

    /** Wrapped text block; returns its height. */
    private fun layout(c: Canvas, text: String, x: Float, y: Float, w: Float, size: Float, color: Int, maxLines: Int): Float {
        val l = StaticLayout.Builder.obtain(text, 0, text.length, paint(size, false, color), w.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).build()
        c.save(); c.translate(x, y); l.draw(c); c.restore()
        return l.height.toFloat()
    }
}
