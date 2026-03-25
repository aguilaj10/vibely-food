---
work_package_id: WP05
title: RLS Integration Tests
lane: "doing"
dependencies: [WP03]
base_branch: 008-database-layer-rls-infrastructure-WP03
base_commit: 57dc7ac8a3d5dcaf81c2c3daf3fca039839fbe5c
created_at: '2026-03-25T02:18:07.168046+00:00'
subtasks:
- T017
- T018
- T019
- T020
phase: Phase C - Integration
assignee: ''
agent: "claude-sonnet-4-6"
shell_pid: "90660"
review_status: "has_feedback"
reviewed_by: "Jonathan Sánchez Muñoz"
review_feedback_file: "/private/var/folders/lk/549xp1m52gg9ycr7sgpl0jcw0000gp/T/spec-kitty-review-feedback-WP05.md"
history:
- timestamp: '2026-03-25T01:40:02Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-002
- FR-003
- FR-007
---

# Work Package Prompt: WP05 – RLS Integration Tests

## ⚠️ IMPORTANT: Review Feedback Status

**Read this first if you are implementing this task!**

- **Has review feedback?**: Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section immediately (right below this notice).
- **You must address all feedback** before your work is complete. Feedback items are your implementation TODO list.
- **Mark as acknowledged**: When you understand the feedback and begin addressing it, update `review_status: acknowledged` in the frontmatter.
- **Report progress**: As you address each feedback item, update the Activity Log explaining what you changed.

---

## Review Feedback

**Reviewed by**: Jonathan Sánchez Muñoz
**Status**: ❌ Changes Requested
**Date**: 2026-03-25
**Feedback file**: `/private/var/folders/lk/549xp1m52gg9ycr7sgpl0jcw0000gp/T/spec-kitty-review-feedback-WP05.md`

# Review Feedback – WP05

## Issue: Missing `testcontainers-junit-jupiter` dependency

`DatabaseFactoryTest.kt` imports `org.testcontainers.junit.jupiter.Container` and `org.testcontainers.junit.jupiter.Testcontainers`, both of which come from `org.testcontainers:junit-jupiter`. This artifact is **not declared** in the WP05 branch — it was removed (relative to the WP01 fixup that added it) because WP05 was based on WP03 which was itself branched from WP01 before the fixup commit.

Without this dependency the test file will **fail to compile**.

## Required Fixes

### 1. `gradle/libs.versions.toml` — add catalog entry if absent

Confirm this line is present in `[libraries]`:

```toml
testcontainers-junit-jupiter = { module = "org.testcontainers:junit-jupiter", version.ref = "testcontainers" }
```

### 2. `core/database/build.gradle.kts` — add `jvmTest` dependency

Inside the `jvmTest.dependencies { }` block, ensure:

```kotlin
jvmTest.dependencies {
    implementation(libs.kotest.assertions.core)
    implementation(libs.testcontainers.postgresql)
    implementation(libs.testcontainers.junit.jupiter)   // ← ADD THIS BACK
    implementation(libs.kotlinx.coroutines.test)
}
```

## No Other Changes Required

The `DatabaseFactoryTest.kt` logic is correct — all four test scenarios (set all 3 vars, null storeId omits store var, vars absent after tx, Flyway V1 baseline) match the spec. Only the missing build dependency needs to be fixed.


## Markdown Formatting
Wrap HTML/XML tags in backticks: `` `<div>` ``, `` `<script>` ``
Use language identifiers in code blocks: ````kotlin`, ````bash`

---

## Objectives & Success Criteria

Validate that `DatabaseFactory.withTenantContext` correctly sets and isolates PostgreSQL RLS session variables, and that Flyway creates the V1 baseline record on first startup.

**Done when**:
- `./gradlew :core:database:jvmTest` passes all four test cases against a Testcontainers PostgreSQL instance
- T018: `withTenantContext` sets all three variables correctly when all are provided
- T019: RLS session variables are absent after the transaction ends (connection isolation)
- T020: Flyway inserts a V1 baseline record in `flyway_schema_history` on first run against an empty history

**Implementation command** (depends on WP03):
```bash
spec-kitty implement WP05 --base WP03
```

---

## Context & Constraints

**Key references**:
- Constitution: `.kittify/memory/constitution.md` — no mocks (fakes only), tests in `jvmTest`, Kotest assertions
- Plan: `kitty-specs/008-database-layer-rls-infrastructure/plan.md` — section 1.10
- Data model: `kitty-specs/008-database-layer-rls-infrastructure/data-model.md` — TenantContext section
- Quickstart: `kitty-specs/008-database-layer-rls-infrastructure/quickstart.md` — "Verify RLS is working" section

