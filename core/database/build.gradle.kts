plugins {
    alias(libs.plugins.kmp.library)
}

kotlin {
    android {
        namespace = "com.vibely.core.database"
    }

    sourceSets {
        jvmMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.domain)
            implementation(libs.bundles.exposed)
            implementation(libs.hikari)
            implementation(libs.flyway.core)
            implementation(libs.flyway.database.postgresql)
            implementation(libs.postgresql)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.testcontainers.postgresql)
            implementation(libs.testcontainers.junit.jupiter)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
