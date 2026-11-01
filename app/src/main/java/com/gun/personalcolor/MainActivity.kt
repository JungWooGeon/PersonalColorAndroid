package com.gun.personalcolor

import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
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
        // Let 틴티's drop-in (≈0.9s) land before handing over, unless the app was already warm.
        val shownAt = android.os.SystemClock.uptimeMillis()
        val cold = savedInstanceState == null
        splash.setKeepOnScreenCondition { vm.prefs.value == null || (cold && android.os.SystemClock.uptimeMillis() - shownAt < 850) }
        // Hand-off: the icon lifts and fades while home's 틴티 pops in underneath.
        splash.setOnExitAnimationListener { view ->
            view.iconView.animate().translationY(-24f * resources.displayMetrics.density).scaleX(1.1f).scaleY(1.1f).alpha(0f)
                .setDuration(260).setInterpolator(android.view.animation.AccelerateInterpolator()).start()
            val fade = android.animation.ObjectAnimator.ofFloat(view.view, android.view.View.ALPHA, 1f, 0f).apply { startDelay = 60; duration = 240 }
            var removed = false
            val remove = { if (!removed) { removed = true; view.remove() } }
            fade.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) = remove()
            })
            fade.start()
            view.view.postDelayed({ remove() }, 700) // never leave the splash up, whatever happens to the animator
        }
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

    /** Leaving the app (e.g. after watching an ad or buying premium): bring the home-screen widget up to date. */
    override fun onStop() {
        super.onStop()
        lifecycleScope.launch { runCatching { TodayColorWidgets.refresh(applicationContext) } }
    }
}
