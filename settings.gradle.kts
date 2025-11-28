rootProject.name = "KompareProj"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://packages.jetbrains.team/maven/p/kpm/public/")
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://packages.jetbrains.team/maven/p/kpm/public/")
        google()
        mavenCentral()
        mavenLocal()

        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
    }
}

include(":kompare")
include(":kompare-kotest")
include(":sample-android")
include(":sample-kmp:android-app")
include(":sample-kmp:shared")
include(":idea")
include(":kompare-image")
