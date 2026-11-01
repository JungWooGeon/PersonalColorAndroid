package com.gun.personalcolor.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.gun.personalcolor.core.model.ThemeMode
import com.gun.personalcolor.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(@SettingsStore private val store: DataStore<Preferences>) {
    val preferences: Flow<UserPreferences> = store.data.map { p ->
        UserPreferences(
            themeMode = p[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            onboardingDone = p[ONBOARDING] ?: false,
            saveHistory = p[SAVE_HISTORY] ?: true,
            myToneId = p[MY_TONE],
            favoriteToneIds = p[FAVORITES] ?: emptySet(),
            showPhotoGuide = p[PHOTO_GUIDE] ?: true,
            dailyNotification = p[NOTIFY] ?: false,
            notificationHour = p[NOTIFY_HOUR] ?: 8,
            notificationMinute = p[NOTIFY_MINUTE] ?: 0,
            coordiTrialStartMillis = p[TRIAL_START],
            autoWallpaper = p[AUTO_WALLPAPER] ?: false,
            autoWallpaperStyle = p[AUTO_WALLPAPER_STYLE] ?: "GRADIENT",
            autoWallpaperTarget = p[AUTO_WALLPAPER_TARGET] ?: "BOTH",
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setOnboardingDone(done: Boolean) = store.edit { it[ONBOARDING] = done }
    suspend fun setSaveHistory(save: Boolean) = store.edit { it[SAVE_HISTORY] = save }
    suspend fun setShowPhotoGuide(show: Boolean) = store.edit { it[PHOTO_GUIDE] = show }
    suspend fun setMyTone(id: String?) = store.edit { if (id == null) it.remove(MY_TONE) else it[MY_TONE] = id }

    suspend fun setDailyNotification(enabled: Boolean, hour: Int, minute: Int) = store.edit {
        it[NOTIFY] = enabled; it[NOTIFY_HOUR] = hour; it[NOTIFY_MINUTE] = minute
    }
    /** Starts the outfit trial once; later calls keep the original start. */
    suspend fun startCoordiTrialIfNeeded(now: Long) = store.edit { if (it[TRIAL_START] == null) it[TRIAL_START] = now }
    suspend fun setAutoWallpaper(enabled: Boolean, style: String, target: String) = store.edit {
        it[AUTO_WALLPAPER] = enabled; it[AUTO_WALLPAPER_STYLE] = style; it[AUTO_WALLPAPER_TARGET] = target
    }

    suspend fun toggleFavorite(id: String) = store.edit {
        val now = it[FAVORITES] ?: emptySet()
        it[FAVORITES] = if (id in now) now - id else now + id
    }

    /** "모든 데이터 초기화": preferences back to defaults, but keep onboarding done and the trial start (no trial reset abuse). */
    suspend fun reset() = store.edit { p ->
        val trial = p[TRIAL_START]
        p.clear(); p[ONBOARDING] = true
        if (trial != null) p[TRIAL_START] = trial
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val PHOTO_GUIDE = booleanPreferencesKey("show_photo_guide")
        val MY_TONE = stringPreferencesKey("my_tone")
        val FAVORITES = stringSetPreferencesKey("favorite_tones")
        val NOTIFY = booleanPreferencesKey("daily_notification")
        val NOTIFY_HOUR = intPreferencesKey("notification_hour")
        val NOTIFY_MINUTE = intPreferencesKey("notification_minute")
        val TRIAL_START = longPreferencesKey("coordi_trial_start")
        val AUTO_WALLPAPER = booleanPreferencesKey("auto_wallpaper")
        val AUTO_WALLPAPER_STYLE = stringPreferencesKey("auto_wallpaper_style")
        val AUTO_WALLPAPER_TARGET = stringPreferencesKey("auto_wallpaper_target")
    }
}
