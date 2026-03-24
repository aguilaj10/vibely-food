// IMPORTANT: This module must have ZERO framework dependencies.
// Only pure Kotlin is allowed. No Android, Ktor, Compose, or any framework imports.
// This is enforced by Detekt ForbiddenImport rules in detekt.yml.
plugins {
    alias(libs.plugins.kmp.library)
}

kotlin {
    android {
        namespace = "com.vibely.core.domain"
    }

    sourceSets {
        jvmTest.dependencies {
            implementation(libs.kotest.assertions.core)
        }
    }
}

// The kmp-library convention plugin adds kotlin("test") to commonTest only.
// jvmTest inherits kotlin-test-junit from commonTest, but Gradle still needs to be told
// which test framework to use so tests are discovered. JUnit 4 (useJUnit) matches the
// kotlin-test-junit artifact that kotlin("test") resolves to on JVM.
tasks.withType<Test>().configureEach {
    useJUnit()
}
