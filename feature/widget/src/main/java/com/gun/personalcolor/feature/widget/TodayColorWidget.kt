package com.gun.personalcolor.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.mood.MoodCopy
import com.gun.personalcolor.core.ui.mood.SceneBed
import com.gun.personalcolor.core.ui.mood.SceneWall
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Optional

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
abstract class LookWidgetProvider internal constructor(internal val defaultSize: WidgetSize) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = async { LookWidgets.update(context, ids) }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) =
        async { LookWidgets.update(context, intArrayOf(id)) }

    private fun async(block: suspend () -> Unit) {
        val pending = goAsync()
        LookWidgets.scope.launch { try { block() } catch (e: Exception) { Log.e(TAG, "update failed", e) } finally { pending.finish() } }
    }
}

/** 4x2 (kept under its old name so widgets already on home screens carry over). */
class TodayColorWidgetReceiver : LookWidgetProvider(WidgetSize.Wide)
class TodayLookSquareReceiver : LookWidgetProvider(WidgetSize.Square)
class TodayLookMiniReceiver : LookWidgetProvider(WidgetSize.Mini)

/** How much of the scene background covers the wallpaper; garments and text stay solid. */
private const val SCENE_ALPHA = 0.55f

private const val TAG = "LookWidget"

internal enum class WidgetSize { Mini, Square, Wide }

/** What a widget shows. [pieces] null = no outfit; [pill] is the quiet call to action (locked, no tone). */
internal class WidgetLook(
    val pieces: List<Pair<String, Int>>?,
    val weather: String,
    val line: String,
    val caption: String,
    val miniCaption: String,
    val locked: Boolean,
    val pill: String? = null,
)

/** Light phases cross-fade in this order (one breath = 0→1→2→1→0), twice, then the still swaps in. */
private val PHASES = intArrayOf(0, 1, 2, 1, 0, 1, 2, 1, 0)
/** ViewFlipper flipInterval in res/layout/widget_look.xml (the fades in res/anim are 1.6 s). */
private const val FLIP_MS = 2400L

internal object LookWidgets {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    private val providers = listOf(TodayColorWidgetReceiver::class.java, TodayLookSquareReceiver::class.java, TodayLookMiniReceiver::class.java)
    /** Bumped per update, so a late still swap never overwrites a newer update. */
    @Volatile private var generation = 0

    suspend fun updateAll(context: Context) {
        val m = AppWidgetManager.getInstance(context)
        update(context, providers.flatMap { m.getAppWidgetIds(ComponentName(context, it)).toList() }.toIntArray())
    }

    suspend fun update(context: Context, ids: IntArray) = lock.withLock {
        if (ids.isEmpty()) return@withLock
        val m = AppWidgetManager.getInstance(context)
        val data = load(context)
        val gen = ++generation
        val stills = ArrayList<Pair<Int, RemoteViews>>()
        for (id in ids) {
            val info = m.getAppWidgetInfo(id) ?: continue
            val default = when (info.provider.className) {
                TodayLookMiniReceiver::class.java.name -> WidgetSize.Mini
                TodayLookSquareReceiver::class.java.name -> WidgetSize.Square
                else -> WidgetSize.Wide
            }
            val o = m.getAppWidgetOptions(id)
            // Portrait size: min width × max height (what One UI reports for the home grid).
            var wDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            var hDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            if (wDp <= 0 || hDp <= 0) when (default) {
                WidgetSize.Mini -> { wDp = 80; hDp = 116 }
                WidgetSize.Square -> { wDp = 169; hDp = 242 }
                WidgetSize.Wide -> { wDp = 348; hDp = 242 }
            }
            val (views, still) = build(context, data, wDp, hDp)
            runCatching { m.updateAppWidget(id, views) }.onFailure { Log.e(TAG, "updateAppWidget $id", it) }
            stills += id to still
        }
        // After the light has breathed, a still frame stops the flipper (no idle redraws). Not awaited: the
        // broadcast finishes now; if the process dies first, the flipper simply keeps breathing.
        scope.launch {
            delay((PHASES.size - 1) * FLIP_MS + 800)
            if (gen == generation) for ((id, v) in stills) runCatching { m.updateAppWidget(id, v) }
        }
    }

