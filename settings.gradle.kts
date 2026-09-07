pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "2-Stroke Lab"
include(":shared")
include(":androidApp")

// Backward compatibility: Android Studio / scripts may still target :app after KMP rename.
include(":app")
project(":app").projectDir = file("androidApp")
