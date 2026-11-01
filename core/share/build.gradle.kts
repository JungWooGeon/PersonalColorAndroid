plugins { id("pc.android.library") }
android { namespace = "com.gun.personalcolor.core.share" }
dependencies {
    implementation(project(":core:model"))
    implementation(libs.kakao.share)
    implementation(libs.androidx.core.ktx)
}
