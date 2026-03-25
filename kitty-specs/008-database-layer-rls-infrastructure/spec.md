# Feature Specification: Database Layer Infrastructure with RLS

**Feature Branch**: `008-database-layer-rls-infrastructure`
**Created**: 2026-03-25
**Status**: Draft
**Input**: Phase 1.1 of the implementation plan (sections 1.1.1, 1.1.3, 1.1.4)

## User Scenarios & Testing *(mandatory)*

This is a foundational infrastructure feature. Scenarios are framed as developer-operator journeys because the primary consumers are engineers wiring database access into the server module.

---

### User Story 1 — Tenant-Isolated Database Query (Priority: P1)

A backend developer needs to execute a database query that is automatically scoped to the correct organization and store, with no risk of cross-tenant data leakage.

**Why this priority**: This is the core RLS safety guarantee. Without it, all subsequent database work is insecure.

**Independent Test**: A developer can call `withTenantContext` inside a coroutine, execute an Exposed `selectAll()`, and verify that the RLS session variables are set for the duration of the transaction and cleared afterward.

**Acceptance Scenarios**:

1. **Given** a valid `TenantContext(organizationId, storeId, userId)`, **When** `withTenantContext` is called, **Then** `app.current_organization_id`, `app.current_store_id`, and `app.current_user_id` are all set via `SET LOCAL` before the block executes.
2. **Given** an org-level operation requiring no store scope, **When** `withTenantContext` is called with `storeId = null`, **Then** only `app.current_organization_id` and `app.current_user_id` are set; `app.current_store_id` is not set.
3. **Given** a transaction that completes normally, **When** the transaction ends, **Then** all three session variables are automatically cleared (SET LOCAL guarantee — no explicit cleanup needed).
4. **Given** a transaction that throws an exception, **When** the exception propagates, **Then** the transaction is rolled back and RLS variables are cleared.

---

### User Story 2 — Application Startup with Database Readiness (Priority: P1)

When the Ktor server starts, the database connection pool is established and Flyway confirms the schema is up to date before the server begins accepting requests.

**Why this priority**: The server must not serve traffic against an unprepared database.

**Independent Test**: Starting the server locally with a running PostgreSQL instance succeeds and logs confirm pool initialization and Flyway migration status.

**Acceptance Scenarios**:

1. **Given** a running PostgreSQL instance reachable via `DB_URL`, **When** `DatabaseFactory` is instantiated, **Then** HikariCP creates the connection pool with the configured min/max sizes and the pool health check passes.
2. **Given** the schema is already current (no pending Flyway migrations), **When** Flyway runs on startup, **Then** it reports success without applying any changes.
3. **Given** the `DEVELOPMENT` environment flag, **When** the pool is configured, **Then** leak detection is enabled with a 60-second threshold.
4. **Given** an unreachable database URL, **When** `DatabaseFactory` is instantiated, **Then** a clear exception is thrown at startup, not silently at query time.

---

### User Story 3 — Cache-First Repository Read (Priority: P2)

A developer implementing a concrete repository needs a base class that automatically handles the cache-first read strategy: serve from local storage immediately, fetch from remote in the background, and keep the local cache fresh.

**Why this priority**: Enables offline-first behaviour for all future entity repositories without reimplementing the strategy each time.

**Independent Test**: A test double implementing `LocalDataSource` and `RemoteDataSource` can be wired into a concrete `BaseRepository` subclass; calling `getById` returns local data immediately when available, and triggers a remote fetch that updates the local store.

**Acceptance Scenarios**:

1. **Given** local data exists for an ID, **When** `getById(id)` is called, **Then** the local result is returned without contacting the remote source.
2. **Given** local data does not exist for an ID, **When** `getById(id)` is called, **Then** the remote source is queried and the result is inserted into the local store before being returned.
3. **Given** the device is online, **When** `save(entity)` is called, **Then** the entity is written to the remote source first; on success it is also written to the local store.
4. **Given** the device is offline (`SyncManager.isOnline()` returns false), **When** `save(entity)` is called, **Then** the entity is written locally and queued for sync via `SyncManager.queueForSync`.
5. **Given** an observer is active on `observeById(id)`, **When** new data arrives from a background sync, **Then** the Flow emits the updated value.

