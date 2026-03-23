---
work_package_id: WP02
title: buildSrc Convention Plugins
lane: "doing"
dependencies: [WP01]
base_branch: 002-kmp-project-structure-setup-WP01
base_commit: 06994075dbf5ca82d9e31cccc788524b8c1a8f4d
created_at: '2026-03-23T22:22:43.305474+00:00'
subtasks:
- T004
- T005
- T006
- T007
phase: Phase 0 - Foundation
assignee: ''
agent: "claude"
shell_pid: "1314"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-23T21:23:31Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-002
- FR-003
- FR-008
---

# Work Package Prompt: WP02 – buildSrc Convention Plugins

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

Create the `buildSrc` Gradle project with three precompiled convention plugins that serve as the single source of truth for all KMP target declarations:
- `kmp-library`: targets Android (minSdk=26), JVM, Kotlin/JS browser — used by `shared`, `core/*`, `feature/*`
- `android-app`: targets Android + Desktop (JVM) — used by `composeApp`
- `jvm-server`: JVM-only with Java 17 toolchain — used by `server`

**Done when:**
- [ ] `buildSrc/build.gradle.kts` and `buildSrc/settings.gradle.kts` exist and are valid
- [ ] Three convention plugin files exist in `buildSrc/src/main/kotlin/`
- [ ] A module applying `kmp-library` compiles for Android, JVM, and JS targets
- [ ] A module applying `jvm-server` does NOT have KMP targets (JVM-only)
- [ ] All plugin references use `libs.*` version catalog accessors — no hardcoded versions

---

## Context & Constraints

- **Spec**: `kitty-specs/002-kmp-project-structure-setup/spec.md`
- **Plan**: `kitty-specs/002-kmp-project-structure-setup/plan.md`
- **Research**: `kitty-specs/002-kmp-project-structure-setup/research.md` — Decisions 1, 2, 3, 4
- **Constitution**: `.kittify/memory/constitution.md`
- **Target files**: `buildSrc/settings.gradle.kts` (new), `buildSrc/build.gradle.kts` (new), `buildSrc/src/main/kotlin/*.gradle.kts` (new)

**Key constraints**:
- `buildSrc` must re-declare the version catalog — it cannot inherit the root project's catalog automatically (see research.md Decision 2).
- Kotlin/JS target MUST use `js(IR) { browser() }` — the legacy compiler is removed in Kotlin 2.x.
- Android block requires `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`.
- `core/domain` module must have zero framework dependencies — the `kmp-library` plugin must NOT add framework dependencies by default.
- `jvm-server` is NOT KMP — it uses `kotlin("jvm")`, not `kotlin-multiplatform`.

**Implement command** (depends on WP01): `spec-kitty implement WP02 --base WP01`

---

## Subtasks & Detailed Guidance

### Subtask T004 – Create `buildSrc` bootstrap files

**Purpose**: `buildSrc` is a special Gradle project that is automatically compiled before the main build. Its `settings.gradle.kts` must re-declare the version catalog so the convention plugins can reference `libs.*` accessors.

**Steps**:

1. Create `buildSrc/settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
```

2. Create `buildSrc/build.gradle.kts`:

```kotlin
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.plugins.kotlin.multiplatform.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.kotlin.android.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.compose.multiplatform.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.compose.compiler.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.android.application.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.android.library.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.ktor.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.kotlin.serialization.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
}
```

**Note**: This pattern (resolving plugin as a dependency) is the correct way to make external plugins available inside `buildSrc` convention plugins in Gradle 9.x.

3. Create the source directory:
```bash
mkdir -p buildSrc/src/main/kotlin
```

**Files**: `buildSrc/settings.gradle.kts` (new), `buildSrc/build.gradle.kts` (new), `buildSrc/src/main/kotlin/` (directory)
**Parallel?**: No — T005/T006/T007 depend on these files existing.

---

### Subtask T005 – Create `kmp-library.gradle.kts` convention plugin

