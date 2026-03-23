# Implementation Plan: KMP Project Structure Setup

**Branch**: `002-kmp-project-structure-setup` | **Date**: 2026-03-23 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `kitty-specs/002-kmp-project-structure-setup/spec.md`

---

## Summary

Scaffold the Vibely POS Gradle multi-module KMP project skeleton. Create 15 modules across `composeApp`, `server`, `shared`, `core/*`, and `feature/*` layers. All modules start as empty shells with valid build files. Centralize build logic in `buildSrc` using three convention plugins (`kmp-library`, `android-app`, `jvm-server`). Wire Detekt + KtLint project-wide and enforce them via a Git pre-commit hook installed through a Gradle `installGitHooks` task.

---

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.3.20 (Kotlin/JVM, Kotlin/Android, Kotlin/JS)
**Primary Dependencies**: Compose Multiplatform 1.10.3, Koin 4.2.0 (declared in catalog; not wired in this phase)
**Storage**: N/A for this phase (no source code, only build files)
**Testing**: N/A for this phase (Detekt + KtLint validate build files; no runtime test code)
**Target Platform**: Android (minSdk 26, compileSdk 35, targetSdk 35), JVM (Java 17+), Web (Kotlin/JS browser)
**Project Type**: Kotlin Multiplatform — Gradle multi-module
**Performance Goals**: `./gradlew build` completes on a clean checkout; pre-commit hook completes within 30 seconds
**Constraints**: Module build files ≤15 lines (excluding comments); no direct version strings in build files; `core/domain` zero framework dependencies
**Scale/Scope**: 15 modules at project creation; designed to scale to 30+ modules without structural changes

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Kotlin Multiplatform 2.3.20 | ✅ PASS | Set in `libs.versions.toml` (feature 001) |
| Gradle 9.4.1 | ✅ PASS | `gradle/wrapper/gradle-wrapper.properties` exists (feature 001) |
| `core/domain` zero framework dependencies | ✅ PASS | Module is an empty shell; convention plugin will not add framework deps to domain |
| Detekt zero violations enforced | ✅ PASS | FR-004; wired via project-level Detekt configuration |
| KtLint clean formatting enforced | ✅ PASS | FR-005; wired via project-level KtLint configuration |
| No `if (isDebug)` in business logic | ✅ PASS | No business logic in this phase |
| No mocks in tests | ✅ PASS | No test code in this phase |
| Version catalog for all dependencies | ✅ PASS | FR-008; convention plugins reference `libs.*` accessors |
| Session pooling for PgBouncer | N/A | No database wiring in this phase |
| Offline-first / outbox pattern | N/A | No network code in this phase |

All applicable gates pass. No constitution violations.

---

## Project Structure

### Documentation (this feature)

```
kitty-specs/002-kmp-project-structure-setup/
├── plan.md              # This file
├── research.md          # Phase 0 output (resolved technical decisions)
├── data-model.md        # N/A — no data entities in this feature
├── contracts/           # N/A — no API contracts in this feature
└── tasks.md             # Phase 2 output (/spec-kitty.tasks — NOT created here)
```

### Source Code (repository root)

