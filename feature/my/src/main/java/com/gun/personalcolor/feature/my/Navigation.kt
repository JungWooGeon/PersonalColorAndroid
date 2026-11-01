package com.gun.personalcolor.feature.my

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object MyRoute
@Serializable data object HistoryRoute

/** Palette wallpaper maker. null toneId → my tone (or the first tone; the user can switch). */
@Serializable data class WallpaperRoute(val toneId: String? = null)

/** Tab "마이" (my tone, favorites, recent results) + full history list. */
fun NavGraphBuilder.myScreens(
    onBack: () -> Unit,
    onTone: (String) -> Unit,
    onAnalyze: () -> Unit,
    onQuiz: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onTones: () -> Unit,
    onWallpaper: (toneId: String?) -> Unit = {},
    onCloset: () -> Unit = {},
    onAddWidget: (() -> Unit)? = null,
) {
    composable<MyRoute> {
        val vm: MyViewModel = hiltViewModel()
        val state by vm.state.collectAsStateWithLifecycle()
        MyScreen(state, onTone, onAnalyze, onQuiz, onHistory, onSettings, onTones, onWallpaper, onCloset, onAddWidget)
    }
    composable<HistoryRoute> {
        val vm: MyViewModel = hiltViewModel()
        val state by vm.state.collectAsStateWithLifecycle()
        HistoryScreen(state, onBack, onTone, onAnalyze, onDelete = vm::delete, onClear = vm::clear)
    }
}

fun NavGraphBuilder.wallpaperScreen(onBack: () -> Unit) {
    composable<WallpaperRoute> { WallpaperRoute(onBack) }
}
