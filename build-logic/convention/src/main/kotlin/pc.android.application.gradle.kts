plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("pc.kotlin.serialization")
    id("pc.hilt")
    id("com.mikepenz.aboutlibraries.plugin.android")
}

android {
    compileSdk = Sdk.COMPILE
    defaultConfig {
        minSdk = Sdk.MIN
        targetSdk = Sdk.TARGET
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}
