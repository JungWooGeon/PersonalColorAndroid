plugins {
    id("pc.jvm.library")
    id("pc.kotlin.serialization")
}
dependencies { api(libs.kotlinx.serialization.json) }
