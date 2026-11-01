package com.gun.personalcolor.feature.analyze

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object AnalyzeRoute

/** Photo diagnosis: tips → camera/gallery → on-device analysis → [onResult]. */
fun NavGraphBuilder.analyzeScreen(onBack: () -> Unit, onResult: () -> Unit, onQuiz: () -> Unit) {
    composable<AnalyzeRoute> { AnalyzeRoute(onBack, onResult, onQuiz) }
}
