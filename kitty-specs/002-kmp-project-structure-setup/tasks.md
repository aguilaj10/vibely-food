---
description: "Work package task list for KMP Project Structure Setup"
---

# Work Packages: KMP Project Structure Setup

**Inputs**: Design documents from `kitty-specs/002-kmp-project-structure-setup/`
**Prerequisites**: plan.md, spec.md, research.md

---

## Work Package WP01: Root Project Configuration (Priority: P0)

**Goal**: Update the version catalog with AGP entries, create the root `settings.gradle.kts` that registers all 15 modules, and create the root `build.gradle.kts` that wires Detekt, KtLint, and the `installGitHooks` task project-wide.

**Independent Test**: `./gradlew projects` lists all 15 modules without errors. `./gradlew tasks` shows `detekt`, `ktlintCheck`, and `installGitHooks` tasks.

**Prompt**: `kitty-specs/002-kmp-project-structure-setup/tasks/WP01-root-project-configuration.md`

**Estimated prompt size**: ~250 lines

### Included Subtasks

- [x] T001 Add `agp` version and `android-application` / `android-library` plugin entries to `gradle/libs.versions.toml`
- [x] T002 Create `settings.gradle.kts` at project root — enable version catalog, include all 15 module paths
- [x] T003 Create root `build.gradle.kts` — apply Detekt + KtLint to all subprojects, register `installGitHooks` task

**Requirements Refs**: FR-001, FR-008, FR-009

### Implementation Notes

1. T001 is a pure catalog edit — append two entries to the existing file from feature 001.
2. T002 declares all module paths using `:core:domain`, `:feature:auth` etc. (colon-separated Gradle paths).
3. T003 applies plugins via `subprojects {}` block; `installGitHooks` task is type `Copy` that copies the pre-commit script.

### Parallel Opportunities

- None — T002 and T003 both need T001's AGP entries resolved first.

### Dependencies

- None (first work package).

### Risks & Mitigations

- Version catalog file already exists — must append, never overwrite. AGP 8.9.0 must align with Kotlin 2.3.20.

---

## Work Package WP02: buildSrc Convention Plugins (Priority: P0)

**Goal**: Create the `buildSrc` project with three precompiled script convention plugins: `kmp-library` (Android + JVM + Kotlin/JS browser), `android-app` (Android + Desktop entry point), and `jvm-server` (JVM-only Ktor server). These are the sole source of truth for target declarations across all modules.

**Independent Test**: A module that applies `alias(libs.plugins.kmpLibrary)` compiles without errors for Android, JVM, and JS targets after running `./gradlew build`.

**Prompt**: `kitty-specs/002-kmp-project-structure-setup/tasks/WP02-buildSrc-convention-plugins.md`

**Estimated prompt size**: ~380 lines

### Included Subtasks

- [x] T004 Create `buildSrc/settings.gradle.kts` (version catalog reference) and `buildSrc/build.gradle.kts` (apply `kotlin-dsl`)
- [x] T005 Create `kmp-library.gradle.kts` — KMP convention plugin (Android target minSdk=26, JVM target, JS browser target)
- [x] T006 Create `android-app.gradle.kts` — Android app convention plugin (composeApp entry point, Android + Desktop)
- [x] T007 Create `jvm-server.gradle.kts` — JVM-only server convention plugin (jvmToolchain 17, Ktor plugin)

**Requirements Refs**: FR-002, FR-003, FR-008

### Implementation Notes

1. `buildSrc/settings.gradle.kts` must re-declare the version catalog with `from(files("../gradle/libs.versions.toml"))`.
2. `kmp-library.gradle.kts` uses `androidTarget()`, `jvm()`, and `js(IR) { browser() }`.
3. `android-app.gradle.kts` uses `androidTarget()` + `jvm()` (Desktop) — NOT js.
4. `jvm-server.gradle.kts` is NOT a KMP module — applies `kotlin("jvm")` + `alias(libs.plugins.ktor)`.

### Parallel Opportunities

- T005, T006, T007 can be written in parallel (different files, same buildSrc project).

### Dependencies

- Depends on WP01 (AGP entries in version catalog, root settings.gradle.kts).

### Risks & Mitigations

- Version catalog access in buildSrc requires explicit re-declaration — see research.md Decision 2.
- Kotlin/JS IR mode is the only supported mode in Kotlin 2.x — do not use legacy.

