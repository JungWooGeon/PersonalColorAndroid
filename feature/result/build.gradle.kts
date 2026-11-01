plugins { id("pc.android.feature") }
android { namespace = "com.gun.personalcolor.feature.result" }
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:share"))
}
