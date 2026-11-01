plugins {
    id("pc.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android { buildFeatures { compose = true } }
dependencies {
    val bom = platform(lib("androidx-compose-bom"))
    "implementation"(bom)
    "implementation"(lib("androidx-compose-ui"))
    "implementation"(lib("androidx-compose-material3"))
    "implementation"(lib("androidx-compose-material-icons"))
    "implementation"(lib("androidx-compose-ui-tooling-preview"))
    "debugImplementation"(lib("androidx-compose-ui-tooling"))
}
