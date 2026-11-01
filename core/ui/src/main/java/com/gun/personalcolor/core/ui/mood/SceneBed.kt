package com.gun.personalcolor.core.ui.mood

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.gun.personalcolor.core.model.BAG_KINDS
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.ui.GarmentArt
import com.gun.personalcolor.core.ui.renderClothingBitmap
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import com.gun.personalcolor.core.designsystem.R as DsR

/**
 * Scene A "햇살 침대" (2x2 and 1x1): today's outfit flat-laid on a linen sheet under drifting window light.
 * Composed from the pre-rendered assets in assets/mood (see A_layout.json) in the same order as compose_ref.py:
 * base → contact shadows → graded garments → shade, warm Screen light → grain → vignette → text.
 * Every asset is optional: a missing one falls back to a plain warm color, so the widget never breaks.
 */
object SceneBed {
    private const val DIR = "mood"
    private val WARM = Color.rgb(0xEF, 0xE4, 0xD3)

    @Volatile private var ko: Typeface? = null
    private val en: Typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    private val cache = HashMap<String, Any?>()

    // ------------------------------------------------------------------------------------------------ assets

    @Synchronized private fun <T> cached(key: String, load: () -> T?): T? =
        if (cache.containsKey(key)) @Suppress("UNCHECKED_CAST") (cache[key] as T?) else load().also { cache[key] = it }

    private fun layout(ctx: Context): JSONObject? = cached("A_layout") {
        runCatching { JSONObject(ctx.assets.open("$DIR/A_layout.json").bufferedReader().use { it.readText() }) }
            .onFailure { Log.w("SceneBed", "A_layout.json missing") }.getOrNull()
    }

    private fun bmp(ctx: Context, name: String, opts: BitmapFactory.Options? = null): Bitmap? =
        runCatching { ctx.assets.open("$DIR/$name").use { BitmapFactory.decodeStream(it, null, opts) } }.getOrNull()

    private fun base(ctx: Context, w: String) = cached("base_$w") { bmp(ctx, "A_${w}_base.webp") }

    /** The light map split into a black shade layer (its alpha) and an opaque warm light layer (its RGB). */
    private fun light(ctx: Context, w: String, phase: Int): Pair<Bitmap, Bitmap>? = cached("light_${w}_$phase") {
        val raw = bmp(ctx, "A_${w}_light$phase.webp", BitmapFactory.Options().apply { inPremultiplied = false })
            ?: return@cached null
        val n = raw.width * raw.height
        val px = IntArray(n).also { raw.getPixels(it, 0, raw.width, 0, 0, raw.width, raw.height) }
        val shade = IntArray(n) { px[it] and 0xFF000000.toInt() }
        val lit = IntArray(n) { px[it] or 0xFF000000.toInt() }
        Bitmap.createBitmap(shade, raw.width, raw.height, Bitmap.Config.ARGB_8888) to
            Bitmap.createBitmap(lit, raw.width, raw.height, Bitmap.Config.ARGB_8888)
    }

    private fun grain(ctx: Context) = cached("grain") { bmp(ctx, "grain.webp") }

    // ------------------------------------------------------------------------------------------------ cast

    private fun roleOf(kind: String): String? = when (CLOTHING_KINDS[kind]?.second) {
        "아우터" -> "outer"
        "상의" -> "top"
        "하의" -> "bottom"
        "원피스" -> "dress"
        "신발" -> "shoes"
        else -> when (kind) {
            in BAG_KINDS -> "bag"
            in setOf("umbrella", "muffler", "scarf", "hat", "beanie", "bucket_hat", "beret", "sunglasses") -> "acc"
            else -> null
        }
    }

    /** Locked: generic pieces in neutral colors, never today's. */
    private val TEASER = listOf("cardigan" to 0xFFEDE3D3.toInt(), "long_skirt" to 0xFFD9CFC2.toInt(),
        "mary_jane" to 0xFFE6DCCD.toInt(), "shoulder_bag" to 0xFFD9C6AE.toInt())

