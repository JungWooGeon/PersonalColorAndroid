package com.gun.personalcolor.feature.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.compose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Debug builds only. Renders the widget at several sizes, light and dark, in every outfit state, to
 * <external files>/widget_snapshots/ — so dark mode can be reviewed without touching the system setting:
 * `adb shell am broadcast -n com.gun.personalcolor/com.gun.personalcolor.feature.widget.WidgetSnapshotReceiver`
 */
class WidgetSnapshotReceiver : BroadcastReceiver() {
    @OptIn(ExperimentalGlanceApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val dir = File(context.getExternalFilesDir(null), "widget_snapshots").apply { deleteRecursively(); mkdirs() }
                val sizes = mapOf(
                    "2x2" to DpSize(160.dp, 160.dp), "mid" to DpSize(190.dp, 200.dp),
                    "4x2s" to DpSize(330.dp, 110.dp), "4x2" to DpSize(370.dp, 190.dp),
                )
                val outfits = mapOf(
                    "real" to null,
                    "open" to WidgetOutfit("카멜 트렌치에 아이보리 니트", "맑음 18°",
                        listOf("trench" to "#C69C6D", "knit" to "#F3E9DC", "slacks" to "#8A6E5A"), locked = false),
                    "locked" to WidgetOutfit("", null, emptyList(), locked = true),
                )
                for ((state, outfit) in outfits) for (night in listOf(false, true)) for ((name, size) in sizes) {
                    if (state != "real" && !name.startsWith("4x2")) continue
                    val cfg = Configuration(context.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
                    }
                    val ctx = context.createConfigurationContext(cfg)
                    val views = TodayColorWidget(outfit).compose(ctx, size = size)
                    val d = ctx.resources.displayMetrics.density
                    val w = (size.width.value * d).toInt(); val h = (size.height.value * d).toInt()
                    val bmp = withContext(Dispatchers.Main) {
                        val v = views.apply(ctx, FrameLayout(ctx))
                        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                        v.layout(0, 0, w, h)
                        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { b ->
                            Canvas(b).apply { drawColor(if (night) 0xFF000000.toInt() else 0xFFE9E4F2.toInt()); v.draw(this) }
                        }
                    }
                    File(dir, "${state}_${name}_${if (night) "dark" else "light"}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
