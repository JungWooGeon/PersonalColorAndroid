package com.gun.personalcolor.view.screen

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.gun.personalcolor.BuildConfig.BANNER_AD_UNIT_ID
import com.gun.personalcolor.R
import com.gun.personalcolor.state.MainState
import com.gun.personalcolor.ui.theme.LocalFirebaseAnalytics
import com.gun.personalcolor.view.Constants.BANNER_AD_WIDTH
import com.gun.personalcolor.view.composable.AdAlertDialog

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isAlertDialog by remember { mutableStateOf(false) }

    val adView = remember {
        AdView(context).apply {
            // https://developers.google.com/admob/android/banner/inline-adaptive?hl=ko#kotlin
            setAdSize(AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, BANNER_AD_WIDTH))
            adUnitId = BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    BackHandler(true) {
        isAlertDialog = true
    }

    MainScreen(
        adView = adView,
        navController = navController,
        currentRoute = currentRoute,
        isAlertDialog = isAlertDialog,
        onDismissExitDialog = { isAlertDialog = false },
        onFinishActivity = { (context as Activity).finish() }
    )
}

@Composable
fun MainScreen(
    adView: AdView,
    navController: NavHostController,
    currentRoute: String?,
    isAlertDialog: Boolean,
    onDismissExitDialog: () -> Unit,
    onFinishActivity: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomNavigationBar(navController, currentRoute) }
    ) {
        Box(
            Modifier
                .padding(it)
                .background(Color.White)) {
            NavigationGraph(navController = navController)
        }
    }

    if (isAlertDialog) {
        AdAlertDialog(
            adView = adView,
            title = stringResource(id = R.string.are_you_sure_end_personal_color_test),
            onConfirm = onFinishActivity,
            onDismiss = onDismissExitDialog
        )
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?
) {
    val items = listOf(
        MainState.AiTest,
        MainState.SelfTest
    )

    NavigationBar(
        containerColor = Color.White
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(id = item.title),
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = { Text(stringResource(id = item.title), fontSize = 9.sp) },
                selected = currentRoute == item.screenRoute,
                alwaysShowLabel = false,
                onClick = {
                    navController.navigate(item.screenRoute) {
                        navController.graph.startDestinationRoute?.let {
                            popUpTo(it) { saveState = true }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun NavigationGraph(navController: NavHostController) {
    val firebaseAnalytics = LocalFirebaseAnalytics.current

    NavHost(navController = navController, startDestination = MainState.AiTest.screenRoute) {
        composable(MainState.AiTest.screenRoute) {
            LaunchedEffect(Unit) {
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, "AI Test Screen")
                }
            }
            AiTestScreen()
        }
        composable(MainState.SelfTest.screenRoute) {
            LaunchedEffect(Unit) {
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, "Self Test Screen")
                }
            }
            SelfTestScreen()
        }
    }
}