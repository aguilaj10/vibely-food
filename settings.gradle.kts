pluginManagement {
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
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "vibely-food"

// Entry-point modules
include(":composeApp")
include(":server")
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
