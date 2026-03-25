---
work_package_id: WP01
title: Foundation Types and Build Configuration
lane: "doing"
dependencies: []
base_branch: main
base_commit: e586b04333e8cf20303115ac3db8b266447865b6
created_at: '2026-03-25T01:54:01.242342+00:00'
subtasks:
- T001
- T002
- T003
phase: Phase A - Foundation (parallel)
assignee: ''
agent: ''
shell_pid: "66520"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-25T01:40:02Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-001
- FR-002
- FR-003
- FR-004
- FR-005
- FR-006
- FR-007
- FR-008
---

# Work Package Prompt: WP01 – Foundation Types and Build Configuration

## ⚠️ IMPORTANT: Review Feedback Status

**Read this first if you are implementing this task!**

- **Has review feedback?**: Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section immediately (right below this notice).
- **You must address all feedback** before your work is complete. Feedback items are your implementation TODO list.
- **Mark as acknowledged**: When you understand the feedback and begin addressing it, update `review_status: acknowledged` in the frontmatter.
- **Report progress**: As you address each feedback item, update the Activity Log explaining what you changed.

---

## Review Feedback

*[This section is empty initially. Reviewers will populate it if the work is returned from review. If you see feedback here, treat each item as a must-do before completion.]*

---

## Markdown Formatting
Wrap HTML/XML tags in backticks: `` `<div>` ``, `` `<script>` ``
Use language identifiers in code blocks: ````kotlin`, ````bash`

---

## Objectives & Success Criteria

This WP creates the three tenant value classes required by `TenantContext`, fills in real `DatabaseConstants` values, and adds jvmMain/jvmTest dependency blocks to `core:database/build.gradle.kts`.

**Done when**:
- `core:domain` and `core:common` compile cleanly with `./gradlew :core:domain:compileKotlinJvm :core:common:compileKotlinJvm`
- `core:database` Gradle sync succeeds with the new dependency declarations
- `OrganizationId`, `StoreId`, `UserId` are visible and importable in other modules
- `DatabaseConstants` has no `TODO()` stubs remaining
- All new/modified files have KDoc on every public member

**Implementation command** (no dependencies):
```bash
spec-kitty implement WP01
```

---

## Context & Constraints

**Key references**:
- Constitution: `.kittify/memory/constitution.md` — zero framework deps in `core:domain`, KDoc mandatory
- Plan: `kitty-specs/008-database-layer-rls-infrastructure/plan.md` — sections 1.1 and 1.2
- Data model: `kitty-specs/008-database-layer-rls-infrastructure/data-model.md` — tenant types table, DatabaseConstants table
- Research: `kitty-specs/008-database-layer-rls-infrastructure/research.md` — Decision 3 (value class types), Decision 4 (constant values)

**Architectural constraints**:
- `core:domain` has zero framework dependencies — `@JvmInline value class` is pure Kotlin
- `MAX_POOL_SIZE` must use `val` (not `const`) because `Runtime.getRuntime()` is a runtime call
- Do NOT change the `alias(libs.plugins.kmp.library)` plugin line in `build.gradle.kts`; only add source set dependency blocks
- All Gradle deps must use `libs.xxx` type-safe accessors (project convention — no string literals)

**Existing file locations**:
- `core/database/build.gradle.kts` — currently has only plugin + android namespace; add `kotlin { sourceSets { jvmMain.dependencies { … } jvmTest.dependencies { … } } }`
- `core/common/src/commonMain/kotlin/com/vibely/common/DatabaseConstants.kt` — replace every `TODO()` with real values

---

## Subtasks & Detailed Guidance

### Subtask T001 – Create tenant value classes (OrganizationId, StoreId, UserId)

