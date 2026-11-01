plugins { `kotlin-dsl` }

// Plugins live here (not in the root build) so precompiled convention scripts can apply them by id.
dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    implementation(libs.serialization.gradlePlugin)
    implementation(libs.ksp.gradlePlugin)
    implementation(libs.hilt.gradlePlugin)
    implementation(libs.aboutlibraries.gradlePlugin)
}
