package com.gun.personalcolor

import android.app.Application
import com.gun.personalcolor.core.share.ToneShare
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PcApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.gun.personalcolor.core.ui.GarmentArt.init(this)
        ToneShare.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        // No capture is in use at process start: drop guided photos a killed process left behind.
        Thread { com.gun.personalcolor.feature.analyze.GuidedPhotos.sweep(cacheDir) }.start()
    }
}
