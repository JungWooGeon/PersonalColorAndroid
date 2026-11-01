plugins { id("pc.android.feature") }
android { namespace = "com.gun.personalcolor.feature.home" }
dependencies {
    implementation(project(":core:weather")) // 틴티's greeting mentions today's weather (only if location was already granted)
}
