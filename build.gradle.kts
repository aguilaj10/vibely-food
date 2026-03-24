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

tasks.register("installGitHooks") {
    description = "Copies git hooks from scripts/git-hooks/ into .git/hooks/ (worktree-aware)"
    group = "setup"
    doLast {
        // In a git worktree, .git is a FILE containing "gitdir: <path-to-worktree-gitdir>".
        // The actual hooks live in the common (main repo) .git/hooks/, two levels up from the
        // worktree-specific gitdir (<main-git>/.git/worktrees/<name>).
        val gitFile = rootProject.file(".git")
        val hooksDir: File = if (gitFile.isDirectory) {
            gitFile.resolve("hooks")
        } else {
            val gitdirRef = gitFile.readText().trim().removePrefix("gitdir:").trim()
            val worktreeGitDir = if (File(gitdirRef).isAbsolute) File(gitdirRef) else rootProject.file(gitdirRef)
            // worktreeGitDir = <main>/.git/worktrees/<name> — two levels up is <main>/.git
            worktreeGitDir.parentFile.parentFile.resolve("hooks")
        }
        hooksDir.mkdirs()
        val sourceDir = rootProject.file("scripts/git-hooks")
        sourceDir.listFiles()?.forEach { hook ->
            val dest = hooksDir.resolve(hook.name)
            hook.copyTo(dest, overwrite = true)
            dest.setExecutable(true)
        }
        println("Git hooks installed to ${hooksDir.absolutePath}")
    }
}
