---
description: "Work package task list for Database Layer Infrastructure with RLS"
---

# Work Packages: Database Layer Infrastructure with RLS

**Inputs**: `/kitty-specs/008-database-layer-rls-infrastructure/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, quickstart.md ✅

**Tests**: Integration tests explicitly required in spec (Testcontainers RLS verification).

**Organization**: 20 subtasks rolled into 5 work packages. WP01 and WP02 are fully parallel (no shared files). WP03 unblocks after WP01. WP04 needs WP02 + WP03. WP05 needs WP03.

---

## Work Package WP01: Foundation Types and Build Configuration (Priority: P0)

**Goal**: Introduce the tenant value classes required by `TenantContext`, fill in real `DatabaseConstants` values, and wire `core:database` build dependencies — all prerequisites for WP03.
**Independent Test**: `core:domain` and `core:common` compile cleanly; `core:database` Gradle sync succeeds with new jvmMain dependencies.
**Prompt**: `tasks/WP01-foundation-types-and-build-config.md`
**Estimated size**: ~230 lines

### Included Subtasks
- [x] T001 Create tenant value classes (OrganizationId, StoreId, UserId) in `core/domain/src/commonMain/kotlin/com/vibely/domain/tenant/`
- [x] T002 [P] Fill in real values in `core/common/src/commonMain/kotlin/com/vibely/common/DatabaseConstants.kt`
- [x] T003 [P] Update `core/database/build.gradle.kts` — add jvmMain + jvmTest dependency blocks

**Requirement Refs**: FR-001, FR-002, FR-003, FR-004, FR-005, FR-006, FR-007, FR-008

### Implementation Notes
- T001 creates a new `tenant/` package; T002 and T003 are edits to existing files.
- T002 and T003 have no shared files — fully parallel after T001.
- `MAX_POOL_SIZE` must be `val` (not `const`) because it calls `Runtime.getRuntime()`.
- `core/database/build.gradle.kts` uses `alias(libs.plugins.kmp.library)` — do NOT change the plugin; only add source set dependencies.

### Parallel Opportunities
- T002 and T003 can be implemented simultaneously once T001 is committed (but T001 is fast — no need to parallelize in practice).

### Dependencies
- None (foundation package — run in parallel with WP02).

### Risks & Mitigations
- `Runtime.getRuntime()` in `commonMain`: compiles per-target; JS/Android targets don't use `DatabaseConstants` at runtime.

---

## Work Package WP02: Repository and Mapper Interfaces (Priority: P0)

**Goal**: Define all `shared:commonMain` infrastructure interfaces — `LocalDataSource`, `RemoteDataSource`, `SyncManager`, `BaseRepository`, `DomainMapper`, `DtoMapper` — establishing the patterns all future feature repositories will use.
**Independent Test**: `shared:commonMain` compiles cleanly; interfaces are visible in IDE with correct generic signatures.
**Prompt**: `tasks/WP02-repository-and-mapper-interfaces.md`
**Estimated size**: ~380 lines

### Included Subtasks
- [x] T004 [P] Create `LocalDataSource<T, ID>` interface in `shared/src/commonMain/kotlin/com/vibely/shared/data/source/`
- [x] T005 [P] Create `RemoteDataSource<T, ID>` interface in same package
- [x] T006 [P] Create `SyncManager` interface in same package
- [x] T007 Create `BaseRepository<T, ID>` abstract class in `shared/src/commonMain/kotlin/com/vibely/shared/data/repository/`
- [x] T008 [P] Create `DomainMapper<Entity, Domain>` interface in `shared/src/commonMain/kotlin/com/vibely/shared/mapper/`
- [x] T009 [P] Create `DtoMapper<Dto, Domain>` interface in same package

**Requirement Refs**: FR-009, FR-010, FR-011, FR-012, FR-013, FR-014, FR-015, FR-016, FR-017, FR-018

### Implementation Notes
- T004, T005, T006, T008, T009 are independent single-file creations — all parallel.
- T007 depends on T004, T005, T006 (uses all three interfaces as abstract properties).
- `BaseRepository` uses `Result<T>` for all fallible operations — no exceptions for control flow.
- Background sync scope is NOT managed by `BaseRepository` itself; subclasses own that concern. The `observeById` default implementation only triggers sync via a protected open function.
- All interfaces must have KDoc on every method (Detekt enforces this).

### Parallel Opportunities
- T004–T006, T008–T009 can all be written simultaneously. T007 can start once T004–T006 exist.

### Dependencies
- None (run in parallel with WP01).

### Risks & Mitigations
- `BaseRepository.save` offline path must queue for sync AND return `Result.success` — failure to queue must not surface as an error to the caller (log and move on).

---

## Work Package WP03: DatabaseFactory with HikariCP, Flyway, and TenantContext (Priority: P0)

**Goal**: Implement the core `core:database:jvmMain` module — `DatabaseConfig`, `TenantContext`, and `DatabaseFactory` with production-grade connection pooling, Flyway baseline wiring, and the `withTenantContext` RLS method.
**Independent Test**: `DatabaseFactory` instantiates without error against a local PostgreSQL instance; `withTenantContext` can be called and returns a result.
**Prompt**: `tasks/WP03-database-factory-hikaricp-flyway-tenantcontext.md`
**Estimated size**: ~450 lines

### Included Subtasks
- [ ] T010 Create `DatabaseConfig.kt` with `Environment` enum and `fromEnvironment()` companion factory
- [ ] T011 [P] Create `TenantContext.kt` data class (depends on OrganizationId/StoreId/UserId from WP01)
- [ ] T012 Create `DatabaseFactory.kt` — HikariCP pool construction from `DatabaseConfig` + `DatabaseConstants`
- [ ] T013 Implement `withTenantContext` inside `DatabaseFactory` — `SET LOCAL` execution + UUID format guard

**Requirement Refs**: FR-001, FR-002, FR-003, FR-004, FR-005, FR-006, FR-007, FR-008

### Implementation Notes
- T010 and T011 have no shared code — parallel.
- T012 depends on T010 (uses `DatabaseConfig` + `Environment`).
- T013 lives inside `DatabaseFactory` — implement in the same file as T012 or as an extension.
- Flyway runs in the `DatabaseFactory` `init` block: `Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).baselineVersion("1").validateOnMigrate(true).load().migrate()`.
- `withTenantContext` uses Exposed `newSuspendedTransaction(Dispatchers.IO, database)`.
- UUID format guard: call `UUID.fromString(value)` on `organizationId.value`, `userId.value`, and `storeId?.value` before interpolating into SQL. Throw `IllegalArgumentException` if invalid — this surfaces at the HTTP layer, not in the DB.
- HikariCP `connectionTestQuery = "SELECT 1"` (required for pgBouncer session pooling compatibility).

### Parallel Opportunities
- T010 and T011 can be written simultaneously.

### Dependencies
- Depends on WP01 (requires `OrganizationId`, `StoreId`, `UserId`, `DatabaseConstants`).

### Risks & Mitigations
- Nested `newSuspendedTransaction` calls: Exposed reuses the outer transaction — document this in KDoc on `withTenantContext`.
- `app.current_store_id` must NOT be set when `storeId` is null — a `SET LOCAL` with an empty string would silently corrupt the RLS context.

---

## Work Package WP04: Stub Removal and PlatformModule Wiring (Priority: P1)

**Goal**: Delete `DatabaseStubs.kt`, update `shared/build.gradle.kts` to depend on `core:database`, and update `shared:jvmMain/PlatformModule.kt` to bind the real `DatabaseConfig` and `DatabaseFactory`.
**Independent Test**: Full project Gradle build completes cleanly; `server` module starts without `ClassNotFoundException` for `DatabaseFactory`.
**Prompt**: `tasks/WP04-stub-removal-and-platform-module-wiring.md`
**Estimated size**: ~210 lines

### Included Subtasks
- [ ] T014 Delete `shared/src/jvmMain/kotlin/com/vibely/shared/di/DatabaseStubs.kt`
- [ ] T015 [P] Add `projects.core.database` to `shared/build.gradle.kts` jvmMain dependencies
- [ ] T016 Update `shared/src/jvmMain/kotlin/com/vibely/shared/di/PlatformModule.kt` — replace inline stub bindings with real `DatabaseConfig.fromEnvironment()` + `DatabaseFactory(get())` Koin bindings

**Requirement Refs**: FR-019, FR-020

### Implementation Notes
- T014 and T015 can be done simultaneously — no shared files.
- T016 depends on T014 and T015 (stub must be gone before referencing `DatabaseConfig` from `core:database`).
- After T014, the compiler will report errors until T015 + T016 are complete — implement all three in the same commit.
- In `PlatformModule.kt`, the Koin bindings become:
  ```kotlin
  single<DatabaseConfig> { DatabaseConfig.fromEnvironment() }
  single<DatabaseFactory> { DatabaseFactory(config = get()) }
  ```
  Remove the old inline `DatabaseConfig(url=..., username=..., password=...)` block entirely.

### Parallel Opportunities
- T014 + T015 can be written simultaneously; T016 follows.

### Dependencies
- Depends on WP02 (BaseRepository interfaces in `shared:commonMain`) and WP03 (`DatabaseFactory` in `core:database:jvmMain`).

### Risks & Mitigations
- Breaking build between T014 and T016: implement all three subtasks atomically (single commit) to avoid leaving the project in a non-compiling state.
- Circular dependency check: ensure `core:database` does NOT depend on `shared` — only `shared` depends on `core:database`.

---

## Work Package WP05: RLS Integration Tests (Priority: P1)

**Goal**: Validate that `DatabaseFactory.withTenantContext` correctly sets and isolates PostgreSQL RLS session variables, and that Flyway creates the V1 baseline record on first startup.
**Independent Test**: `./gradlew :core:database:jvmTest` passes all four test cases against a Testcontainers PostgreSQL instance.
**Prompt**: `tasks/WP05-rls-integration-tests.md`
**Estimated size**: ~350 lines

### Included Subtasks
- [ ] T017 Configure `PostgreSQLContainer` and create `DatabaseFactoryTest` test class skeleton
- [ ] T018 Write test: `withTenantContext` sets `app.current_organization_id`, `app.current_store_id`, `app.current_user_id` correctly
- [ ] T019 Write test: RLS session variables are absent after transaction ends (isolation across connections)
- [ ] T020 Write test: Flyway inserts V1 baseline record in `flyway_schema_history` on empty-history DB

**Requirement Refs**: FR-002, FR-003, FR-007

### Implementation Notes
- Tests live in `core/database/src/jvmTest/kotlin/com/vibely/database/DatabaseFactoryTest.kt`.
- Use `@Testcontainers` + `companion object { @Container val postgres = PostgreSQLContainer("postgres:17") }`.
- For T018: execute `SELECT current_setting('app.current_organization_id', true)` inside `withTenantContext` and assert the value matches `ctx.organizationId.value`.
- For T019: after `withTenantContext` returns, open a raw JDBC connection and query `current_setting('app.current_store_id', true)` — the result must be empty string or blank (PostgreSQL returns `""` for unset GUCs when the `missing_ok` flag is true).
- For T020: point `DatabaseFactory` at the container; after construction check `SELECT version FROM flyway_schema_history LIMIT 1` returns `"1"`.
- Use `kotest-assertions` `shouldBe` / `shouldNotBeNull` throughout — no JUnit 4 `assertEquals`.
- Project convention: tests in `jvmTest`, not `commonTest`.

### Parallel Opportunities
- T018, T019, T020 can each be written in parallel once T017 (the shared test container setup) is complete.

### Dependencies
- Depends on WP03 (`DatabaseFactory` must be implemented before tests can reference it).

### Risks & Mitigations
- Testcontainers Docker daemon requirement: ensure Docker is running in CI (Jenkins). If not available, mark test as `@Disabled("requires Docker")` and document the CI prerequisite.
- Flyway test (T020): the Testcontainers postgres instance starts empty — `flyway_schema_history` won't exist until Flyway runs. `DatabaseFactory` init runs Flyway, so by the time any test method runs, Flyway has already executed.

---

## Dependency & Execution Summary

```
WP01 ─────────────────────────────┐
                                   ├──► WP03 ──► WP05