- **Purpose**: Provide strongly-typed UUID wrappers for the three tenant identity dimensions. `TenantContext` (built in WP03) depends on these. Prevents passing `organizationId` where `storeId` is expected at compile time.
- **Parallel?**: No — T002 and T003 can be done after T001 completes (though T001 is fast).
- **Files**: Create the following three new files:
  - `core/domain/src/commonMain/kotlin/com/vibely/domain/tenant/OrganizationId.kt`
  - `core/domain/src/commonMain/kotlin/com/vibely/domain/tenant/StoreId.kt`
  - `core/domain/src/commonMain/kotlin/com/vibely/domain/tenant/UserId.kt`
- **Steps**:
  1. Create the package directory `com/vibely/domain/tenant/` under `core/domain/src/commonMain/kotlin/`.
  2. Write each file following the pattern used for all existing domain IDs in this project (e.g., `OrderId`, `EmployeeId`):
     ```kotlin
     package com.vibely.domain.tenant

     /**
      * Strongly-typed identifier for an Organization (top-level tenant).
      *
      * Backed by a UUID string. UUID format is guaranteed by the authentication
      * layer and is not validated inside this class.
      */
     @JvmInline
     value class OrganizationId(val value: String)
     ```
  3. Apply the same structure for `StoreId` and `UserId` with appropriate KDoc.
- **Notes**:
  - UUID format validation is NOT done inside the value class — the auth layer guarantees valid UUIDs. The `withTenantContext` method in WP03 adds a defence-in-depth `UUID.fromString()` check.
  - No `Serializable`, no framework annotations — pure Kotlin.
  - Confirm similar IDs exist at `core/domain/src/commonMain/kotlin/com/vibely/domain/` to follow the exact naming and formatting conventions.

---

### Subtask T002 – Fill in real values in DatabaseConstants.kt

- **Purpose**: Replace all `TODO()` stubs with production-grade HikariCP constants for PostgreSQL. These values are referenced by `DatabaseFactory` in WP03.
- **Parallel?**: Yes — can be done simultaneously with T003 after T001.
- **Files**: Edit `core/common/src/commonMain/kotlin/com/vibely/common/DatabaseConstants.kt`
- **Steps**:
  1. Read the existing file to understand current stub layout.
  2. Replace every `TODO()` with the following values:
     ```kotlin
     object DatabaseConstants {

         /** Maximum connection pool size: 2× CPU count, minimum 10. */
         val MAX_POOL_SIZE: Int = (Runtime.getRuntime().availableProcessors() * 2).coerceAtLeast(10)

         /** Minimum idle connections kept in the pool. */
         const val MIN_IDLE: Int = 10

         /** Maximum time (ms) to wait for a connection from the pool. */
         const val CONNECTION_TIMEOUT_MS: Long = 30_000L

         /** Maximum time (ms) a connection may remain idle before eviction. */
         const val IDLE_TIMEOUT_MS: Long = 600_000L

         /** Maximum lifetime (ms) of a connection in the pool. */
         const val MAX_LIFETIME_MS: Long = 1_800_000L

         /** Number of prepared statement cache entries per connection. */
         const val PREPARED_STATEMENT_CACHE_QUERIES: Int = 256

         /** Size limit (MiB) for the prepared statement cache per connection. */
         const val PREPARED_STATEMENT_CACHE_SIZE_MIB: Int = 5

         /** Leak detection threshold (ms) — enabled only in DEVELOPMENT environment. */
         const val LEAK_DETECTION_THRESHOLD_MS: Long = 60_000L
     }
     ```
  3. Ensure `MAX_POOL_SIZE` uses `val` (not `const val`) — `Runtime.getRuntime()` is a JVM runtime call, not a compile-time constant.
  4. Add KDoc to every property if not already present.
- **Notes**:
  - `MAX_LIFETIME_MS = 1_800_000L` (30 minutes) is intentionally set slightly below PostgreSQL's default `wait_timeout` to prevent connections from being killed mid-use.
  - `commonMain` compiles per KMP target; `Runtime.getRuntime()` is JVM-specific but compiles fine in KMP `commonMain` (KMP provides a compatibility shim). JS and Android targets won't use `DatabaseConstants` at runtime.

---

### Subtask T003 – Update core:database build.gradle.kts dependency blocks

