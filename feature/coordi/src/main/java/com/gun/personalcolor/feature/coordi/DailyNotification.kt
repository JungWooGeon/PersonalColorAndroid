package com.gun.personalcolor.feature.coordi

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDateTime

/** Launcher-intent extra the notification sets; MainActivity opens the daily outfit when it sees it. */
object CoordiDeepLink {
    const val EXTRA_OPEN = "open"
    const val VALUE = "coordi"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface CoordiEntryPoint {
    fun provider(): TodayCoordiProvider
    fun prefs(): UserPreferencesRepository
}

/**
 * Daily "오늘의 컬러·코디" notification via AlarmManager (exact when allowed, else setAndAllowWhileIdle): fires near the set time even in Doze,
 * unlike WorkManager periodic work which OEMs (e.g. Samsung) may hold back for a long time. Never asks the user for the exact-alarm permission.
 * Each alarm posts the notification and schedules the next day; [DailyNotificationReceiver] also restores it after reboot.
 */
class DailyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val entry = EntryPointAccessors.fromApplication(app, CoordiEntryPoint::class.java)
                val prefs = entry.prefs().preferences.first()
                if (intent.action == ACTION_FIRE && prefs.dailyNotification) post(app, entry.provider())
                DailyNotificationScheduler.sync(app, prefs)   // next day (or restore after boot / app update)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun post(ctx: Context, provider: TodayCoordiProvider) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val summary = runCatching { provider.summary() }.getOrNull()
        val (short, long) = notificationText(summary)
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
    }

    companion object {
        internal const val ACTION_FIRE = "com.gun.personalcolor.action.DAILY_NOTIFICATION"
        private const val CHANNEL = "daily"
        private const val NOTIFICATION_ID = 1001
    }
}

/** (short line, expanded text). Fallback copy when there's no tone/summary yet. */
internal fun notificationText(summary: CoordiSummary?): Pair<String, String?> =
    if (summary == null) {
        "오늘 나에게 어울리는 컬러와 코디를 확인해 보세요." to null
    } else {
        val weather = summary.weatherLine?.let { "$it · " }.orEmpty()
        "오늘의 컬러는 ${summary.todayColorName} · ${summary.outfitHeadline}" to
            "오늘의 컬러는 '${summary.todayColorName}'이에요.\n$weather${summary.outfitHeadline} 어때요?"
    }

/** Next local time [hour]:[minute] strictly after [now] (today if still ahead, else tomorrow). */
internal fun nextTrigger(now: LocalDateTime, hour: Int, minute: Int): LocalDateTime {
    val today = now.toLocalDate().atTime(hour, minute)
    return if (today.isAfter(now)) today else today.plusDays(1)
}

object DailyNotificationScheduler {
    private const val LEGACY_WORK = "daily_coordi_notification"

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, DailyNotificationReceiver::class.java).setAction(DailyNotificationReceiver.ACTION_FIRE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Next alarm at [hour]:[minute] local time; replaces any previous one. */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val at = nextTrigger(LocalDateTime.now(), hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val alarms = context.getSystemService(AlarmManager::class.java)
        // Exact when the OS allows it (pre-granted up to Android 13); otherwise inexact (may run up to ~1h late).
        val exactOk = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
        if (exactOk) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context))
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context))
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK)   // drop the old WorkManager-based schedule
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK)
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