    /** One piece per role and the layout key it uses. */
    private fun cast(pieces: List<Pair<String, Int>>, mini: Boolean): Pair<String, Map<String, Pair<String, Int>>> {
        val by = LinkedHashMap<String, Pair<String, Int>>()
        for (p in pieces) roleOf(p.first)?.let { if (it !in by) by[it] = p }
        if ("dress" in by) { by.remove("top"); by.remove("bottom") }
        // An outer with nothing under it lies where the top would.
        if ("outer" in by && "top" !in by && "dress" !in by) by["top"] = by.remove("outer")!!
        if (mini) {
            // 1x1: the silhouette only; the outer stands in when there's no top.
            val top = by["top"] ?: by["outer"]
            by.keys.retainAll(setOf("bottom", "dress", "shoes"))
            if (top != null && "dress" !in by) by["top"] = top
            return (if ("dress" in by) "dress" else "top_bottom") to by
        }
        val key = when {
            "dress" in by && "outer" in by -> "outer_dress"
            "dress" in by -> "dress"
            "outer" in by -> "outer_top_bottom"
            else -> "top_bottom"
        }
        return key to by
    }

    // ------------------------------------------------------------------------------------------------ render

    /** Scene A at [wPx]×[hPx] for light [phase] (0..2); [mini] = the 1x1 crop. */
    /**
     * [pieces] (kind, ARGB) in wearing order; [weather] sunny|cloudy|rain|night; [phase] the light phase 0..2.
     * [locked]: generic pieces under a frosted veil (the real outfit is never drawn). [mini]: the 1x1 crop, caption only.
     * [pill]: the quiet call to action under the line (bottom center on the 1x1).
     */
    fun render(
        ctx: Context, pieces: List<Pair<String, Int>>, weather: String, line: String, caption: String,
        wPx: Int, hPx: Int, phase: Int, locked: Boolean, mini: Boolean, pill: String? = if (locked) "광고 보고 열기" else null,
        rounded: Boolean = true,
        hero: Boolean = false,
    ): Bitmap {
        val d = ctx.resources.displayMetrics.density
        if (ko == null) ko = runCatching { ResourcesCompat.getFont(ctx, DsR.font.nanum_myeongjo) }.getOrNull() ?: Typeface.SERIF
        val L = layout(ctx)
        val w = weather
        val size = L?.optJSONArray("size")
        val bw = size?.optDouble(0, 760.0)?.toFloat() ?: 760f
        val bh = size?.optDouble(1, 1088.0)?.toFloat() ?: 1088f
        // A cropped view ("mini" 1x1, "hero" 4:5 in-app/share) has its own crop, layouts and text, relative to the crop.
        val miniL = (if (mini) L?.optJSONObject("mini") else if (hero) L?.optJSONObject("hero") else null)
        val cropped = miniL != null
        val region = miniL?.optJSONArray("crop")?.let(::rect) ?: RectF(0f, 0f, bw, bh)

        val out = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(WARM)
        // Base px → widget px, center-cropped.
        val s = max(wPx / region.width(), hPx / region.height())
        val m = Matrix().apply {
            setTranslate(-region.centerX(), -region.centerY()); postScale(s, s); postTranslate(wPx / 2f, hPx / 2f)
        }
        c.save(); c.concat(m)
        val filter = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        base(ctx, w)?.let { c.drawBitmap(it, null, RectF(0f, 0f, bw, bh), filter) }

        // Garments (+ their contact shadows).
        val drawPieces = if (locked) TEASER else pieces
        if (drawPieces.isNotEmpty() && L != null) {
            val (key, by) = cast(drawPieces, mini)
            val slots = (if (cropped) miniL?.optJSONObject("layouts") else L.optJSONObject("layouts"))?.optJSONObject(key)
            if (slots != null) {
                // Locked: the teaser pieces go on their own layer, which is frosted before it joins the scene.
                val layer = if (locked) Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888) else null
                val gc = if (layer != null) Canvas(layer).apply { concat(m) } else c
                if (cropped) { gc.save(); gc.translate(region.left, region.top) }
                val drawn = by.mapNotNull { (role, p) -> slots.optJSONObject(role)?.let { Triple(role, p, it) } }
                    .sortedBy { it.third.optInt("z") }
                val shadow = L.optJSONObject("contactShadow")
                val grade = L.optJSONObject("garmentGrade")?.let { it.optJSONObject(w) ?: it.optJSONObject("sunny") }
                for ((role, p, slot) in drawn) {
                    val art = garment(p.first, p.second) ?: continue
                    // The rim only on shoes and bags: on a coat it reads as a sticker outline.
                    placeGarment(gc, art, role, slot, shadow, grade, role in setOf("shoes", "bag") && lum(p.second) < .12f)
                }
                if (cropped) gc.restore()
                if (layer != null) frost(c, layer)
            }
        }

