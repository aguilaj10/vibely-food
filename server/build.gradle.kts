plugins {
    alias(libs.plugins.jvm.server)
}

application {
    mainClass.set("com.vibely.server.MainKt")
}

dependencies {
    implementation(libs.koin.core)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(projects.shared)
}
