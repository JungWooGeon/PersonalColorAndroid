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
import androidx.core.content.res.ResourcesCompat
import com.gun.personalcolor.core.model.BAG_KINDS
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.ui.GarmentArt
import com.gun.personalcolor.core.ui.renderClothingBitmap
import org.json.JSONObject
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import com.gun.personalcolor.core.designsystem.R as DsR

/**
 * 4x2 "옷걸이 벽" mood scene: pre-rendered wall (assets/mood/B_*) + today's pieces on hangers, composed on device.
 * Mirrors compose_B() in assets/mood/compose_ref_b.py, same order: base -> contact shadows -> garments (graded,
 * dark rim) -> hangers -> shade -> warm light (screen) -> grain (overlay) -> vignette -> text.
 * Everything except grain/vignette is drawn in base pixels (1520x1056) through one center-crop matrix.
 */
object SceneWall {
    private const val DIR = "mood"

    // Neutral stand-ins for the locked state: never the real outfit.
    private val GENERIC = listOf("knit" to 0xFFD8CFC4.toInt(), "wide" to 0xFFB9AFA4.toInt(), "loafers" to 0xFF8C8279.toInt(), "shoulder_bag" to 0xFFCDBFAF.toInt())

    private var layout: JSONObject? = null
    private var base: Triple<String, Int, Bitmap>? = null // weather, w*65536+h, base scaled to the output
    private val lights = HashMap<String, Pair<Bitmap, Bitmap>>() // "weather#phase" -> (shade, warm light), 380x264 each
    private var grain: Bitmap? = null
    private val hangers = HashMap<String, Bitmap>()
    private var ko: Typeface? = null

