plugins { id("com.android.library") }

android {
    compileSdk = Sdk.COMPILE
    defaultConfig { minSdk = Sdk.MIN }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies { "testImplementation"(lib("junit")) }
