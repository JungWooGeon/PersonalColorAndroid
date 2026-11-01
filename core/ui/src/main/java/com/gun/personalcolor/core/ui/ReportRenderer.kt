package com.gun.personalcolor.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.drawTintyOn
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone

/** Brand images (light theme): the 1080×1920 report and the 1080×1350 share card. Never contain the user's photo. */
object ReportRenderer {
    private const val W = 1080
    private const val H = 1920
    private const val M = 72f
    private const val CW = W - 2 * M
    private const val BG = 0xFFFAF8F6.toInt()
    private const val INK = 0xFF1D1B1A.toInt()
    private const val MUTED = 0xFF66605B.toInt()
    private const val FAINT = 0xFF9A918B.toInt()
    private const val ACCENT = 0xFF5B4B8A.toInt()
    private const val ACCENT_BG = 0xFFEAE4F8.toInt()
    private const val LINE = 0x1F1D1B1A

    @Suppress("UNUSED_PARAMETER") // kept for future localized strings/resources
    fun render(context: Context, tone: Tone): Bitmap {
        useBrandFont(context)
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(BG)

        // Hero: pastel wash of the tone with its 틴티 on the right.
        val tc = tone.tintyColors
        val header = RectF(M, 56f, W - M, 424f)
        c.drawRoundRect(header, 64f, 64f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(header.left, header.top, header.right, header.bottom,
                lift(tc.bodyTop, 0.72f), lift(tc.bodyBottom, 0.74f), Shader.TileMode.CLAMP)
        })
        val tintySize = 300f
        drawTintyOn(c, header.right - tintySize - 12f, header.top + 40f, tintySize, tc, TintyExpression.Cheer)
        val textW = header.width() - tintySize - 72f
        var y = header.top + 92f
        c.drawText("나의 퍼스널컬러 리포트", header.left + 48f, y, paint(30f, true, ACCENT))
        y += 112f
        val name = fit(tone.nameKo, 92f, 48f, true, textW)
        c.drawText(name.second, header.left + 48f, y, paint(name.first, true, INK))
        y += 62f
        val sub = "${tone.seasonKo} · ${tone.temperatureKo}"
        c.drawText(ellipsize(sub, paint(32f, false, MUTED), textW), header.left + 48f, y, paint(32f, false, MUTED))
        y += 48f
        c.drawText(ellipsize(tone.nameEn, paint(28f, false, MUTED), textW), header.left + 48f, y, paint(28f, false, MUTED))

        y = header.bottom + 32f
        var x = M
        for (k in tone.keywords) {
            val w = pillWidth("#$k", 26f)
            if (x + w > W - M) break
            pill(c, "#$k", x, y, 26f, ACCENT_BG, ACCENT)
            x += w + 12f
        }
        y += 52f

        y = title(c, "BEST 컬러", y)
        y = grid(c, tone.bestColors.take(12), y, swatchH = 104f) + 10f
        y = title(c, "WORST 컬러", y)
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

