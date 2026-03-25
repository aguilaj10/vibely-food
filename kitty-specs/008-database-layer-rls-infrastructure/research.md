# Research: Database Layer Infrastructure with RLS

**Feature**: 008-database-layer-rls-infrastructure
**Date**: 2026-03-25

---

## Decision 1: Flyway Strategy with Pre-Existing Schema

**Decision**: Use `baselineOnMigrate = true` with `baselineVersion = "1"`.

**Rationale**: The PostgreSQL schema was applied from `docs/database-schema.sql` directly — Flyway has no record of it in `flyway_schema_history`. When Flyway's `migrate()` runs on a database with an empty history table, it would normally try to apply V1, V2, ... from scratch. With `baselineOnMigrate = true`: if the history table is empty, Flyway inserts a baseline record at V1 (marking the existing schema as "already applied") and then only applies migrations V2+. This is idempotent — on subsequent startups the history table is non-empty so baseline is skipped.

**Validate-only rejected because**: It leaves `flyway_schema_history` empty forever. Adding the first real migration file later would require a manual `flyway baseline` command before switching to migrate mode — an error-prone operational step. `baselineOnMigrate` handles this automatically with zero operational overhead.

**Alternatives considered**:
- Validate-only: Trivial no-op now; requires manual remediation before first real migration
- `repair()`: Not appropriate — no checksum failures exist
- Skip Flyway entirely: Violates the infrastructure requirement to have migration wiring in place

---

## Decision 2: SyncManager as Interface vs Expect/Actual

**Decision**: Plain `interface` in `shared:commonMain`; concrete platform implementations registered via Koin.

**Rationale**: `SyncManager` has two responsibilities: `isOnline()` (platform-specific) and `queueForSync()` (likely platform-specific for outbox pattern). The existing pattern in this project for platform-specific services is `expect/actual` for capabilities with no behaviour (e.g., `SecureStorage`, `PlatformLogger`), and plain interfaces for services with richer contracts. `SyncManager` is closer to a service — it will have a JVM implementation (always online for the server, or network probe) and an Android implementation (ConnectivityManager). Using a plain interface keeps it consistent with `LocalDataSource` / `RemoteDataSource` and avoids the Kotlin `expect/actual` overhead for what is fundamentally a service abstraction.

**Alternatives considered**:
- `expect interface SyncManager`: Valid, but `expect/actual` for interfaces is only worth it when the interface itself needs to differ per platform; here the contract is identical — only the implementation differs
- Single hardcoded implementation: Violates the constitution's "abstraction layer required" for infrastructure

---

## Decision 3: TenantContext ID Types

**Decision**: `OrganizationId`, `StoreId`, `UserId` as new `@JvmInline value class(val value: String)` in `core:domain:commonMain`, package `com.vibely.domain.tenant`.

**Rationale**: The schema uses UUID primary keys for all tenant-scoped entities. All existing domain IDs in this project (`OrderId`, `EmployeeId`, `MenuItemId`, etc.) are `@JvmInline value class(val value: String)` — using `String` as the backing type for UUID strings. `TenantContext` in `core:database:jvmMain` depends on `core:domain`, so these classes must live in `core:domain`. Creating them in `core:database` would be the wrong layer (domain concepts do not belong in infrastructure).

**`Long` rejected because**: The schema uses UUID, not serial/bigserial, for org/store/user IDs.
**Raw `String` rejected because**: Loses type safety — passing `organizationId` where `storeId` is expected would compile silently.

---

## Decision 4: DatabaseConstants Fill-In

**Decision**: Replace all `TODO()` stubs in `core/common/src/commonMain/kotlin/com/vibely/common/DatabaseConstants.kt` with real values from section 0.3 of the implementation plan.

**Values**:
- `MAX_POOL_SIZE`: `(Runtime.getRuntime().availableProcessors() * 2).coerceAtLeast(10)` — dynamic `val`, not `const`
- `MIN_IDLE`: `10`
- `CONNECTION_TIMEOUT_MS`: `30_000L` (30 s)
- `IDLE_TIMEOUT_MS`: `600_000L` (10 min)
- `MAX_LIFETIME_MS`: `1_800_000L` (30 min, slightly below PostgreSQL's default `wait_timeout`)
- `PREPARED_STATEMENT_CACHE_QUERIES`: `256`
- `PREPARED_STATEMENT_CACHE_SIZE_MIB`: `5`
- `LEAK_DETECTION_THRESHOLD_MS`: `60_000L` (60 s — development only)

**Rationale**: These values are well-known HikariCP defaults for PostgreSQL, validated by the Vibely implementation plan and the HikariCP documentation. `MAX_POOL_SIZE` uses the "((core_count × 2) + effective_spindle_count)" formula; for an SSD-backed server spindle = 0, so it simplifies to `core_count × 2`, minimum 10.

---

## Decision 5: Library Versions

**Decision**: No version changes required.

**Finding**: `libs.versions.toml` already contains correct versions matching the constitution:
- `exposed = "1.1.1"` ✅
- `hikari = "7.0.2"` ✅ (newer than implementation plan's 6.0.0 — constitution says "always latest stable")
- `flyway = "12.1.1"` ✅
- `postgresql = "42.7.10"` ✅

All library aliases (`libs.bundles.exposed`, `libs.hikari`, `libs.bundles.flyway`, `libs.postgresql`) also already exist in the catalog.

---

## Decision 6: RLS Session Variable Set

**Decision**: `withTenantContext` sets three variables — `app.current_organization_id`, `app.current_user_id` always; `app.current_store_id` only when `ctx.storeId != null`.

**Rationale**: Inspecting `docs/database-schema.sql`:
- `stores` table policies check only `app.current_organization_id`
- All other tables check both `app.current_organization_id` AND `app.current_store_id`
- The audit log trigger uses `current_setting('app.current_user_id', true)` — if not set, the audit record silently records `null`

Setting `app.current_user_id` from day one ensures audit log integrity even before a dedicated auth layer is introduced. Org-level operations (e.g., reading organization metadata) need only `organizationId` + `userId` set, hence `storeId` is optional in `TenantContext`.

**`withStoreContext` name rejected**: Clashes with Kotlin's stdlib `withContext`. Renamed to `withTenantContext`.
