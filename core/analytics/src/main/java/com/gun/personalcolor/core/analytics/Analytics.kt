package com.gun.personalcolor.core.analytics

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App event log. No analytics backend (Firebase was removed on purpose): events only go to Logcat, which helps
 * debugging. Call sites stay in place, so a backend can be plugged in here later without touching the screens.
 */
@Singleton
class Analytics @Inject constructor() {
    fun log(event: String, vararg params: Pair<String, String>) {
        Log.d(TAG, if (params.isEmpty()) event else "$event ${params.joinToString { "${it.first}=${it.second}" }}")
    }

    fun error(e: Throwable) {
        Log.e(TAG, "error", e)
    }

    fun screen(name: String) = log("screen_view", "screen" to name)

    private companion object { const val TAG = "PersonalColor" }
}

/** Provided by MainActivity. Null in previews. */
val LocalAnalytics = staticCompositionLocalOf<Analytics?> { null }

/** Logs a screen view once per entry into the composition. */
@Composable
fun TrackScreen(name: String) {
    val analytics = LocalAnalytics.current
    LaunchedEffect(name) { analytics?.screen(name) }
}
