package com.gun.personalcolor.feature.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Debug builds only. Renders every widget size in every state, each light phase, to
 * <external files>/widget_snapshots/ (PNGs, as they float on the wallpaper):
 * `adb shell am broadcast -n com.gun.personalcolor/com.gun.personalcolor.feature.widget.WidgetSnapshotReceiver`
 */
class WidgetSnapshotReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val tones = dagger.hilt.android.EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).content().tones
                // The approved mockups' looks: W1 = today (rain, 9°), S1 = spring (sun, 17°), D1 = a dress on a cloudy day.
                val samples = mapOf(
                    "W1" to LookWidgets.Data(tones.first { it.id == "winter_deep" }, WidgetOutfit("비 오는 날 와인 포인트 룩", "영등포구 9°C 이슬비",
                        listOf("coat" to "#0F1A3A", "knit" to "#5E1224", "long_skirt" to "#9B9FA7", "loafers" to "#1E1E20",
                            "hobo" to "#F4F3EF", "umbrella" to "#F4F3EF"), false), false, false, hour = 14),
                    "S1" to LookWidgets.Data(tones.first { it.id == "spring_light" }, WidgetOutfit("봄볕 데일리 피치 가디건 룩", "망원동 17°C 맑음",
                        listOf("cardigan" to "#FFCBA4", "jeans" to "#A9C1DB", "mary_jane" to "#F2EEE5", "shoulder_bag" to "#D9B38C"), false), false, false, hour = 14),
                    "D1" to LookWidgets.Data(tones.first { it.id == "summer_light" }, WidgetOutfit("흐린 날 라벤더 원피스 룩", "망원동 21°C 흐림",
                        listOf("dress" to "#C8B6E2", "flats" to "#EFE6D8", "bag" to "#E8DCC8"), false), false, false, hour = 14),
                )
                // `--es pin mini|square|wide`: the launcher's "add widget" prompt (review on emulators).
                intent.getStringExtra("pin")?.let { which ->
                    val cls = when (which) { "mini" -> TodayLookMiniReceiver::class.java; "square" -> TodayLookSquareReceiver::class.java; else -> TodayColorWidgetReceiver::class.java }
                    android.appwidget.AppWidgetManager.getInstance(context).requestPinAppWidget(android.content.ComponentName(context, cls), null, null)
                    return@launch
                }
                // `--es apply W1|S1|D1|locked|off`: put a sample on the real widgets (home-screen review without an account).
                // A "_dark" suffix forces the night scene.
                intent.getStringExtra("apply")?.let { arg ->
                    val key = arg.removeSuffix("_dark")
                    val night = arg.endsWith("_dark")
                    LookWidgets.debugOverride = when (key) {
                        "off" -> null
                        "locked" -> samples.getValue("W1").let { LookWidgets.Data(it.tone, WidgetOutfit("", null, emptyList(), true), false, night) }
                        else -> samples.getValue(key).let { LookWidgets.Data(it.tone, it.outfit, false, night, hour = if (night) -1 else 14) }
                    }
                    LookWidgets.updateAll(context)
                    return@launch
                }
                val dir = File(context.getExternalFilesDir(null), "widget_snapshots").apply { deleteRecursively(); mkdirs() }
                val real = LookWidgets.load(context)
                val states = samples + mapOf(
                    "W1night" to samples.getValue("W1").let { LookWidgets.Data(it.tone, it.outfit, false, true) },
                    "real" to real,
                    "locked" to LookWidgets.Data(samples.getValue("W1").tone, WidgetOutfit("", null, emptyList(), locked = true), false, false, hour = 14),
                    "notone" to LookWidgets.Data(null, null, false, false, hour = 14),
                )
                val sizes = mapOf("1x1" to (80 to 116), "2x2" to (169 to 242), "4x2" to (348 to 242))
                for ((state, data) in states) for ((name, wh) in sizes) {
                    val look = LookWidgets.look(data).first
                    for (phase in 0..2) {
                        if (phase > 0 && state !in samples) continue
                        val b = LookWidgets.render(context, look, wh.first, wh.second, phase)
                        File(dir, "${state}_${name}_$phase.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
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