- **Purpose**: Declare `jvmMain` and `jvmTest` source set dependencies so `DatabaseFactory`, `TenantContext`, and the Testcontainers tests can compile.
- **Parallel?**: Yes — can be done simultaneously with T002 after T001.
- **Files**: Edit `core/database/build.gradle.kts`
- **Steps**:
  1. Read the existing `build.gradle.kts` to understand current structure (plugin line + android block).
  2. Add the `kotlin { sourceSets { … } }` block after the android namespace block:
     ```kotlin
     kotlin {
         sourceSets {
             jvmMain.dependencies {
                 implementation(projects.core.common)
                 implementation(projects.core.domain)
                 implementation(libs.bundles.exposed)
                 implementation(libs.hikari)
                 implementation(libs.bundles.flyway)
                 implementation(libs.postgresql)
                 implementation(libs.kotlinx.coroutines.core)
             }
             jvmTest.dependencies {
                 implementation(libs.kotest.assertions.core)
                 implementation(libs.testcontainers.postgresql)
                 implementation(libs.kotlinx.coroutines.test)
             }
         }
     }
     ```
  3. Do NOT touch the `alias(libs.plugins.kmp.library)` line or the android namespace block.
  4. Verify all catalog aliases exist in `gradle/libs.versions.toml` — confirmed present: `bundles.exposed`, `hikari`, `bundles.flyway`, `postgresql`, `kotlinx.coroutines.core`, `kotest.assertions.core`, `testcontainers.postgresql`, `kotlinx.coroutines.test`.
- **Notes**:
  - `projects.core.common` and `projects.core.domain` are type-safe project accessors from the Gradle version catalog.
  - If `testcontainers.postgresql` is not in the catalog as a standalone alias, check if it appears under `testcontainers` bundle. Add the specific alias if needed.
  - Run `./gradlew :core:database:dependencies` after to confirm no unresolved deps.

---

## Test Strategy

No dedicated tests required for this WP. Verification is compile-time:

```bash
# Verify core:domain and core:common compile cleanly
./gradlew :core:domain:compileKotlinJvm :core:common:compileKotlinJvm

# Verify core:database Gradle sync
./gradlew :core:database:dependencies
```

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| `Runtime.getRuntime()` in `commonMain` causes KMP compile error | KMP provides compatibility shim for `commonMain`; compiles fine per-target. If issues arise, move to a JVM-only `expect/actual` constant. |
| `testcontainers.postgresql` alias missing from `libs.versions.toml` | Check existing aliases; add if needed: `testcontainers-postgresql = { group = "org.testcontainers", name = "postgresql", version.ref = "testcontainers" }` |
| Gradle sync fails due to wrong KMP source set syntax | Use `jvmMain.dependencies { … }` not `named("jvmMain") { dependencies { … } }` — both work but the former is idiomatic for newer KMP. |

---

## Review Guidance

- [ ] `OrganizationId`, `StoreId`, `UserId` are in `com.vibely.domain.tenant` package, each in its own file
- [ ] All three value classes use `@JvmInline value class(val value: String)` — no framework annotations
- [ ] KDoc present on all three classes
- [ ] `DatabaseConstants.MAX_POOL_SIZE` uses `val` (not `const val`)
- [ ] All 8 constants in `DatabaseConstants` match the data-model spec values exactly
- [ ] `core/database/build.gradle.kts` has `jvmMain.dependencies` and `jvmTest.dependencies` blocks
- [ ] All deps in `build.gradle.kts` use `libs.xxx` type-safe accessors
- [ ] Plugin line unchanged: `alias(libs.plugins.kmp.library)` still present
- [ ] `./gradlew :core:domain:compileKotlinJvm :core:common:compileKotlinJvm` passes
- [ ] `./gradlew :core:database:dependencies` resolves successfully

---

## Activity Log

> **CRITICAL**: Activity log entries MUST be in chronological order (oldest first, newest last).

- 2026-03-25T01:40:02Z – system – lane=planned – Prompt created.
