---
work_package_id: WP03
title: Entry Points and app-web Scaffold
lane: "for_review"
dependencies: [WP02]
base_branch: 007-kmp-platform-abstractions-and-di-wiring-WP02
base_commit: 8345accf6e6bf6ab5fe52a54093cfd5beb79fe57
created_at: '2026-03-24T23:31:57.888566+00:00'
subtasks:
- T011
- T012
- T013
phase: Phase 1 - Platform Abstractions
assignee: ''
agent: "claude"
shell_pid: "35955"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-24T22:28:04Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-006
---

# Work Package Prompt: WP03 – Entry Points and `app-web` Scaffold

## ⚠️ IMPORTANT: Review Feedback Status

**Read this first if you are implementing this task!**

- **Has review feedback?**: Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section immediately (right below this notice).
- **You must address all feedback** before your work is complete.
- **Mark as acknowledged**: When you understand the feedback, update `review_status: acknowledged` in frontmatter.

---

## Review Feedback

*[This section is empty initially. Reviewers will populate it if the work is returned from review.]*

---

## Objectives & Success Criteria

- `VibelyApp.kt` exists in `composeApp:androidMain` and is registered as `android:name` in `AndroidManifest.xml`.
- `server/Main.kt` calls `startKoin` before the Ktor engine; `koin-core` and Ktor Netty are in `server/build.gradle.kts`.
- `app-web` module exists with `build.gradle.kts` and a JS entry point that starts Koin.
- `app-web` is registered in `settings.gradle.kts`.
- `./gradlew :composeApp:assembleDebug :server:build :app-web:build` all pass.
- Zero Detekt violations.

**Implement with**: `spec-kitty implement WP03 --base WP02`

---

## Context & Constraints

- **Plan**: `kitty-specs/007-kmp-platform-abstractions-and-di-wiring/plan.md`
- **Spec**: `kitty-specs/007-kmp-platform-abstractions-and-di-wiring/spec.md`
- **Constitution**: `.kittify/memory/constitution.md` — no hardcoded credentials; KDoc on all public symbols.
- **Dependency**: WP02 must be on the branch — `commonModule()` and `platformModule()` must be compiled.
- **Gradle**: Use `libs.xxx` type-safe accessors only.
- **Parallelization**: T011, T012, T013 are fully independent — they touch three separate modules.
- **Scope**: These entry points are intentionally thin. No business logic goes here. Ktor routes and Compose UI are deferred to later phases.

---

## Subtasks & Detailed Guidance

### Subtask T011 – Create `VibelyApp.kt` and update `AndroidManifest.xml`

**Purpose**: Wire Koin initialisation into the Android application lifecycle so all components can inject `SecureStorage`, `PlatformLogger`, and `UserPreferencesRepository` without referencing Android APIs directly.

**Steps**:

1. Check for any existing `Application` subclass in `composeApp/src/androidMain/kotlin/com/vibely/`. If one exists, extend it rather than creating a duplicate.

2. Create (or update) `composeApp/src/androidMain/kotlin/com/vibely/VibelyApp.kt`:
   ```kotlin
   package com.vibely

   import android.app.Application
   import com.vibely.common.platform.ApplicationContextHolder
   import com.vibely.shared.di.commonModule
   import com.vibely.shared.di.platformModule
   import org.koin.android.ext.koin.androidContext
   import org.koin.core.context.startKoin

   /**
    * Application entry point.
    * Initialises the application context holder and Koin with [commonModule] and
    * [platformModule] before any Activity or Service is created.
    */
   class VibelyApp : Application() {
       override fun onCreate() {
           super.onCreate()
           ApplicationContextHolder.init(this)
           startKoin {
               androidContext(this@VibelyApp)
               modules(commonModule(), platformModule())
           }
       }
   }
   ```

3. Register the Application class in `composeApp/src/androidMain/AndroidManifest.xml`. Add `android:name=".VibelyApp"` to the `<application>` element:
   ```xml
   <application
       android:name=".VibelyApp"
       ... >
   ```
   If the manifest already has `android:name`, update it to `.VibelyApp`.

**Files**:
- `composeApp/src/androidMain/kotlin/com/vibely/VibelyApp.kt`
- `composeApp/src/androidMain/AndroidManifest.xml`

**Validation**:
- [ ] `VibelyApp` extends `Application`.
- [ ] `ApplicationContextHolder.init(this)` is called before `startKoin`.
- [ ] `startKoin` passes both `commonModule()` and `platformModule()`.
- [ ] `AndroidManifest.xml` has `android:name=".VibelyApp"`.
- [ ] `VibelyApp` class has KDoc.

---

### Subtask T012 – Update `server/Main.kt` with Koin wiring

**Purpose**: Start Koin before the Ktor engine so all route handlers and repositories can inject dependencies from `platformModule()`.

**Steps**:

1. Open (or create) `server/src/main/kotlin/com/vibely/server/Main.kt`.

2. Replace the `main()` function body with:
   ```kotlin
   package com.vibely.server

   import com.vibely.shared.di.commonModule
   import com.vibely.shared.di.platformModule
   import io.ktor.server.engine.embeddedServer
   import io.ktor.server.netty.Netty
   import org.koin.core.context.startKoin

   /**
    * JVM server entry point.
    * Koin is started before the Ktor engine so all injection is available
    * to route handlers and repositories.
    */
   fun main() {
       startKoin {
           modules(commonModule(), platformModule())
       }
       embeddedServer(Netty, port = 8080) {
           // Ktor configuration follows in Phase 1.2
       }.start(wait = true)
   }
   ```