---

### User Story 4 — Bidirectional Domain Mapping (Priority: P2)

A developer implementing a data layer component needs a standard contract for converting between domain models, persistence entities, and network DTOs without leaking representation concerns across layer boundaries.

**Why this priority**: Prevents DTO/entity types from polluting the domain layer, enforcing Clean Architecture boundaries.

**Independent Test**: Two mapper interface implementations (one `DomainMapper`, one `DtoMapper`) can be instantiated and called to convert in both directions without data loss.

**Acceptance Scenarios**:

1. **Given** a persistence entity, **When** `DomainMapper.toDomain(entity)` is called, **Then** a domain model instance is returned with all fields correctly mapped.
2. **Given** a domain model, **When** `DomainMapper.toEntity(domain)` is called, **Then** a persistence entity is returned with all fields correctly mapped.
3. **Given** a network DTO, **When** `DtoMapper.toDomain(dto)` is called, **Then** a domain model instance is returned with all fields correctly mapped.
4. **Given** a domain model, **When** `DtoMapper.toDto(domain)` is called, **Then** a network DTO is returned ready for serialization.
5. **Given** any mapper, **When** a round-trip conversion is performed (domain → entity → domain), **Then** the result is equal to the original domain model.

---

### Edge Cases

- What happens when `withTenantContext` is called with an empty `organizationId`? The system must validate the context before opening a transaction.
- How does the system handle connection pool exhaustion? A clear timeout error with pool stats in the message.
- What if `SyncManager.queueForSync` throws? The local write must not be rolled back — the entity is persisted locally; the sync failure is logged.
- What if `remoteDataSource.getById` succeeds but `localDataSource.insert` fails during the cache update? The remote result is returned to the caller; the cache miss is logged but does not surface as an error.
- What happens during `observeById` when the local store is empty and the background sync is still in flight? The Flow emits nothing until data arrives (no null/empty intermediate emission unless the local store explicitly records a "not found" state).

---

## Requirements *(mandatory)*

### Functional Requirements

#### core:database Module (jvmMain)

- **FR-001**: `TenantContext` MUST carry `organizationId: OrganizationId` (required), `storeId: StoreId?` (optional), and `userId: UserId` (required).
- **FR-002**: `withTenantContext(ctx: TenantContext, block)` MUST set `app.current_organization_id` and `app.current_user_id` via `SET LOCAL` on every call; MUST additionally set `app.current_store_id` only when `ctx.storeId` is non-null.
- **FR-003**: All three session variables MUST be set within a single suspended transaction; they MUST NOT be set outside a transaction boundary.
- **FR-004**: `DatabaseFactory` MUST configure HikariCP with `maximumPoolSize`, `minimumIdle`, `connectionTimeout`, `maxLifetime`, `idleTimeout` drawn from `DatabaseConstants`.
- **FR-005**: `DatabaseFactory` MUST enable PostgreSQL prepared-statement caching, binary transfer, and application name via JDBC data source properties.
- **FR-006**: `DatabaseFactory` MUST enable leak detection only when `DatabaseConfig.environment == Environment.DEVELOPMENT`.
- **FR-007**: `DatabaseFactory` MUST run Flyway on startup pointing at the existing database; it MUST NOT include any migration SQL files (schema already exists).
- **FR-008**: `DatabaseConfig` MUST read `url`, `username`, `password`, and `environment` from the environment at construction time.

#### shared:commonMain — Repository & Sync Interfaces

