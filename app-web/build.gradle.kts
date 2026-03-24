plugins {
    alias(libs.plugins.kmp.js.app)
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
        }
    }
    sourceSets {
        jsMain.dependencies {
            implementation(libs.koin.core)
            implementation(projects.shared)
        }
    }
}
