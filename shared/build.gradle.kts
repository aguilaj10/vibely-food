plugins {
    alias(libs.plugins.kmp.library)
}

kotlin {
    android {
        namespace = "com.vibely.shared"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(projects.core.common)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.datastore.preferences)
            implementation(libs.room.runtime)
        }
    }
}
