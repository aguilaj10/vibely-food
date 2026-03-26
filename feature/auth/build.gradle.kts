buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    configurations.all {
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:2.3.20")
    }
    dependencies {
        classpath("com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:0.17.1")
    }
}

plugins {
    alias(libs.plugins.kmp.library)
}

apply(plugin = "com.codingfeline.buildkonfig")

kotlin {
    android {
        namespace = "com.vibely.feature.auth"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.network)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.security.crypto)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

configure<com.codingfeline.buildkonfig.gradle.BuildKonfigExtension> {
    packageName = "com.vibely.feature.auth"
    val authMode = System.getenv("AUTH_MODE") ?: "production"
    defaultConfigs {
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "AUTH_MODE", authMode)
    }
}

tasks.withType<Test>().configureEach {
    useJUnit()
}

// Exclude buildkonfig-generated sources from ktlint — generated code does not
// comply with project indentation rules and cannot be fixed manually.
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude { element ->
            val path = element.file.path
            path.contains("/buildkonfig/") || path.contains("/generated/")
        }
    }
}