    fun render(
        context: Context,
        pieces: List<Pair<String, Int>>, // kind, ARGB, wearing order
        weather: String, // sunny|cloudy|rain|night
        line: String,
        caption: String,
        wPx: Int,
        hPx: Int,
        phase: Int, // 0..2 light phase
        locked: Boolean,
        bgAlpha: Float = 1f, // < 1: the scene shows the wallpaper through; garments, hangers and text stay solid
    ): Bitmap = synchronized(this) {
        val ctx = context.applicationContext
        val wx = weather.takeIf { it in setOf("sunny", "cloudy", "rain", "night") } ?: "cloudy"
        val ph = phase.coerceIn(0, 2)
        val L = layout ?: JSONObject(ctx.assets.open("$DIR/B_layout.json").bufferedReader().use { it.readText() }).also { layout = it }
        val (bw, bh) = L.getJSONArray("size").let { it.getInt(0) to it.getInt(1) }
        val out = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val s = max(wPx / bw.toFloat(), hPx / bh.toFloat())
        val m = Matrix().apply { setScale(s, s); postTranslate((wPx - bw * s) / 2, (hPx - bh * s) / 2) }

        // 1. base (decoded once per weather + size, kept at output size)
        c.drawBitmap(baseFor(ctx, wx, wPx, hPx, m, bw, bh), 0f, 0f, null)
        c.save(); c.concat(m)

        // 2-4. garments with contact shadows, then hangers
        val grade = L.getJSONObject("garmentGrade").getJSONObject(wx)
        val gradeFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setSaturation(grade.getDouble("saturation").toFloat())
            val mc = Color.parseColor(grade.getString("multiply"))
            postConcat(ColorMatrix().apply { setScale(Color.red(mc) / 255f, Color.green(mc) / 255f, Color.blue(mc) / 255f, 1f) })
        })
        val look = roles(if (locked) GENERIC else pieces)
        val lay = L.getJSONObject("layouts").getJSONObject(layoutFor(look.keys))
        val cs = L.getJSONObject("contactShadow")
        val rim = L.getJSONObject("darkRim")
        val hung = ArrayList<JSONObject>()
        // Coverage mask: background alpha everywhere, full under garments (and their soft contact shadows).
        val mask = if (bgAlpha < 1f) Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888) else null
        val mc = mask?.let { Canvas(it).apply { drawColor(Color.argb((255 * bgAlpha).toInt(), 0, 0, 0)); concat(m) } }
        lay.keys().asSequence().map { it to lay.getJSONObject(it) }.sortedBy { it.second.getInt("z") }.forEach { (role, slot) ->
            val (kind, argb) = look[role] ?: return@forEach
            val art = garment(kind, argb, gradeFilter, rim) ?: return@forEach
            place(c, art, slot, cs)
            mc?.let { place(it, art, slot, cs) }
            art.recycle()
            if (slot.optString("hanger") in setOf("hanger", "clip")) hung += slot
        }
        val hp = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = gradeFilter }
        val hdefs = L.getJSONObject("hangers")
        for (slot in hung) {
            val def = hdefs.getJSONObject(slot.getString("hanger"))
            val img = hangers.getOrPut(def.getString("file")) { ctx.assets.open("$DIR/${def.getString("file")}").use(BitmapFactory::decodeStream) }
            val hook = slot.getJSONArray("hook"); val anc = def.getJSONArray("anchor")
            c.drawBitmap(img, (hook.getInt(0) - anc.getInt(0)).toFloat(), (hook.getInt(1) - anc.getInt(1)).toFloat(), hp)
            mc?.drawBitmap(img, (hook.getInt(0) - anc.getInt(0)).toFloat(), (hook.getInt(1) - anc.getInt(1)).toFloat(), hp)
        }

        // 5. light: shade (alpha as black), then warm light (screen)
        val (shade, warm) = lightFor(ctx, L, wx, ph)
        val full = RectF(0f, 0f, bw.toFloat(), bh.toFloat())
        val lp = Paint(Paint.FILTER_BITMAP_FLAG)
        c.drawBitmap(shade, null, full, lp)
        c.drawBitmap(warm, null, full, lp.apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN) })
        c.restore()

        // 6. grain (overlay, device px) 7. vignette
        val g = grain ?: ctx.assets.open("$DIR/grain.webp").use(BitmapFactory::decodeStream).also { grain = it }
        c.drawRect(0f, 0f, wPx.toFloat(), hPx.toFloat(), Paint().apply {
            shader = BitmapShader(g, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            alpha = (255 * L.getDouble("grainAlpha")).toInt()
        })
        val vig = (255 * L.getDouble("vignette")).toInt().coerceIn(0, 255)
        c.drawRect(0f, 0f, wPx.toFloat(), hPx.toFloat(), Paint().apply {
            shader = RadialGradient(wPx / 2f, hPx / 2f, hypot(wPx / 2f, hPx / 2f), intArrayOf(0, 0, Color.argb(vig, 0, 0, 0)), floatArrayOf(0f, .45f, 1f), Shader.TileMode.CLAMP)
        })

        mask?.let { c.drawBitmap(it, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }); it.recycle() }

        // Locked: soft frosted veil over the generic pieces (the text column stays readable).
        if (locked) frost(out)

        // 8. text
        c.save(); c.concat(m)
        text(ctx, c, L.getJSONObject("text"), wx, line, caption, locked)
        c.restore()
        out
    }

    /** kind list -> role -> (kind, argb); first piece per role wins. Only an umbrella takes the acc spot by the window. */
    private fun roles(pieces: List<Pair<String, Int>>): Map<String, Pair<String, Int>> {
        val r = LinkedHashMap<String, Pair<String, Int>>()
        for (p in pieces) {
            val role = when {
                p.first in BAG_KINDS -> "bag"
                p.first == "umbrella" -> "acc"
                else -> when (CLOTHING_KINDS[p.first]?.second) {
                    "아우터" -> "outer"; "상의" -> "top"; "하의" -> "bottom"; "원피스" -> "dress"; "신발" -> "shoes"; else -> null
                }
            } ?: continue
            r.putIfAbsent(role, p)
        }
        // An outer alone (no top, no dress) hangs on the top's hanger so the rail isn't half empty.
        if ("outer" in r && "top" !in r && "dress" !in r) r["top"] = r.remove("outer")!!
        return r
    }

    private fun layoutFor(roles: Set<String>) = ("outer_".takeIf { "outer" in roles } ?: "") + if ("dress" in roles) "dress" else "top_bottom"

    /** Garment cropped to its bounds, graded, with the highlight rim for dark colors (compose_ref_b.rim). A fresh bitmap: recycle after use. */
    private fun garment(kind: String, argb: Int, grade: ColorMatrixColorFilter, rim: JSONObject): Bitmap? {
        val cc = androidx.compose.ui.graphics.Color(argb)
        val raw = GarmentArt.bitmap(kind, cc) ?: runCatching { renderClothingBitmap(kind, cc, 420, 4.2f) }.getOrNull() ?: return null
        val w = raw.width; val h = raw.height
        val px = IntArray(w * h).also { raw.getPixels(it, 0, w, 0, 0, w, h) }
        var x0 = w; var y0 = h; var x1 = -1; var y1 = -1
        for (y in 0 until h) for (x in 0 until w) if (px[y * w + x] ushr 24 != 0) {
            if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y
        }
        if (x1 < 0) return null
        val cw = x1 - x0 + 1; val ch = y1 - y0 + 1
        val out = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
        val oc = Canvas(out)
        oc.drawBitmap(raw, -x0.toFloat(), -y0.toFloat(), Paint().apply { colorFilter = grade })
        val lum = .2126f * Color.red(argb) / 255f + .7152f * Color.green(argb) / 255f + .0722f * Color.blue(argb) / 255f
        if (lum < rim.getDouble("lumBelow")) {
            // top edge = alpha minus alpha shifted by (dx, dy), blurred, drawn over the garment inside its silhouette
            val dx = rim.getInt("dx"); val dy = rim.getInt("dy")
            val a = IntArray(cw * ch); out.getPixels(a, 0, cw, 0, 0, cw, ch)
            val edge = IntArray(cw * ch)
            for (y in 0 until ch) for (x in 0 until cw) {
                val av = a[y * cw + x] ushr 24
                val sx = x - dx; val sy = y - dy
                val sv = if (sx in 0 until cw && sy in 0 until ch) a[sy * cw + sx] ushr 24 else 0
                edge[y * cw + x] = (av - sv).coerceAtLeast(0) shl 24
            }
            val mask = Bitmap.createBitmap(edge, cw, ch, Bitmap.Config.ARGB_8888).let { b -> b.extractAlpha().also { b.recycle() } }
            val blur = rim.getDouble("blur").toFloat()
            oc.drawBitmap(mask, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(rim.getString("color"))
                alpha = (255 * rim.getDouble("alpha")).toInt()
                if (blur > .5f) maskFilter = BlurMaskFilter((blur - .5f) / .57735f + .5f, BlurMaskFilter.Blur.NORMAL)
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            })
            mask.recycle()
        }
        return out
    }

    /** Aspect-fit in slot.rect (align top|bottom|center), rotated clockwise about the rect center, with its contact shadow. */
    private fun place(c: Canvas, art: Bitmap, slot: JSONObject, cs: JSONObject) {
        val r = slot.getJSONArray("rect").let { RectF(it.getInt(0).toFloat(), it.getInt(1).toFloat(), (it.getInt(0) + it.getInt(2)).toFloat(), (it.getInt(1) + it.getInt(3)).toFloat()) }
        val k = min(r.width() / art.width, r.height() / art.height)
        val gw = art.width * k; val gh = art.height * k
        val top = when (slot.optString("align")) { "top" -> r.top; "bottom" -> r.bottom - gh; else -> r.centerY() - gh / 2 }
        val dst = RectF(r.centerX() - gw / 2, top, r.centerX() + gw / 2, top + gh)
        val rot = slot.optDouble("rot", 0.0).toFloat()
        val mask = art.extractAlpha()
        c.save()
        c.translate(cs.getDouble("dx").toFloat(), cs.getDouble("dy").toFloat())
        c.rotate(rot, r.centerX(), r.centerY())
        val sigma = cs.getDouble("blur").toFloat() / 2
        c.drawBitmap(mask, null, dst, Paint(Paint.FILTER_BITMAP_FLAG).apply {
            color = Color.BLACK
            alpha = (255 * cs.getDouble("alpha")).toInt()
            if (sigma > .5f) maskFilter = BlurMaskFilter((sigma - .5f) / .57735f, BlurMaskFilter.Blur.NORMAL)
        })
        c.restore()
        mask.recycle()
        c.save()
        c.rotate(rot, r.centerX(), r.centerY())
        c.drawBitmap(art, null, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        c.restore()
    }

    private fun baseFor(ctx: Context, wx: String, w: Int, h: Int, m: Matrix, bw: Int, bh: Int): Bitmap {
        base?.let { (bwx, size, b) -> if (bwx == wx && size == w * 65536 + h) return b }
        base?.third?.recycle(); base = null
        // ponytail: full-res decode then one downscale; inSampleSize=2 if the 6.4MB transient ever matters
        val src = ctx.assets.open("$DIR/B_${wx}_base.webp").use(BitmapFactory::decodeStream)
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(b).apply { concat(m) }.drawBitmap(src, null, RectF(0f, 0f, bw.toFloat(), bh.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        src.recycle()
        base = Triple(wx, w * 65536 + h, b)
        return b
    }

    /** Splits the straight-alpha light map into a black shade layer (alpha * shadeMax) and an opaque warm layer (rgb * lightMax). */
    private fun lightFor(ctx: Context, L: JSONObject, wx: String, ph: Int): Pair<Bitmap, Bitmap> {
        lights["$wx#$ph"]?.let { return it }
        if (lights.keys.any { !it.startsWith("$wx#") }) { lights.values.forEach { it.first.recycle(); it.second.recycle() }; lights.clear() }
        val src = ctx.assets.open("$DIR/B_${wx}_light$ph.webp").use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inPremultiplied = false })
        }!!
        val w = src.width; val h = src.height
        val px = IntArray(w * h).also { src.getPixels(it, 0, w, 0, 0, w, h) }
        src.recycle()
        val sm = L.getJSONObject("shadeMax").getDouble(wx).toFloat()
        val lm = L.getJSONObject("lightMax").getDouble(wx).toFloat()
        val sh = IntArray(w * h) { ((px[it] ushr 24) * sm + .5f).toInt() shl 24 }
        val lt = IntArray(w * h) {
            val p = px[it]
            Color.rgb(((p shr 16 and 0xFF) * lm + .5f).toInt(), ((p shr 8 and 0xFF) * lm + .5f).toInt(), ((p and 0xFF) * lm + .5f).toInt())
        }
        return (Bitmap.createBitmap(sh, w, h, Bitmap.Config.ARGB_8888) to Bitmap.createBitmap(lt, w, h, Bitmap.Config.ARGB_8888)).also { lights["$wx#$ph"] = it }
    }

    /** Cheap frosted glass: a down/up-scaled copy over the scene plus a warm milky wash. */
    private fun frost(out: Bitmap) {
        val small = Bitmap.createScaledBitmap(out, max(1, out.width / 14), max(1, out.height / 14), true)
        val c = Canvas(out)
        c.drawBitmap(small, null, RectF(0f, 0f, out.width.toFloat(), out.height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 220 })
        small.recycle()
        c.drawColor(Color.argb(70, 246, 240, 232))
    }

    private fun text(ctx: Context, c: Canvas, T: JSONObject, wx: String, line: String, caption: String, locked: Boolean) {
        val koFace = ko ?: (runCatching { ResourcesCompat.getFont(ctx, DsR.font.nanum_myeongjo) }.getOrNull() ?: Typeface.SERIF).also { ko = it }
        val ls = T.getJSONObject("line"); val cap = T.getJSONObject("caption")
        val lr = ls.getJSONArray("rect")
        val lineInk = Color.parseColor(ls.getJSONObject("color").getString(wx))
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply { typeface = koFace; color = lineInk; textSize = ls.getDouble("size").toFloat() }
        val maxW = lr.getInt(2).toFloat()
        val lines = listOf(SceneBed.oneLine(line, p, maxW)) // always one line
        val step = p.textSize * ls.getDouble("lineSpacing").toFloat()
        var y = lr.getInt(1) - p.fontMetrics.ascent
        for (ln in lines) { c.drawText(ln, lr.getInt(0).toFloat(), y, p); y += step }

        val cr = cap.getJSONArray("rect")
        val cp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC); color = Color.parseColor(cap.getJSONObject("color").getString(wx))
            textSize = cap.getDouble("size").toFloat()
        }
        // Long lines (3 rows) push the caption down so they never collide.
        val capTop = max(cr.getInt(1).toFloat(), y - step + p.fontMetrics.descent + cp.textSize * .6f)
        if (caption.isNotEmpty()) {
            cp.measureText(caption).takeIf { it > cr.getInt(2) }?.let { cp.textSize *= cr.getInt(2) / it }
            c.drawText(caption, cr.getInt(0).toFloat(), capTop - cp.fontMetrics.ascent, cp)
        }
        if (locked) {
            // small quiet pill under the caption, same typography
            val pp = Paint(p).apply { textSize = cap.getDouble("size").toFloat() * .9f }
            val label = "광고 보고 열기"
            val padX = pp.textSize * .9f; val hgt = pp.textSize * 2f
            val top = capTop + cp.textSize * 2.2f
            val box = RectF(cr.getInt(0).toFloat(), top, cr.getInt(0) + pp.measureText(label) + 2 * padX, top + hgt)
            val night = wx == "night"
            c.drawRoundRect(box, hgt / 2, hgt / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (night) Color.argb(40, 255, 255, 255) else Color.argb(110, 255, 252, 246) })
            c.drawRoundRect(box, hgt / 2, hgt / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 1.6f; color = lineInk; alpha = 90
            })
            val fm = pp.fontMetrics
            c.drawText(label, box.left + padX, box.centerY() - (fm.ascent + fm.descent) / 2, pp)
        }
    }

}
