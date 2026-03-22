# Implementation Plan: Update Version Catalog to Latest

**Branch**: `001-update-version-catalog-to-latest` | **Date**: 2026-03-22 | **Spec**: [spec.md](spec.md)

## Summary

Update the existing `gradle/libs.versions.toml` to replace all outdated library versions
with their latest stable counterparts as mandated by the project constitution. Additionally,
create the Gradle wrapper (`gradle/wrapper/gradle-wrapper.properties`) which is missing from
the project — this pins the Gradle version (9.4.1) for reproducible builds across all
contributors and CI pipelines.

No new files, modules, or architectural patterns are introduced. This is a pure
configuration update with two deliverables: an updated version catalog and a new Gradle
wrapper properties file.

---

## Technical Context

**Language/Version**: Kotlin 2.3.20 (KMP)
**Primary Dependencies**: Gradle 9.4.1 (wrapper), all libraries in `gradle/libs.versions.toml`
**Storage**: N/A
**Testing**: Verify by clean build + all modules compile
**Target Platform**: JVM, Android, Web (KMP)
**Project Type**: Kotlin Multiplatform (multi-module)
**Performance Goals**: N/A
**Constraints**: All versions must be the latest stable release; all declared versions must be mutually compatible (confirmed per constitution)
**Scale/Scope**: Single file update (`gradle/libs.versions.toml`) + one new file (`gradle/wrapper/gradle-wrapper.properties`)

---

## Constitution Check

*GATE: Must pass before Phase 0. Re-checked after Phase 1.*

| Rule | Status | Notes |
|------|--------|-------|
| Always use latest stable versions | ✅ This feature's purpose | Updating to comply |
| Never use `if (platform == ...)` in shared code | ✅ Not applicable | No code changes |
| Domain layer must be pure Kotlin | ✅ Not applicable | No code changes |
| Use `Result<T>` for fallible operations | ✅ Not applicable | No code changes |
| Detekt + KtLint enforced | ✅ Not applicable | No code changes |
| Use Context7 MCP for research | ✅ Applies to Phase 0 | Version lookups via Context7 or Maven Central |

**Gate result**: PASS — no violations.

---

## Project Structure

### Documentation (this feature)

```
kitty-specs/001-update-version-catalog-to-latest/
├── plan.md              ← This file
├── research.md          ← Phase 0 output (version lookup results)
└── tasks.md             ← Phase 2 output (/spec-kitty.tasks — NOT created here)
```

> No `data-model.md` or `contracts/` needed — this feature has no entities or API surface.

### Source Files Affected

```
gradle/
├── libs.versions.toml                        ← UPDATE (version numbers only)
└── wrapper/
    └── gradle-wrapper.properties             ← CREATE (does not exist yet)
```

---

## Phase 0: Research — Resolve Remaining Versions

**Goal**: Look up the latest stable version for every library in `libs.versions.toml`
not already resolved during constitution creation.

The following were resolved during constitution work:

| Library | Current | Resolved Latest |
|---------|---------|-----------------|
| kotlin | 2.1.0 | **2.3.20** |
| compose | 1.7.1 | **1.10.3** |
| koin | 4.0.0 | **4.2.0** |
| ktor | 3.0.1 | **3.4.1** |
| exposed | 0.56.0 | **1.1.1** |
| sqldelight | 2.0.2 | **2.3.2** |
| flyway | 10.20.1 | **12.1.1** |
| Gradle wrapper | — | **9.4.1** |

The following still need resolution — look up on Maven Central / GitHub releases:

| Library | Current | Lookup Target |
|---------|---------|---------------|
| compose-compiler | 1.5.15 | Check: may be **removed** — Kotlin 2.x bundles the Compose compiler plugin |
| hikari | 6.0.0 | Latest stable on Maven Central (`com.zaxxer:HikariCP`) |
| postgresql | 42.7.4 | Latest stable on Maven Central (`org.postgresql:postgresql`) |
| kotlinx-coroutines | 1.9.0 | Latest stable (`org.jetbrains.kotlinx:kotlinx-coroutines-core`) |
| kotlinx-serialization | 1.7.3 | Latest stable (`org.jetbrains.kotlinx:kotlinx-serialization-json`) |
| kotlinx-datetime | 0.6.1 | Latest stable (`org.jetbrains.kotlinx:kotlinx-datetime`) |
| turbine | 1.1.0 | Latest stable (`app.cash.turbine:turbine`) |
| kotest | 5.9.1 | Latest stable (`io.kotest:kotest-assertions-core`) |
| testcontainers | 1.20.4 | Latest stable (`org.testcontainers:postgresql`) |
| detekt | 1.23.7 | Latest stable (`io.gitlab.arturbosch.detekt`) |
| ktlint | 12.1.1 | Latest stable (`org.jlleitschuh.gradle.ktlint`) |

**Output**: `research.md` with all resolved versions and the `compose-compiler` decision.

---

## Phase 1: Implementation

**Prerequisites**: `research.md` complete with all versions resolved.

### Step 1 — Update `[versions]` block in `gradle/libs.versions.toml`

Replace each version string with the resolved latest stable value from `research.md`.

Key decision from research:
- If `compose-compiler` is confirmed redundant under Kotlin 2.x → **remove** the
  `compose-compiler` entry from `[versions]` and its corresponding plugin entry.
- Otherwise, update to the latest compatible version.

### Step 2 — Verify `[libraries]`, `[plugins]`, `[bundles]` blocks

These blocks reference versions via `version.ref` — no changes expected unless a
library changed its Maven coordinates between versions. Confirm coordinates are still
valid for each updated library.

### Step 3 — Create `gradle/wrapper/gradle-wrapper.properties`

Create the file with the following content, using Gradle 9.4.1:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### Step 4 — Verify

Run a clean build to confirm:
- All dependencies resolve without errors
- No version conflict warnings
- All modules compile successfully

---

## Complexity Tracking

*No constitution violations — section not applicable.*

---

## Parallel Work Analysis

Single-agent task. All steps are sequential with no parallel opportunities:

```
Phase 0: Resolve versions → Phase 1: Update file → Phase 1: Create wrapper → Verify build
```