WP02 ──────────────────────────── ┤
                                   └──► WP04
```

- **Parallel start**: WP01 and WP02 (no shared files).
- **WP03** unblocks when WP01 is in `done`.
- **WP04** unblocks when WP02 AND WP03 are in `done`.
- **WP05** unblocks when WP03 is in `done` (can run in parallel with WP04).
- **MVP scope**: WP01 + WP02 + WP03 — provides the database factory and all commonMain interfaces. WP04 + WP05 complete the integration.

---

## Subtask Index

| Subtask ID | Summary | Work Package | Priority | Parallel? |
|------------|---------|--------------|----------|-----------|
| T001 | Create OrganizationId, StoreId, UserId value classes | WP01 | P0 | No |
| T002 | Fill in DatabaseConstants real values | WP01 | P0 | Yes (after T001) |
| T003 | Update core:database build.gradle.kts | WP01 | P0 | Yes (after T001) |
| T004 | Create LocalDataSource interface | WP02 | P0 | Yes |
| T005 | Create RemoteDataSource interface | WP02 | P0 | Yes |
| T006 | Create SyncManager interface | WP02 | P0 | Yes |
| T007 | Create BaseRepository abstract class | WP02 | P0 | No (needs T004-T006) |
| T008 | Create DomainMapper interface | WP02 | P0 | Yes |
| T009 | Create DtoMapper interface | WP02 | P0 | Yes |
| T010 | Create DatabaseConfig + Environment | WP03 | P0 | Yes |
| T011 | Create TenantContext data class | WP03 | P0 | Yes |
| T012 | Create DatabaseFactory (HikariCP + Flyway) | WP03 | P0 | No (needs T010) |
| T013 | Implement withTenantContext | WP03 | P0 | No (needs T011+T012) |
| T014 | Delete DatabaseStubs.kt | WP04 | P1 | Yes |
| T015 | Add core:database dep to shared build.gradle.kts | WP04 | P1 | Yes |
| T016 | Update PlatformModule.kt bindings | WP04 | P1 | No (needs T014+T015) |
| T017 | Configure Testcontainers + test skeleton | WP05 | P1 | No |
| T018 | Test: withTenantContext sets RLS variables | WP05 | P1 | Yes (after T017) |
| T019 | Test: RLS variables absent after transaction | WP05 | P1 | Yes (after T017) |
| T020 | Test: Flyway V1 baseline record | WP05 | P1 | Yes (after T017) |
