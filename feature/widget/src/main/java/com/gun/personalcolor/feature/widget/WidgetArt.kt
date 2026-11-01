package com.gun.personalcolor.feature.widget

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.gun.personalcolor.core.model.BAG_KINDS
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.ui.GarmentArt
import com.gun.personalcolor.core.ui.renderClothingBitmap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The widget as a YouTube-lookbook thumbnail floating on the wallpaper (no card): today's pieces as sticker cut-outs
 * (thin white edge + soft shadow, so black clothes still read on dark wallpapers) overlapped in wearing order, a bold
 * title with the point color on a marker, and a small date/weather pill.
 *
 * Everything is drawn into bitmaps placed at exact px offsets. The landing animation is a list of [Frame]s that only
 * re-use the per-piece sticker bitmaps (no full-size bitmap per frame), which keeps the RemoteViews small.
 */
internal object WidgetArt {
    enum class Size { Mini, Square, Wide }

    /** A bitmap at (x, y) px in the widget. Low-res overlays carry half the bitmap density, so they draw 2x. */
    class Placed(val bmp: Bitmap, val x: Int, val y: Int)

    /** One animation frame: stickers in z order, plus an optional shine overlay. */
    class Frame(val layers: List<Placed>, val shine: Placed? = null)

    class Art(
        /** Pill + title (never animated). */
        val still: Placed?,
        /** The finished collage in one bitmap: the resting frame (and the only frame when [landing] is empty). */
        val collage: Placed?,
        /** Landing sequence, ends with the shine; empty = static (locked). */
        val landing: List<Frame>,
    )

    /** What to draw. [pieces] = (CLOTHING_KINDS key, ARGB) in any order; null while locked: never the real outfit. */
    class Look(
        val pieces: List<Pair<String, Int>>?,
        val title: Title,
        /** "11/1 · 9° 비" */
        val pill: String,
        /** 1x1: "9° 비" */
        val pillShort: String,
        /** Tiny 틴티 in the pill; null while our live wallpaper already shows 틴티. */
        val mark: Bitmap?,
        val night: Boolean,
        /** Locked teaser: the frosted pieces' tint and the button label ("광고 보고 열기"). */
        val lockLabel: String? = null,
        /** The wallpaper is bright (system hint): ink type with a light halo instead of white with a dark one. */
        val darkText: Boolean = false,
    )

    /** "비 오는 날 / [와인] 포인트 룩": [key] sits on a marker in [accent]; [sub] = small line under it (locked). */
    class Title(val before: String, val key: String?, val after: String, val accent: Int, val sub: String? = null)

    lateinit var font: Typeface

    /** Set per render: title color scheme for the wallpaper under the widget. */
    @Volatile private var darkText = false

    // ------------------------------------------------------------------------------------------------ composition

    private enum class Role { Outer, Top, Bottom, Dress, Shoes, Bag, Acc }

    private fun roleOf(kind: String): Role? = when (CLOTHING_KINDS[kind]?.second) {
        "아우터" -> Role.Outer
        "상의" -> Role.Top
        "하의" -> Role.Bottom
        "원피스" -> Role.Dress
        "신발" -> Role.Shoes
        else -> when (kind) {
            in BAG_KINDS -> Role.Bag
            // Small things (socks, earrings, rings…) would be specks at widget size.
            in BIG_ACCESSORIES -> Role.Acc
            else -> null
        }
    }

    private val BIG_ACCESSORIES = setOf(
        "umbrella", "muffler", "scarf", "hat", "beanie", "bucket_hat", "beret", "sunglasses", "ribbon", "hair_clip", "gloves",
    )

    /** Spot in box units (height = 1, width ≈ .85): center, size (height; shoes/bag/acc: longest side), tilt, z. */
    private class Spot(val cx: Float, val cy: Float, val size: Float, val rot: Float, val z: Int)

