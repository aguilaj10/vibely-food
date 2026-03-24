// KMP library convention: applies kotlin.multiplatform (for jvm/js/sourceSets DSL)
// alongside com.android.kotlin.multiplatform.library (AGP 9.x unified KMP plugin
// that provides android {} and is compatible with kotlin.multiplatform).
// The old combination of kotlin.multiplatform + com.android.library is no longer
// supported since AGP 9.0; this pairing is the correct migration path.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    android {
        // compileSdk and minSdk defaults; each module sets its own namespace:
        //   kotlin { android { namespace = "com.vibely.<name>" } }
        compileSdk = 36
        minSdk = 26
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
