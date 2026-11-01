package com.gun.personalcolor.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.designsystem.tintyBitmap
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.hexColor
import com.gun.personalcolor.core.ui.tintyColors
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.IdentityHashMap
import java.util.Optional
import com.gun.personalcolor.core.designsystem.R as DsR

/** Today's outfit for the widgets. The app binds a [WidgetOutfitSource]. */
data class WidgetOutfit(
    /** e.g. "비 오는 날 와인 포인트 룩" */
    val headline: String,
    /** e.g. "영등포구 10°C 이슬비"; null hides the weather. */
    val weatherLine: String?,
    /** (CLOTHING_KINDS key, "#RRGGBB") in wearing order. */
    val pieces: List<Pair<String, String>>,
    /** True when the user has no access right now (trial over, not premium, not unlocked today). Nothing above is shown then. */
    val locked: Boolean,
)

interface WidgetOutfitSource {
    suspend fun today(): WidgetOutfit?
}

@Module
@InstallIn(SingletonComponent::class)
interface WidgetModule {
    @BindsOptionalOf
    fun outfitSource(): WidgetOutfitSource
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun preferences(): UserPreferencesRepository
    fun content(): ContentRepository
    fun outfitSource(): Optional<WidgetOutfitSource>
}

/** Called by the app when the tone, today's unlock, premium or the outfit changes. */
object TodayColorWidgets {
    suspend fun refresh(context: Context) = LookWidgets.updateAll(context.applicationContext)

    /** Launcher supports the "add this widget?" system prompt (most launchers on Android 8+). */
    fun canPin(context: Context): Boolean = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /** Shows the launcher's "홈 화면에 추가할까요?" prompt (the 2x2). Returns false if the launcher can't do it. */
    fun requestPin(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(ComponentName(context, TodayLookSquareReceiver::class.java), null, null)
    }
}

/** One provider per picker size (One UI lists each with its own preview); all of them resize freely. */
abstract class LookWidgetProvider internal constructor(internal val defaultSize: WidgetArt.Size) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = async { LookWidgets.update(context, ids) }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) =
        async { LookWidgets.update(context, intArrayOf(id)) }

    private fun async(block: suspend () -> Unit) {
        val pending = goAsync()
        LookWidgets.scope.launch { try { block() } catch (e: Exception) { Log.e(TAG, "update failed", e) } finally { pending.finish() } }
    }
}

/** 4x2 (kept under its old name so widgets already on home screens carry over). */
class TodayColorWidgetReceiver : LookWidgetProvider(WidgetArt.Size.Wide)
class TodayLookSquareReceiver : LookWidgetProvider(WidgetArt.Size.Square)
class TodayLookMiniReceiver : LookWidgetProvider(WidgetArt.Size.Mini)

private const val TAG = "LookWidget"

/** Rest slots after the landing: the same Bitmap object each time, so they cost views, not memory (~45 s at 90 ms). */
private const val REST = 500

internal object LookWidgets {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    private val providers = listOf(TodayColorWidgetReceiver::class.java, TodayLookSquareReceiver::class.java, TodayLookMiniReceiver::class.java)

    suspend fun updateAll(context: Context) {
        val m = AppWidgetManager.getInstance(context)
        update(context, providers.flatMap { m.getAppWidgetIds(ComponentName(context, it)).toList() }.toIntArray())
    }

    suspend fun update(context: Context, ids: IntArray) = lock.withLock {
        if (ids.isEmpty()) return@withLock
        val m = AppWidgetManager.getInstance(context)
        val data = load(context)
        for (id in ids) {
            val info = m.getAppWidgetInfo(id) ?: continue
            val default = when (info.provider.className) {
                TodayLookMiniReceiver::class.java.name -> WidgetArt.Size.Mini
                TodayLookSquareReceiver::class.java.name -> WidgetArt.Size.Square
                else -> WidgetArt.Size.Wide
            }
            val o = m.getAppWidgetOptions(id)
            // Portrait size: min width × max height (what One UI reports for the home grid).
            var wDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            var hDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            if (wDp <= 0 || hDp <= 0) when (default) {
                WidgetArt.Size.Mini -> { wDp = 80; hDp = 116 }
                WidgetArt.Size.Square -> { wDp = 170; hDp = 240 }
                WidgetArt.Size.Wide -> { wDp = 350; hDp = 240 }
            }
            val views = build(context, data, wDp, hDp)
            runCatching { m.updateAppWidget(id, views) }.onFailure { Log.e(TAG, "updateAppWidget $id", it) }
        }
    }

    fun sizeOf(wDp: Int, hDp: Int) = when {
        wDp < 120 && hDp < 150 -> WidgetArt.Size.Mini
        wDp >= 220 && wDp >= hDp * 1.3f -> WidgetArt.Size.Wide
        else -> WidgetArt.Size.Square
    }

