plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm()

    js(IR) {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            // No default dependencies — each module declares its own.
            // IMPORTANT: core:domain must have zero framework dependencies.
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Each module must set its own namespace:
    //   android { namespace = "com.vibely.<module>" }
}
