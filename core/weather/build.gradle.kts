plugins {
    id("pc.android.library")
    id("pc.hilt")
    id("pc.kotlin.serialization")
}
android { namespace = "com.gun.personalcolor.core.weather" }
dependencies {
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.serialization.json)
}