```
vibely-food-pos/
├── buildSrc/
│   ├── build.gradle.kts                       # buildSrc bootstrap (applies version catalog)
│   └── src/main/kotlin/
│       ├── kmp-library.gradle.kts             # Convention plugin: KMP library modules
│       ├── android-app.gradle.kts             # Convention plugin: Android app entry point
│       └── jvm-server.gradle.kts              # Convention plugin: JVM-only server module
├── composeApp/                                # Android + Desktop entry point
│   ├── build.gradle.kts                       # applies android-app convention plugin
│   └── src/
│       ├── androidMain/kotlin/                # (empty)
│       └── desktopMain/kotlin/                # (empty)
├── server/                                    # Ktor backend (JVM-only)
│   ├── build.gradle.kts                       # applies jvm-server convention plugin
│   └── src/main/kotlin/                       # (empty)
├── shared/                                    # Shared KMP library (all 3 targets)
│   ├── build.gradle.kts                       # applies kmp-library convention plugin
│   └── src/
│       ├── commonMain/kotlin/                 # (empty)
│       ├── androidMain/kotlin/                # (empty)
│       ├── jvmMain/kotlin/                    # (empty)
│       └── jsMain/kotlin/                     # (empty)
├── core/
│   ├── domain/                                # Pure Kotlin — zero framework deps
│   │   ├── build.gradle.kts                   # applies kmp-library (no extra deps)
│   │   └── src/commonMain/kotlin/             # (empty)
│   ├── network/
│   │   ├── build.gradle.kts                   # applies kmp-library
│   │   └── src/commonMain/kotlin/             # (empty)
│   ├── database/
│   │   ├── build.gradle.kts                   # applies kmp-library
│   │   └── src/commonMain/kotlin/             # (empty)
│   ├── ui/
│   │   ├── build.gradle.kts                   # applies kmp-library
│   │   └── src/commonMain/kotlin/             # (empty)
│   └── common/
│       ├── build.gradle.kts                   # applies kmp-library
│       └── src/commonMain/kotlin/             # (empty)
├── feature/
│   ├── auth/
│   │   ├── build.gradle.kts                   # applies kmp-library
│   │   └── src/commonMain/kotlin/             # (empty)
│   ├── menu/        (same structure as auth)
│   ├── orders/      (same structure as auth)
│   ├── payments/    (same structure as auth)
│   ├── inventory/   (same structure as auth)
│   ├── customers/   (same structure as auth)
│   ├── reports/     (same structure as auth)
│   └── settings/    (same structure as auth)
├── gradle/
│   ├── libs.versions.toml                     # Already exists (feature 001)
│   └── wrapper/
│       └── gradle-wrapper.properties          # Already exists (feature 001)
├── settings.gradle.kts                        # Includes all 15 modules
├── build.gradle.kts                           # Root: Detekt + KtLint project-wide config
├── detekt.yml                                 # Detekt ruleset configuration
├── .gitignore                                 # Updated: exclude .gradle/, build/, local.properties
├── scripts/
│   └── git-hooks/
│       └── pre-commit                         # Hook script: runs detekt + ktlintCheck
└── gradlew / gradlew.bat                      # Gradle wrapper scripts
```

**Structure Decision**: Kotlin Multiplatform multi-module with three module types.
- `kmp-library`: Applied to `shared`, all `core/*`, all `feature/*` — declares Android + JVM + JS targets.
- `android-app`: Applied to `composeApp` — Android app entry point + Desktop (JVM) target.
- `jvm-server`: Applied to `server` — JVM-only, no KMP targets.

---

## Complexity Tracking

No constitution violations requiring justification. All decisions align with established standards.

---

## Phase 0: Research

*All resolved in `research.md`.*

Key decisions researched:
1. **Convention plugin approach for Gradle 9.x + KMP 2.3.x** — verified `buildSrc` precompiled script plugins work with version catalog accessors in Gradle 9.4.1.
2. **Kotlin/JS browser target in KMP 2.3.x** — `js(IR) { browser() }` is the correct declaration; `wasmJs` deferred.
3. **Detekt + KtLint project-wide wiring pattern** — root `build.gradle.kts` applies plugins to all subprojects via `allprojects {}` or explicit subproject inclusion.
4. **Git pre-commit hook via Gradle task** — `installGitHooks` task copies `scripts/git-hooks/pre-commit` to `.git/hooks/pre-commit` and sets executable bit.
5. **Android SDK in KMP convention plugin** — `android { compileSdk = 35; defaultConfig { minSdk = 26; targetSdk = 35 } }`.

---

## Phase 1: Implementation Steps

### Step 1 — Root project files (`settings.gradle.kts`, root `build.gradle.kts`)

**`settings.gradle.kts`**: Enable version catalog feature preview (Gradle 9 default), declare all 15 module includes using composite build paths (e.g., `include(":core:domain")`).

