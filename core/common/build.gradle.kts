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
    }
}
