plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

dependencies {
    "implementation"(lib("hilt-android"))
    "ksp"(lib("hilt-compiler"))
}

// KSP2 runs in-process in the Gradle daemon. Run in parallel there (cold daemon especially), a module's
// kspDebugKotlin can finish "successfully" with NO Hilt code -> ViewModel missing from Hilt's map -> crash
// "Cannot create an instance of ...ViewModel". One KSP task at a time; never reuse a KSP output from the build cache.
val kspSerial = gradle.sharedServices.registerIfAbsent("kspSerial", KspSerial::class) { maxParallelUsages = 1 }
tasks.withType<com.google.devtools.ksp.gradle.KspAATask>().configureEach {
    usesService(kspSerial)
    outputs.doNotCacheIf("KSP2 output can be silently empty") { true }
}
