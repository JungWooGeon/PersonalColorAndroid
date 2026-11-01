import java.util.Properties

plugins {
    id("pc.android.compose")
    id("pc.hilt")
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
// Google's official AdMob test unit IDs, used for debug builds.
val testAdIds = mapOf(
    "reward_ad_unit_id" to "ca-app-pub-3940256099942544/5224354917",
    "interstitial_ad_unit_id" to "ca-app-pub-3940256099942544/1033173712",
    "native_ad_unit_id" to "ca-app-pub-3940256099942544/2247696110",
)

android {
    namespace = "com.gun.personalcolor.core.ads"
    buildFeatures { buildConfig = true }
    buildTypes {
        debug { testAdIds.forEach { (key, id) -> buildConfigField("String", key.uppercase(), "\"$id\"") } }
        release {
            testAdIds.keys.forEach { key ->
                buildConfigField("String", key.uppercase(), "\"${localProps.getProperty(key, "")}\"")
            }
        }
    }
}

// Never ship a release with missing (or test) ad unit IDs.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        val missing = testAdIds.keys.filter { localProps.getProperty(it).isNullOrBlank() }
        require(missing.isEmpty()) { "local.properties에 광고 단위 ID가 없습니다: $missing" }
    }
}

dependencies {
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.billing)
    implementation(libs.androidx.activity.compose)
    implementation(project(":core:designsystem"))
}
