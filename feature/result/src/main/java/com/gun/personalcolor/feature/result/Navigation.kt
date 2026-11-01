package com.gun.personalcolor.feature.result

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object ResultRoute

/** Result of the latest diagnosis (photo or quiz), from DiagnosisSession. */
fun NavGraphBuilder.resultScreen(onBack: () -> Unit, onDrape: () -> Unit, onTone: (String) -> Unit, onRetryPhoto: () -> Unit, onColorMatch: () -> Unit,
    onWallpaper: (toneId: String) -> Unit = {}, onRefine: () -> Unit = {}) {
    composable<ResultRoute> { ResultRoute(onBack, onDrape, onTone, onRetryPhoto, onColorMatch, onWallpaper, onRefine) }
}
