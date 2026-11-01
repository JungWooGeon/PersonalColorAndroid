package com.gun.personalcolor.core.model

import kotlinx.serialization.Serializable

@Serializable enum class ResultSource { AI, QUIZ }

/** Only the result is stored on device — never the photo. */
@Serializable data class HistoryEntry(
    val toneId: String,
    val source: ResultSource,
    val timeMillis: Long,
    val top: List<TonePercent> = emptyList(),   // top-3
)

@Serializable data class TonePercent(val toneId: String, val probability: Double)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingDone: Boolean = false,
    /** Save each result to 내 기록. */
    val saveHistory: Boolean = true,
    /** Tone the user picked as "mine" (from a result or manually). Drives home, color check, etc. */
    val myToneId: String? = null,
    val favoriteToneIds: Set<String> = emptySet(),
    /** Show the camera/photo tips before every analysis. */
    val showPhotoGuide: Boolean = true,
    /** Daily "오늘의 컬러·코디" notification and its local time. */
    val dailyNotification: Boolean = false,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0,
    /** First time the daily outfit was opened; the free trial runs [COORDI_TRIAL_DAYS] days from here. */
    val coordiTrialStartMillis: Long? = null,
    /** Daily auto wallpaper from today's color. */
    val autoWallpaper: Boolean = false,
    val autoWallpaperStyle: String = "GRADIENT",
    /** "HOME" | "LOCK" | "BOTH" */
    val autoWallpaperTarget: String = "BOTH",
)

const val COORDI_TRIAL_DAYS = 7
