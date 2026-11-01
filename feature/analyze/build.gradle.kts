plugins { id("pc.android.feature") }
android { namespace = "com.gun.personalcolor.feature.analyze" }
dependencies {
    implementation(project(":core:domain"))
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.compose)
    implementation(libs.mediapipe.tasks.vision)
}