**Technical constraints**:
- Tests live in `core/database/src/jvmTest/` (project convention: tests in `jvmTest`, not `commonTest`)
- Use Kotest assertions: `shouldBe`, `shouldNotBeNull`, `shouldBeEmpty` — no JUnit 4 `assertEquals`
- No mocks — use the real `DatabaseFactory` with Testcontainers PostgreSQL
- Docker must be running for Testcontainers to work
- `PostgreSQLContainer("postgres:17")` for the container image
- `@Testcontainers` + `companion object { @Container val postgres = PostgreSQLContainer(...) }` pattern
- `DatabaseFactory.database` must be accessible from the test — either make it `internal` (accessible within the module) or expose a test-only accessor

---

## Subtasks & Detailed Guidance

### Subtask T017 – Configure PostgreSQLContainer and create DatabaseFactoryTest skeleton

- **Purpose**: Establishes the shared test infrastructure — Testcontainers setup, `DatabaseFactory` instantiation, and test class skeleton — that T018, T019, and T020 build upon.
- **Parallel?**: No — T018, T019, T020 all depend on this skeleton.
- **Files**: Create `core/database/src/jvmTest/kotlin/com/vibely/database/DatabaseFactoryTest.kt`
- **Steps**:
  1. Create the test file with container setup and factory instantiation:
     ```kotlin
     package com.vibely.database

     import com.vibely.domain.tenant.OrganizationId
     import com.vibely.domain.tenant.StoreId
     import com.vibely.domain.tenant.UserId
     import io.kotest.matchers.shouldBe
     import io.kotest.matchers.string.shouldBeBlank
     import kotlinx.coroutines.test.runTest
     import org.jetbrains.exposed.sql.transactions.TransactionManager
     import org.junit.jupiter.api.Test
     import org.testcontainers.containers.PostgreSQLContainer
     import org.testcontainers.junit.jupiter.Container
     import org.testcontainers.junit.jupiter.Testcontainers
     import java.sql.DriverManager

     @Testcontainers
     class DatabaseFactoryTest {

         companion object {
             @Container
             val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:17")

             val factory: DatabaseFactory by lazy {
                 val config = DatabaseConfig(
                     url = postgres.jdbcUrl,
                     username = postgres.username,
                     password = postgres.password,
                     environment = Environment.DEVELOPMENT,
                 )
                 DatabaseFactory(config)
             }
         }

         // T018, T019, T020 test methods go here
     }
     ```
  2. Verify that `DatabaseFactory.database` is accessible from `jvmTest` — it is declared `internal` in WP03. Since both the factory and the test are in `com.vibely.database`, `internal` visibility is sufficient.
- **Notes**:
  - `by lazy` ensures the factory (and Flyway) runs exactly once for the entire test class.
  - Testcontainers auto-starts and auto-stops `postgres` around the test class lifecycle via `@Testcontainers` + `@Container`.
  - `postgres:17` matches the production PostgreSQL version.
  - The `DatabaseFactory` init block runs Flyway, so by the time any `@Test` method executes, Flyway has already run.

---

### Subtask T018 – Test: withTenantContext sets RLS variables correctly

- **Purpose**: Validates the core RLS functionality — that all three session variables are set to the expected values during a transaction.
- **Parallel?**: Yes — can be written after T017.
- **Files**: Edit `core/database/src/jvmTest/kotlin/com/vibely/database/DatabaseFactoryTest.kt`
- **Steps**:
  1. Add the following test method to `DatabaseFactoryTest`:
     ```kotlin
     @Test
     fun `withTenantContext sets all three RLS session variables`() = runTest {
         val orgId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
         val storeId = "b2c3d4e5-f6a7-8901-bcde-f01234567891"
         val userId = "c3d4e5f6-a7b8-9012-cdef-012345678901"

         val ctx = TenantContext(
             organizationId = OrganizationId(orgId),
             storeId = StoreId(storeId),
             userId = UserId(userId),
         )

         val results = factory.withTenantContext(ctx) {
             val org = TransactionManager.current().exec(
                 "SELECT current_setting('app.current_organization_id', true)"
             ) { rs -> rs.next(); rs.getString(1) }
             val store = TransactionManager.current().exec(
                 "SELECT current_setting('app.current_store_id', true)"
             ) { rs -> rs.next(); rs.getString(1) }
             val user = TransactionManager.current().exec(
                 "SELECT current_setting('app.current_user_id', true)"
             ) { rs -> rs.next(); rs.getString(1) }
             Triple(org, store, user)
         }

         results.first shouldBe orgId
         results.second shouldBe storeId
         results.third shouldBe userId
     }
     ```
  2. Also add a test for the null `storeId` case:
     ```kotlin
     @Test
     fun `withTenantContext does not set store_id when storeId is null`() = runTest {
         val orgId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
         val userId = "c3d4e5f6-a7b8-9012-cdef-012345678901"

         val ctx = TenantContext(
             organizationId = OrganizationId(orgId),
             storeId = null,
             userId = UserId(userId),
         )

         val storeValue = factory.withTenantContext(ctx) {
             TransactionManager.current().exec(
                 "SELECT current_setting('app.current_store_id', true)"
             ) { rs -> rs.next(); rs.getString(1) }
         }

         // PostgreSQL returns empty string for unset GUCs when missing_ok = true
         storeValue?.shouldBeBlank()
     }
     ```