**Purpose**: This plugin is applied by all KMP library modules (`shared`, `core/*`, `feature/*`). It declares the three platform targets, configures the Android target's SDK levels, and sets up the Kotlin source sets. Keeping this in one place means target changes are made once.

**Steps**:

1. Create `buildSrc/src/main/kotlin/kmp-library.gradle.kts`:

```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm()

    js(IR) {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            // No default dependencies — each module declares its own
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // namespace is set per-module in each module's build.gradle.kts
}
```

2. **Important**: Each module applying `kmp-library` must set its own `android { namespace = "com.vibely.<module>" }` in its build file. The convention plugin does NOT set namespace (it differs per module).

**Files**: `buildSrc/src/main/kotlin/kmp-library.gradle.kts` (new)
**Parallel?**: Yes — can be written alongside T006 and T007.

---

### Subtask T006 – Create `android-app.gradle.kts` convention plugin

**Purpose**: This plugin is applied exclusively by `composeApp`. It configures the Android application entry point with Compose enabled, targeting Android + Desktop (JVM). The Web target is NOT included since the web UI entry point is a separate concern.

**Steps**:

1. Create `buildSrc/src/main/kotlin/android-app.gradle.kts`:

```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
```

**Files**: `buildSrc/src/main/kotlin/android-app.gradle.kts` (new)
**Parallel?**: Yes — alongside T005 and T007.
**Notes**: The JVM target is named `"desktop"` to distinguish it from the standard `jvm()` target used in library modules.

---

### Subtask T007 – Create `jvm-server.gradle.kts` convention plugin

**Purpose**: This plugin is applied by the `server` module. The backend is JVM-only (no KMP targets). It sets up the Java 17 toolchain and the Ktor server plugin.

**Steps**:

1. Create `buildSrc/src/main/kotlin/jvm-server.gradle.kts`:

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.ktor.plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

**Files**: `buildSrc/src/main/kotlin/jvm-server.gradle.kts` (new)
**Parallel?**: Yes — alongside T005 and T006.
**Notes**: This is NOT a KMP plugin — it uses `kotlin("jvm")` / `org.jetbrains.kotlin.jvm`. No `kotlin {}` multiplatform block.

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Version catalog not accessible in `buildSrc` | `buildSrc/settings.gradle.kts` re-declares catalog via `from(files("../gradle/libs.versions.toml"))` — confirmed working in Gradle 9.x (research.md Decision 2) |
| Kotlin/JS IR target causes empty source set warning | Suppress with `kotlin.js.generate.executable.default=false` in `gradle.properties` if needed (WP07 handles this) |
| `android {}` block requires `namespace` — fails if not set | Each module must set `android { namespace = "..." }` in its own `build.gradle.kts`. Document this requirement clearly in WP04/WP05/WP06. |
| `compose.runtime` accessor not available in `kmp-library` | `kmp-library` does NOT apply Compose by default — only `android-app` does. Feature modules that need Compose will add it themselves. |
| `buildSrc` plugin dependency pattern is verbose | This is the standard Gradle 9.x approach; no simpler alternative exists for external plugins in buildSrc |

---

## Review Guidance

Reviewers should verify:
1. `buildSrc/settings.gradle.kts` declares the version catalog pointing to `../gradle/libs.versions.toml`.
2. `kmp-library.gradle.kts` declares exactly three targets: `androidTarget()`, `jvm()`, `js(IR) { browser() }`.
3. `android-app.gradle.kts` declares exactly two targets: `androidTarget()`, `jvm("desktop")` — NO `js` target.
4. `jvm-server.gradle.kts` has NO `kotlin {}` multiplatform block — pure JVM.
5. No hardcoded version strings in any convention plugin file — all versions come from the Kotlin/AGP/Ktor plugin versions declared in `libs.versions.toml`.
6. `kmp-library.gradle.kts` has an empty `commonMain.dependencies {}` block — no default framework deps (critical for `core/domain`).

---

## Activity Log

- 2026-03-23T21:23:31Z – system – lane=planned – Prompt created.
- 2026-03-23T22:22:43Z – claude – shell_pid=1314 – lane=doing – Assigned agent via workflow command
