pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
        plugins {
            id("com.android.application") version "8.6.0" apply false
            id("com.android.library")     version "8.6.0" apply false
            kotlin("android")             version "2.1.0" apply false
            id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
            id("com.google.gms.google-services") version "4.4.2" apply false
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "MyStyleBox"
include(":app")
