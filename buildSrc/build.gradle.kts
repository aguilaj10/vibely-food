plugins {
    `kotlin-dsl`
}

dependencies {
    // AGP - single artifact covers all com.android.* plugins including
    // com.android.kotlin.multiplatform.library (the AGP 9.x KMP library plugin)
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    // Kotlin plugins — kotlin.multiplatform needed for jvm()/js()/sourceSets API in kmp-library
    // kotlin.android kept for Android Kotlin type resolution in android-app (not applied, just types)
    implementation(libs.plugins.kotlin.multiplatform.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.kotlin.android.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.kotlin.jvm.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.kotlin.serialization.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    // Compose
    implementation(libs.plugins.compose.multiplatform.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.compose.compiler.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    // Ktor
    implementation(libs.plugins.ktor.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
}