**Root `build.gradle.kts`**: Apply Detekt and KtLint to all subprojects. Configure Detekt to use `detekt.yml` baseline. Configure KtLint with editorconfig. Register `installGitHooks` task.

### Step 2 — `buildSrc` convention plugins

**`buildSrc/build.gradle.kts`**: Apply `kotlin-dsl` plugin. Reference version catalog via `versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }`.

**`kmp-library.gradle.kts`**:
- Applies `kotlin-multiplatform` and `kotlin-serialization` plugins via `alias(libs.plugins.kotlin.multiplatform)`
- Declares three targets: `androidTarget()`, `jvm()`, `js(IR) { browser() }`
- Configures Android block: `compileSdk = 35`, `minSdk = 26` (set via extension or property)
- Sets `commonMain` source set with no default dependencies (caller adds their own)

**`android-app.gradle.kts`**:
- Applies `com.android.application` + `kotlin-multiplatform` + `compose-multiplatform` + `compose-compiler`
- Declares `androidTarget()` + `jvm()` (Desktop) targets
- Android block: `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`
- Default Compose UI bundle wired into `commonMain`

**`jvm-server.gradle.kts`**:
- Applies `kotlin-jvm` + `ktor` plugin
- Sets `jvmToolchain(17)`
- No KMP targets — pure JVM

### Step 3 — Module scaffold

For each of the 15 modules:
1. Create directory structure matching the source tree above
2. Create `build.gradle.kts` applying the appropriate convention plugin
3. Create empty `src/commonMain/kotlin/` (KMP modules) or `src/main/kotlin/` (server)
4. Add a `.gitkeep` placeholder in each empty source directory

`core/domain` build file explicitly declares no dependencies beyond `kotlin-stdlib` (enforced by convention plugin having no default deps for this module type).

### Step 4 — Detekt configuration (`detekt.yml`)

Configure rulesets aligned with constitution:
- `complexity`: enable `CyclomaticComplexity`, `NestedBlockDepth`, `LongMethod`, `TooManyFunctions`
- `style`: enable `UndocumentedPublicClass`, `UndocumentedPublicFunction`, `UndocumentedPublicProperty`
- `potential-bugs`: enable all
- `performance`: enable all
- `coroutines`: enable all
- Exclude `build/` and `buildSrc/` directories from analysis

### Step 5 — Git pre-commit hook

**`scripts/git-hooks/pre-commit`** (bash script):
```
#!/bin/sh
./gradlew detekt ktlintCheck --daemon
```
Abort commit on non-zero exit.

**`installGitHooks` Gradle task** (in root `build.gradle.kts`):
- Type: `Copy`
- Copies `scripts/git-hooks/pre-commit` → `.git/hooks/pre-commit`
- Sets executable permission via `doLast { file(".git/hooks/pre-commit").setExecutable(true) }`

**README update**: Document that new contributors must run `./gradlew installGitHooks` once after cloning.

### Step 6 — Gradle wrapper scripts

Verify `gradlew` and `gradlew.bat` exist. If missing, generate via `gradle wrapper --gradle-version=9.4.1` or copy standard wrapper scripts. These are committed to the repository.

---

## Risks & Mitigations

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Version catalog not accessible in `buildSrc` on Gradle 9.x | Medium | Use `versionCatalogs { create("libs") { from(files(...)) } }` in `buildSrc/build.gradle.kts`; verified in research |
| Kotlin/JS target configuration changes in KMP 2.3.x | Low | Use `js(IR) { browser() }` — stable IR compiler; wasmJs deferred |
| Android target in KMP requires AGP version alignment | Medium | Pin AGP version in version catalog; research resolves exact compatible version |
| `detekt` scan too slow in pre-commit (>30s) | Low | Run `--daemon` for warm JVM; scope to changed files via `--input` if needed |
| `installGitHooks` silently not run by contributors | Medium | Document in README; optionally add hook to `prepareKotlinBuildScriptModel` task |
| Empty source sets causing Kotlin/JS compiler warnings | Low | Suppress via `kotlin.js.generate.executable.default=false` property if needed |
