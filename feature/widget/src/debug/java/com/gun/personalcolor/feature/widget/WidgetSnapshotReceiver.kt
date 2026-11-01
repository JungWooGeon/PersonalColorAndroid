package com.gun.personalcolor.feature.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Debug builds only. Renders every widget size in every state, light and dark, plus each landing frame, to
 * <external files>/widget_snapshots/ (transparent PNGs, as they float on the wallpaper):
 * `adb shell am broadcast -n com.gun.personalcolor/com.gun.personalcolor.feature.widget.WidgetSnapshotReceiver`
 */
class WidgetSnapshotReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val tones = dagger.hilt.android.EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).content().tones
                val samples = mapOf(
                    "W1" to LookWidgets.Data(tones.first { it.id == "winter_deep" }, WidgetOutfit("비 오는 날 와인 포인트 룩", "망원동 9°C 비",
                        listOf("short_coat" to "#2F3136", "turtleneck" to "#5E1224", "wide" to "#26324A", "loafers" to "#151515", "shoulder_bag" to "#161616"), false), false, false),
                    "S1" to LookWidgets.Data(tones.first { it.id == "spring_light" }, WidgetOutfit("봄볕 데일리 피치 가디건 룩", "망원동 18°C 맑음",
                        listOf("cardigan" to "#FFCBA4", "wide" to "#A9BCD6", "mary_jane" to "#E8DCC8", "shoulder_bag" to "#D9B38C"), false), false, false),
                    "D1" to LookWidgets.Data(tones.first { it.id == "summer_light" }, WidgetOutfit("햇살 좋은 날 라벤더 원피스 룩", "망원동 26°C 맑음",
                        listOf("dress" to "#C9B6E4", "sandals" to "#EFE6D8", "bag" to "#E8DCC8", "sunglasses" to "#1A1A1A"), false), false, false),
                ).let { m -> m + ("L1" to m.getValue("W1").let { LookWidgets.Data(it.tone, WidgetOutfit("", null, emptyList(), true), false, false) }) +
                    ("B1" to m.getValue("S1").let { LookWidgets.Data(it.tone, it.outfit, false, false, darkText = true) }) }
                // `--es pin mini|square|wide`: the launcher's "add widget" prompt (review on emulators).
                intent.getStringExtra("pin")?.let { which ->
                    val cls = when (which) { "mini" -> TodayLookMiniReceiver::class.java; "square" -> TodayLookSquareReceiver::class.java; else -> TodayColorWidgetReceiver::class.java }
                    android.appwidget.AppWidgetManager.getInstance(context).requestPinAppWidget(android.content.ComponentName(context, cls), null, null)
                    return@launch
                }
                // `--es apply W1|S1|D1|locked|off`: put a sample on the real widgets (home-screen review without an account).
                // A "_dark" suffix renders the dark-mode variant without touching the system setting.
                intent.getStringExtra("apply")?.let { arg ->
                    val key = arg.removeSuffix("_dark")
                    val live = LookWidgets.load(context.also { LookWidgets.debugOverride = null }).liveTinty
                    LookWidgets.debugOverride = when (key) {
                        "off" -> null
                        "locked" -> samples.getValue("W1").let { LookWidgets.Data(it.tone, WidgetOutfit("", null, emptyList(), true), live, false) }
                        else -> samples.getValue(key).let { LookWidgets.Data(it.tone, it.outfit, live, false, it.darkText) }
                    }?.let { if (arg.endsWith("_dark")) LookWidgets.Data(it.tone, it.outfit, it.liveTinty, true, it.darkText) else it }
                    LookWidgets.updateAll(context)
                    return@launch
                }
                val dir = File(context.getExternalFilesDir(null), "widget_snapshots").apply { deleteRecursively(); mkdirs() }
                val real = LookWidgets.load(context)
                val states = samples + mapOf(
                    "real" to real,
                    "locked" to LookWidgets.Data(real.tone, WidgetOutfit("", null, emptyList(), locked = true), real.liveTinty, real.night),
                    "nooutfit" to LookWidgets.Data(real.tone, null, real.liveTinty, real.night),
                )
                val sizes = mapOf("1x1" to (80 to 116), "2x2" to (170 to 242), "4x2" to (350 to 242), "4x2s" to (330 to 160))
                for ((state, data) in states) for (night in listOf(false, true)) for ((name, wh) in sizes) {
                    val (art, _) = LookWidgets.render(context, data, wh.first, wh.second, night)
                    val d = context.resources.displayMetrics.density
                    val w = (wh.first * d).toInt(); val h = (wh.second * d).toInt()
                    fun save(file: String, layers: List<WidgetArt.Placed?>) {
                        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val c = Canvas(b)
                        val p = Paint(Paint.FILTER_BITMAP_FLAG)
                        for (l in layers.filterNotNull()) {
                            val s = l.bmp.density.takeIf { it > 0 }?.let { b.density.toFloat() / it } ?: 1f
                            c.drawBitmap(l.bmp, null, android.graphics.RectF(l.x.toFloat(), l.y.toFloat(), l.x + l.bmp.width * s, l.y + l.bmp.height * s), p)
                        }
                        File(dir, file).outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                    val tag = "${state}_${name}_${if (night) "dark" else "light"}"
                    save("$tag.png", listOf(art.collage, art.still))
                    if (state in samples && !night) art.landing.forEachIndexed { i, f ->
                        save("anim_${state}_${name}_%02d.png".format(i), f.layers + f.shine + art.still)
                    }
                }
                android.util.Log.i("WidgetSnapshot", "done")
            } catch (e: Exception) {
                android.util.Log.e("WidgetSnapshot", "failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