        // Light: shade (alpha as black), then warm light (RGB, Screen).
        light(ctx, w, phase)?.let { (shade, lit) ->
            val dst = RectF(0f, 0f, bw, bh)
            c.drawBitmap(shade, null, dst, Paint(filter).apply { alpha = (255 * by(L?.optJSONObject("shadeMax"), w, .4f)).toInt() })
            c.drawBitmap(lit, null, dst, Paint(filter).apply {
                val k = by(L?.optJSONObject("lightMax"), w, .3f)
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(k, k, k, 1f) })
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            })
        }
        // Grain, tiled in base pixels like the reference.
        grain(ctx)?.let { g ->
            c.drawRect(0f, 0f, bw, bh, Paint().apply {
                shader = BitmapShader(g, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                alpha = (255 * (L?.optDouble("grainAlpha", .1) ?: .1)).toInt()
                xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            })
        }
        c.restore()

        // Vignette over the visible frame.
        val v = (L?.optDouble("vignette", .22) ?: .22).toFloat()
        c.drawRect(0f, 0f, wPx.toFloat(), hPx.toFloat(), Paint().apply {
            shader = RadialGradient(wPx / 2f, hPx / 2f, kotlin.math.hypot(wPx / 2f, hPx / 2f),
                intArrayOf(0, 0, Color.argb((255 * v).toInt(), 0, 0, 0)), floatArrayOf(0f, .45f, 1f), Shader.TileMode.CLAMP)
        })

        // Text, in base px through the same matrix.
        c.save(); c.concat(m)
        if (cropped) c.translate(region.left, region.top)
        val text = (if (cropped) miniL?.optJSONObject("text") else L?.optJSONObject("text"))
        var pillAt: RectF? = null
        if (!mini) text?.optJSONObject("line")?.let { t -> pillAt = drawLine(c, line, t, w) }
        text?.optJSONObject("caption")?.let { t -> drawCaption(c, caption, t, w) }
        pill?.let { label ->
            val ink = colorOf(text?.optJSONObject("line")?.opt("color") ?: text?.optJSONObject("caption")?.opt("color"), w, Color.rgb(0x4A, 0x3A, 0x2E))
            drawPill(c, label, pillAt, if (cropped) RectF(0f, 0f, region.width(), region.height()) else region, if (mini) 30f else 38f, ink, w == "night")
        }
        c.restore()
        return if (rounded) round(out, 18 * d) else out
    }

    private fun garment(kind: String, color: Int): Bitmap? {
        val cc = androidx.compose.ui.graphics.Color(color)
        val raw = GarmentArt.bitmap(kind, cc) ?: renderClothingBitmap(kind, cc, 420, 4.2f)
        return crop(raw)
    }

    private fun placeGarment(c: Canvas, art: Bitmap, role: String, slot: JSONObject, shadow: JSONObject?, grade: JSONObject?, dark: Boolean) {
        val r = slot.optJSONArray("rect")?.let(::rect) ?: return
        val k = minOf(r.width() / art.width, r.height() / art.height)
        val gw = art.width * k; val gh = art.height * k
        val left = r.centerX() - gw / 2
        val top = if (role == "shoes") r.bottom - gh else r.centerY() - gh / 2
        val dst = RectF(left, top, left + gw, top + gh)
        c.save()
        c.rotate(slot.optDouble("rot", 0.0).toFloat(), r.centerX(), r.centerY())
        if (shadow != null) {
            val blur = shadow.optDouble("blur", 18.0).toFloat()
            val sp = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                color = Color.BLACK
                alpha = (255 * shadow.optDouble("alpha", .35)).toInt()
                if (blur > 0) maskFilter = BlurMaskFilter(blur / 2 / .57f, BlurMaskFilter.Blur.NORMAL)
            }
            val dx = shadow.optDouble("dx", 6.0).toFloat(); val dy = shadow.optDouble("dy", 10.0).toFloat()
            c.drawBitmap(art.extractAlpha(), null, RectF(dst).apply { offset(dx, dy) }, sp)
        }
        c.drawBitmap(graded(art, grade, dark), null, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        c.restore()
    }

    /**
     * The garment graded into the scene (saturation, then a multiply tint). Dark ones get compose_ref's highlight rim:
     * their alpha minus itself shifted (4, 4) px, a top-left edge catching the window light, so black never becomes a blob.
     */
    private fun graded(art: Bitmap, grade: JSONObject?, dark: Boolean): Bitmap {
        val out = Bitmap.createBitmap(art.width, art.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        if (grade != null) {
            val cm = ColorMatrix().apply { setSaturation(grade.optDouble("saturation", 1.0).toFloat()) }
            val mul = colorOf(grade.opt("multiply"), "", Color.WHITE)
            cm.postConcat(ColorMatrix().apply { setScale(Color.red(mul) / 255f, Color.green(mul) / 255f, Color.blue(mul) / 255f, 1f) })
            p.colorFilter = ColorMatrixColorFilter(cm)
        }
        c.drawBitmap(art, 0f, 0f, p)
        if (dark) {
            val a = art.extractAlpha()
            val rim = Bitmap.createBitmap(art.width, art.height, Bitmap.Config.ARGB_8888)
            Canvas(rim).apply {
                drawBitmap(a, 0f, 0f, Paint().apply { color = Color.rgb(176, 168, 158) })
                drawBitmap(a, 4f, 4f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) })
            }
            c.drawBitmap(rim, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (255 * .75f).toInt(); maskFilter = BlurMaskFilter(1.5f, BlurMaskFilter.Blur.NORMAL)
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            })
        }
        return out
    }

    /** Locked: the teaser pieces' layer, blurred and faded into the sheet (a frosted veil over them only). */
    private fun frost(c: Canvas, layer: Bitmap) {
        // A downscale is a soft blur without RenderScript: the shapes stay, the details (colors, prints) don't.
        fun scaled(b: Bitmap, k: Float) = Bitmap.createScaledBitmap(b, max(1, (b.width * k).toInt()), max(1, (b.height * k).toInt()), true)
        // Down in two steps, then half way back up, so the bilinear upscale doesn't show blocks.
        val small = scaled(scaled(scaled(layer, 1 / 3f), 1 / 3f), 3f)
        c.save(); c.setMatrix(Matrix())
        c.drawBitmap(small, null, RectF(0f, 0f, layer.width.toFloat(), layer.height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 210 })
        c.restore()
    }

    /** The Korean line; wraps after the comma when it doesn't fit. Returns the box it took (for the pill). */
    private fun drawLine(c: Canvas, s: String, t: JSONObject, w: String): RectF {
        val r = t.optJSONArray("rect")?.let(::rect) ?: RectF(60f, 80f, 700f, 240f)
        val p = paint(ko, t.optDouble("size", 46.0).toFloat(), colorOf(t.opt("color"), w, Color.rgb(0x4A, 0x3A, 0x2E)), t)
        val lines = wrap(s, p, r.width())
        val lh = p.textSize * t.optDouble("lineSpacing", 1.25).toFloat()
        var y = r.top - p.ascent()
        for (l in lines) { c.drawText(l, x(r, p, t), y, p); y += lh }
        return RectF(r.left, r.top, r.right, y - lh + p.descent())
    }

    private fun drawCaption(c: Canvas, s: String, t: JSONObject, w: String) {
        val r = t.optJSONArray("rect")?.let(::rect) ?: return
        val p = paint(en, t.optDouble("size", 24.0).toFloat(), colorOf(t.opt("color"), w, Color.rgb(0x6B, 0x5A, 0x4A)), t)
        while (p.measureText(s) > r.width() && p.textSize > 8) p.textSize *= .95f
        c.drawText(s, x(r, p, t), r.top - p.ascent(), p)
    }

    private fun paint(tf: Typeface?, size: Float, color: Int, t: JSONObject) = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = tf; textSize = size; this.color = color
        textAlign = when (t.optString("align")) { "right" -> Paint.Align.RIGHT; "center" -> Paint.Align.CENTER; else -> Paint.Align.LEFT }
    }

    private fun x(r: RectF, p: Paint, t: JSONObject) = when (p.textAlign) { Paint.Align.RIGHT -> r.right; Paint.Align.CENTER -> r.centerX(); else -> r.left }

    internal fun wrap(s: String, p: Paint, maxW: Float): List<String> {
        val i = s.indexOf(", ")
        if (i > 0) return listOf(s.substring(0, i + 1), s.substring(i + 2))
        if (s.length <= 8 && p.measureText(s) <= maxW) return listOf(s)
        val cut = s.indices.filter { s[it] == ' ' }.minByOrNull { kotlin.math.abs(it - s.length / 2f) } ?: return listOf(s)
        return listOf(s.substring(0, cut), s.substring(cut + 1))
    }

    /** The quiet pill: milk glass, myeongjo label, under the line (or bottom center on the 1x1). */
    private fun drawPill(c: Canvas, label: String, under: RectF?, region: RectF, size: Float, ink: Int, night: Boolean) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = ko; textSize = size; color = ink; textAlign = Paint.Align.CENTER }
        val tw = p.measureText(label)
        val ph = size * 1.9f; val pw = tw + size * 1.8f
        val box = if (under != null) RectF(under.left, under.bottom + size * .9f, under.left + pw, under.bottom + size * .9f + ph)
        else RectF(region.centerX() - pw / 2, region.bottom - ph * 2.4f, region.centerX() + pw / 2, region.bottom - ph * 1.4f)
        c.drawRoundRect(box, ph / 2, ph / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (night) Color.argb(70, 255, 248, 238) else Color.argb(190, 252, 247, 240)
        })
        c.drawRoundRect(box, ph / 2, ph / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = size * .06f; color = ink; alpha = 70
        })
        c.drawText(label, box.centerX(), box.centerY() - (p.ascent() + p.descent()) / 2, p)
    }

    private fun round(b: Bitmap, r: Float): Bitmap {
        val out = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawRoundRect(RectF(0f, 0f, b.width.toFloat(), b.height.toFloat()), r, r, p)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        c.drawBitmap(b, 0f, 0f, p)
        b.recycle()
        return out
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private fun rect(a: JSONArray) = RectF(a.optDouble(0).toFloat(), a.optDouble(1).toFloat(),
        (a.optDouble(0) + a.optDouble(2)).toFloat(), (a.optDouble(1) + a.optDouble(3)).toFloat())

    private fun by(o: JSONObject?, w: String, def: Float) = (o?.optDouble(w, Double.NaN)?.takeIf { !it.isNaN() } ?: def.toDouble()).toFloat()

    /** "#RRGGBB" or {"sunny": "#..", ...}. */
    private fun colorOf(v: Any?, w: String, def: Int): Int = runCatching {
        when (v) {
            is String -> Color.parseColor(v)
            is JSONObject -> Color.parseColor(v.optString(w).ifEmpty { v.optString("sunny") })
            else -> def
        }
    }.getOrDefault(def)

    private fun crop(b: Bitmap): Bitmap? {
        val w = b.width; val h = b.height
        val px = IntArray(w * h).also { b.getPixels(it, 0, w, 0, 0, w, h) }
        var l = w; var t = h; var r = -1; var bt = -1
        for (y in 0 until h) for (x in 0 until w) if (px[y * w + x] ushr 24 > 8) {
            if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > bt) bt = y
        }
        if (r < 0) return null
        return Bitmap.createBitmap(b, l, t, r - l + 1, bt - t + 1)
    }

    private fun lum(c: Int) = (.2126f * Color.red(c) + .7152f * Color.green(c) + .0722f * Color.blue(c)) / 255f
}
