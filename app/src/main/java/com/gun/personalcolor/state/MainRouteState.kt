package com.gun.personalcolor.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.gun.personalcolor.R

sealed class MainState (
    val title: Int,
    val icon: ImageVector,
    val screenRoute: String
) {
    data object AiTest : MainState(R.string.ai_test, Icons.Default.Face , ScreenRoute.AI_TEST)
    data object SelfTest : MainState(R.string.self_test, Icons.Default.Person, ScreenRoute.SELF_TEST)
}

object ScreenRoute {
    const val AI_TEST = "https://personalcolorotest.netlify.app/personalcolortest.html"
    const val SELF_TEST = "https://personalcolorotest.netlify.app/selftest"
}