        y = title(c, "코디", y)
        val bullet = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT }
        for (s in tone.fashion.styling.take(3)) {
            c.drawCircle(M + 8f, y + 24f, 6f, bullet)
            c.drawText(ellipsize(s, paint(30f, false, INK), CW - 32f), M + 32f, y + 34f, paint(30f, false, INK))
            y += 48f
        }

        val footer = paint(26f, true, FAINT).apply { textAlign = Paint.Align.CENTER }
        c.drawText("나의 컬러는?", W / 2f, H - 44f, footer)
        return bmp
    }

    /**
     * 1080×1350 share card, the same picture as the result reveal's last frame: the tone's wash, 틴티 cheering in the
     * tone's colors with a burst of best-color confetti, the name, and 12 best colors.
     */
    fun renderCard(context: Context, tone: Tone): Bitmap {
        useBrandFont(context)
        val w = 1080; val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val tc = tone.tintyColors
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), lift(tc.bodyTop, 0.74f), lift(tc.bodyBottom, 0.78f), Shader.TileMode.CLAMP)
        })
        // A soft white halo behind 틴티 so it pops off the wash.
        c.drawCircle(w / 2f, 440f, 330f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(w / 2f, 440f, 330f, 0x99FFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP)
        })
        // Confetti: deterministic per tone, in its best colors, fanning up and out from 틴티.
        val rnd = java.util.Random(tone.id.hashCode().toLong())
        val colors = tone.bestColors.map { hexColor(it.hex).toArgb() }.ifEmpty { listOf(ACCENT) }
        val piece = Paint(Paint.ANTI_ALIAS_FLAG)
        repeat(30) { i ->
            val a = Math.toRadians(-170.0 + 160.0 * rnd.nextDouble())
            val d = 250f + 230f * rnd.nextFloat()
            val x = w / 2f + (kotlin.math.cos(a) * d).toFloat(); val y = 470f + (kotlin.math.sin(a) * d * 0.8).toFloat()
            val s = 14f + 14f * rnd.nextFloat()
            piece.color = colors[i % colors.size]
            c.save(); c.rotate(rnd.nextFloat() * 360f, x, y)
            if (rnd.nextBoolean()) c.drawCircle(x, y, s * 0.45f, piece) else c.drawRoundRect(RectF(x - s / 2, y - s * 0.3f, x + s / 2, y + s * 0.3f), 4f, 4f, piece)
            c.restore()
        }
        val center = paint(40f, true, ACCENT).apply { textAlign = Paint.Align.CENTER }
        c.drawText("나의 퍼스널컬러는", w / 2f, 130f, center)
        drawTintyOn(c, (w - 500f) / 2f, 180f, 500f, tc, TintyExpression.Cheer)
        val name = fit(tone.nameKo, 116f, 56f, true, w - 2 * M)
        c.drawText(name.second, w / 2f, 800f, paint(name.first, true, INK).apply { textAlign = Paint.Align.CENTER })
        val sub = "${tone.nameEn} · ${tone.seasonKo} ${tone.temperatureKo}"
        c.drawText(ellipsize(sub, paint(36f, false, MUTED), w - 2 * M), w / 2f, 862f, paint(36f, false, MUTED).apply { textAlign = Paint.Align.CENTER })

        // Best colors: white card with 2 rows of 6 chips.
        val card = RectF(M, 902f, w - M, 1252f)
        c.drawRoundRect(card, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        val gap = 16f; val pad = 36f
        val size = (card.width() - 2 * pad - gap * 5) / 6
        tone.bestColors.take(12).forEachIndexed { i, col ->
            val l = card.left + pad + (i % 6) * (size + gap)
            val t = card.top + pad + (i / 6) * (size + gap)
            swatch(c, RectF(l, t, l + size, t + size), col.hex, 24f)
        }
        c.drawText("나의 컬러는? · 틴티", w / 2f, h - 46f, paint(28f, true, MUTED).apply { textAlign = Paint.Align.CENTER })
        return bmp
    }

    /** Pretendard (bundled variable font) for the images too; system font if it can't be loaded. */
    private var face: Typeface? = null
    private fun useBrandFont(context: Context) {
        if (face == null) face = runCatching { context.resources.getFont(com.gun.personalcolor.core.designsystem.R.font.pretendard) }.getOrNull()
    }

    private fun lift(color: androidx.compose.ui.graphics.Color, t: Float): Int {
        fun ch(v: Float) = ((v + (1 - v) * t) * 255).toInt()
        return Color.rgb(ch(color.red), ch(color.green), ch(color.blue))
    }

    private fun paint(size: Float, bold: Boolean, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color
        val f = face
        if (f != null) { typeface = f; fontVariationSettings = if (bold) "'wght' 760" else "'wght' 420" }
        else typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
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
        c.drawRoundRect(r, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = hexColor(hex).toArgb() })
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
