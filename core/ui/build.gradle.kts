plugins { id("pc.android.compose") }
android { namespace = "com.gun.personalcolor.core.ui" }
dependencies {
    api(project(":core:model"))
    api(project(":core:designsystem"))
    implementation(libs.androidx.core.ktx) // ResourcesCompat.getFont for the mood scenes
}
