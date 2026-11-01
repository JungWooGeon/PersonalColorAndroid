plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

dependencies {
    "implementation"(lib("hilt-android"))
    "ksp"(lib("hilt-compiler"))
}
