package com.gun.personalcolor.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.gun.personalcolor.feature.widget.TodayColorWidgets
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.gun.personalcolor.feature.analyze.AnalyzeRoute
import com.gun.personalcolor.feature.analyze.analyzeScreen
import com.gun.personalcolor.feature.colormatch.ClosetRoute
import com.gun.personalcolor.feature.coordi.CoordiRoute
import com.gun.personalcolor.feature.coordi.coordiScreen
import com.gun.personalcolor.feature.colormatch.closetScreen
import com.gun.personalcolor.feature.colormatch.colorMatchScreen
import com.gun.personalcolor.feature.drape.DrapeRoute
import com.gun.personalcolor.feature.drape.drapeScreen
import com.gun.personalcolor.feature.home.HomeRoute
import com.gun.personalcolor.feature.home.homeScreen
import com.gun.personalcolor.feature.learn.ArticleRoute
import com.gun.personalcolor.feature.learn.LearnRoute
import com.gun.personalcolor.feature.learn.learnScreens
import com.gun.personalcolor.feature.my.HistoryRoute
import com.gun.personalcolor.feature.my.WallpaperRoute
import com.gun.personalcolor.feature.my.myScreens
import com.gun.personalcolor.feature.my.wallpaperScreen
import com.gun.personalcolor.feature.onboarding.onboardingScreen
import com.gun.personalcolor.feature.quiz.QuizRoute
import com.gun.personalcolor.feature.quiz.quizScreen
import com.gun.personalcolor.feature.result.ResultRoute
import com.gun.personalcolor.feature.result.resultScreen
import com.gun.personalcolor.feature.settings.LegalDoc
import com.gun.personalcolor.feature.settings.LegalRoute
import com.gun.personalcolor.feature.settings.LicensesRoute
import com.gun.personalcolor.feature.settings.SettingsRoute
import com.gun.personalcolor.feature.settings.settingsScreens
import com.gun.personalcolor.feature.tones.CompareRoute
import com.gun.personalcolor.feature.tones.ToneRoute
import com.gun.personalcolor.feature.tones.tonesScreens

/**
 * The whole navigation graph. Features don't know about each other; every cross-feature jump is wired here.
 */
@Composable
fun PcNavHost(nav: NavHostController) {
    val context = LocalContext.current
    val back: () -> Unit = { nav.popBackStack() }
    val toTone: (String) -> Unit = { nav.navigate(ToneRoute(it)) }
    val toAnalyze: () -> Unit = { nav.navigate(AnalyzeRoute) }
    val toQuiz: () -> Unit = { nav.navigate(QuizRoute) }
    val toArticle: (String) -> Unit = { nav.navigate(ArticleRoute(it)) }
    val toSettings: () -> Unit = { nav.navigate(SettingsRoute) }

    NavHost(nav, startDestination = HomeRoute) {
        onboardingScreen(onDone = back)

        homeScreen(
            onAnalyze = toAnalyze,
            onQuiz = toQuiz,
            onTone = toTone,
            onTones = { nav.navigateToTopLevel(TopLevel.TONES) },
            onColorMatch = { nav.navigateToTopLevel(TopLevel.COLOR_MATCH) },
            onLearn = { nav.navigate(LearnRoute) },
            onArticle = toArticle,
            onSettings = toSettings,
            onCoordi = { nav.navigate(CoordiRoute) },
        )

        analyzeScreen(
            onBack = back,
            onResult = { nav.navigate(ResultRoute) { popUpTo<AnalyzeRoute> { inclusive = true } } },
            onQuiz = { nav.navigate(QuizRoute) { popUpTo<AnalyzeRoute> { inclusive = true } } },
        )
        quizScreen(
            onBack = back,
            onResult = { nav.navigate(ResultRoute) { popUpTo<QuizRoute> { inclusive = true } } },
        )
        resultScreen(
            onBack = back,
            onDrape = { nav.navigate(DrapeRoute) },
            onTone = toTone,
            onRetryPhoto = { nav.navigate(AnalyzeRoute) { popUpTo<ResultRoute> { inclusive = true } } },
            onColorMatch = { nav.navigateToTopLevel(TopLevel.COLOR_MATCH) },
            onWallpaper = { nav.navigate(WallpaperRoute(it)) },
        )
        drapeScreen(onBack = back)

        tonesScreens(
            onBack = back,
            onTone = toTone,
            onCompare = { a, b -> nav.navigate(CompareRoute(a, b)) },
        )
        colorMatchScreen(
            onTone = toTone, onAnalyze = toAnalyze, onTones = { nav.navigateToTopLevel(TopLevel.TONES) },
            onCloset = { nav.navigate(ClosetRoute) },
        )
        closetScreen(onBack = back, onColorMatch = { nav.navigateToTopLevel(TopLevel.COLOR_MATCH) })
        learnScreens(onBack = back, onArticle = toArticle, onAnalyze = toAnalyze, onQuiz = toQuiz)

        myScreens(
            onBack = back,
            onTone = toTone,
            onAnalyze = toAnalyze,
            onQuiz = toQuiz,
            onHistory = { nav.navigate(HistoryRoute) },
            onSettings = toSettings,
            onTones = { nav.navigateToTopLevel(TopLevel.TONES) },
            onWallpaper = { nav.navigate(WallpaperRoute(it)) },
            onCloset = { nav.navigate(ClosetRoute) },
            // Launchers that support it show "add widget?"; otherwise the row is hidden.
            onAddWidget = if (TodayColorWidgets.canPin(context)) {{ TodayColorWidgets.requestPin(context) }} else null,
        )
        wallpaperScreen(onBack = back)
        coordiScreen(onBack = back, onTone = toTone, onCloset = { nav.navigate(ClosetRoute) }, onAnalyze = toAnalyze)
        settingsScreens(
            onBack = back,
            onLicenses = { nav.navigate(LicensesRoute) },
            onLegal = { doc: LegalDoc -> nav.navigate(LegalRoute(doc)) },
            onOnboarding = { nav.navigate(com.gun.personalcolor.feature.onboarding.OnboardingRoute) },
        )
    }
}
