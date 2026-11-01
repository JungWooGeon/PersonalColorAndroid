package com.gun.personalcolor.feature.coordi

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gun.personalcolor.core.model.UserPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Launcher-intent extra the notification sets; MainActivity opens the daily outfit when it sees it. */
object CoordiDeepLink {
    const val EXTRA_OPEN = "open"
    const val VALUE = "coordi"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface CoordiEntryPoint {
    fun provider(): TodayCoordiProvider
}

/** Posts "오늘의 컬러·코디" once a day. */
class DailyNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val summary = runCatching {
            EntryPointAccessors.fromApplication(ctx, CoordiEntryPoint::class.java).provider().summary()
        }.getOrNull()
        val (short, long) = if (summary == null) {
            "오늘 나에게 어울리는 컬러와 코디를 확인해 보세요." to null
        } else {
            val weather = summary.weatherLine?.let { "$it · " }.orEmpty()
            "오늘의 컬러는 ${summary.todayColorName} · ${summary.outfitHeadline}" to
                "오늘의 컬러는 '${summary.todayColorName}'이에요.\n$weather${summary.outfitHeadline} 어때요?"
        }

        val manager = ctx.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "오늘의 컬러 알림", NotificationManager.IMPORTANCE_DEFAULT))
        val open = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            ?.putExtra(CoordiDeepLink.EXTRA_OPEN, CoordiDeepLink.VALUE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val builder = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_coordi)
            .setContentTitle("오늘의 컬러·코디")
            .setContentText(short)
            .setAutoCancel(true)
        long?.let { builder.setStyle(NotificationCompat.BigTextStyle().bigText(it)) }
        summary?.let { runCatching { builder.setColor(android.graphics.Color.parseColor(it.todayColorHex)) } }
        open?.let { builder.setContentIntent(PendingIntent.getActivity(ctx, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)) }
        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) { /* permission revoked meanwhile */ }
        return Result.success()
    }

    private companion object {
        const val CHANNEL = "daily"
        const val NOTIFICATION_ID = 1001
    }
}

object DailyNotificationScheduler {
    private const val WORK = "daily_coordi_notification"

    /** Every 24h starting at the next [hour]:[minute]. Re-enqueues so a changed time takes effect. */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour, minute)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<DailyNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        // Not UPDATE: for periodic work that already ran, UPDATE keeps the old run time and ignores the new initial delay.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }

    fun sync(context: Context, prefs: UserPreferences) =
        if (prefs.dailyNotification) schedule(context, prefs.notificationHour, prefs.notificationMinute) else cancel(context)
}

/**
 * Keeps the scheduled notification in line with the saved setting (the settings screen only writes preferences).
 * Call once from the app shell with the current preferences.
 */
@Composable
fun DailyNotificationSync(prefs: UserPreferences) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(prefs.dailyNotification, prefs.notificationHour, prefs.notificationMinute) {
        runCatching { DailyNotificationScheduler.sync(context, prefs) }
    }
}
