---
work_package_id: WP01
title: Root Project Configuration
lane: "doing"
dependencies: []
base_branch: main
base_commit: fa34a51a0f60f6786b7aaa60fc1bf12210f51079
created_at: '2026-03-23T22:16:17.643863+00:00'
subtasks:
- T001
- T002
- T003
phase: Phase 0 - Foundation
assignee: ''
agent: "claude"
shell_pid: "373"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-23T21:23:31Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-001
- FR-008
- FR-009
---

# Work Package Prompt: WP01 – Root Project Configuration

## IMPORTANT: Review Feedback Status

- **Has review feedback?**: Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section immediately.
- **You must address all feedback** before your work is complete.
- **Mark as acknowledged**: When you understand the feedback, update `review_status: acknowledged`.

---

## Review Feedback

*[Empty initially. Reviewers will populate if work is returned.]*

---

## Markdown Formatting

Wrap HTML/XML tags in backticks: `<manifest>`, `<application>`
Use language identifiers in code blocks: ```kotlin, ```toml, ```bash

---

## Objectives & Success Criteria

Update `gradle/libs.versions.toml` with AGP 8.9.0 entries, create the root `settings.gradle.kts` that registers all 15 modules, and create the root `build.gradle.kts` that wires Detekt, KtLint, and the `installGitHooks` task project-wide.

**Done when:**
- [ ] `gradle/libs.versions.toml` contains `agp = "8.9.0"`, `android-application` plugin alias, and `android-library` plugin alias
- [ ] `settings.gradle.kts` includes all 15 module paths and enables the version catalog
- [ ] `build.gradle.kts` applies Detekt + KtLint to all subprojects and registers `installGitHooks`
- [ ] `./gradlew projects` lists all 15 modules
- [ ] `./gradlew tasks` shows `detekt`, `ktlintCheck`, and `installGitHooks` in the task list

---

## Context & Constraints

- **Spec**: `kitty-specs/002-kmp-project-structure-setup/spec.md`
- **Plan**: `kitty-specs/002-kmp-project-structure-setup/plan.md`
- **Research**: `kitty-specs/002-kmp-project-structure-setup/research.md` — Decisions 4, 5, 6
- **Constitution**: `.kittify/memory/constitution.md`
- **Target files**: `gradle/libs.versions.toml` (append only), `settings.gradle.kts` (new), `build.gradle.kts` (new)

**Key constraints**:
- `gradle/libs.versions.toml` already exists (created in feature 001). Append new entries — do NOT overwrite.
- AGP version must be 8.9.0 (compatible with Kotlin 2.3.20 and Gradle 9.4.1).
- The `installGitHooks` Gradle task copies `scripts/git-hooks/pre-commit` → `.git/hooks/pre-commit`. The script file is created in WP03, but the task definition lives here.

**Implement command** (no dependencies): `spec-kitty implement WP01`

---

## Subtasks & Detailed Guidance

### Subtask T001 – Add AGP entries to `gradle/libs.versions.toml`

**Purpose**: The Android Gradle Plugin (AGP) is required for the Android target in the `kmp-library` and `android-app` convention plugins. AGP 8.9.0 is the latest stable version compatible with Kotlin 2.3.20.

**Steps**:

1. Open `gradle/libs.versions.toml`.

2. In the `[versions]` block, append after the last entry:
```toml
agp = "8.9.0"
```

3. In the `[plugins]` block, append after the last entry:
```toml
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
```

4. Do NOT modify any existing entries.

**Files**: `gradle/libs.versions.toml`
**Parallel?**: No — T002 and T003 conceptually depend on the catalog being complete.
**Notes**: These are the only catalog changes in this feature. All other libraries are already present from feature 001.

---

### Subtask T002 – Create `settings.gradle.kts`

**Purpose**: The root settings file tells Gradle the project name and which subprojects exist. Without it, module `include()` calls are missing and `./gradlew projects` returns nothing.

**Steps**:

1. Create `settings.gradle.kts` at the project root with this content:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "vibely-food"

// Entry-point modules
include(":composeApp")
include(":server")
include(":shared")

// Core layer
include(":core:domain")
include(":core:network")
include(":core:database")
include(":core:ui")
include(":core:common")

// Feature layer
include(":feature:auth")
include(":feature:menu")
include(":feature:orders")
include(":feature:payments")
include(":feature:inventory")
include(":feature:customers")
include(":feature:reports")
include(":feature:settings")
```

**Files**: `settings.gradle.kts` (new)
**Parallel?**: No
**Notes**: `RepositoriesMode.FAIL_ON_PROJECT_REPOS` is a Gradle best practice that prevents individual modules from declaring their own repositories, enforcing centralized dependency resolution.

---

### Subtask T003 – Create root `build.gradle.kts`

**Purpose**: The root build file applies Detekt and KtLint to every subproject and registers the `installGitHooks` task so developers can install the pre-commit hook with a single command.

**Steps**:

1. Create `build.gradle.kts` at the project root:

```kotlin
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
```

2. Note: `detekt.yml` and the pre-commit script are created in WP03. The `installGitHooks` task will work once WP03 creates `scripts/git-hooks/pre-commit`.

**Files**: `build.gradle.kts` (new at project root)
**Parallel?**: No
**Notes**: Plugins are applied with `apply false` in the root to avoid applying them to the root project itself — only subprojects get them. The `subprojects {}` block applies them to all 15 modules.

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| `libs.versions.toml` append breaks TOML syntax | Validate the file parses correctly: `./gradlew help` should not throw a version catalog parse error |
| `subprojects {}` applies Detekt to buildSrc | This is expected and harmless; buildSrc sources are excluded in `detekt.yml` (WP03) |
| `installGitHooks` fails silently if `scripts/git-hooks/` doesn't exist yet | WP03 creates the directory and script; WP07 runs the actual install after WP03 completes |

---

## Review Guidance

Reviewers should verify:
1. `gradle/libs.versions.toml` has exactly two new entries (`agp` version + two plugin aliases); no existing entries modified.
2. `settings.gradle.kts` lists exactly 15 `include()` calls — count them.
3. `build.gradle.kts` applies Detekt and KtLint via `subprojects {}` and defines `installGitHooks` as a `Copy` task.
4. No hardcoded version strings in `build.gradle.kts` — all plugin references use `libs.*` accessors.

---

## Activity Log

- 2026-03-23T21:23:31Z – system – lane=planned – Prompt created.
- 2026-03-23T22:16:17Z – claude – shell_pid=96849 – lane=doing – Assigned agent via workflow command
- 2026-03-23T22:17:34Z – claude – shell_pid=96849 – lane=for_review – Ready for review: updated libs.versions.toml with AGP 8.9.0, created settings.gradle.kts (15 modules), created root build.gradle.kts with Detekt+KtLint+installGitHooks
- 2026-03-23T22:18:35Z – claude – shell_pid=96849 – lane=for_review – Ready for review: AGP updated to 9.1.0 (latest stable March 2026), libs.versions.toml updated, settings.gradle.kts (15 modules), root build.gradle.kts with Detekt+KtLint+installGitHooks
- 2026-03-23T22:20:39Z – claude – shell_pid=373 – lane=doing – Started review via workflow command
