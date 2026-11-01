package com.gun.personalcolor.feature.quiz

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object QuizRoute

/** 12-question self diagnosis → [onResult]. */
fun NavGraphBuilder.quizScreen(onBack: () -> Unit, onResult: () -> Unit) {
    composable<QuizRoute> { QuizRoute(onBack, onResult) }
}