- **Notes**:
  - `current_setting('app.current_organization_id', true)` — the `true` flag is `missing_ok`; PostgreSQL returns `""` if the variable is not set (rather than throwing an error).
  - `TransactionManager.current().exec(...)` accesses the raw Exposed transaction to execute a query and map the result set.
  - Test UUIDs use realistic UUID format to pass the `UUID.fromString()` guard in `withTenantContext`.

---

### Subtask T019 – Test: RLS variables are absent after transaction ends

- **Purpose**: Validates that `SET LOCAL` truly scopes variables to the transaction — a key correctness property of the RLS implementation. If `SET SESSION` were accidentally used instead, variables would persist across connections and contaminate other tenants.
- **Parallel?**: Yes — can be written after T017.
- **Files**: Edit `core/database/src/jvmTest/kotlin/com/vibely/database/DatabaseFactoryTest.kt`
- **Steps**:
  1. Add the following test method:
     ```kotlin
     @Test
     fun `RLS session variables are absent after withTenantContext returns`() = runTest {
         val ctx = TenantContext(
             organizationId = OrganizationId("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
             storeId = StoreId("b2c3d4e5-f6a7-8901-bcde-f01234567891"),
             userId = UserId("c3d4e5f6-a7b8-9012-cdef-012345678901"),
         )

         // Execute a transaction that sets RLS variables
         factory.withTenantContext(ctx) { /* no-op */ }

         // After the transaction, open a raw JDBC connection and verify variables are unset
         val storeValue = DriverManager.getConnection(
             postgres.jdbcUrl, postgres.username, postgres.password
         ).use { conn ->
             conn.createStatement().use { stmt ->
                 stmt.executeQuery(
                     "SELECT current_setting('app.current_store_id', true)"
                 ).use { rs ->
                     rs.next()
                     rs.getString(1)
                 }
             }
         }

         // PostgreSQL returns "" for unset GUCs when missing_ok = true
         storeValue.shouldBeBlank()
     }
     ```
- **Notes**:
  - A raw `DriverManager.getConnection` (bypassing HikariCP) ensures a fresh connection with no lingering session state.
  - `SET LOCAL` variables are transaction-scoped — they are cleared when the transaction commits. This test verifies that the transaction actually committed and the variable is gone.
  - `shouldBeBlank()` accepts both `null` and `""` — PostgreSQL returns `""` for unset GUCs with `missing_ok = true`.

---

### Subtask T020 – Test: Flyway inserts V1 baseline record on fresh database

- **Purpose**: Validates that `DatabaseFactory`'s Flyway wiring creates the expected baseline record in `flyway_schema_history`. This confirms the `baselineOnMigrate = true` strategy works correctly and future migrations will have a valid baseline to build on.
- **Parallel?**: Yes — can be written after T017.
- **Files**: Edit `core/database/src/jvmTest/kotlin/com/vibely/database/DatabaseFactoryTest.kt`
- **Steps**:
  1. Add the following test method:
     ```kotlin
     @Test
     fun `Flyway creates V1 baseline record in flyway_schema_history`() = runTest {
         // DatabaseFactory init block runs Flyway — already executed via `factory` lazy
         val version = DriverManager.getConnection(
             postgres.jdbcUrl, postgres.username, postgres.password
         ).use { conn ->
             conn.createStatement().use { stmt ->
                 stmt.executeQuery(
                     "SELECT version FROM flyway_schema_history LIMIT 1"
                 ).use { rs ->
                     if (rs.next()) rs.getString("version") else null
                 }
             }
         }

         version shouldBe "1"
     }
     ```
