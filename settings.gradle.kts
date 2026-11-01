pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Kakao SDK (KakaoTalk share)
        maven("https://devrepo.kakao.com/nexus/content/groups/public/") { content { includeGroup("com.kakao.sdk") } }
    }
}

rootProject.name = "PersonalColor"
include(":app")
include(
    ":core:model", ":core:analysis", ":core:vision", ":core:data", ":core:domain",
    ":core:designsystem", ":core:ui", ":core:ads", ":core:analytics", ":core:weather", ":core:share",
)
include(
    ":feature:onboarding", ":feature:home", ":feature:analyze", ":feature:result", ":feature:drape",
    ":feature:quiz", ":feature:tones", ":feature:colormatch", ":feature:learn",  ":feature:my", ":feature:settings", ":feature:coordi", ":feature:widget",
)
