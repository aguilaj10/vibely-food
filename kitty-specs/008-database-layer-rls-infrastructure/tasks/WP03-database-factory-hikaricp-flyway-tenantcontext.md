---
work_package_id: WP03
title: DatabaseFactory with HikariCP, Flyway, and TenantContext
lane: "doing"
dependencies: [WP01]
base_branch: 008-database-layer-rls-infrastructure-WP01
base_commit: d9673e7a874113bb255a661c3848c4a1627d4c80
created_at: '2026-03-25T02:15:40.135845+00:00'
subtasks:
- T010
- T011
- T012
- T013
phase: Phase B - Core Database Layer
assignee: ''
agent: "claude-sonnet-4-6"
shell_pid: "76843"
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

# Work Package Prompt: WP03 – DatabaseFactory with HikariCP, Flyway, and TenantContext

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

Implement the core `core:database:jvmMain` module — `DatabaseConfig`, `TenantContext`, and `DatabaseFactory` with production-grade connection pooling, Flyway baseline wiring, and the `withTenantContext` RLS method.

**Done when**:
- `DatabaseFactory` instantiates without error against a local PostgreSQL instance
- `withTenantContext` can be called and returns a result
- Flyway runs in `init` block and does not fail on a database with an existing schema
- `withTenantContext` sets exactly three PostgreSQL session variables via `SET LOCAL`
- UUID format guard prevents non-UUID strings from reaching SQL
- All public API has KDoc

**Implementation command** (depends on WP01):
```bash
spec-kitty implement WP03 --base WP01
```

---

## Context & Constraints

**Key references**:
- Constitution: `.kittify/memory/constitution.md` — framework deps only in jvmMain, KDoc mandatory
- Plan: `kitty-specs/008-database-layer-rls-infrastructure/plan.md` — sections 1.3, 1.4, 1.5, 1.6
- Data model: `kitty-specs/008-database-layer-rls-infrastructure/data-model.md` — DatabaseConfig, TenantContext, DatabaseFactory sections
- Research: `kitty-specs/008-database-layer-rls-infrastructure/research.md` — Decision 1 (Flyway), Decision 6 (RLS variables)
- Quickstart: `kitty-specs/008-database-layer-rls-infrastructure/quickstart.md` — Scenarios 1 and 2

**Architectural constraints**:
- `core:database` is a KMP module; implementation lives in `jvmMain` only
- HikariCP version: `7.0.2` (from `libs.hikari`)
- Exposed version: `1.1.1` — use `newSuspendedTransaction(Dispatchers.IO, database)` for transactions
- Flyway: `baselineOnMigrate = true`, `baselineVersion = "1"`, `validateOnMigrate = true` — no SQL migration files needed
- `app.current_store_id` must NOT be set when `storeId` is null — a `SET LOCAL` with an empty string would silently corrupt RLS
- UUID format guard is mandatory before interpolating IDs into SQL: call `UUID.fromString(value)` and throw `IllegalArgumentException` on failure
- `connectionTestQuery = "SELECT 1"` required for PgBouncer session pooling compatibility
- This WP depends on WP01: `OrganizationId`, `StoreId`, `UserId`, and `DatabaseConstants` must exist

---

## Subtasks & Detailed Guidance

### Subtask T010 – Create DatabaseConfig with Environment enum and fromEnvironment() factory

