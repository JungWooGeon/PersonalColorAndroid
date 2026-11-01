plugins {
    id("pc.android.library")
    id("pc.hilt")
}
android { namespace = "com.gun.personalcolor.core.domain" }
dependencies {
    api(project(":core:analysis"))
    api(project(":core:vision"))
    api(project(":core:data"))
    implementation(project(":core:analytics"))
}
