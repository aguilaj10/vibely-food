pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // No repositoriesMode restriction: the Kotlin/JS toolchain adds its own distribution
    // repositories (Node.js, Yarn) programmatically; setting FAIL_ON_PROJECT_REPOS or
    // PREFER_SETTINGS blocks those downloads. The declared repositories below are used
    // for all regular dependency resolution.
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
    // gradle/libs.versions.toml is auto-discovered by Gradle 9.x as the "libs" catalog.
    // No explicit versionCatalogs declaration needed here.
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "vibely-food"

// Entry-point modules
include(":composeApp")
include(":server")
include(":app-web")
include(":shared")

// Core layer
include(":core:domain")
include(":core:network")
include(":core:database")
include(":core:ui")
include(":core:common")

// Feature layer
include(":feature:auth")
include(":feature:menu")
include(":feature:orders")
include(":feature:payments")
include(":feature:inventory")
include(":feature:customers")
include(":feature:reports")
include(":feature:settings")