    private fun spot(role: Role, roles: Set<Role>, size: Size): Spot {
        val outer = Role.Outer in roles
        val dress = Role.Dress in roles
        if (size == Size.Mini) return when (role) {
            Role.Dress -> Spot(.40f, .46f, .90f, -2f, 1)
            Role.Bottom -> Spot(.54f, .54f, .86f, 4f, 1)
            Role.Shoes -> Spot(.20f, .88f, .34f, -4f, 3)
            else -> Spot(.28f, .34f, .62f, -5f, 2) // top (or the outer standing in for it)
        }
        return when (role) {
            Role.Outer -> Spot(.24f, .41f, .56f, -5f, 1)
            // No outer: the top goes up right and the bottom down left, so the pill's corner stays free and the
            // top-right corner isn't empty.
            Role.Top -> if (outer) Spot(.56f, .27f, .50f, 5f, 3) else Spot(.52f, .26f, .50f, 5f, 3)
            Role.Bottom -> if (outer) Spot(.50f, .64f, .70f, 3f, 2) else Spot(.32f, .62f, .74f, -3f, 2)
            Role.Dress -> if (outer) Spot(.55f, .52f, .86f, 3f, 2) else Spot(.38f, .48f, .90f, -2f, 2)
            Role.Shoes -> if (outer || dress) Spot(.17f, .88f, .30f, -4f, 4) else Spot(.12f, .90f, .30f, -4f, 4)
            Role.Bag -> if (outer || dress) Spot(.74f, .83f, .25f, 7f, 5) else Spot(.74f, .80f, .29f, 7f, 5)
            // The top-right corner is the one that goes empty (2x2): accessories fill it.
            Role.Acc -> when { outer -> Spot(.82f, .12f, .22f, 10f, 6); dress -> Spot(.74f, .19f, .25f, 8f, 6); else -> Spot(.82f, .54f, .24f, 8f, 6) }
        }
    }

    /** One per role, in wearing order (outer → top → bottom/dress → shoes → bag → accessory). */
    private fun cast(pieces: List<Pair<String, Int>>, size: Size): List<Triple<String, Int, Role>> {
        val byRole = LinkedHashMap<Role, Pair<String, Int>>()
        for ((kind, color) in pieces) {
            val r = roleOf(kind) ?: continue
            if (r !in byRole) byRole[r] = kind to color
        }
        if (Role.Dress in byRole) { byRole.remove(Role.Top); byRole.remove(Role.Bottom) }
        if (size == Size.Mini) {
            // 1x1: the silhouette only. The outer stands in when there's no top.
            val keep = buildList {
                (byRole[Role.Top] ?: byRole[Role.Outer])?.let { add(Role.Top to it) }
                byRole[Role.Bottom]?.let { add(Role.Bottom to it) }
                byRole[Role.Dress]?.let { add(Role.Dress to it) }
                byRole[Role.Shoes]?.let { add(Role.Shoes to it) }
            }
            byRole.clear(); keep.forEach { (r, p) -> byRole[r] = p }
        }
        return byRole.entries.sortedBy { it.key.ordinal }.map { Triple(it.value.first, it.value.second, it.key) }
    }

    /** Locked: generic pieces (never today's), drawn frosted. */
    private val TEASER = listOf("cardigan" to 0, "long_skirt" to 0, "mary_jane" to 0, "shoulder_bag" to 0)

    private class Sticker(val bmp: Bitmap, val x: Float, val y: Float, val z: Int)

