plugins {
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("detekt.yml"))
        buildUponDefaultConfig = true
        allRules = false
    }
}

tasks.register<Copy>("installGitHooks") {
    description = "Copies git hooks from scripts/git-hooks/ into .git/hooks/"
    group = "setup"
    from(rootProject.file("scripts/git-hooks/"))
    into(rootProject.file(".git/hooks/"))
    doLast {
        rootProject.file(".git/hooks/pre-commit").setExecutable(true)
        println("Git hooks installed. Pre-commit hook is now active.")
    }
}
