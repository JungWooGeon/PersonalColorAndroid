package com.gun.personalcolor.feature.coordi

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object CoordiRoute

/** 오늘의 코디 (daily outfit). */
fun NavGraphBuilder.coordiScreen(onBack: () -> Unit, onTone: (String) -> Unit, onCloset: () -> Unit, onAnalyze: () -> Unit) {
    composable<CoordiRoute> { CoordiRouteScreen(onBack, onTone, onCloset, onAnalyze) }
}
