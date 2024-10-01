package com.gun.personalcolor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.gun.personalcolor.ui.theme.LocalFirebaseAnalytics
import com.gun.personalcolor.ui.theme.PersonalColorTheme
import com.gun.personalcolor.view.screen.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var firebaseCrashlytics: FirebaseCrashlytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        setContent {
            CompositionLocalProvider(LocalFirebaseAnalytics provides firebaseAnalytics) {
                PersonalColorTheme {
                    MainScreen()
                }
            }
        }
    }
}