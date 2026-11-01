plugins { id("pc.android.compose") }
android { namespace = "com.gun.personalcolor.core.designsystem" }
dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons)
    api(libs.androidx.compose.material3.adaptive)
}
