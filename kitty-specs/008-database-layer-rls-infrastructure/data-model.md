# Data Model: Database Layer Infrastructure with RLS

**Feature**: 008-database-layer-rls-infrastructure
**Date**: 2026-03-25

---

## Overview

This feature introduces infrastructure-level types — not persistent entities. The "data model" here covers the Kotlin types and their relationships across layers.

---

## Tenant Identity Types — `core:domain:commonMain`

Package: `com.vibely.domain.tenant`

| Type | Backing | Purpose |
|---|---|---|
| `OrganizationId` | `String` (UUID) | Top-level tenant identifier |
| `StoreId` | `String` (UUID) | Store-level isolation boundary |
| `UserId` | `String` (UUID) | Authenticated user for audit trail |

All three are `@JvmInline value class`. UUID format is guaranteed by the authentication layer, not validated inside these classes.

**Relationships**:
- An `OrganizationId` owns zero or more `StoreId`s
- A `UserId` belongs to exactly one `OrganizationId` and one `StoreId`
- These relationships are enforced by the database schema, not by the Kotlin type system

---

## TenantContext — `core:database:jvmMain`

Package: `com.vibely.database`

```
TenantContext
├── organizationId: OrganizationId   (required)
├── storeId: StoreId?                (null = org-level operation)
└── userId: UserId                   (required — needed for audit log)
```

**Validation rules**:
- `organizationId.value` must be non-blank
- `userId.value` must be non-blank
- `storeId?.value` must be non-blank if present
- All values are validated with `UUID.fromString()` before use in `SET LOCAL`

**Lifecycle**: Created per-request at the HTTP handler level (from validated JWT claims); discarded after `withTenantContext` returns.

---

## DatabaseConfig — `core:database:jvmMain`

Package: `com.vibely.database`

```
DatabaseConfig
├── url: String              (JDBC URL — points to PgBouncer, not PostgreSQL directly)
├── username: String
├── password: String
└── environment: Environment (DEVELOPMENT | STAGING | PRODUCTION)
```

```
Environment
├── DEVELOPMENT   → leak detection enabled, verbose logging
├── STAGING       → production-like, additional validation
└── PRODUCTION    → maximum performance, minimal overhead
```

---

## DatabaseFactory — `core:database:jvmMain`

Package: `com.vibely.database`

Not a data class — a stateful singleton managed by Koin.

```
DatabaseFactory(config: DatabaseConfig)
├── dataSource: HikariDataSource  (private, created in init)
├── database: Database            (Exposed Database instance, private)
└── withTenantContext(ctx: TenantContext, block): T  (suspend)
```

**Pool properties** (sourced from `DatabaseConstants`):

| Property | Value |
|---|---|
| `maximumPoolSize` | `DatabaseConstants.MAX_POOL_SIZE` |
| `minimumIdle` | `DatabaseConstants.MIN_IDLE` |
| `connectionTimeout` | `DatabaseConstants.CONNECTION_TIMEOUT_MS` |
| `idleTimeout` | `DatabaseConstants.IDLE_TIMEOUT_MS` |
| `maxLifetime` | `DatabaseConstants.MAX_LIFETIME_MS` |
| `connectionTestQuery` | `SELECT 1` |
| `leakDetectionThreshold` | `DatabaseConstants.LEAK_DETECTION_THRESHOLD_MS` (DEVELOPMENT only) |

**JDBC data source properties**:

| Property | Value |
|---|---|
| `preparedStatementCacheQueries` | `DatabaseConstants.PREPARED_STATEMENT_CACHE_QUERIES` (256) |
| `preparedStatementCacheSizeMiB` | `DatabaseConstants.PREPARED_STATEMENT_CACHE_SIZE_MIB` (5) |
| `binaryTransfer` | `true` |
| `socketTimeout` | `30` |
| `ApplicationName` | `vibely-pos` |

---

## Repository Interfaces — `shared:commonMain`

Package: `com.vibely.shared.data.source`

```
LocalDataSource<T : Any, ID : Any>
├── suspend getById(id: ID): Result<T>
├── suspend insert(entity: T): Result<T>
├── suspend delete(id: ID): Result<Unit>
└── observeById(id: ID): Flow<T>

RemoteDataSource<T : Any, ID : Any>
├── suspend getById(id: ID): Result<T>
└── suspend save(entity: T): Result<T>

SyncManager
├── isOnline(): Boolean
└── suspend queueForSync(entity: Any)
```

Package: `com.vibely.shared.data.repository`

```
BaseRepository<T : Any, ID : Any>  (abstract class)
├── protected abstract localDataSource: LocalDataSource<T, ID>
├── protected abstract remoteDataSource: RemoteDataSource<T, ID>
├── protected abstract syncManager: SyncManager
├── suspend getById(id: ID): Result<T>
├── suspend save(entity: T): Result<T>
└── observeById(id: ID): Flow<T>
```

**Read strategy** (getById):
```
1. Try localDataSource.getById(id)
2. On success → return local result immediately
3. On failure → call remoteDataSource.getById(id)
4. On remote success → insert into local, return result
5. On remote failure → propagate failure
```

**Write strategy** (save):
```
Online path:
  1. remoteDataSource.save(entity)
  2. On success → localDataSource.insert(result)
  3. Return result

Offline path:
  1. localDataSource.insert(entity)
  2. On success → syncManager.queueForSync(entity)
  3. Return local result
```

---

## Mapper Interfaces — `shared:commonMain`

Package: `com.vibely.shared.mapper`

```
DomainMapper<Entity, Domain>
├── toDomain(entity: Entity): Domain
└── toEntity(domain: Domain): Entity

DtoMapper<Dto, Domain>
├── toDomain(dto: Dto): Domain
└── toDto(domain: Domain): Dto
```

**Layer responsibilities**:
- `Entity` — flat, persistence-optimised struct (Room entity, Exposed ResultRow mapping)
- `Dto` — serialisation-optimised struct for network (`@Serializable`)
- `Domain` — rich model with business logic and validation (`core:domain`)
- Mappers live in the data layer; domain models never import Entity or Dto types

---

## DatabaseConstants (updated) — `core:common:commonMain`

Package: `com.vibely.common`

| Constant | Type | Value |
|---|---|---|
| `MAX_POOL_SIZE` | `val Int` | `(Runtime.availableProcessors() × 2).coerceAtLeast(10)` |
| `MIN_IDLE` | `const val Int` | `10` |
| `CONNECTION_TIMEOUT_MS` | `const val Long` | `30_000L` |
| `IDLE_TIMEOUT_MS` | `const val Long` | `600_000L` |
| `MAX_LIFETIME_MS` | `const val Long` | `1_800_000L` |
| `PREPARED_STATEMENT_CACHE_QUERIES` | `const val Int` | `256` |
| `PREPARED_STATEMENT_CACHE_SIZE_MIB` | `const val Int` | `5` |
| `LEAK_DETECTION_THRESHOLD_MS` | `const val Long` | `60_000L` |
