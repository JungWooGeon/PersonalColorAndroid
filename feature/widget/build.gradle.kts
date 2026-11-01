plugins { id("pc.android.feature") }
android { namespace = "com.gun.personalcolor.feature.widget" }

dependencies {
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
}
