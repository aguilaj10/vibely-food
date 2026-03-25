# Implementation Plan: Database Layer Infrastructure with RLS

**Branch**: `008-database-layer-rls-infrastructure` | **Date**: 2026-03-25 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/kitty-specs/008-database-layer-rls-infrastructure/spec.md`

---

## Summary

Establish the foundational database infrastructure for Vibely POS: a production-ready `core:database` module with HikariCP connection pooling, Flyway migration wiring, and a `TenantContext`-based RLS API that sets all three PostgreSQL session variables (`app.current_organization_id`, `app.current_store_id`, `app.current_user_id`) in one transaction. Simultaneously introduces `BaseRepository` cache-first strategy and `DomainMapper`/`DtoMapper` interfaces in `shared:commonMain`. Removes `DatabaseStubs.kt` and wires the real `DatabaseFactory` into `shared:jvmMain/PlatformModule`.

---

## Technical Context

**Language/Version**: Kotlin 2.3.20, Kotlin Multiplatform
**Primary Dependencies**:
- `core:database` (jvmMain): `exposed 1.1.1`, `hikari 7.0.2`, `flyway 12.1.1`, `postgresql 42.7.10`
- `shared:commonMain`: pure Kotlin interfaces (no framework deps)
- `core:domain` (commonMain): pure Kotlin `@JvmInline value class`

**Storage**: PostgreSQL via HikariCP → PgBouncer (session pooling mode)
**Testing**: Kotest assertions + Testcontainers (`testcontainers-postgresql`) for RLS integration; fake implementations for `BaseRepository` unit tests (in `jvmTest` per project convention)
**Target Platform**: JVM server (Ktor / Netty)
**Performance Goals**: RLS overhead < 5ms per query; pool sized per `DatabaseConstants` (CPU × 2, min 10)
**Constraints**: PgBouncer in session pooling mode; `core:domain` zero framework deps; no mocks in tests; KDoc on all public API

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Rule | Status | Notes |
|---|---|---|
| `core:domain` zero framework dependencies | ✅ PASS | `OrganizationId`, `StoreId`, `UserId` are pure Kotlin `@JvmInline value class` |
| Framework deps in `core:database` jvmMain only | ✅ PASS | Exposed, HikariCP, Flyway, PostgreSQL driver — all jvmMain |
| `shared:commonMain` interfaces have no framework deps | ✅ PASS | `BaseRepository`, data source, mapper interfaces are pure Kotlin |
| `Result<T>` for all fallible operations | ✅ PASS | `BaseRepository.getById` / `save` return `Result<T>` |
| No mocks — fake implementations in tests | ✅ PASS | `FakeLocalDataSource`, `FakeRemoteDataSource`, `FakeSyncManager` |
| PgBouncer session pooling documented | ✅ PASS | `SET LOCAL` requires session pooling; documented in Assumptions |
| Library versions match constitution | ✅ PASS | All versions already present in `libs.versions.toml` |
| KDoc on all public classes, functions, properties | ✅ REQUIRED | Detekt enforces this; all deliverables must include KDoc |

---

## Project Structure

### Documentation (this feature)

```
kitty-specs/008-database-layer-rls-infrastructure/
├── plan.md              ← this file
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── quickstart.md        ← Phase 1 output
└── tasks.md             ← Phase 2 output (/spec-kitty.tasks)
```

### Source Code (repository root)

```
core/
├── domain/
│   └── src/commonMain/kotlin/com/vibely/domain/tenant/
│       ├── OrganizationId.kt        # NEW
│       ├── StoreId.kt               # NEW
│       └── UserId.kt                # NEW
│
├── common/
│   └── src/commonMain/kotlin/com/vibely/common/
│       └── DatabaseConstants.kt     # UPDATE — fill in real values
│
└── database/
    └── src/jvmMain/kotlin/com/vibely/database/
        ├── DatabaseConfig.kt        # NEW
        ├── DatabaseFactory.kt       # NEW
        └── TenantContext.kt         # NEW
    └── src/jvmTest/kotlin/com/vibely/database/
        └── DatabaseFactoryTest.kt   # NEW — Testcontainers RLS integration test

shared/
└── src/
    ├── commonMain/kotlin/com/vibely/shared/
    │   ├── data/
    │   │   ├── repository/
    │   │   │   └── BaseRepository.kt    # NEW
    │   │   └── source/
    │   │       ├── LocalDataSource.kt   # NEW
    │   │       ├── RemoteDataSource.kt  # NEW
    │   │       └── SyncManager.kt       # NEW
    │   └── mapper/
    │       ├── DomainMapper.kt          # NEW
    │       └── DtoMapper.kt             # NEW
    ├── jvmMain/kotlin/com/vibely/shared/di/
    │   ├── DatabaseStubs.kt             # DELETE
    │   └── PlatformModule.kt            # UPDATE
    └── build.gradle.kts                 # UPDATE — add jvmMain dep on projects.core.database
