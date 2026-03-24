// AGP 9.x: 'com.android.application' + 'kotlin.multiplatform' is not supported.
// composeApp uses 'kotlin.android' for the Android target. The Desktop target
// will be wired to the 'shared' KMP module in a future feature iteration.
// AGP 9.x: 'org.jetbrains.kotlin.android' is no longer needed — Kotlin support
// is built into 'com.android.application' since AGP 9.0.
plugins {
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Each module must set its own namespace:
    //   android { namespace = "com.vibely.<module>" }
}
