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
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "WanAndroid-AI"
include(":app")
include(
    ":core:common",
    ":core:result",
    ":core:model",
    ":core:network",
    ":core:data",
    ":core:database"
)
include(":core:designsystem", ":core:ui", ":core:navigation")
include(":feature:home", ":feature:topics", ":feature:profile")
include(":feature:article", ":feature:auth")
check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    "WanAndroid-AI requires JDK 17+"
}