- **FR-009**: `LocalDataSource<T, ID>` MUST declare `getById`, `insert`, `delete`, and `observeById` operations.
- **FR-010**: `RemoteDataSource<T, ID>` MUST declare `getById` and `save` operations.
- **FR-011**: `SyncManager` MUST declare `isOnline(): Boolean` and `queueForSync(entity: Any)` operations.
- **FR-012**: `BaseRepository<T, ID>` MUST implement cache-first `getById`, write-through `save`, and reactive `observeById` using the three interfaces above.
- **FR-013**: `BaseRepository.getById` MUST return local data immediately when available, without contacting the remote source.
- **FR-014**: `BaseRepository.save` MUST write to the remote source first when online; on remote success it MUST also update the local store.
- **FR-015**: `BaseRepository.save` MUST write locally and queue for sync when offline.

#### shared:commonMain — Mapper Interfaces

- **FR-016**: `DomainMapper<Entity, Domain>` MUST declare `toDomain(entity: Entity): Domain` and `toEntity(domain: Domain): Entity`.
- **FR-017**: `DtoMapper<Dto, Domain>` MUST declare `toDomain(dto: Dto): Domain` and `toDto(domain: Domain): Dto`.
- **FR-018**: Mapper interfaces MUST live in the domain/data boundary package and MUST NOT carry any framework or serialization dependency.

#### shared:jvmMain — Stub Removal

- **FR-019**: `DatabaseStubs.kt` MUST be deleted from `shared:jvmMain`.
- **FR-020**: `shared:jvmMain/PlatformModule` MUST bind `DatabaseConfig` and `DatabaseFactory` sourced from `core:database`, not from inline definitions.

### Key Entities

- **TenantContext**: Represents the security context for a single database transaction. Carries `organizationId` (required), `storeId` (optional for org-level operations), and `userId` (required for audit trail).
- **DatabaseConfig**: Holds JDBC connection parameters and the runtime environment indicator; populated from environment variables at startup.
- **DatabaseFactory**: Manages the HikariCP connection pool and exposes `withTenantContext` as the sole entry point for transactional database access.
- **BaseRepository**: Abstract base class parameterised over a domain type `T` and its identifier `ID`. Encapsulates cache-first and write-through strategies.
- **LocalDataSource / RemoteDataSource / SyncManager**: Interfaces that concrete data-layer components implement to plug into `BaseRepository`.
- **DomainMapper / DtoMapper**: Interfaces that enforce Clean Architecture layer separation by mandating explicit, bidirectional conversion functions.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Any call to `withTenantContext` without a valid `organizationId` fails fast at the application level before a database connection is acquired.
- **SC-002**: All RLS session variables set inside `withTenantContext` are absent in a subsequent query issued outside any tenant context on the same connection.
- **SC-003**: Cache-first `getById` returns a locally cached value without incurring any network round-trip when local data is present.
- **SC-004**: All required RLS session variables are set within a single database round-trip (one `SET LOCAL` statement per variable, all inside the same transaction).
- **SC-005**: `DatabaseFactory` startup completes and logs a successful pool initialisation message before the Ktor server begins accepting connections.
- **SC-006**: Flyway runs on every server start and reports its migration status (up to date or migrations applied) without throwing when the schema is current.
- **SC-007**: `BaseRepository.save` in offline mode persists the entity locally and enqueues a sync task without data loss; a subsequent `getById` call returns the locally saved entity.
- **SC-008**: Round-trip conversion through any `DomainMapper` implementation (domain → entity → domain) produces a value equal to the original.

---

## Assumptions

- PgBouncer will be configured in **session pooling mode** externally (infrastructure concern, not a code deliverable). The application connects to PgBouncer, not directly to PostgreSQL.
- The existing `database-schema.sql` represents the current, applied schema. No Flyway migration SQL files are created by this feature.
- `core:database` remains a KMP module (using the `kmp.library` convention plugin) but only the `jvmMain` source set carries code; `commonMain`, `androidMain`, and `jsMain` remain empty.
- `DatabaseConstants` already exists or will be created in `core:common` as documented in section 0.3 of the implementation plan.
- `OrganizationId`, `StoreId`, and `UserId` value classes already exist in `core:domain`.
