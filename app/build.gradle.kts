import java.util.Properties

plugins { id("pc.android.application") }

// Kakao native app key (local.properties: kakao_native_app_key). Blank = KakaoTalk share falls back to the share sheet.
val kakaoKey = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}.getProperty("kakao_native_app_key", "").trim()

android {
    namespace = "com.gun.personalcolor"
    defaultConfig {
        applicationId = "com.gun.personalcolor"
        versionCode = 23
        versionName = "2.0.0"
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoKey\"")
        manifestPlaceholders["kakaoScheme"] = if (kakaoKey.isBlank()) "kakaonokey" else "kakao$kakaoKey"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { buildConfig = true }
    androidResources {
        noCompress += listOf("task", "tflite")   // MediaPipe models are memory-mapped
        localeFilters += listOf("ko", "en")
    }
}

// The aboutlibraries plugin writes res/raw/aboutlibraries.json (open-source licenses), read by :feature:settings.

dependencies {
    listOf(
        ":core:model", ":core:data", ":core:designsystem", ":core:ui", ":core:ads", ":core:analytics",
        ":feature:onboarding", ":feature:home", ":feature:analyze", ":feature:result", ":feature:drape",
        ":feature:quiz", ":feature:tones", ":feature:colormatch", ":feature:learn", ":feature:my", ":feature:settings", ":feature:widget", ":feature:coordi", ":core:share", ":core:weather",
    ).forEach { implementation(project(it)) }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.navigationSuite)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

}
dependencies { testImplementation(libs.junit) }
