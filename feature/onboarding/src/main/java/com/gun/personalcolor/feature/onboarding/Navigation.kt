package com.gun.personalcolor.feature.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute

/** First-launch intro. [onDone] is called after the user finishes or skips (mark onboarding done before calling). */
fun NavGraphBuilder.onboardingScreen(onDone: () -> Unit) {
    composable<OnboardingRoute> { OnboardingRoute(onDone) }
}
