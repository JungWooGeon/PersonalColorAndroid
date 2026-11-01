package com.gun.personalcolor.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
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
    HOME(HomeRoute, HomeRoute::class, "홈", Icons.Outlined.Home, Icons.Filled.Home),
    TONES(TonesRoute, TonesRoute::class, "컬러 도감", Icons.Outlined.Palette, Icons.Filled.Palette),
    COLOR_MATCH(ColorMatchRoute, ColorMatchRoute::class, "컬러 체크", Icons.Outlined.Colorize, Icons.Filled.Colorize),
    MY(MyRoute, MyRoute::class, "마이", Icons.Outlined.Person, Icons.Filled.Person),
}

/** App shell: bottom bar on phones, navigation rail on tablets/landscape; hidden on pushed screens. */
@Composable
fun PcApp(showOnboarding: Boolean, pendingRoute: Any? = null, onRouteHandled: () -> Unit = {}) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val dest = entry?.destination
    val current = TopLevel.entries.firstOrNull { t -> dest?.hierarchy?.any { it.hasRoute(t.routeClass) } == true }

    val layout = if (current == null) NavigationSuiteType.None
    else NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())

    NavigationSuiteScaffold(
        layoutType = layout,
        containerColor = MaterialTheme.colorScheme.background,
        navigationSuiteItems = {
            TopLevel.entries.forEach { t ->
                item(
                    selected = t == current,
                    onClick = { nav.navigateToTopLevel(t) },
                    icon = { Icon(if (t == current) t.selectedIcon else t.icon, null) },
                    label = { Text(t.label, maxLines = 1) },
                )
            }
        },
    ) {
        PcNavHost(nav)
        // First launch: onboarding on top of home, so finishing it just pops back.
        LaunchedEffect(Unit) { if (showOnboarding) nav.navigate(OnboardingRoute) }
        // Deep link (Kakao share / App Link / notification): push the requested screen, then clear it.
        LaunchedEffect(pendingRoute) {
            if (pendingRoute != null) { nav.navigate(pendingRoute) { launchSingleTop = true }; onRouteHandled() }
        }
    }
}

fun NavHostController.navigateToTopLevel(t: TopLevel) = navigate(t.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
