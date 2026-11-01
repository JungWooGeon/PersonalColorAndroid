package com.gun.personalcolor.feature.drape

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object DrapeRoute
@Serializable data object RefineRoute

/** Virtual draping on the user's photo from the latest diagnosis. */
fun NavGraphBuilder.drapeScreen(onBack: () -> Unit) {
    composable<DrapeRoute> { DrapeRoute(onBack) }
}

/** 정밀 진단: drape comparisons that refine the latest photo diagnosis. [onDone] → show the (updated) result. */
fun NavGraphBuilder.refineScreen(onBack: () -> Unit, onDone: () -> Unit) {
    composable<RefineRoute> { RefineRoute(onBack, onDone) }
}

