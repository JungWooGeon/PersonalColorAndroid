plugins {
    id("pc.android.library")
    id("pc.hilt")
}
android { namespace = "com.gun.personalcolor.core.vision" }
dependencies {
    api(project(":core:analysis"))
    // FacePhoto exposes landmarks as Compose Offsets.
    api(platform(libs.androidx.compose.bom))
    api("androidx.compose.ui:ui-geometry")
    implementation(libs.androidx.exifinterface)
    // On-device face landmarks. Pinned: 0.10.33+ SIGILLs in XNNPACK SME kernels on Apple M4/M5 emulators
    // (real-device impact unverified). 0.10.32 is the newest that runs there and is 16KB-page aligned.
    implementation(libs.mediapipe.tasks.vision)
}