    fun sizeOf(wDp: Int, hDp: Int) = when {
        wDp < 120 && hDp < 150 -> WidgetSize.Mini
        wDp >= 220 && wDp >= hDp * 1.3f -> WidgetSize.Wide
        else -> WidgetSize.Square
    }

    /** [night] forces the night scene (debug); otherwise the scene follows the hour. [hour] -1 = now. */
    class Data(val tone: Tone?, val outfit: WidgetOutfit?, val liveTinty: Boolean, val night: Boolean, val darkText: Boolean = false, val hour: Int = -1)

    /** Debug only (WidgetSnapshotReceiver): sample looks on a real home screen. */
    @Volatile var debugOverride: Data? = null

    suspend fun load(context: Context): Data {
        debugOverride?.let { return it }
        val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val tone = entry.content().toneOrNull(entry.preferences().preferences.first().myToneId)?.takeIf { it.bestColors.isNotEmpty() }
        val outfit = if (tone == null) null else runCatching { entry.outfitSource().orElse(null)?.today() }.getOrNull()
        return Data(tone, outfit, ourLiveWallpaperOn(context), false, brightWallpaper(context))
    }

    /** The system says dark text reads better on this wallpaper (its colors hint, API 27+). */
    private fun brightWallpaper(context: Context) = android.os.Build.VERSION.SDK_INT >= 27 && runCatching {
        val colors = android.app.WallpaperManager.getInstance(context).getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM)
        colors != null && colors.colorHints and android.app.WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
    }.getOrDefault(false)

    /** One 틴티 per screen: our live wallpaper already shows one. */
    private fun ourLiveWallpaperOn(context: Context) =
        runCatching { android.app.WallpaperManager.getInstance(context).wallpaperInfo?.packageName == context.packageName }.getOrDefault(false)

    /** "영등포구 9°C 이슬비" + hour → sunny|cloudy|rain|night (MoodCopy.sky). */
    internal fun skyOf(weatherLine: String?, hour: Int): String = MoodCopy.sky(weather(weatherLine).second, hour)

    /** What [data] looks like in the widget, and its contentDescription. */
    fun look(data: Data): Pair<WidgetLook, String> {
        val now = java.time.LocalDateTime.now()
        val hour = if (data.night) 23 else data.hour.takeIf { it >= 0 } ?: now.hour
        val date = "${java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.ENGLISH).format(now)}"
        val tone = data.tone
        val outfit = data.outfit
        if (tone == null) {
            val sky = skyOf(null, hour)
            return WidgetLook(null, sky, "내 컬러부터 찾아볼까요?", "$date · find your color", "my color",
                locked = false, pill = "진단하러 가기") to "나의 컬러는? 탭해서 퍼스널 컬러를 진단해요"
        }
        if (outfit == null || outfit.locked) {
            // Never the real outfit here: the scene shows generic pieces under a veil.
            val sky = skyOf(null, hour)
            return WidgetLook(null, sky, "오늘의 룩이 도착했어요", "$date · today's look", "today's look", locked = true,
                pill = if (outfit == null) "열어보기" else "광고 보고 열기") to "오늘의 코디가 도착했어요. 탭해서 열어보세요"
        }
        val (tempS, cond) = weather(outfit.weatherLine)
        val temp = tempS?.removeSuffix("°")?.toIntOrNull()
        val sky = skyOf(outfit.weatherLine, hour)
        val pieces = outfit.pieces.mapNotNull { (k, hex) -> runCatching { k to android.graphics.Color.parseColor(hex) }.getOrNull() }
        val line = MoodCopy.line(sky, cond, temp, MoodCopy.accent(tone, pieces))
        val caption = MoodCopy.caption(sky, cond, temp, now)
        val mini = listOfNotNull(tempS, when (sky) { "sunny" -> "sun"; "rain" -> if (cond.orEmpty().contains("눈")) "snow" else "rain"; else -> sky }).joinToString(" · ")
        val names = outfit.pieces.joinToString(", ") { CLOTHING_KINDS[it.first]?.first ?: it.first }
        return WidgetLook(pieces, sky, line, caption, mini, locked = false) to "오늘의 코디: $line. ${outfit.headline}. $names"
    }

    /** "망원동 9°C 이슬비" → ("9°", "이슬비"). */
    private fun weather(line: String?): Pair<String?, String?> {
        line ?: return null to null
        val m = Regex("(-?\\d+)°C?\\s*(.*)").find(line) ?: return null to line.substringAfterLast(' ').ifBlank { null }
        return "${m.groupValues[1]}°" to m.groupValues[2].trim().ifBlank { null }
    }


    /** The widget at [wDp]×[hDp] for light [phase]. */
    fun render(context: Context, look: WidgetLook, wDp: Int, hDp: Int, phase: Int): Bitmap {
        val d = context.resources.displayMetrics.density
        val w = (wDp * d).toInt(); val h = (hDp * d).toInt()
        return when (val size = sizeOf(wDp, hDp)) {
            WidgetSize.Wide -> SceneWall.render(context, if (look.locked) emptyList() else look.pieces.orEmpty(),
                look.weather, look.line, look.caption, w, h, phase, look.locked, bgAlpha = SCENE_ALPHA)
            else -> {
                val mini = size == WidgetSize.Mini
                SceneBed.render(context, if (look.locked) emptyList() else look.pieces.orEmpty(), look.weather, look.line,
                    if (mini) look.miniCaption else look.caption, w, h, phase, look.locked, mini, look.pill, bgAlpha = SCENE_ALPHA)
            }
        }
    }

    /** (views that breathe now, the still to swap in after). */
    private suspend fun build(context: Context, data: Data, wDp: Int, hDp: Int): Pair<RemoteViews, RemoteViews> {
        val t0 = System.currentTimeMillis()
        val (look, description) = look(data)
        val frames = withContext(Dispatchers.Default) { (0..2).map { render(context, look, wDp, hDp, it) } }
        val rv = shell(context, data, description, frames)
        val still = shell(context, data, description, listOf(frames[0]))
        logMemory(context, frames, wDp, hDp, System.currentTimeMillis() - t0)
        return rv to still
    }

    private fun shell(context: Context, data: Data, description: String, frames: List<Bitmap>): RemoteViews {
        val pkg = context.packageName
        val rv = RemoteViews(pkg, R.layout.widget_look)
        rv.removeAllViews(R.id.frames)
        if (frames.size == 1) {
            rv.setImageViewBitmap(R.id.base, frames[0])
            rv.setViewVisibility(R.id.base, View.VISIBLE)
            rv.setViewVisibility(R.id.frames, View.GONE)
        } else {
            rv.setViewVisibility(R.id.base, View.GONE)
            rv.setViewVisibility(R.id.frames, View.VISIBLE)
            // Same Bitmap objects repeat: RemoteViews' bitmap cache sends each once.
            for (p in PHASES) rv.addView(R.id.frames, RemoteViews(pkg, R.layout.widget_rest).apply { setImageViewBitmap(R.id.p0, frames[p]) })
        }
        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            val intent = if (data.tone == null) launch else Intent(launch).putExtra("open", "coordi")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            rv.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(context, 7, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        }
        rv.setContentDescription(R.id.root, description)
        return rv
    }

    /** RemoteViews refuses bitmaps over ~1.5× the screen's ARGB size; log what one update holds. */
    private fun logMemory(context: Context, frames: List<Bitmap>, wDp: Int, hDp: Int, ms: Long) {
        val bytes = frames.sumOf { it.byteCount.toLong() }
        val dm = context.resources.displayMetrics
        val limit = dm.widthPixels.toLong() * dm.heightPixels * 4 * 3 / 2
        Log.i(TAG, "${wDp}x${hDp}dp ${sizeOf(wDp, hDp)}: ${frames.size} frames ${frames[0].width}x${frames[0].height}, " +
            "${bytes / 1024} KB of ${limit / 1024} KB limit, rendered in $ms ms")
    }
}
