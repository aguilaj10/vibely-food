plugins {
    alias(libs.plugins.kmp.library)
}

kotlin {
    android {
        namespace = "com.vibely.core.common"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
        }
        androidMain.dependencies {
            implementation(libs.security.crypto)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.assertions.core)
        }
    }
}
