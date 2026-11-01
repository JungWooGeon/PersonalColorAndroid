package com.gun.personalcolor.feature.colormatch

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object ColorMatchRoute
@Serializable data object ClosetRoute

/** Tab: "컬러 체크" — pick a color from a photo and see how well it suits my tone. */
fun NavGraphBuilder.colorMatchScreen(onTone: (String) -> Unit, onAnalyze: () -> Unit, onTones: () -> Unit, onCloset: () -> Unit = {}) {
    composable<ColorMatchRoute> { ColorMatchRoute(onTone, onAnalyze, onTones, onCloset) }
}

/** Pushed: "내 옷장" — saved color-check results, re-judged against the current my tone. */
fun NavGraphBuilder.closetScreen(onBack: () -> Unit, onColorMatch: () -> Unit) {
    composable<ClosetRoute> { ClosetRoute(onBack, onColorMatch) }
}