    /**
     * Lays the pieces out in [box] (px) and returns stickers in wearing order. The spots are fitted to the real
     * union of the pieces, so a look without an outer (or with a dress) still fills the box instead of looking sparse.
     */
    private fun collage(
        pieces: List<Pair<String, Int>>, size: Size, box: RectF, d: Float, frosted: Boolean, night: Boolean,
    ): List<Sticker> {
        val cast = cast(pieces, size)
        if (cast.isEmpty()) return emptyList()
        val roles = cast.map { it.third }.toSet()
        class P(val art: Bitmap, val sp: Spot, val w: Float, val h: Float)
        val ps = cast.mapNotNull { (kind, color, role) ->
            val art = garment(kind, if (frosted) Color.WHITE else color) ?: return@mapNotNull null
            val sp = spot(role, roles, size)
            val aspect = art.width.toFloat() / art.height
            // Clothes are sized by height; shoes/bags/accessories by their longest side (boots vs loafers).
            val (w, h) = if (role in setOf(Role.Shoes, Role.Bag, Role.Acc)) {
                if (aspect >= 1f) sp.size to sp.size / aspect else sp.size * aspect to sp.size
            } else sp.size * aspect to sp.size
            P(art, sp, w, h)
        }
        if (ps.isEmpty()) return emptyList()
        // Union of the rotated boxes, in box units.
        val u = RectF(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        for (p in ps) {
            val r = Math.toRadians(p.sp.rot.toDouble())
            val hw = (abs(cos(r)) * p.w + abs(sin(r)) * p.h).toFloat() / 2
            val hh = (abs(sin(r)) * p.w + abs(cos(r)) * p.h).toFloat() / 2
            val cx = p.sp.cx
            u.union(cx - hw, p.sp.cy - hh, cx + hw, p.sp.cy + hh)
        }
        val edge = (if (size == Size.Mini) 1.1f else 1.4f) * d
        val margin = edge * 2 + 2 * d
        val bw = box.width() - margin * 2; val bh = box.height() - margin * 2
        // Fill the box, but never blow 2 pieces up to poster size.
        val s = min(min(bw / u.width(), bh / u.height()), bh * 1.12f)
        val ox = box.left + margin + (bw - u.width() * s) / 2 - u.left * s
        val oy = box.top + margin + (bh - u.height() * s) / 2 - u.top * s
        return ps.map { p ->
            val bmp = sticker(p.art, p.w * s, p.h * s, p.sp.rot, edge, d, frosted, night)
            Sticker(bmp, ox + p.sp.cx * s - bmp.width / 2f, oy + p.sp.cy * s - bmp.height / 2f, p.sp.z)
        }
    }

    /** Painted art if the kind has it, else the vector illustration; cropped to its pixels. */
    private fun garment(kind: String, color: Int): Bitmap? {
        val c = androidx.compose.ui.graphics.Color(color)
        val raw = GarmentArt.bitmap(kind, c) ?: renderClothingBitmap(kind, c, 420, 4.2f)
        return crop(raw)
    }

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

    /**
     * The cut-out at [w]×[h] px, tilted, with a thin white sticker edge and a soft shadow.
     * [frosted]: a frosted-glass silhouette (locked teaser) that also holds up on bright wallpapers.
     */
    private fun sticker(art: Bitmap, w: Float, h: Float, rot: Float, edge: Float, d: Float, frosted: Boolean, night: Boolean): Bitmap {
        val r = Math.toRadians(rot.toDouble())
        val rw = (abs(cos(r)) * w + abs(sin(r)) * h).toFloat()
        val rh = (abs(sin(r)) * w + abs(cos(r)) * h).toFloat()
        val pad = (edge + 7 * d).roundToInt()
        val ow = (rw + pad * 2).roundToInt(); val oh = (rh + pad * 2).roundToInt()
        val piece = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
        val m = Matrix().apply {
            postScale(w / art.width, h / art.height)
            postTranslate(-w / 2, -h / 2); postRotate(rot); postTranslate(ow / 2f, oh / 2f)
        }
        Canvas(piece).drawBitmap(art, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        val alpha = piece.extractAlpha()
        val out = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        // Shadow: soft and a touch below, so the piece lifts off any wallpaper.
        p.color = Color.argb(if (frosted) 120 else 105, 12, 8, 20)
        p.maskFilter = BlurMaskFilter(edge * 2.2f + 2.5f * d, BlurMaskFilter.Blur.NORMAL)
        c.drawBitmap(alpha, 0f, 1.6f * d, p)
        p.maskFilter = null
        // Sticker edge: the silhouette dilated by [edge].
        p.color = if (frosted) Color.argb(235, 255, 255, 255) else Color.argb(245, 255, 255, 255)
        for (i in 0 until 16) {
            val a = i * Math.PI / 8
            c.drawBitmap(alpha, (cos(a) * edge).toFloat(), (sin(a) * edge).toFloat(), p)
        }
        if (frosted) {
            // Frosted fill: milky lavender glass, darker at the hem so it reads on a bright sky too.
            val glass = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
            val g = Canvas(glass)
            g.drawBitmap(alpha, 0f, 0f, Paint().apply { color = Color.BLACK })
            val top = if (night) Color.argb(150, 214, 204, 245) else Color.argb(170, 232, 226, 252)
            val bottom = if (night) Color.argb(185, 150, 132, 210) else Color.argb(205, 170, 152, 222)
            g.drawRect(0f, 0f, ow.toFloat(), oh.toFloat(), Paint().apply {
                shader = LinearGradient(0f, 0f, ow * .3f, oh.toFloat(), top, bottom, Shader.TileMode.CLAMP)
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            })
            // Knock the glass out of the white edge so it looks inset, then lay it on.
            c.drawBitmap(alpha, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT); this.alpha = 150 })
            c.drawBitmap(glass, 0f, 0f, null)
            glass.recycle()
        } else {
            c.drawBitmap(piece, 0f, 0f, null)
        }
        alpha.recycle(); piece.recycle()
        return out
    }

