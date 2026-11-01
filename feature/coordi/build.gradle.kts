plugins { id("pc.android.feature") }
android { namespace = "com.gun.personalcolor.feature.coordi" }
dependencies {
    implementation(project(":core:weather"))
    implementation(project(":core:share"))
    implementation(project(":core:domain")) // exposes core:analysis (ColorMath)
    implementation(libs.work.runtime)
}
