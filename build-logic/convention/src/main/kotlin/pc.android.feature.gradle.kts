// A feature = one screen group: UI + ViewModel + its navigation entry points. Features never depend on each other;
// the app module wires navigation between them.
plugins {
    id("pc.android.compose")
    id("pc.hilt")
    id("pc.kotlin.serialization")
}

dependencies {
    "implementation"(project(":core:model"))
    "implementation"(project(":core:data"))
    "implementation"(project(":core:designsystem"))
    "implementation"(project(":core:ui"))
    "implementation"(project(":core:analytics"))
    "implementation"(project(":core:ads"))
    "implementation"(lib("androidx-navigation-compose"))
    "implementation"(lib("androidx-hilt-navigation-compose"))
    "implementation"(lib("androidx-lifecycle-runtime-compose"))
    "implementation"(lib("androidx-lifecycle-viewmodel-compose"))
    "implementation"(lib("kotlinx-serialization-json"))
}