    // ------------------------------------------------------------------------------------------------ frames

    /** Px the landing piece starts above its spot, per frame (eased: fast, then settling). No translucent frames. */
    private val LIFTS = floatArrayOf(20f, 9f, 3f)

    fun render(look: Look, size: Size, w: Int, h: Int, d: Float): Art {
        val W = w.toFloat(); val H = h.toFloat()
        darkText = look.darkText
        val k = when (size) { Size.Wide -> min(1f, H / (230 * d)); Size.Square -> min(1f, W / (165 * d)); Size.Mini -> min(1f, W / (80 * d)) }
        val still = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val sc = Canvas(still)
        val frosted = look.pieces == null
        val box: RectF
        when (size) {
            Size.Wide -> {
                val bw = min(W * .56f, H * .95f)
                box = RectF(W - bw - 2 * d, 0f, W - 2 * d, H)
                val pillB = pill(sc, look.pill, 10 * d, 8 * d, 11.5f * k * d, look.mark, look.night)
                title(sc, look.title, 12 * d, pillB, H - 8 * d, W - bw - 18 * d, k * d, stacked = true)
            }
            Size.Square -> {
                val titleH = 50 * k * d
                box = RectF(0f, 0f, W, H - titleH - 2 * d)
                pill(sc, look.pill, 6 * d, 4 * d, 10.5f * k * d, look.mark, look.night)
                title(sc, look.title, 8 * d, H - titleH, H - 4 * d, W - 14 * d, k * d, stacked = false)
            }
            Size.Mini -> {
                val ph = 19 * k * d
                box = RectF(0f, 0f, W, H - ph - 5 * d)
                miniPill(sc, look.pillShort, W / 2, H - ph - 1 * d, ph, k * d, look.night, locked = look.lockLabel != null)
            }
        }
        // RemoteViews padding can't be negative: crop each sticker to the widget's top-left once, so every frame
        // re-uses the same bitmaps.
        val stickers = collage(look.pieces ?: TEASER, size, box, d, frosted, look.night).map { st ->
            val sx = max(0, -st.x.toInt()); val sy = max(0, -st.y.toInt())
            if (sx == 0 && sy == 0) st
            else Sticker(Bitmap.createBitmap(st.bmp, sx, sy, st.bmp.width - sx, st.bmp.height - sy), st.x + sx, st.y + sy, st.z)
        }
        if (frosted) {
            if (size != Size.Mini) lockPill(sc, look.lockLabel ?: "열어보기", box.centerX(), box.centerY() - box.height() * .06f, k * d, look.night)
            sparkle(sc, box.left + box.width() * .22f, box.top + box.height() * .28f, 6 * k * d)
            sparkle(sc, box.left + box.width() * .82f, box.top + box.height() * .72f, 4.5f * k * d)
            if (size != Size.Mini) sparkle(sc, box.left + box.width() * .74f, box.top + box.height() * .14f, 3.5f * k * d)
        }
        val stillPlaced = trim(still)
        if (stickers.isEmpty()) return Art(stillPlaced, null, emptyList())

        // The finished collage, cropped to the stickers.
        val bounds = RectF(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        stickers.forEach { bounds.union(it.x, it.y, it.x + it.bmp.width, it.y + it.bmp.height) }
        bounds.intersect(0f, 0f, W, H)
        val cr = Rect(bounds.left.toInt(), bounds.top.toInt(), bounds.right.roundToInt(), bounds.bottom.roundToInt())
        val full = Bitmap.createBitmap(cr.width().coerceAtLeast(1), cr.height().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val fc = Canvas(full)
        stickers.sortedBy { it.z }.forEach { fc.drawBitmap(it.bmp, it.x - cr.left, it.y - cr.top, null) }
        val collage = Placed(full, cr.left, cr.top)
        if (frosted) return Art(stillPlaced, collage, emptyList())

        // Landing: each piece drops in, in wearing order, fully opaque, onto the ones already down.
        val frames = ArrayList<Frame>()
        for (i in stickers.indices) for (lift in LIFTS) {
            val layers = stickers.filterIndexed { j, _ -> j <= i }.sortedBy { it.z }.map { s ->
                val dy = if (s === stickers[i]) min(lift * d, s.y.coerceAtLeast(0f)) else 0f
                Placed(s.bmp, s.x.roundToInt(), (s.y - dy).roundToInt())
            }
            frames += Frame(layers)
        }
        // Then a sheen sweeps across the clothes (half-res overlays; it's a soft glow anyway).
        for (t in SHINES) frames += Frame(listOf(collage), shine(full, t, cr))
        return Art(stillPlaced, collage, frames)
    }

    private val SHINES = floatArrayOf(.2f, .45f, .7f, .95f)

    private fun shine(full: Bitmap, t: Float, at: Rect): Placed {
        val sw = (full.width / 2).coerceAtLeast(1); val sh = (full.height / 2).coerceAtLeast(1)
        val out = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val mask = full.extractAlpha()
        c.drawBitmap(mask, null, Rect(0, 0, sw, sh), Paint(Paint.FILTER_BITMAP_FLAG).apply { color = Color.BLACK })
        mask.recycle()
        // Diagonal band (top-left → bottom-right sweep): a wide soft glow with a thin bright core.
        val span = sw * .75f + sh * .25f
        val c0 = t * 1.5f - .25f
        fun at(v: Float) = v.coerceIn(0f, 1f)
        val stops = floatArrayOf(at(c0 - .16f), at(c0 - .05f), at(c0), at(c0 + .02f), at(c0 + .12f)).let { s ->
            for (i in 1 until s.size) if (s[i] <= s[i - 1]) s[i] = min(1f, s[i - 1] + 1e-4f); s
        }
        val colors = intArrayOf(0x00FFFFFF, 0x66FFFFFF, 0xB0FFFFFF.toInt(), 0x55FFFFFF, 0x00FFFFFF)
        c.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, span, span * sh / sw * .33f, colors, stops, Shader.TileMode.CLAMP)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        })
        out.density = full.density / 2
        return Placed(out, at.left, at.top)
    }

    /** Crops a mostly transparent layer to its pixels. */
    private fun trim(b: Bitmap): Placed? {
        val w = b.width; val h = b.height
        val px = IntArray(w * h).also { b.getPixels(it, 0, w, 0, 0, w, h) }
        var l = w; var t = h; var r = -1; var bt = -1
        for (y in 0 until h) for (x in 0 until w) if (px[y * w + x] ushr 24 != 0) {
            if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > bt) bt = y
        }
        if (r < 0) return null
        return Placed(Bitmap.createBitmap(b, l, t, r - l + 1, bt - t + 1), l, t)
    }

    // ------------------------------------------------------------------------------------------------ type

    private fun titlePaint(px: Float, weight: Int) = textPaint(px, weight, if (darkText) INK else Color.WHITE)

    private fun textPaint(px: Float, weight: Int, color: Int = Color.WHITE) = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = font; textSize = px; this.color = color
        fontVariationSettings = "'wght' $weight"
    }

    /** White thumbnail type with a soft dark halo, readable on any wallpaper. */
    private fun haloText(c: Canvas, s: String, x: Float, y: Float, p: Paint, d: Float, strong: Float = 1f) {
        val col = p.color
        val (r, g, b) = if (darkText) Triple(255, 255, 255) else Triple(14, 8, 22)
        val k = if (darkText) 1.6f else 1f
        p.setShadowLayer(6 * d, 0f, 1 * d, Color.argb((120 * strong * k).toInt().coerceAtMost(255), r, g, b)); c.drawText(s, x, y, p)
        p.setShadowLayer(1.6f * d, 0f, 1 * d, Color.argb((115 * strong * k).toInt().coerceAtMost(255), r, g, b)); c.drawText(s, x, y, p)
        p.clearShadowLayer(); p.color = col
    }

    /** Shrinks [p] until [s] fits [maxW]. */
    private fun fit(p: Paint, s: String, maxW: Float) {
        while (p.measureText(s) > maxW && p.textSize > 8f) p.textSize *= .94f
    }

    private fun marker(c: Canvas, s: String, x: Float, y: Float, p: Paint, accent: Int, d: Float): Float {
        val kw = p.measureText(s); val sz = p.textSize
        val r = RectF(x - 3.5f * d, y - sz * .92f, x + kw + 4f * d, y + sz * .2f)
        val mp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent or 0xFF000000.toInt() }
        mp.setShadowLayer(4 * d, 0f, 2 * d, Color.argb(90, 10, 5, 15))
        c.drawRoundRect(r, 5.5f * d, 5.5f * d, mp)
        val light = lum(accent) > .72f
        val tp = Paint(p).apply { color = if (light) INK else Color.WHITE }
        c.drawText(s, x, y, tp)
        return r.right
    }

    /**
     * Title block between [top] and [bottom] (px), left-aligned at [x].
     * stacked (4x2): before / [KEY] / after, big; otherwise (2x2): before / [KEY] after, bottom-aligned.
     */
    /** Two balanced lines: at " + " when there is one, else at the space nearest the middle. */
    private fun split2(s: String): Pair<String, String> {
        s.indexOf(" + ").takeIf { it > 0 }?.let { return s.substring(0, it + 2).trim() to s.substring(it + 3).trim() }
        val spaces = s.indices.filter { s[it] == ' ' }
        val at = spaces.minByOrNull { kotlin.math.abs(it - s.length / 2) } ?: return s to ""
        return s.substring(0, at).trim() to s.substring(at + 1).trim()
    }

    private fun title(c: Canvas, t: Title, x: Float, top: Float, bottom: Float, maxW: Float, d: Float, stacked: Boolean) {
        if (stacked) {
            val p1 = titlePaint(22 * d, 800); val pk = titlePaint(36 * d, 800); val p3 = titlePaint(29 * d, 800)
            val ps = titlePaint(13 * d, 700)
            fit(p1, t.before, maxW); t.key?.let { fit(pk, it, maxW - 8 * d) }
            // A long tail wraps onto two lines instead of shrinking to a caption.
            var after = listOf(t.after).filter { it.isNotEmpty() }
            if (t.after.isNotEmpty() && p3.measureText(t.after) > maxW * 1.12f) {
                val (a, b) = split2(t.after); after = listOf(a, b).filter { it.isNotEmpty() }
                p3.textSize = 24 * d
            }
            after.forEach { fit(p3, it, maxW) }
            t.sub?.let { fit(ps, it, maxW) }
            val total = (if (t.before.isNotEmpty()) p1.textSize * 1.3f else 0f) + (if (t.key != null) pk.textSize * 1.22f else 0f) +
                after.size * p3.textSize * 1.2f + (if (t.sub != null) ps.textSize * 1.9f else 0f)
            var y = top + ((bottom - top) - total) / 2 + 4 * d
            if (t.before.isNotEmpty()) { y += p1.textSize * 1.3f; haloText(c, t.before, x, y - p1.textSize * .3f, p1, d) }
            if (t.key != null) { y += pk.textSize * 1.22f; marker(c, t.key, x + 3 * d, y - pk.textSize * .22f, pk, t.accent, d) }
            for (line in after) { y += p3.textSize * 1.2f; haloText(c, line, x, y - p3.textSize * .2f, p3, d) }
            if (t.sub != null) { y += ps.textSize * 1.9f; haloText(c, t.sub, x, y - ps.textSize * .3f, ps, d, .9f) }
        } else {
            val sz = 17.5f * d
            val p1 = titlePaint(sz, 800); val p2 = titlePaint(sz, 800); val ps = titlePaint(12 * d, 700)
            if (t.sub != null) {
                fit(ps, t.sub, maxW)
                val one = "${t.before} ${t.after}".trim()
                fit(p1, one, maxW)
                haloText(c, one, x, bottom - ps.textSize * 1.5f - 2 * d, p1, d)
                haloText(c, t.sub, x, bottom - 2 * d, ps, d, .9f)
                return
            }
            // Line 1 / line 2. With the color word first ("크림슨 니트 + 그레이 롱스커트"), the marker leads line 1.
            val keyFirst = t.before.isEmpty() && t.key != null
            val (l1, l2) = if (keyFirst) split2(t.after) else t.before to t.after
            val keyLine = if (keyFirst) 1 else 2
            val kw = t.key?.let { p2.measureText(it) + 12 * d } ?: 0f
            fit(p1, l1, maxW - if (keyLine == 1) kw else 0f); fit(p2, l2, maxW - if (keyLine == 2) kw else 0f)
            val s = min(p1.textSize, p2.textSize); p1.textSize = s; p2.textSize = s
            val lh = s * 1.24f
            val y2 = bottom - s * .28f
            fun line(text: String, y: Float, p: Paint, withKey: Boolean) {
                var kx = x
                if (withKey && t.key != null) kx = marker(c, t.key, x + 3 * d, y, p, t.accent, d) + 5 * d
                if (text.isNotEmpty()) haloText(c, text, kx, y, p, d)
            }
            if (l1.isNotEmpty() || keyLine == 1) line(l1, if (l2.isEmpty()) y2 else y2 - lh, p1, keyLine == 1)
            if (l2.isNotEmpty() || keyLine == 2) line(l2, y2, p2, keyLine == 2)
        }
    }

    /** Small frosted pill with the brand mark: "11/1 · 9° 비". Returns its bottom. */
    private fun pill(c: Canvas, s: String, x: Float, y: Float, sz: Float, mark: Bitmap?, night: Boolean): Float {
        val tp = textPaint(sz, 700, if (night) Color.rgb(0xEA, 0xE6, 0xE3) else INK)
        val ph = sz * 1.95f
        val mw = if (mark != null) sz * 1.35f else 0f
        val pw = tp.measureText(s) + sz * 1.25f + (if (mark != null) mw + sz * .3f else 0f)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (night) Color.argb(232, 0x2A, 0x27, 0x25) else Color.argb(236, 255, 255, 255)
            setShadowLayer(sz * .45f, 0f, sz * .15f, Color.argb(70, 10, 5, 15))
        }
        c.drawRoundRect(x, y, x + pw, y + ph, ph / 2, ph / 2, bg)
        var tx = x + sz * .62f
        if (mark != null) {
            c.drawBitmap(mark, null, RectF(tx - sz * .1f, y + (ph - mw) / 2, tx - sz * .1f + mw, y + (ph + mw) / 2),
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            tx += mw + sz * .2f
        }
        c.drawText(s, tx, y + ph / 2 - (tp.descent() + tp.ascent()) / 2, tp)
        return y + ph
    }

    /** 1x1 pill under the mini collage, centered at [cx]; locked: purple with a lock. */
    private fun miniPill(c: Canvas, s: String, cx: Float, y: Float, ph: Float, d: Float, night: Boolean, locked: Boolean) {
        val tp = textPaint(11 * d, 700, if (locked) Color.WHITE else if (night) Color.rgb(0xEA, 0xE6, 0xE3) else INK)
        fit(tp, s, c.width - 24 * d)
        val lw = if (locked) 13 * d else 0f
        val pw = tp.measureText(s) + 13 * d + lw
        val x = cx - pw / 2
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when { locked -> if (night) Color.rgb(0x6E, 0x5D, 0xA6) else ACCENT; night -> Color.argb(232, 0x2A, 0x27, 0x25); else -> Color.argb(236, 255, 255, 255) }
            setShadowLayer(4 * d, 0f, 1.5f * d, Color.argb(70, 10, 5, 15))
        }
        c.drawRoundRect(x, y, x + pw, y + ph, ph / 2, ph / 2, bg)
        if (locked) lock(c, x + 6.5f * d + 4 * d, y + ph / 2 + .5f * d, d * .9f, Color.WHITE)
        c.drawText(s, x + 6.5f * d + lw, y + ph / 2 - (tp.descent() + tp.ascent()) / 2, tp)
    }

    /** "🔒 광고 보고 열기": solid brand purple so it holds on a bright sky as well as a dark wallpaper. */
    private fun lockPill(c: Canvas, label: String, cx: Float, cy: Float, d: Float, night: Boolean) {
        val tp = textPaint(12.5f * d, 700, Color.WHITE)
        val ph = 31 * d; val pw = tp.measureText(label) + 42 * d
        val r = RectF(cx - pw / 2, cy - ph / 2, cx + pw / 2, cy + ph / 2)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (night) Color.rgb(0x6E, 0x5D, 0xA6) else ACCENT
            setShadowLayer(6 * d, 0f, 2.5f * d, Color.argb(110, 20, 10, 40))
        }
        c.drawRoundRect(r, ph / 2, ph / 2, bg)
        c.drawRoundRect(r, ph / 2, ph / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.2f * d; color = Color.argb(150, 255, 255, 255)
        })
        lock(c, r.left + 16 * d, cy + .5f * d, d, Color.WHITE)
        c.drawText(label, r.left + 27 * d, cy - (tp.descent() + tp.ascent()) / 2, tp)
    }

    private fun lock(c: Canvas, x: Float, y: Float, d: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        c.drawRoundRect(x - 5 * d, y - 1.5f * d, x + 5 * d, y + 6 * d, 1.6f * d, 1.6f * d, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.7f * d
        c.drawArc(x - 3.3f * d, y - 7.5f * d, x + 3.3f * d, y + .5f * d, 180f, 180f, false, p)
    }

    private fun sparkle(c: Canvas, x: Float, y: Float, r: Float) {
        val path = Path().apply {
            moveTo(x, y - r); lineTo(x + r * .22f, y - r * .22f); lineTo(x + r, y); lineTo(x + r * .22f, y + r * .22f)
            lineTo(x, y + r); lineTo(x - r * .22f, y + r * .22f); lineTo(x - r, y); lineTo(x - r * .22f, y - r * .22f); close()
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; setShadowLayer(r * .5f, 0f, 0f, Color.argb(120, 60, 40, 110)) }
        c.drawPath(path, p)
    }

    private val INK = Color.rgb(0x1D, 0x1B, 0x1A)
    private val ACCENT = Color.rgb(0x5B, 0x4B, 0x8A)

    fun lum(c: Int) = (.2126f * Color.red(c) + .7152f * Color.green(c) + .0722f * Color.blue(c)) / 255f
}
