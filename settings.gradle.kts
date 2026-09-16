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
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Use Google's canonical Maven host explicitly. This avoids repository
        // resolution issues on CI runners where google() can resolve through
        // the dl.google.com endpoint but fail to retrieve AndroidX artifacts.
        maven {
            name = "GoogleAndroidMaven"
            url = uri("https://maven.google.com")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "KFCCMobileApp"
include(":app")
