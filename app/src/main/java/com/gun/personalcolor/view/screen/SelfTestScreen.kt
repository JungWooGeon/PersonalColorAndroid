package com.gun.personalcolor.view.screen

import androidx.compose.runtime.Composable
import com.gun.personalcolor.state.MainState
import com.gun.personalcolor.view.composable.WebView

@Composable
fun SelfTestScreen() {
    WebView(MainState.SelfTest.screenRoute)
}