- **Purpose**: Provides a typed, immutable configuration carrier for HikariCP. `fromEnvironment()` reads standard env vars with safe defaults for local development.
- **Parallel?**: Yes — can be done simultaneously with T011.
- **Files**: Create `core/database/src/jvmMain/kotlin/com/vibely/database/DatabaseConfig.kt`
- **Steps**:
  1. Create the file:
     ```kotlin
     package com.vibely.database

     /**
      * Identifies the deployment environment, controlling pool diagnostics and logging.
      */
     enum class Environment {
         /** Development: leak detection enabled, verbose logging. */
         DEVELOPMENT,
         /** Staging: production-like configuration with additional validation. */
         STAGING,
         /** Production: maximum performance, minimal overhead. */
         PRODUCTION,
     }

     /**
      * Immutable configuration for the HikariCP connection pool.
      *
      * @property url JDBC URL pointing to PgBouncer (not PostgreSQL directly).
      * @property username Database username.
      * @property password Database password.
      * @property environment Deployment environment — controls diagnostics.
      */
     data class DatabaseConfig(
         val url: String,
         val username: String,
         val password: String,
         val environment: Environment,
     ) {
         companion object {
             /**
              * Creates a [DatabaseConfig] from environment variables, falling back to
              * safe defaults suitable for local development.
              *
              * | Env Var      | Default                                   |
              * |---|---|
              * | `DB_URL`     | `jdbc:postgresql://localhost:5432/vibely` |
              * | `DB_USER`    | `vibely`                                  |
              * | `DB_PASSWORD`| `password`                                |
              * | `APP_ENV`    | `DEVELOPMENT`                             |
              */
             fun fromEnvironment(): DatabaseConfig = DatabaseConfig(
                 url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/vibely",
                 username = System.getenv("DB_USER") ?: "vibely",
                 password = System.getenv("DB_PASSWORD") ?: "password",
                 environment = System.getenv("APP_ENV")
                     ?.let { runCatching { Environment.valueOf(it) }.getOrNull() }
                     ?: Environment.DEVELOPMENT,
             )
         }
     }
     ```
- **Notes**:
  - `runCatching { Environment.valueOf(it) }.getOrNull()` safely ignores unknown env values and falls back to `DEVELOPMENT`.
  - The URL points to PgBouncer, not PostgreSQL directly — document this in KDoc.

---

### Subtask T011 – Create TenantContext data class

- **Purpose**: Carries the three tenant identity values required to set PostgreSQL RLS session variables. Created per-request at the HTTP handler level from validated JWT claims.
- **Parallel?**: Yes — can be done simultaneously with T010.
- **Files**: Create `core/database/src/jvmMain/kotlin/com/vibely/database/TenantContext.kt`
- **Steps**:
  1. Create the file:
     ```kotlin
     package com.vibely.database

     import com.vibely.domain.tenant.OrganizationId
     import com.vibely.domain.tenant.StoreId
     import com.vibely.domain.tenant.UserId

     /**
      * Carries tenant identity context for a single database transaction.
      *
      * Created per-request from validated JWT claims and passed to
      * [DatabaseFactory.withTenantContext]. Discarded after the transaction returns.
      *
      * All three RLS session variables are set from this context:
      * - `app.current_organization_id` — always set
      * - `app.current_user_id` — always set (required for audit trail)
      * - `app.current_store_id` — set only when [storeId] is non-null
      *
      * @property organizationId Top-level tenant identifier (required).
      * @property storeId Store-level isolation boundary. `null` for org-level operations
      *   (e.g., reading organisation metadata). When null, `app.current_store_id` is
      *   NOT set — setting it to an empty string would silently corrupt RLS.
      * @property userId Authenticated user identifier, required for the audit log trigger.
      */
     data class TenantContext(
         val organizationId: OrganizationId,
         val storeId: StoreId?,
         val userId: UserId,
     )
     ```
- **Notes**:
  - `storeId` is nullable by design — org-level operations don't need store isolation.
  - KDoc explicitly explains why setting `app.current_store_id` to empty string is forbidden.

---

### Subtask T012 – Create DatabaseFactory with HikariCP pool construction and Flyway init

- **Purpose**: The production-ready database factory. Builds the HikariCP pool from `DatabaseConfig` + `DatabaseConstants`, connects Exposed, and runs Flyway on startup.
- **Parallel?**: No — depends on T010 (`DatabaseConfig`) being implemented first.
- **Files**: Create `core/database/src/jvmMain/kotlin/com/vibely/database/DatabaseFactory.kt`
- **Steps**:
  1. Create the file with the pool construction and Flyway wiring (T013 adds `withTenantContext` to the same class):
     ```kotlin
     package com.vibely.database

     import com.vibely.common.DatabaseConstants
     import com.zaxxer.hikari.HikariConfig
     import com.zaxxer.hikari.HikariDataSource
     import org.flywaydb.core.Flyway
     import org.jetbrains.exposed.sql.Database

     /**
      * Manages the HikariCP connection pool and provides tenant-scoped database access
      * via [withTenantContext].
      *
      * Constructed by the Koin DI container as a singleton. On construction:
      * 1. Builds the HikariCP pool from [config] and [DatabaseConstants].
      * 2. Connects Exposed to the pool.
      * 3. Runs Flyway baseline migration (idempotent — safe on repeated startup).
      *
      * @param config Immutable pool configuration.
      */
     class DatabaseFactory(private val config: DatabaseConfig) {

         private val dataSource: HikariDataSource = buildDataSource()
         internal val database: Database = Database.connect(dataSource)

         init {
             runFlyway()
         }

         private fun buildDataSource(): HikariDataSource {
             val hikariConfig = HikariConfig().apply {
                 jdbcUrl = config.url
                 username = config.username
                 password = config.password
                 maximumPoolSize = DatabaseConstants.MAX_POOL_SIZE
                 minimumIdle = DatabaseConstants.MIN_IDLE
                 connectionTimeout = DatabaseConstants.CONNECTION_TIMEOUT_MS
                 idleTimeout = DatabaseConstants.IDLE_TIMEOUT_MS
                 maxLifetime = DatabaseConstants.MAX_LIFETIME_MS
                 connectionTestQuery = "SELECT 1"

                 if (config.environment == Environment.DEVELOPMENT) {
                     leakDetectionThreshold = DatabaseConstants.LEAK_DETECTION_THRESHOLD_MS
                 }

                 addDataSourceProperty("preparedStatementCacheQueries",
                     DatabaseConstants.PREPARED_STATEMENT_CACHE_QUERIES.toString())
                 addDataSourceProperty("preparedStatementCacheSizeMiB",
                     DatabaseConstants.PREPARED_STATEMENT_CACHE_SIZE_MIB.toString())
                 addDataSourceProperty("binaryTransfer", "true")
                 addDataSourceProperty("socketTimeout", "30")
                 addDataSourceProperty("ApplicationName", "vibely-pos")
             }
             return HikariDataSource(hikariConfig)
         }

         private fun runFlyway() {
             Flyway.configure()
                 .dataSource(dataSource)
                 .baselineOnMigrate(true)
                 .baselineVersion("1")
                 .validateOnMigrate(true)
                 .load()
                 .migrate()
         }
     }
     ```
  2. Leave the `withTenantContext` method for T013 — it will be added to the same class.
- **Notes**:
  - `database` is `internal` to allow `DatabaseFactoryTest` to access it without exposing it publicly.
  - `leakDetectionThreshold` is only set in DEVELOPMENT to avoid performance overhead in production.
  - `addDataSourceProperty` sets PostgreSQL JDBC driver properties via HikariCP's passthrough mechanism.
  - `Flyway.configure()` chain: `baselineOnMigrate(true)` + `baselineVersion("1")` means on first run against an empty `flyway_schema_history`, Flyway inserts a V1 baseline record and skips applying V1 migration file (none exists). On subsequent runs, history is already populated so baseline is skipped.

---

### Subtask T013 – Implement withTenantContext inside DatabaseFactory

- **Purpose**: Provides the single entry point for all tenant-scoped database access. Opens an Exposed transaction, sets the three PostgreSQL RLS session variables, invokes the caller's block, and returns the result. Variables are auto-cleared when the transaction ends (SET LOCAL scope).
- **Parallel?**: No — depends on T011 (`TenantContext`) and T012 (`DatabaseFactory` skeleton).
- **Files**: Edit `core/database/src/jvmMain/kotlin/com/vibely/database/DatabaseFactory.kt` (add method to the class)
- **Steps**:
  1. Add the following imports at the top of `DatabaseFactory.kt`:
     ```kotlin
     import com.vibely.domain.tenant.OrganizationId
     import com.vibely.domain.tenant.StoreId
     import com.vibely.domain.tenant.UserId
     import kotlinx.coroutines.Dispatchers
     import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
     import java.util.UUID
     ```
  2. Add the `withTenantContext` method to the `DatabaseFactory` class:
     ```kotlin
     /**
      * Executes [block] within a database transaction that has the three PostgreSQL
      * RLS session variables set for the given [ctx].
      *
      * Session variables set (all via `SET LOCAL` — auto-cleared on transaction end):
      * - `app.current_organization_id` — always set
      * - `app.current_user_id` — always set
      * - `app.current_store_id` — set only when [TenantContext.storeId] is non-null
      *
      * **UUID format guard**: Each ID value is validated with [UUID.fromString] before
      * being interpolated into SQL. An [IllegalArgumentException] is thrown for invalid
      * UUIDs — this surfaces at the HTTP layer, not in the database.
      *
      * **Nested transaction behaviour**: Exposed re-uses the outer transaction when
      * [withTenantContext] is called from within an existing transaction. RLS variables
      * set by the outer call remain in effect.
      *
      * @param ctx Tenant context carrying the identity values.
      * @param block The database operations to execute within the tenant scope.
      * @return The result of [block].
      * @throws IllegalArgumentException if any non-null ID value is not a valid UUID.
      */
     suspend fun <T> withTenantContext(ctx: TenantContext, block: suspend () -> T): T {
         val orgId = ctx.organizationId.value.also { validateUuid(it, "organizationId") }
         val userId = ctx.userId.value.also { validateUuid(it, "userId") }
         val storeId = ctx.storeId?.value?.also { validateUuid(it, "storeId") }

         return newSuspendedTransaction(Dispatchers.IO, database) {
             exec("SET LOCAL app.current_organization_id = '$orgId'")
             exec("SET LOCAL app.current_user_id = '$userId'")
             if (storeId != null) {
                 exec("SET LOCAL app.current_store_id = '$storeId'")
             }
             block()
         }
     }

     private fun validateUuid(value: String, fieldName: String) {
         try {
             UUID.fromString(value)
         } catch (e: IllegalArgumentException) {
             throw IllegalArgumentException(
                 "TenantContext.$fieldName must be a valid UUID, got: $value", e
             )
         }
     }
     ```
- **Notes**:
  - `SET LOCAL` scope: variables are auto-cleared when the transaction commits or rolls back — no manual cleanup needed.
  - `storeId` conditional: the `if (storeId != null)` block is critical. Never set `app.current_store_id` to an empty string.
  - UUID validation happens BEFORE opening the transaction to fail fast and avoid consuming a pool connection on invalid input.
  - The `exec()` function is Exposed's raw SQL executor inside a transaction.
  - Single-quote wrapping `'$orgId'` is safe after `UUID.fromString()` validation — UUIDs cannot contain SQL-injection characters.

---

## Test Strategy

Manual smoke test (integration test suite is in WP05):

```bash
# Ensure local PostgreSQL is running with the schema applied
export DB_URL="jdbc:postgresql://localhost:5432/vibely"
export DB_USER="vibely"
export DB_PASSWORD="password"
export APP_ENV="DEVELOPMENT"

