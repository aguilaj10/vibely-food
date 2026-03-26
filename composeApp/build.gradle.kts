plugins {
    alias(libs.plugins.android.app)
}

android {
    namespace = "com.vibely.composeapp"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.network)
    implementation(projects.feature.auth)
    implementation(projects.shared)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
}
