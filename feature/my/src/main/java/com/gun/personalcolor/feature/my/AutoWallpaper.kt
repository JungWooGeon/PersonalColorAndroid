package com.gun.personalcolor.feature.my

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.WallpaperRenderer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** "HOME" | "LOCK" | "BOTH", stored in UserPreferences.autoWallpaperTarget. */
enum class WallpaperTarget(val label: String, val short: String, val flags: Int) {
    HOME("홈 화면", "홈", WallpaperManager.FLAG_SYSTEM),
    LOCK("잠금 화면", "잠금", WallpaperManager.FLAG_LOCK),
    BOTH("홈과 잠금 화면 모두", "둘 다", WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}

/** Daily wallpaper in the chosen style with that day's color, shortly after midnight. WorkManager persists it across reboots. */
object AutoWallpaperScheduler {
    private const val WORK = "auto_wallpaper_daily"

    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        val next = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 5))
        val request = PeriodicWorkRequestBuilder<AutoWallpaperWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }

    /** Renders today's wallpaper at full screen size and sets it. False if unsupported or it failed. */
    internal suspend fun apply(context: Context, tone: Tone, style: WallpaperRenderer.Style, dark: Boolean, target: WallpaperTarget): Boolean =
        setWallpaper(context, target) { w, h -> WallpaperRenderer.render(tone, style, w, h, dark) }
}

/** Stored in UserPreferences.autoWallpaperStyle: "GLOW" or "GLOW_DARK". */
internal fun encodeStyle(style: WallpaperRenderer.Style, dark: Boolean) =
    if (dark && style.supportsDark) "${style.name}_DARK" else style.name

internal fun decodeStyle(value: String): Pair<WallpaperRenderer.Style, Boolean> =
    (WallpaperRenderer.Style.entries.firstOrNull { it.name == value.removeSuffix("_DARK") } ?: WallpaperRenderer.Style.TINTY) to
        value.endsWith("_DARK")

/** Renders at full screen size off the main thread and sets it. False if unsupported or it failed. */
internal suspend fun setWallpaper(context: Context, target: WallpaperTarget, render: (w: Int, h: Int) -> Bitmap): Boolean =
    withContext(Dispatchers.Default) {
        if (!canSetWallpaper(context)) return@withContext false
        var bmp: Bitmap? = null
        try {
            val (w, h) = screenSizePx(context)
            bmp = render(w, h)
            WallpaperManager.getInstance(context).setBitmap(bmp, null, true, target.flags)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) { // incl. OutOfMemoryError for a full-screen bitmap on low-memory phones
            Log.w("PcWallpaper", "set failed", e)
            false
        } finally {
            bmp?.recycle()
        }
    }

internal fun canSetWallpaper(context: Context): Boolean =
    runCatching { WallpaperManager.getInstance(context).let { it.isWallpaperSupported && it.isSetWallpaperAllowed } }.getOrDefault(false)

/** Physical screen size in px, portrait. */
internal fun screenSizePx(context: Context): Pair<Int, Int> {
    val (w, h) = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds.let { it.width() to it.height() }
        } else {
            @Suppress("DEPRECATION")
            DisplayMetrics().also { context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY).getRealMetrics(it) }
                .let { it.widthPixels to it.heightPixels }
        }
    }.getOrNull()?.takeIf { (w, h) -> w > 0 && h > 0 }
        ?: context.resources.displayMetrics.let { it.widthPixels to it.heightPixels }
    return portrait(w, h)
}

/** Portrait (short, long), never 0: the preview's aspect ratio and bitmap sizes divide by these. */
internal fun portrait(w: Int, h: Int): Pair<Int, Int> {
    val a = w.coerceAtLeast(1); val b = h.coerceAtLeast(1)
    return minOf(a, b) to maxOf(a, b)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutoWallpaperEntryPoint {
    fun preferences(): UserPreferencesRepository
    fun content(): ContentRepository
}

/** Light: reads prefs, draws one bitmap, sets it. No network. With our live wallpaper on, it only refreshes the lock screen still. */
class AutoWallpaperWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (isLiveWallpaperActive(applicationContext)) {
            val choice = LiveWallpaperPrefs.read(applicationContext)
            val tone = liveTone(applicationContext, choice) ?: return Result.success()
            return if (setLiveLockFrame(applicationContext, tone, choice)) Result.success() else Result.failure()
        }
        val entry = EntryPointAccessors.fromApplication(applicationContext, AutoWallpaperEntryPoint::class.java)
        val prefs = entry.preferences().preferences.first()
        val tone = entry.content().toneOrNull(prefs.myToneId)
        if (!prefs.autoWallpaper || tone == null) return Result.success()
        val (style, dark) = decodeStyle(prefs.autoWallpaperStyle)
        val target = WallpaperTarget.entries.firstOrNull { it.name == prefs.autoWallpaperTarget } ?: WallpaperTarget.BOTH
        val ok = AutoWallpaperScheduler.apply(applicationContext, tone, style, dark, target)
        return if (ok) Result.success() else Result.failure()
    }
}
