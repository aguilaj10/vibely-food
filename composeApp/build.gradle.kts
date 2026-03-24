plugins {
    alias(libs.plugins.android.app)
}

android {
    namespace = "com.vibely.composeapp"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.shared)
    implementation(libs.koin.android)
}