3. Open `server/build.gradle.kts` and ensure the following dependencies are present:
   ```kotlin
   dependencies {
       implementation(libs.koin.core)
       implementation(libs.ktor.server.core)
       implementation(libs.ktor.server.netty)
       implementation(projects.shared)
   }
   ```
   Verify the Ktor Netty alias (`libs.ktor.server.netty`) is in `libs.versions.toml`. If not, add:
   ```toml
   ktor-server-netty = { module = "io.ktor:ktor-server-netty", version.ref = "ktor" }
   ```
   (The `ktor` version should already be in the catalog from prior features.)

**Files**:
- `server/src/main/kotlin/com/vibely/server/Main.kt`
- `server/build.gradle.kts`

**Validation**:
- [ ] `startKoin` is called before `embeddedServer`.
- [ ] `main()` function has KDoc.
- [ ] `server/build.gradle.kts` has `libs.koin.core`, Ktor core, and Ktor Netty using type-safe accessors.
- [ ] Ktor server body is a stub with comment `// Ktor configuration follows in Phase 1.2`.

---

### Subtask T013 – Scaffold the `app-web` module

**Purpose**: Create the Kotlin/JS browser module that serves as the Web/JS entry point, wiring Koin before any UI is rendered. This module is intentionally minimal — Compose for Web setup follows in Phase 1.3.

**Steps**:

1. Create the module directory structure:
   ```
   app-web/
   ├── build.gradle.kts
   └── src/
       └── jsMain/
           └── kotlin/
               └── com/vibely/web/
                   └── Main.kt
   ```

2. **`app-web/build.gradle.kts`**:
   ```kotlin
   plugins {
       alias(libs.plugins.kotlin.multiplatform)
   }

   kotlin {
       js(IR) {
           browser {
               binaries.executable()
           }
       }
       sourceSets {
           jsMain.dependencies {
               implementation(libs.koin.core)
               implementation(projects.shared)
           }
       }
   }
   ```

3. **`app-web/src/jsMain/kotlin/com/vibely/web/Main.kt`**:
   ```kotlin
   package com.vibely.web

   import com.vibely.shared.di.commonModule
   import com.vibely.shared.di.platformModule
   import org.koin.core.context.startKoin

   /**
    * Web/JS application entry point.
    * Initialises Koin before the UI is rendered.
    */
   fun main() {
       startKoin {
           modules(commonModule(), platformModule())
       }
       // Compose for Web / UI initialisation follows in Phase 1.3
   }
   ```

4. **Register in `settings.gradle.kts`**: Add `include(":app-web")` following the existing module registration pattern. Check the existing `settings.gradle.kts` for the correct placement (typically alongside other `app-*` modules):
   ```kotlin
   include(":app-web")
   ```

**Files**:
- `app-web/build.gradle.kts`
- `app-web/src/jsMain/kotlin/com/vibely/web/Main.kt`
- `settings.gradle.kts` (modified — add `include(":app-web")`)

**Validation**:
- [ ] `app-web/build.gradle.kts` uses `alias(libs.plugins.kotlin.multiplatform)` — no string-based plugin application.
- [ ] `main()` function has KDoc.
- [ ] `startKoin` passes both `commonModule()` and `platformModule()`.
- [ ] `settings.gradle.kts` includes `":app-web"`.
- [ ] `./gradlew :app-web:build` passes.

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Existing Application class in `composeApp` | Check before creating — extend or update the existing class |
| `AndroidManifest.xml` missing `android:name` | Add or update the attribute — do not add a second `<application>` element |
| Ktor Netty alias missing from catalog | Add `ktor-server-netty` to `[libraries]` in `libs.versions.toml` |
| `app-web` module name conflict | Verify `settings.gradle.kts` and choose consistent `:app-web` project path |
| `binaries.executable()` requires Webpack config | Not needed for this feature — just needs to compile; browser testing deferred to Phase 1.3 |

---

## Review Guidance

- Confirm `ApplicationContextHolder.init(this)` precedes `startKoin` in `VibelyApp.kt`.
- Confirm `AndroidManifest.xml` has `android:name=".VibelyApp"`.
- Confirm `server/Main.kt` starts Koin before `embeddedServer`.
- Confirm `app-web` is in `settings.gradle.kts`.
- Confirm `app-web/build.gradle.kts` uses only `libs.xxx` type-safe accessors.
- Run `./gradlew :composeApp:assembleDebug :server:build :app-web:build` — all must pass.

---

## Activity Log

- 2026-03-24T22:28:04Z – system – lane=planned – Prompt created.
- 2026-03-24T23:32:00Z – claude – shell_pid=35955 – lane=doing – Assigned agent via workflow command
- 2026-03-24T23:46:31Z – claude – shell_pid=35955 – lane=for_review – Ready for review: VibelyApp.kt (T011), server/Main.kt (T012), app-web scaffold (T013). Added kmp-js-app convention plugin for type-safe alias() in app-web. Bumped compileSdk/targetSdk to 36 in android-app convention (shared's datastore dep requires it). Yarn lock updated. :composeApp:assembleDebug :server:build :app-web:build all pass. Detekt clean.