    class Data(val tone: Tone?, val outfit: WidgetOutfit?, val liveTinty: Boolean, val night: Boolean, val darkText: Boolean = false)

    /** Debug only (WidgetSnapshotReceiver): sample looks on a real home screen. */
    @Volatile var debugOverride: Data? = null

    suspend fun load(context: Context): Data {
        debugOverride?.let { return it }
        val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val tone = entry.content().toneOrNull(entry.preferences().preferences.first().myToneId)?.takeIf { it.bestColors.isNotEmpty() }
        val outfit = if (tone == null) null else runCatching { entry.outfitSource().orElse(null)?.today() }.getOrNull()
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return Data(tone, outfit, ourLiveWallpaperOn(context), night, brightWallpaper(context))
    }

    /** The system says dark text reads better on this wallpaper (its colors hint, API 27+). */
    private fun brightWallpaper(context: Context) = android.os.Build.VERSION.SDK_INT >= 27 && runCatching {
        val colors = android.app.WallpaperManager.getInstance(context).getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM)
        colors != null && colors.colorHints and android.app.WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
    }.getOrDefault(false)

    /** One 틴티 per screen: our live wallpaper already shows one. */
    private fun ourLiveWallpaperOn(context: Context) =
        runCatching { android.app.WallpaperManager.getInstance(context).wallpaperInfo?.packageName == context.packageName }.getOrDefault(false)

    /** What [data] looks like in the widget; [night] overrides the system (debug snapshots). */
    fun look(context: Context, data: Data, night: Boolean = data.night): Pair<WidgetArt.Look, String> {
        val d = context.resources.displayMetrics.density
        val tone = data.tone
        val mark = if (data.liveTinty) null else tintyBitmap((15 * d).toInt(), tone?.tintyColors ?: TintyColors.Brand)
        val today = LocalDate.now().let { "${it.monthValue}/${it.dayOfMonth}" }
        val outfit = data.outfit
        if (tone == null) {
            return WidgetArt.Look(null, WidgetArt.Title("내 컬러부터", null, "찾아볼까요?", 0, sub = "탭해서 진단해요"), today, today, mark, night,
                lockLabel = "진단하러 가기", darkText = data.darkText) to "나의 컬러는? 탭해서 퍼스널 컬러를 진단해요"
        }
        if (outfit == null || outfit.locked) {
            return WidgetArt.Look(null, WidgetArt.Title("오늘의 룩 도착!", null, "", 0, sub = "탭하면 오늘 코디가 열려요"), today, "오늘의 룩", mark, night,
                lockLabel = if (outfit == null) "열어보기" else "광고 보고 열기", darkText = data.darkText) to "오늘의 코디가 도착했어요. 탭해서 열어보세요"
        }
        val (temp, cond) = weather(outfit.weatherLine)
        val pill = listOfNotNull(today, listOfNotNull(temp, cond).joinToString(" ").ifEmpty { null }).joinToString(" · ")
        val short = listOfNotNull(temp, cond).joinToString(" ").ifEmpty { today }
        val pieces = outfit.pieces.mapNotNull { (k, hex) -> runCatching { k to android.graphics.Color.parseColor(hex) }.getOrNull() }
        val title = titleOf(outfit.headline, tone, pieces)
        val names = outfit.pieces.joinToString(", ") { CLOTHING_KINDS[it.first]?.first ?: it.first }
        return WidgetArt.Look(pieces, title, pill, short, mark, night, darkText = data.darkText) to "오늘의 코디: ${outfit.headline}. $names"
    }

    /** "망원동 9°C 이슬비" → ("9°", "이슬비"). */
    private fun weather(line: String?): Pair<String?, String?> {
        line ?: return null to null
        val m = Regex("(-?\\d+)°C?\\s*(.*)").find(line) ?: return null to line.substringAfterLast(' ').ifBlank { null }
        return "${m.groupValues[1]}°" to m.groupValues[2].trim().ifBlank { null }
    }

    /**
     * Splits the headline around the point color's name, which goes on the marker: "비 오는 날 [와인] 포인트 룩".
     * The point color is the tone color named in the headline; without one, the title is just wrapped.
     */
    internal fun titleOf(headline: String, tone: Tone, pieces: List<Pair<String, Int>>): WidgetArt.Title {
        val h = headline.trim()
        val named = tone.bestColors.flatMap { c -> listOf(c.name, c.name.substringAfterLast(' ')).distinct().map { it to c.hex } }
            .filter { it.first.length >= 2 && it.first in h }.maxByOrNull { it.first.length }
        if (named != null) {
            val (key, hex) = named
            val i = h.indexOf(key)
            return WidgetArt.Title(h.substring(0, i).trim(), key, h.substring(i + key.length).trim(), hexColor(hex).toArgb())
        }
        // No color word: two even lines, the most colorful piece as the (unused) accent.
        val words = h.split(' ').filter { it.isNotBlank() }
        val cut = (1..words.size).minByOrNull { k -> kotlin.math.abs(words.take(k).joinToString(" ").length - h.length / 2) } ?: 0
        val accent = pieces.maxByOrNull { chroma(it.second) }?.second ?: 0
        return WidgetArt.Title(words.take(cut).joinToString(" "), null, words.drop(cut).joinToString(" "), accent)
    }

    private fun chroma(c: Int): Int {
        val r = android.graphics.Color.red(c); val g = android.graphics.Color.green(c); val b = android.graphics.Color.blue(c)
        return maxOf(r, g, b) - minOf(r, g, b)
    }

    fun render(context: Context, data: Data, wDp: Int, hDp: Int, night: Boolean = data.night): Pair<WidgetArt.Art, String> {
        val d = context.resources.displayMetrics.density
        if (!fontReady) {
            WidgetArt.font = ResourcesCompat.getFont(context, DsR.font.pretendard) ?: android.graphics.Typeface.DEFAULT_BOLD
            fontReady = true
        }
        val (look, description) = look(context, data, night)
        val size = sizeOf(wDp, hDp)
        return WidgetArt.render(look, size, (wDp * d).toInt(), (hDp * d).toInt(), d) to description
    }

    @Volatile private var fontReady = false

    private suspend fun build(context: Context, data: Data, wDp: Int, hDp: Int): RemoteViews {
        val t0 = System.currentTimeMillis()
        val (art, description) = withContext(Dispatchers.Default) { render(context, data, wDp, hDp) }
        val pkg = context.packageName
        val rv = RemoteViews(pkg, R.layout.widget_look)
        place(rv, R.id.still, art.still)
        rv.removeAllViews(R.id.frames)
        if (art.landing.isEmpty()) {
            place(rv, R.id.base, art.collage)
            rv.setViewVisibility(R.id.frames, View.GONE)
        } else {
            rv.setViewVisibility(R.id.base, View.GONE)
            rv.setViewVisibility(R.id.frames, View.VISIBLE)
            for (f in art.landing) rv.addView(R.id.frames, frame(pkg, f))
            val rest = RemoteViews(pkg, R.layout.widget_rest).also { place(it, R.id.p0, art.collage) }
            repeat(REST) { rv.addView(R.id.frames, rest) }
        }
        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            val intent = if (data.tone == null) launch else Intent(launch).putExtra("open", "coordi")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            rv.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(context, 7, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        }
        rv.setContentDescription(R.id.root, description)
        logMemory(context, art, wDp, hDp, System.currentTimeMillis() - t0)
        return rv
    }

    private val PIECES = intArrayOf(R.id.p0, R.id.p1, R.id.p2, R.id.p3, R.id.p4, R.id.p5, R.id.p6)

    private fun frame(pkg: String, f: WidgetArt.Frame): RemoteViews {
        val v = RemoteViews(pkg, R.layout.widget_frame)
        PIECES.forEachIndexed { i, id -> place(v, id, f.layers.getOrNull(i)) }
        place(v, R.id.shine, f.shine)
        return v
    }

    /** Matrix ImageView: the bitmap draws at its own size (half-density overlays draw 2x) at its padding offset. */
    private fun place(rv: RemoteViews, id: Int, p: WidgetArt.Placed?) {
        if (p == null) { rv.setViewVisibility(id, View.GONE); return }
        rv.setViewVisibility(id, View.VISIBLE)
        rv.setImageViewBitmap(id, p.bmp)
        rv.setViewPadding(id, p.x, p.y, 0, 0)
    }

    /** RemoteViews refuses bitmaps over ~1.5× the screen's ARGB size; log what one update holds. */
    private fun logMemory(context: Context, art: WidgetArt.Art, wDp: Int, hDp: Int, ms: Long) {
        val seen = IdentityHashMap<Bitmap, Unit>()
        listOfNotNull(art.still, art.collage).forEach { seen[it.bmp] = Unit }
        art.landing.forEach { f -> f.layers.forEach { seen[it.bmp] = Unit }; f.shine?.let { seen[it.bmp] = Unit } }
        val bytes = seen.keys.sumOf { it.byteCount.toLong() }
        val dm = context.resources.displayMetrics
        val limit = dm.widthPixels.toLong() * dm.heightPixels * 4 * 3 / 2
        Log.i(TAG, "${wDp}x${hDp}dp ${sizeOf(wDp, hDp)}: ${art.landing.size} frames, ${seen.size} bitmaps, " +
            "${bytes / 1024} KB of ${limit / 1024} KB limit, rendered in $ms ms")
    }
}
