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
}