# Compile the module
./gradlew :core:database:compileKotlinJvm
```

Full RLS integration tests are covered by WP05 using Testcontainers.

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Nested `newSuspendedTransaction` re-uses outer transaction — RLS vars set in outer call persist | Documented in KDoc on `withTenantContext`. Callers must not assume a clean RLS context if already inside a transaction. |
| UUID injection via `SET LOCAL` despite format guard | `UUID.fromString()` guarantees only hex and hyphens reach the SQL. Single-quote wrapping is safe for validated UUIDs. |
| `app.current_store_id` set to empty string if storeId null | The `if (storeId != null)` guard prevents this. KDoc on `TenantContext.storeId` explains the risk. |
| Flyway fails on non-empty `flyway_schema_history` with mismatched checksum | `validateOnMigrate(true)` will catch drift; `baselineOnMigrate(true)` skips baseline on non-empty history — these are complementary. |
| `Database.connect(dataSource)` called before `init` completes | Not possible — `database` is initialized before `init {}` block runs in Kotlin. |

---

## Review Guidance

- [ ] `DatabaseConfig` is a `data class` with `Environment` enum and `fromEnvironment()` companion factory
- [ ] `Environment` has DEVELOPMENT, STAGING, PRODUCTION with KDoc on each
- [ ] `TenantContext` is a `data class` with `storeId: StoreId?` (nullable)
- [ ] `DatabaseFactory` uses HikariCP 7.0.2 with all properties from `DatabaseConstants`
- [ ] `connectionTestQuery = "SELECT 1"` is set (required for PgBouncer)
- [ ] `leakDetectionThreshold` is set only when `environment == Environment.DEVELOPMENT`
- [ ] All 5 PostgreSQL JDBC data source properties are set via `addDataSourceProperty`
- [ ] Flyway chain: `baselineOnMigrate(true)`, `baselineVersion("1")`, `validateOnMigrate(true)`, `.migrate()`
- [ ] `withTenantContext` uses `newSuspendedTransaction(Dispatchers.IO, database)`
- [ ] UUID validation happens before the transaction is opened
- [ ] `SET LOCAL` for `app.current_store_id` only executes when `storeId != null`
- [ ] All three files have KDoc on every public member
- [ ] `./gradlew :core:database:compileKotlinJvm` passes

---

## Activity Log

> **CRITICAL**: Activity log entries MUST be in chronological order (oldest first, newest last).

- 2026-03-25T01:40:02Z – system – lane=planned – Prompt created.
- 2026-03-25T02:15:41Z – claude-sonnet-4-6 – shell_pid=76843 – lane=doing – Assigned agent via workflow command