- **Notes**:
  - The `factory` lazy property already ran Flyway when it was first accessed in a previous test. `@Testcontainers` keeps the container alive for the entire test class, so `flyway_schema_history` is already populated.
  - If tests run in isolation (parallel test execution), ensure `factory` is initialized before this test. The `companion object` `by lazy` initialization is thread-safe in Kotlin.
  - `version = "1"` corresponds to `baselineVersion("1")` set in `DatabaseFactory.runFlyway()`.

---

## Test Strategy

```bash
# Prerequisites: Docker must be running
docker info

# Run the integration tests
./gradlew :core:database:jvmTest

# Expected output:
# DatabaseFactoryTest > withTenantContext sets all three RLS session variables PASSED
# DatabaseFactoryTest > withTenantContext does not set store_id when storeId is null PASSED
# DatabaseFactoryTest > RLS session variables are absent after withTenantContext returns PASSED
# DatabaseFactoryTest > Flyway creates V1 baseline record in flyway_schema_history PASSED
```

If Docker is not available in CI:
```kotlin
// Add this annotation to the class if Docker is unavailable:
@DisabledIfEnvironmentVariable(named = "CI_NO_DOCKER", matches = "true")
```

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Docker not running in CI | Check Docker availability in CI (Jenkins). If unavailable, add `@DisabledIfEnvironmentVariable` and document the prerequisite. |
| Testcontainers `postgres:17` image not pulled in CI | Pre-pull the image in CI pipeline or use a local registry mirror. |
| `flyway_schema_history` table doesn't exist yet when T020 runs | `DatabaseFactory` init runs Flyway before any test executes (via `by lazy` factory initialization). The table is created by Flyway on first contact. |
| `TransactionManager.current()` throws outside transaction | The `exec` calls in T018 are inside `withTenantContext` block — always within a transaction. |
| `DriverManager` in test depends on PostgreSQL driver on classpath | `libs.postgresql` is already in `jvmTest.dependencies` (added in WP01/T003). |

---

## Review Guidance

- [ ] Test file at `core/database/src/jvmTest/kotlin/com/vibely/database/DatabaseFactoryTest.kt`
- [ ] `@Testcontainers` annotation on class
- [ ] `@Container` on companion object `PostgreSQLContainer("postgres:17")` field
- [ ] `DatabaseFactory` instantiated once via `by lazy` with container JDBC URL
- [ ] T018: asserts `app.current_organization_id`, `app.current_store_id`, `app.current_user_id` match context values
- [ ] T018: separate test case for null `storeId` verifying `app.current_store_id` is blank
- [ ] T019: opens a fresh raw JDBC connection (not HikariCP) after transaction ends
- [ ] T019: asserts `app.current_store_id` is blank after transaction completes
- [ ] T020: queries `flyway_schema_history` and asserts `version = "1"`
- [ ] All assertions use Kotest (`shouldBe`, `shouldBeBlank`) — no JUnit `assertEquals`
- [ ] `runTest` used for suspend test functions
- [ ] `./gradlew :core:database:jvmTest` passes all four test cases

---

## Activity Log

> **CRITICAL**: Activity log entries MUST be in chronological order (oldest first, newest last).

- 2026-03-25T01:40:02Z – system – lane=planned – Prompt created.
- 2026-03-25T02:18:08Z – claude-sonnet-4-6 – shell_pid=78832 – lane=doing – Assigned agent via workflow command
- 2026-03-25T02:23:04Z – claude-sonnet-4-6 – shell_pid=78832 – lane=for_review – Ready for review: T017 PostgreSQLContainer + by-lazy DatabaseFactory; T018 withTenantContext sets all 3 RLS vars (incl. null storeId case); T019 RLS vars absent after tx ends via raw JDBC conn; T020 Flyway V1 baseline record verified
- 2026-03-25T02:34:59Z – claude-sonnet-4-6 – shell_pid=89058 – lane=doing – Started review via workflow command
- 2026-03-25T02:35:59Z – claude-sonnet-4-6 – shell_pid=89058 – lane=planned – Moved to planned
- 2026-03-25T02:36:50Z – claude-sonnet-4-6 – shell_pid=89058 – lane=for_review – Fixed: added testcontainers-junit-jupiter catalog entry to libs.versions.toml and libs.testcontainers.junit.jupiter to jvmTest.dependencies in core/database/build.gradle.kts. Tests now have all required dependencies for @Testcontainers/@Container annotations.
- 2026-03-25T02:36:55Z – claude-sonnet-4-6 – shell_pid=90660 – lane=doing – Started review via workflow command
