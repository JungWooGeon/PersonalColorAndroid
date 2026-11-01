package com.gun.personalcolor.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gun.personalcolor.core.designsystem.PcColors
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.drawDropGlyph
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gun.personalcolor.feature.colormatch.ColorMatchRoute
import com.gun.personalcolor.feature.home.HomeRoute
import com.gun.personalcolor.feature.my.MyRoute
import com.gun.personalcolor.feature.onboarding.OnboardingRoute
import com.gun.personalcolor.feature.tones.TonesRoute
import kotlin.reflect.KClass

enum class TopLevel(val route: Any, val routeClass: KClass<*>, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME(HomeRoute, HomeRoute::class, "홈", Icons.Outlined.Home, Icons.Rounded.Home),
    TONES(TonesRoute, TonesRoute::class, "컬러 도감", Icons.Outlined.Palette, Icons.Rounded.Palette),
    COLOR_MATCH(ColorMatchRoute, ColorMatchRoute::class, "컬러 체크", Icons.Outlined.Colorize, Icons.Rounded.Colorize),
    MY(MyRoute, MyRoute::class, "마이", Icons.Outlined.Person, Icons.Rounded.Person),
}

/** App shell: bottom bar on phones, navigation rail on tablets/landscape; hidden on pushed screens. */
@Composable
fun PcApp(showOnboarding: Boolean, pendingRoute: Any? = null, onRouteHandled: () -> Unit = {}) {
    val nav = rememberNavController()
    // Onboarding is pushed once; after recreation (rotation, dark mode, locale) the restored back stack already has it.
    var onboardingPushed by rememberSaveable { mutableStateOf(false) }
    val entry by nav.currentBackStackEntryAsState()
    val dest = entry?.destination
    val current = TopLevel.entries.firstOrNull { t -> dest?.hierarchy?.any { it.hasRoute(t.routeClass) } == true }
    val adaptive = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
    // Phones get the brand bottom bar (below); tablets keep the Material rail.
    val phoneBar = adaptive == NavigationSuiteType.NavigationBar || adaptive == NavigationSuiteType.ShortNavigationBarCompact
    val layout = if (current == null || phoneBar) NavigationSuiteType.None else adaptive

    val cs = MaterialTheme.colorScheme
    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = cs.primary, selectedTextColor = cs.primary, indicatorColor = cs.primaryContainer,
            unselectedIconColor = cs.onSurfaceVariant, unselectedTextColor = cs.onSurfaceVariant,
        ),
    )
    Column(Modifier.fillMaxSize().background(cs.background)) {
        NavigationSuiteScaffold(
            modifier = Modifier.weight(1f).then(if (phoneBar && current != null) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
            layoutType = layout,
            containerColor = cs.background,
            navigationSuiteColors = NavigationSuiteDefaults.colors(navigationRailContainerColor = cs.background),
            navigationSuiteItems = {
                TopLevel.entries.forEach { t ->
                    item(
                        selected = t == current,
                        onClick = { nav.navigateToTopLevel(t) },
                        icon = { TabIcon(t, t == current) },
                        label = { Text(t.label, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                        colors = itemColors,
                    )
                }
            },
        ) {
            PcNavHost(nav)
            // First launch: onboarding on top of home, so finishing it just pops back.
            LaunchedEffect(Unit) { if (showOnboarding && !onboardingPushed) { onboardingPushed = true; nav.navigate(OnboardingRoute) } }
            // Deep link (Kakao share / App Link / notification): push the requested screen, then clear it.
            LaunchedEffect(pendingRoute) {
                if (pendingRoute != null) { nav.navigate(pendingRoute) { launchSingleTop = true }; onRouteHandled() }
            }
        }
        if (phoneBar && current != null) PcBottomBar(current) { nav.navigateToTopLevel(it) }
    }
}

/**
 * Bottom bar: a drop-cornered indicator slides to the selected tab (spring), the icon pops, a light tick of haptics.
 * Home is 틴티 itself: an outlined drop, filled with the brand gradient and given eyes when selected.
 */
@Composable
private fun PcBottomBar(current: TopLevel, onSelect: (TopLevel) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val index = TopLevel.entries.indexOf(current)
    val x by animateFloatAsState(index.toFloat(), spring(dampingRatio = 0.72f, stiffness = 520f), label = "indicator")
    val indicator = cs.primaryContainer
    Surface(color = cs.surfaceContainer, shadowElevation = 0.dp) {
        Column {
            HorizontalDivider(color = cs.outlineVariant.copy(alpha = if (PcTheme.colors.isDark) 1f else 0.6f))
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).height(68.dp).selectableGroup()
                    .drawBehind {
                        val w = size.width / TopLevel.entries.size
                        val iw = 60.dp.toPx(); val ih = 32.dp.toPx()
                        val left = w * x + (w - iw) / 2; val top = 8.dp.toPx()
                        val r = 16.dp.toPx(); val tr = 6.dp.toPx()
                        val path = Path().apply {
                            addRoundRect(RoundRect(left, top, left + iw, top + ih, CornerRadius(r), CornerRadius(tr), CornerRadius(r), CornerRadius(r)))
                        }
                        drawPath(path, indicator)
                    },
            ) {
                TopLevel.entries.forEach { t ->
                    val selected = t == current
                    val pop = remember { Animatable(1f) }
                    LaunchedEffect(selected) { if (selected) { pop.snapTo(0.8f); pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f)) } }
                    Column(
                        // Label on the tab node itself: some OEMs clip child bounds near the nav bar, which hid the text from TalkBack.
                        Modifier.weight(1f).fillMaxHeight().semantics(mergeDescendants = true) { contentDescription = t.label }
                            .selectable(selected, role = Role.Tab, interactionSource = null, indication = null) {
                                if (!selected) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(t)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.padding(top = 8.dp).size(60.dp, 32.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }, contentAlignment = Alignment.Center) {
                            TabIcon(t, selected)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(t.label, maxLines = 1, style = MaterialTheme.typography.labelMedium,
                            color = if (selected) cs.primary else cs.onSurfaceVariant, modifier = Modifier.clearAndSetSemantics {})
                    }
                }
            }
        }
    }
}

private val BrandFill = Brush.linearGradient(listOf(PcColors.Peach, PcColors.Lavender))

@Composable
private fun TabIcon(t: TopLevel, selected: Boolean) {
    if (t == TopLevel.HOME) {
        val c = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(Modifier.size(22.dp, 24.dp)) {
            if (selected) drawDropGlyph(c, brush = BrandFill, eyes = true)
            else drawDropGlyph(c, strokePx = 1.9.dp.toPx())
        }
    } else Icon(if (selected) t.selectedIcon else t.icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
}

fun NavHostController.navigateToTopLevel(t: TopLevel) = navigate(t.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
