package com.gun.personalcolor.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute

fun NavGraphBuilder.homeScreen(
    onAnalyze: () -> Unit, onQuiz: () -> Unit, onTone: (String) -> Unit, onTones: () -> Unit,
    onColorMatch: () -> Unit, onLearn: () -> Unit, onArticle: (String) -> Unit, onSettings: () -> Unit,
    onCoordi: () -> Unit = {},
) {
    composable<HomeRoute> {
        HomeRoute(HomeActions(onAnalyze, onQuiz, onTone, onTones, onColorMatch, onLearn, onArticle, onSettings, onCoordi))
    }
}