---

## Work Package WP03: Quality Tool Configuration (Priority: P0)

**Goal**: Configure Detekt and KtLint with constitution-aligned rulesets, create the Git pre-commit hook script, and verify the Gradle wrapper scripts are present.

**Independent Test**: `./gradlew detekt ktlintCheck` passes on the empty skeleton with no violations. Running `git commit` on a file with a known lint violation aborts the commit.

**Prompt**: `kitty-specs/002-kmp-project-structure-setup/tasks/WP03-quality-tool-configuration.md`

**Estimated prompt size**: ~260 lines

### Included Subtasks

- [x] T008 Create `detekt.yml` at project root with constitution-aligned ruleset (complexity, KDoc enforcement, coroutines, performance)
- [x] T009 Create `.editorconfig` at project root for KtLint (indent=4, max_line_length=120, no-wildcard-imports)
- [x] T010 [P] Create `scripts/git-hooks/pre-commit` bash script that runs `./gradlew detekt ktlintCheck --daemon`
- [x] T011 [P] Verify `gradlew` and `gradlew.bat` exist; document how to regenerate if missing

**Requirements Refs**: FR-004, FR-005, FR-006, FR-009

### Implementation Notes

1. `detekt.yml` must enable `UndocumentedPublicClass`, `UndocumentedPublicFunction`, `UndocumentedPublicProperty` (constitution requirement).
2. `.editorconfig` applies to `*.kt` and `*.kts` files.
3. Pre-commit script must have `#!/bin/sh` header and be marked executable via `installGitHooks` task (defined in WP01's root `build.gradle.kts`).
4. T011 is a verification step — check if files exist, add note in README if they need regeneration.

### Parallel Opportunities

- T010 and T011 can run in parallel (independent files).
- WP03 can run in parallel with WP02 (different concerns).

### Dependencies

- Depends on WP01 (root `build.gradle.kts` defines the `installGitHooks` task that copies the pre-commit script).

### Risks & Mitigations

- Detekt scan on empty source sets should produce zero violations — verify this before WP07 smoke test.
- `.editorconfig` must exist at root; KtLint 14.x reads it automatically.

---

## Work Package WP04: App & Server Entry-Point Modules (Priority: P1)

**Goal**: Create the three entry-point modules: `composeApp` (Android + Desktop), `server` (JVM-only Ktor backend), and `shared` (full KMP library targeting all three platforms).

**Independent Test**: `./gradlew :composeApp:build :server:build :shared:build` completes without errors. Each module appears in `./gradlew projects` output.

**Prompt**: `kitty-specs/002-kmp-project-structure-setup/tasks/WP04-app-server-modules.md`

**Estimated prompt size**: ~220 lines

### Included Subtasks

- [x] T012 [P] Create `composeApp/` module — `build.gradle.kts` applying `android-app` plugin, empty source dirs for androidMain + desktopMain
- [x] T013 [P] Create `server/` module — `build.gradle.kts` applying `jvm-server` plugin, empty `src/main/kotlin/` dir
- [x] T014 [P] Create `shared/` module — `build.gradle.kts` applying `kmp-library` plugin, empty source dirs for commonMain + androidMain + jvmMain + jsMain

**Requirements Refs**: FR-001, FR-002

### Implementation Notes

1. All three are independent — can be created in parallel.
2. Each module needs a `.gitkeep` in each empty source directory so git tracks the directory.
3. `composeApp/build.gradle.kts` should declare no module-level dependencies in this phase (empty shell).

### Parallel Opportunities

- T012, T013, T014 can all run in parallel (different directories).

### Dependencies

- Depends on WP02 (convention plugins must exist before modules apply them).

### Risks & Mitigations

- `android-app` plugin requires an `AndroidManifest.xml` placeholder — create minimal manifest in `composeApp/src/androidMain/`.

---

## Work Package WP05: Core Layer Modules (Priority: P1)

**Goal**: Create the five `core/*` modules: `domain` (pure Kotlin, zero framework deps), `network`, `database`, `ui`, and `common`. Each applies the `kmp-library` convention plugin and has empty source sets.

**Independent Test**: `./gradlew :core:domain:build :core:network:build` (and remaining core modules) completes without errors. `core/domain` build file contains no dependency declarations other than what the convention plugin provides.

**Prompt**: `kitty-specs/002-kmp-project-structure-setup/tasks/WP05-core-layer-modules.md`

**Estimated prompt size**: ~300 lines

### Included Subtasks

- [x] T015 Create `core/domain/` module — `build.gradle.kts` applying `kmp-library` with explicit comment: "No framework dependencies allowed. Pure Kotlin only."
- [x] T016 [P] Create `core/network/` module — `build.gradle.kts` applying `kmp-library`
- [x] T017 [P] Create `core/database/` module — `build.gradle.kts` applying `kmp-library`
- [x] T018 [P] Create `core/ui/` module — `build.gradle.kts` applying `kmp-library`
- [x] T019 [P] Create `core/common/` module — `build.gradle.kts` applying `kmp-library`

**Requirements Refs**: FR-001, FR-002, FR-007

### Implementation Notes

1. `core/domain` is the most critical — its build file must have a comment block explicitly documenting the zero-framework-deps rule.
2. T016–T019 are identical in structure; create them in parallel.
3. Each module needs a `core/<name>/` directory in `settings.gradle.kts` — already included in WP01's T002.

### Parallel Opportunities

- T016, T017, T018, T019 can run in parallel (different directories).

### Dependencies

- Depends on WP02 (convention plugins must exist).
- Can run in parallel with WP04.

### Risks & Mitigations

- Verify Detekt `ForbiddenImport` rule is scoped to `core/domain` in `detekt.yml` (WP03) — prevents accidental framework imports.

---

## Work Package WP06: Feature Layer Module Placeholders (Priority: P1) MVP

**Goal**: Create all eight `feature/*` module placeholder directories: `auth`, `menu`, `orders`, `payments`, `inventory`, `customers`, `reports`, `settings`. Each is an empty KMP library shell applying the `kmp-library` convention plugin.

**Independent Test**: `./gradlew :feature:auth:build` (and all other feature modules) completes without errors. `./gradlew projects` shows all 8 feature modules.

**Prompt**: `kitty-specs/002-kmp-project-structure-setup/tasks/WP06-feature-module-placeholders.md`

**Estimated prompt size**: ~280 lines

### Included Subtasks

- [x] T020 [P] Create `feature/auth/` module (pattern module — defines the structure for all feature modules)
- [x] T021 [P] Create `feature/menu/` module (same pattern as T020)
- [x] T022 [P] Create `feature/orders/` module (same pattern as T020)
- [x] T023 [P] Create `feature/payments/` module (same pattern as T020)
- [x] T024 [P] Create `feature/inventory/` module (same pattern as T020)
- [x] T025 [P] Create `feature/customers/` module (same pattern as T020)
- [x] T026 [P] Create `feature/reports/` module (same pattern as T020)
- [x] T027 [P] Create `feature/settings/` module (same pattern as T020)

**Requirements Refs**: FR-001, FR-002

### Implementation Notes

1. T020 is the reference implementation — defines the exact file structure. T021–T027 replicate it with the correct name.
2. Each module contains: `build.gradle.kts` (10 lines max), `src/commonMain/kotlin/.gitkeep`.
3. Feature modules in this phase declare no inter-module dependencies (no `projects.core.domain` yet).

### Parallel Opportunities

- All 8 subtasks can run in parallel (independent directories).

### Dependencies

- Depends on WP02 (kmp-library convention plugin must exist).
- Can run in parallel with WP04 and WP05.

### Risks & Mitigations

- Feature modules do NOT depend on `core/domain` in this phase — that wiring comes in later features.

---

## Work Package WP07: Smoke Test & Documentation (Priority: P0)

**Goal**: Run the full build smoke test, verify the Git hook installation, update `.gitignore` for Gradle artifacts, and add developer setup instructions to `README.md`.

**Independent Test**: `./gradlew build` passes on a clean checkout. `./gradlew installGitHooks` installs the hook. A commit with a lint violation is rejected.

**Prompt**: `kitty-specs/002-kmp-project-structure-setup/tasks/WP07-smoke-test-and-docs.md`

**Estimated prompt size**: ~250 lines

### Included Subtasks

- [x] T028 Update `.gitignore` — add Gradle artifact directories (`build/`, `.gradle/`, `local.properties`) and confirm existing entries are preserved
- [x] T029 Update `README.md` — add "Getting Started" section with setup commands (`./gradlew installGitHooks`, `./gradlew build`)
- [x] T030 Run `./gradlew build` smoke test — verify all 15 modules compile; document any empty-source-set warnings and how to suppress them
- [ ] T031 Run `./gradlew installGitHooks` — verify `.git/hooks/pre-commit` exists and is executable; test by staging a trivial lint violation

**Requirements Refs**: FR-001, FR-002, FR-003, FR-004, FR-005, FR-006, FR-007, FR-008, FR-009

### Implementation Notes

1. T028 and T029 can run in parallel (different files).
2. T030 is the acceptance gate — if the build fails, diagnose and fix in the same WP.
3. T031 validates SC-004 (commit rejected in <30s).

### Parallel Opportunities

- T028 and T029 can run in parallel.

### Dependencies

- Depends on WP04, WP05, WP06 (all modules must exist for the smoke test).

### Risks & Mitigations

- Empty Kotlin/JS source sets may emit a warning — suppress with `kotlin.js.generate.executable.default=false` in `gradle.properties` if needed.
- `gradlew` wrapper scripts may be missing — T030 will catch this; generate via `gradle wrapper --gradle-version=9.4.1` if so.

---

## Dependency & Execution Summary

- **Sequence**: WP01 → WP02 → (WP03, WP04, WP05, WP06 in parallel) → WP07
- **Parallelization**: WP03, WP04, WP05, WP06 can all execute simultaneously after WP02 completes.
- **MVP Scope**: WP01 + WP02 + WP04 = minimum compilable project. WP03 + WP05 + WP06 + WP07 complete the full deliverable.

---

## Subtask Index (Reference)

| Subtask ID | Summary                                              | Work Package | Priority | Parallel? |
|------------|------------------------------------------------------|--------------|----------|-----------|
| T001       | Add AGP entries to libs.versions.toml                | WP01         | P0       | No        |
| T002       | Create root settings.gradle.kts                      | WP01         | P0       | No        |
| T003       | Create root build.gradle.kts                         | WP01         | P0       | No        |
| T004       | Create buildSrc settings + build files               | WP02         | P0       | No        |
| T005       | Create kmp-library convention plugin                 | WP02         | P0       | Yes       |
| T006       | Create android-app convention plugin                 | WP02         | P0       | Yes       |
| T007       | Create jvm-server convention plugin                  | WP02         | P0       | Yes       |
| T008       | Create detekt.yml                                    | WP03         | P0       | No        |
| T009       | Create .editorconfig                                 | WP03         | P0       | Yes       |
| T010       | Create scripts/git-hooks/pre-commit                  | WP03         | P0       | Yes       |
| T011       | Verify gradlew/gradlew.bat exist                     | WP03         | P0       | Yes       |
| T012       | Create composeApp module                             | WP04         | P1       | Yes       |
| T013       | Create server module                                 | WP04         | P1       | Yes       |
| T014       | Create shared module                                 | WP04         | P1       | Yes       |
| T015       | Create core/domain module (zero framework deps)      | WP05         | P1       | No        |
| T016       | Create core/network module                           | WP05         | P1       | Yes       |
| T017       | Create core/database module                          | WP05         | P1       | Yes       |
| T018       | Create core/ui module                                | WP05         | P1       | Yes       |
| T019       | Create core/common module                            | WP05         | P1       | Yes       |
| T020       | Create feature/auth module (reference pattern)       | WP06         | P1       | Yes       |
| T021       | Create feature/menu module                           | WP06         | P1       | Yes       |
| T022       | Create feature/orders module                         | WP06         | P1       | Yes       |
| T023       | Create feature/payments module                       | WP06         | P1       | Yes       |
| T024       | Create feature/inventory module                      | WP06         | P1       | Yes       |
| T025       | Create feature/customers module                      | WP06         | P1       | Yes       |
| T026       | Create feature/reports module                        | WP06         | P1       | Yes       |
| T027       | Create feature/settings module                       | WP06         | P1       | Yes       |
| T028       | Update .gitignore                                    | WP07         | P0       | Yes       |
| T029       | Update README.md                                     | WP07         | P0       | Yes       |
| T030       | Run ./gradlew build smoke test                       | WP07         | P0       | No        |
| T031       | Run installGitHooks + verify pre-commit hook         | WP07         | P0       | No        |
