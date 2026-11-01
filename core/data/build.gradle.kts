plugins {
    id("pc.android.library")
    id("pc.hilt")
    id("pc.kotlin.serialization")
}
android {
    namespace = "com.gun.personalcolor.core.data"
    // Repositories log failed writes with android.util.Log; let JVM tests run through it.
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
    api(project(":core:model"))
    api(libs.androidx.datastore.preferences)
}
dependencies { testImplementation(libs.kotlinx.coroutines.test) }
