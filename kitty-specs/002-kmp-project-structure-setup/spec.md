# Feature Specification: KMP Project Structure Setup

**Feature Branch**: `002-kmp-project-structure-setup`
**Created**: 2026-03-23
**Status**: Draft
**Input**: Phase 0.1 of IMPLEMENTATION_PLAN.md — scaffold the Gradle multi-module KMP project for Vibely POS

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Developer Can Build the Project Immediately (Priority: P1)

A developer clones the repository, runs the build command, and the entire project compiles successfully across all three platforms (Android, JVM, Web). Modules are empty shells but structurally complete — all Gradle configurations, source set declarations, and inter-module dependencies are wired and valid.

**Why this priority**: The project cannot advance to any feature work until a clean, buildable skeleton exists. Every other phase depends on this foundation.

**Independent Test**: `./gradlew build` completes without errors or unresolved dependency warnings on a clean checkout with no prior Gradle cache.

**Acceptance Scenarios**:

1. **Given** a clean checkout with no Gradle cache, **When** a developer runs `./gradlew build`, **Then** all modules compile without errors across Android, JVM, and Web targets.
2. **Given** a module with an empty source set, **When** the build runs, **Then** the module participates in the multi-module build graph without skipping or failing.
3. **Given** the project root, **When** `./gradlew projects` is run, **Then** all defined modules (composeApp, server, shared, core/*, feature/*) appear in the project hierarchy.

---

### User Story 2 - Developer Violating Quality Rules Is Blocked at Commit Time (Priority: P2)

A developer attempts to commit code that contains a lint or style violation. The Git pre-commit hook intercepts the commit, runs quality checks, reports the violation, and aborts the commit. The developer fixes the issue and the subsequent commit is accepted.

**Why this priority**: Quality gates must be enforced locally before code reaches CI. This prevents feedback loops that slow down development and ensures every committed state is clean.

**Independent Test**: Add a file with a known lint violation, run `git commit`, verify the hook aborts the commit and displays the violation. Fix the file, re-commit, and verify the commit succeeds.

**Acceptance Scenarios**:

1. **Given** a staged file containing a Detekt rule violation, **When** the developer runs `git commit`, **Then** the commit is aborted and the violation location is printed.
2. **Given** a staged file containing a KtLint formatting issue, **When** the developer runs `git commit`, **Then** the commit is aborted and the affected line is reported.
3. **Given** all staged files pass Detekt and KtLint, **When** the developer runs `git commit`, **Then** the commit completes normally without additional prompts.

---

### User Story 3 - Developer Can Add a New Module Without Duplicating Build Boilerplate (Priority: P3)

A developer creating a new feature module applies a single convention plugin in the module's build file, and the standard KMP target configuration, dependency management, and quality tool wiring are inherited automatically. No copy-pasting of target declarations or plugin application blocks is needed.

**Why this priority**: Convention plugins prevent configuration drift across modules. As the number of modules grows, maintainability depends on a single source of truth for build logic.

**Independent Test**: Create a minimal new module that applies only the KMP convention plugin; verify it compiles for all three targets and participates in Detekt + KtLint scans without any additional configuration.

**Acceptance Scenarios**:

1. **Given** a new module with a build file that applies only the KMP convention plugin, **When** `./gradlew build` runs, **Then** the module compiles for Android, JVM, and Web without additional target declarations.
2. **Given** a convention plugin change (e.g., adding a new target), **When** `./gradlew build` runs across all modules, **Then** every module picks up the change without individual build file edits.
3. **Given** a module using the convention plugin, **When** `./gradlew detekt` runs, **Then** Detekt analyzes that module's source sets without requiring module-level Detekt configuration.

---

### Edge Cases

- What happens when a KMP target (e.g., Web) has no source files? The build must still succeed without emitting warnings about empty source sets.
- What happens if the pre-commit hook is skipped via `git commit --no-verify`? The skip is permitted (developer override) but CI quality gates still catch violations before merge.
- What if a developer adds a circular dependency between modules (e.g., `core/domain` depending on a `feature` module)? The build must fail with a clear error message.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The project MUST have a Gradle multi-module hierarchy containing the following modules: `composeApp`, `server`, `shared`, `core/domain`, `core/network`, `core/database`, `core/ui`, `core/common`, and placeholder modules for each feature subdirectory (`feature/auth`, `feature/menu`, `feature/orders`, `feature/payments`, `feature/inventory`, `feature/customers`, `feature/reports`, `feature/settings`).
- **FR-002**: Every KMP library module MUST declare all three Kotlin Multiplatform targets: Android, JVM, and Web (Kotlin/JS). Targets may have empty source sets in this phase.
- **FR-003**: Build logic shared across modules (KMP target declarations, plugin applications, common dependency blocks) MUST be encapsulated in convention plugins inside `buildSrc`. Individual module build files MUST apply the convention plugin rather than repeat the configuration.
- **FR-004**: Detekt MUST be configured project-wide and MUST analyze all modules with a single `./gradlew detekt` command.
- **FR-005**: KtLint MUST be configured project-wide and MUST check all modules with a single `./gradlew ktlintCheck` and auto-format with `./gradlew ktlintFormat`.
- **FR-006**: A Git pre-commit hook MUST run Detekt and KtLint before any commit is accepted. The commit MUST be aborted if either check fails.
- **FR-007**: The module dependency graph MUST enforce Clean Architecture layer rules: `feature/*` modules MAY depend on `core/domain`; `core/domain` MUST NOT depend on any other module in the project. Circular dependencies MUST cause the build to fail.
- **FR-008**: All dependency declarations across all modules MUST use the version catalog (`libs.versions.toml`) type-safe accessors. Direct version strings in build files are not permitted.
- **FR-009**: The `gradlew` and `gradlew.bat` wrapper scripts MUST be present and functional so the project builds without a locally installed Gradle distribution.

### Key Entities

- **Module**: A Gradle subproject within the multi-module hierarchy. Has a defined layer (core, feature, app, server), a build file applying one or more convention plugins, and declared inter-module dependencies.
- **Convention Plugin**: A reusable Kotlin build script inside `buildSrc` that encapsulates shared Gradle configuration for a given module type (e.g., KMP library, Android app, JVM server). Applied by module build files with a single `alias(...)` or `id(...)` call.
- **Quality Gate**: A Gradle task or Git hook that runs Detekt and/or KtLint and fails the current operation (build, commit) if violations are found.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `./gradlew build` completes without errors on a clean checkout with no Gradle cache present.
- **SC-002**: A feature module's `build.gradle.kts` file is 15 lines or fewer (excluding comments), with all target and quality configuration inherited from the convention plugin.
- **SC-003**: `./gradlew detekt ktlintCheck` analyzes all modules and produces a consolidated report with a single command invocation.
- **SC-004**: A commit containing a Detekt or KtLint violation is rejected by the pre-commit hook within 30 seconds of running `git commit`.
- **SC-005**: Adding a new KMP module to the project requires editing only two files: the root `settings.gradle.kts` (to include the module) and the module's own `build.gradle.kts` (to apply the convention plugin).

---

## Assumptions

- Gradle 9.4.1 is used (pinned via `gradle/wrapper/gradle-wrapper.properties` from feature 001).
- The Web target uses Kotlin/JS in this phase; Kotlin/WASM toolchain setup is deferred.
- Feature modules created in this phase are empty shells — no source code, only the `build.gradle.kts` and empty `src/` directory structure.
- The `server` module is JVM-only (not a KMP multiplatform module). It applies a JVM-specific convention plugin.
- Git hooks are installed via a Gradle `installGitHooks` task that developers run once after cloning. Hooks are not committed as executable binaries.
- The `composeApp` module targets Android and Desktop (JVM). Web UI entry point is a separate module (`shared` or a dedicated web module) and is deferred.
