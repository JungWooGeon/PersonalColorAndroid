package com.gun.personalcolor.feature.tones

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable data object TonesRoute
@Serializable data class ToneRoute(val id: String)
/** Compare tone [a] with [b]; null [b] = let the user pick. */
@Serializable data class CompareRoute(val a: String, val b: String? = null)

/** Tab "컬러 도감" (12-tone list) + tone detail + tone comparison. */
fun NavGraphBuilder.tonesScreens(onBack: () -> Unit, onTone: (String) -> Unit, onCompare: (String, String?) -> Unit) {
    composable<TonesRoute> {
        val vm: TonesViewModel = hiltViewModel()
        val state by vm.state.collectAsStateWithLifecycle()
        TonesScreen(state, onTone, vm::toggleFavorite)
    }
    composable<ToneRoute> { entry ->
        val id = entry.toRoute<ToneRoute>().id
        val vm: TonesViewModel = hiltViewModel()
        val state by vm.state.collectAsStateWithLifecycle()
        ToneDetailScreen(
            state = state, id = id, onBack = onBack, onTone = onTone,
            onToggleFavorite = { vm.toggleFavorite(id) },
            onSetMine = { vm.setMyTone(id) },
            onCompare = { onCompare(id, null) },
        )
    }
    composable<CompareRoute> { entry ->
        val route = entry.toRoute<CompareRoute>()
        val vm: TonesViewModel = hiltViewModel()
        val state by vm.state.collectAsStateWithLifecycle()
        CompareScreen(state.tones, route.a, route.b, onBack)
    }
}
