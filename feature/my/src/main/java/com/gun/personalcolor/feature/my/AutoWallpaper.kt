package com.gun.personalcolor.feature.my

import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** "HOME" | "LOCK" | "BOTH", stored in UserPreferences.autoWallpaperTarget. */
enum class WallpaperTarget(val label: String, val flags: Int) {
    HOME("홈 화면", WallpaperManager.FLAG_SYSTEM),
    LOCK("잠금 화면", WallpaperManager.FLAG_LOCK),
    BOTH("둘 다", WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}

/** Daily "today's color" wallpaper, shortly after midnight. WorkManager persists it across reboots. */
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

    /** Renders today's color wallpaper at full screen size and sets it. False if unsupported or it failed. */
    internal suspend fun apply(context: Context, tone: Tone, style: WallpaperRenderer.Style, target: WallpaperTarget, dark: Boolean): Boolean =
        withContext(Dispatchers.Default) {
            val wm = WallpaperManager.getInstance(context)
            if (!canSetWallpaper(context)) return@withContext false
            try {
                val (w, h) = screenSizePx(context)
                val bmp = WallpaperRenderer.render(tone, style, w, h, dark, todayColor = WallpaperRenderer.todayColor(tone))
                wm.setBitmap(bmp, null, true, target.flags)
                bmp.recycle()
                true
            } catch (e: Exception) {
                false
            }
        }
}

internal fun canSetWallpaper(context: Context): Boolean =
    WallpaperManager.getInstance(context).let { it.isWallpaperSupported && it.isSetWallpaperAllowed }

/** Physical screen size in px, portrait. */
internal fun screenSizePx(context: Context): Pair<Int, Int> {
    val (w, h) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds.let { it.width() to it.height() }
    } else {
        @Suppress("DEPRECATION")
        DisplayMetrics().also { context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY).getRealMetrics(it) }
            .let { it.widthPixels to it.heightPixels }
    }
    return minOf(w, h) to maxOf(w, h)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutoWallpaperEntryPoint {
    fun preferences(): UserPreferencesRepository
    fun content(): ContentRepository
}

/** Light: reads prefs, draws one bitmap, sets it. No network. */
class AutoWallpaperWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, AutoWallpaperEntryPoint::class.java)
        val prefs = entry.preferences().preferences.first()
        val tone = entry.content().toneOrNull(prefs.myToneId)
        if (!prefs.autoWallpaper || tone == null) return Result.success()
        val style = WallpaperRenderer.Style.entries.firstOrNull { it.name == prefs.autoWallpaperStyle } ?: WallpaperRenderer.Style.TODAY
        val target = WallpaperTarget.entries.firstOrNull { it.name == prefs.autoWallpaperTarget } ?: WallpaperTarget.BOTH
        val night = applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val ok = AutoWallpaperScheduler.apply(applicationContext, tone, style, target, night)
        return if (ok) Result.success() else Result.failure()
    }
}
