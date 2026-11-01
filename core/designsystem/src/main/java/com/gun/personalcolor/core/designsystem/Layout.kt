package com.gun.personalcolor.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/** Horizontal screen padding. */
val ScreenPadding = 20.dp

/** Text-heavy screens stop growing past this width on tablets/landscape. */
val ReadableWidth = 680.dp

/** Full width, centered, capped at [max]. Use on the content column of every screen so tablets don't stretch lines. */
fun Modifier.readableWidth(max: Dp = ReadableWidth) =
    fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max).fillMaxWidth()

/** True on tablets, foldables and phones in landscape (window ≥ 600dp wide). */
@Composable
fun isWideWindow(): Boolean =
    currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

/** True when the window is wide enough for two content panes (≥ 840dp). */
@Composable
fun isExpandedWindow(): Boolean =
    currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
