package com.gun.personalcolor.feature.drape

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object DrapeRoute

/** Virtual draping on the user's photo from the latest diagnosis. */
fun NavGraphBuilder.drapeScreen(onBack: () -> Unit) {
    composable<DrapeRoute> { DrapeRoute(onBack) }
}


