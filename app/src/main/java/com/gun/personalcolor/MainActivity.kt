package com.gun.personalcolor

import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.gun.personalcolor.feature.widget.TodayColorWidgets
import com.gun.personalcolor.feature.coordi.DailyNotificationSync
import com.gun.personalcolor.feature.my.AutoWallpaperScheduler
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.ads.AdManager
import com.gun.personalcolor.core.ads.LocalAdManager
import com.gun.personalcolor.core.analytics.Analytics
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.model.ThemeMode
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.feature.coordi.CoordiRoute
import com.gun.personalcolor.feature.tones.ToneRoute
import com.gun.personalcolor.navigation.PcApp
import kotlinx.coroutines.flow.MutableStateFlow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var ads: AdManager
    @Inject lateinit var analytics: Analytics
    @Inject lateinit var content: ContentRepository
    /** Route requested by a deep link / launcher extra, consumed by PcApp once the nav host exists. */
    private val pendingRoute = MutableStateFlow<Any?>(null)
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { vm.prefs.value == null }
        enableEdgeToEdge()
        ads.gatherConsent(this)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            val p = prefs ?: return@setContent
            val dark = when (p.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Home-screen widget shows today's color of "my tone": refresh when it changes.
            LaunchedEffect(p.myToneId) { runCatching { TodayColorWidgets.refresh(applicationContext) } }
            // Keeps the daily notification schedule in sync with the settings switch/time.
            DailyNotificationSync(p)
            // Auto wallpaper turned off (incl. "모든 데이터 초기화") → drop its scheduled job too.
            LaunchedEffect(p.autoWallpaper) { if (!p.autoWallpaper) AutoWallpaperScheduler.cancel(applicationContext) }
            // System bar icons follow the app theme, not just the system setting.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            CompositionLocalProvider(LocalAdManager provides ads, LocalAnalytics provides analytics) {
                PcTheme(darkTheme = dark) {
                    val route by pendingRoute.collectAsStateWithLifecycle()
                    PcApp(showOnboarding = !p.onboardingDone, pendingRoute = route, onRouteHandled = { pendingRoute.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val uri = intent.data
        pendingRoute.value = when (val link = parseDeepLink(
            openExtra = intent.getStringExtra("open"), scheme = uri?.scheme, host = uri?.host, path = uri?.path,
            toneParam = uri?.let { if (it.isHierarchical) it.getQueryParameter("tone") else null },
            isValidTone = { content.toneOrNull(it) != null },
        )) {
            DeepLink.Coordi -> CoordiRoute
            is DeepLink.ToneLink -> ToneRoute(link.id)
            null -> return
        }
    }
}
