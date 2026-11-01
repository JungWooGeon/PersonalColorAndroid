plugins {
    id("pc.android.library")
    id("pc.hilt")
    id("pc.kotlin.serialization")
}
android { namespace = "com.gun.personalcolor.core.data" }
dependencies {
    api(project(":core:model"))
    api(libs.androidx.datastore.preferences)
}