```

---

## Parallel Work Analysis

### Dependency Graph

```
Phase A (parallel — no dependencies):
  A1 — core:domain tenant value classes (OrganizationId, StoreId, UserId)
  A2 — core:common DatabaseConstants real values
  A3 — shared:commonMain interfaces (BaseRepository, LocalDataSource, RemoteDataSource,
        SyncManager, DomainMapper, DtoMapper)

Phase B (depends on A1 + A2):
  B1 — core:database jvmMain (DatabaseConfig, TenantContext, DatabaseFactory)
  B2 — core:database build.gradle.kts dependency wiring

Phase C (depends on A3 + B1 + B2):
  C1 — shared:jvmMain stub removal + PlatformModule update + build.gradle.kts update
  C2 — core:database jvmTest Testcontainers integration test
```

### Work Distribution

- **Sequential gate**: Phase A must be complete before Phase B begins
- **Parallel streams**: A1 / A2 / A3 are fully independent
- **Integration**: C2 can be written in parallel with C1; both need B1 compiled

---

## Implementation Phases

### Phase 0: Research

*See [research.md](research.md) for full findings.*

Key decisions resolved prior to implementation:

| Decision | Resolution |
|---|---|
| Flyway strategy (existing schema, empty history) | `baselineOnMigrate = true`, `baselineVersion = "1"` — auto-baseline on first startup |
| `SyncManager` as `expect/actual` vs plain interface | Plain interface; Koin-injected platform implementations (consistent with `SecureStorage`/`PlatformLogger`) |
| `TenantContext` ID types | `OrganizationId`, `StoreId`, `UserId` — new value classes in `core:domain:commonMain` |
| `DatabaseConstants` status | Exists in `core:common` with `TODO()` stubs — fill in real values in this feature |
| Library version gaps | None — all versions already correct in `libs.versions.toml` |
| RLS session variable set | Both `app.current_organization_id` + `app.current_store_id` + `app.current_user_id`; store is optional |

---

### Phase 1: Design

#### 1.1 Tenant Value Classes — `core:domain:commonMain`

New package: `com.vibely.domain.tenant`

All three are `@JvmInline value class` with `String` backing (UUIDs). UUID format is validated at the auth layer, not in the value class itself, keeping domain models free of framework deps.

```
OrganizationId(val value: String)
StoreId(val value: String)
UserId(val value: String)
```

---

#### 1.2 DatabaseConstants — `core:common:commonMain`

Replace all `TODO()` stubs with real values sourced from section 0.3 of the implementation plan:

| Constant | Value | Source |
|---|---|---|
| `MAX_POOL_SIZE` | `(Runtime.getRuntime().availableProcessors() * 2).coerceAtLeast(10)` | Formula from impl plan |
| `MIN_IDLE` | `10` | impl plan |
| `CONNECTION_TIMEOUT_MS` | `30_000L` | impl plan |
| `IDLE_TIMEOUT_MS` | `600_000L` | impl plan |
| `MAX_LIFETIME_MS` | `1_800_000L` | impl plan |
| `PREPARED_STATEMENT_CACHE_QUERIES` | `256` | impl plan |
| `PREPARED_STATEMENT_CACHE_SIZE_MIB` | `5` | impl plan |
| `LEAK_DETECTION_THRESHOLD_MS` | `60_000L` | new — for development leak detection |

> `MAX_POOL_SIZE` uses `val` (not `const`) because `Runtime.getRuntime()` is a runtime call. All other constants use `const val`.

---

#### 1.3 `DatabaseConfig` — `core:database:jvmMain`

```
data class DatabaseConfig(
    url: String,
    username: String,
    password: String,
    environment: Environment,
)
```

- `Environment` enum: `DEVELOPMENT`, `STAGING`, `PRODUCTION`
- `companion object { fun fromEnvironment(): DatabaseConfig }` reads from env vars:
  - `DB_URL` (default: `jdbc:postgresql://localhost:5432/vibely`)
  - `DB_USER` (default: `vibely`)
  - `DB_PASSWORD` (default: `password`)
  - `APP_ENV` (default: `DEVELOPMENT`)

---

#### 1.4 `TenantContext` — `core:database:jvmMain`

```
data class TenantContext(
    organizationId: OrganizationId,
    storeId: StoreId?,        // null = org-level operation
    userId: UserId,
)
```

---

#### 1.5 `DatabaseFactory` — `core:database:jvmMain`

Construction sequence:
1. Build `HikariConfig` from `DatabaseConfig` + `DatabaseConstants`
2. Set PostgreSQL JDBC data source properties (prepared statement cache, binary transfer, socket timeout, application name)
3. Enable leak detection when `environment == DEVELOPMENT`
4. Create `HikariDataSource` → call `Database.connect(dataSource)`
5. Run `Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).baselineVersion("1").load().migrate()` in `init` block

`withTenantContext` signature:
```kotlin
suspend fun <T> withTenantContext(ctx: TenantContext, block: suspend () -> T): T
```

Implementation:
- Validates `ctx.organizationId.value` and `ctx.userId.value` are non-blank before opening a transaction
- Opens `newSuspendedTransaction(Dispatchers.IO, database)`
- Executes `SET LOCAL app.current_organization_id = '...'`
- Executes `SET LOCAL app.current_user_id = '...'`
- If `ctx.storeId != null`: executes `SET LOCAL app.current_store_id = '...'`
- Invokes `block()`

> **Defence-in-depth**: Wrap UUID values with `UUID.fromString(value)` before interpolating into SQL to guard against non-UUID strings reaching `SET LOCAL`.

---

#### 1.6 `core:database` build.gradle.kts

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

---

#### 1.7 Repository & Sync Interfaces — `shared:commonMain`

Package `com.vibely.shared.data.source`:

**`LocalDataSource<T, ID>`**
- `suspend fun getById(id: ID): Result<T>`
- `suspend fun insert(entity: T): Result<T>`
- `suspend fun delete(id: ID): Result<Unit>`
- `fun observeById(id: ID): Flow<T>`

**`RemoteDataSource<T, ID>`**
- `suspend fun getById(id: ID): Result<T>`
- `suspend fun save(entity: T): Result<T>`

**`SyncManager`**
- `fun isOnline(): Boolean`
- `suspend fun queueForSync(entity: Any)`

Package `com.vibely.shared.data.repository`:

**`BaseRepository<T : Any, ID : Any>`** (abstract class)
- `getById(id: ID): Result<T>` — cache-first: local → remote on miss → insert into local
- `save(entity: T): Result<T>` — write-through: remote first if online, local + queue if offline
- `observeById(id: ID): Flow<T>` — reactive local stream; background sync triggered on subscription
- Background sync scope management delegated to concrete subclasses

---

#### 1.8 Mapper Interfaces — `shared:commonMain`

Package `com.vibely.shared.mapper`:

**`DomainMapper<Entity, Domain>`**
- `fun toDomain(entity: Entity): Domain`
- `fun toEntity(domain: Domain): Entity`

**`DtoMapper<Dto, Domain>`**
- `fun toDomain(dto: Dto): Domain`
- `fun toDto(domain: Domain): Dto`

---

#### 1.9 `shared:jvmMain` Stub Removal

1. Delete `shared/src/jvmMain/kotlin/com/vibely/shared/di/DatabaseStubs.kt`
2. In `shared/build.gradle.kts`: add `projects.core.database` to `jvmMain.dependencies`
3. In `shared/src/jvmMain/kotlin/com/vibely/shared/di/PlatformModule.kt`:
   - Remove inline `DatabaseConfig` and `DatabaseFactory` definitions
   - Add Koin bindings: `single<DatabaseConfig> { DatabaseConfig.fromEnvironment() }` and `single<DatabaseFactory> { DatabaseFactory(config = get()) }`

---

#### 1.10 Integration Test — `core:database:jvmTest`

File: `core/database/src/jvmTest/kotlin/com/vibely/database/DatabaseFactoryTest.kt`

Uses `PostgreSQLContainer` from Testcontainers. Test cases:
1. `withTenantContext` — `app.current_organization_id` is set correctly during transaction
2. `withTenantContext` — `app.current_store_id` is set when `storeId` is non-null
3. `withTenantContext` — `app.current_store_id` is absent when `storeId` is null
4. After transaction completes — session variables are not visible in a subsequent connection (isolation)
5. Flyway baseline — `flyway_schema_history` contains a V1 baseline record after first startup on empty history

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| `Runtime.getRuntime()` in `commonMain` `DatabaseConstants` | Low | Medium | KMP compiles per-target; JVM target handles it correctly. JS/Android targets don't use `DatabaseConstants` |
| UUID injection via `SET LOCAL` | Low | High | Add `UUID.fromString(value)` format guard in `withTenantContext` before SQL execution |
| Flyway baseline fails if `flyway_schema_history` already exists | Low | Low | `baselineOnMigrate` is idempotent — skips baseline if history already populated |
| Nested `newSuspendedTransaction` calls | Medium | Medium | Exposed re-uses the outer transaction for nested calls; document this behaviour for implementers |
| `shared:jvmMain` → `core:database` circular dependency | Low | High | `core:database` depends on `core:domain` + `core:common`; `shared` depends on `core:database` — no cycle as long as `core:database` does not depend on `shared` |
