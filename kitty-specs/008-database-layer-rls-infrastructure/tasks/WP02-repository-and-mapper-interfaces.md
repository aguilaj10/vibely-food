---
work_package_id: WP02
title: Repository and Mapper Interfaces
lane: "doing"
dependencies: []
base_branch: main
base_commit: 7937812966dbb33aa5dcc1a2b0656ad7d3c058ce
created_at: '2026-03-25T01:54:09.948564+00:00'
subtasks:
- T004
- T005
- T006
- T007
- T008
- T009
phase: Phase A - Foundation (parallel)
assignee: ''
agent: ''
shell_pid: "66797"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-25T01:40:02Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-009
- FR-010
- FR-011
- FR-012
- FR-013
- FR-014
- FR-015
- FR-016
- FR-017
- FR-018
---

# Work Package Prompt: WP02 – Repository and Mapper Interfaces

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

Define all `shared:commonMain` infrastructure interfaces — `LocalDataSource`, `RemoteDataSource`, `SyncManager`, `BaseRepository`, `DomainMapper`, `DtoMapper` — establishing the patterns all future feature repositories will follow.

**Done when**:
- `shared:commonMain` compiles cleanly
- All 6 types are in the correct packages and visible in IDE with correct generic signatures
- `BaseRepository` correctly implements cache-first read and write-through write strategies
- Every public interface, class, and method has KDoc

**Implementation command** (no dependencies):
```bash
spec-kitty implement WP02
```

---

## Context & Constraints

**Key references**:
- Constitution: `.kittify/memory/constitution.md` — `Result<T>` for fallible ops, KDoc mandatory, no mocks (use fakes), zero framework deps in `shared:commonMain`
- Plan: `kitty-specs/008-database-layer-rls-infrastructure/plan.md` — sections 1.7 and 1.8
- Data model: `kitty-specs/008-database-layer-rls-infrastructure/data-model.md` — repository interfaces section, mapper interfaces section
- Quickstart: `kitty-specs/008-database-layer-rls-infrastructure/quickstart.md` — Scenarios 3 and 4 show concrete usage

**Architectural constraints**:
- All interfaces live in `shared:commonMain` — ZERO framework dependencies (no Ktor, no Room, no Exposed, no Koin)
- `Result<T>` is used for all fallible operations — never throw exceptions for control flow
- Background sync scope is NOT managed by `BaseRepository`; subclasses own that concern
- `observeById` default implementation in `BaseRepository` only triggers sync via a protected open function — it does not manage coroutine scopes directly
- All interfaces must have KDoc on every method (Detekt enforces this)

**Dependency within this WP**:
- T004, T005, T006, T008, T009 are independent single-file creations — all parallel
- T007 depends on T004, T005, T006 (uses all three as abstract properties in `BaseRepository`)

---

## Subtasks & Detailed Guidance

### Subtask T004 – Create LocalDataSource interface

- **Purpose**: Defines the contract for local persistence (Room on Android, SQLite/Exposed on JVM). All feature-specific local data sources implement this.
- **Parallel?**: Yes — independent of T005, T006, T008, T009.
- **Files**: Create `shared/src/commonMain/kotlin/com/vibely/shared/data/source/LocalDataSource.kt`
- **Steps**:
  1. Create the file with the following content:
     ```kotlin
     package com.vibely.shared.data.source

     import kotlinx.coroutines.flow.Flow

     /**
      * Contract for a local persistence data source that stores entities of type [T]
      * identified by [ID].
      *
      * Implementations are platform-specific (e.g., Room on Android, Exposed on JVM).
      */
     interface LocalDataSource<T : Any, ID : Any> {

         /**
          * Retrieves an entity by its [id].
          *
          * @return [Result.success] with the entity if found, [Result.failure] if not found
          *         or if a storage error occurs.
          */
         suspend fun getById(id: ID): Result<T>

         /**
          * Inserts or replaces an entity.
          *
          * @return [Result.success] with the persisted entity, [Result.failure] on storage error.
          */
         suspend fun insert(entity: T): Result<T>

         /**
          * Deletes the entity identified by [id].
          *
          * @return [Result.success] on deletion, [Result.failure] if the entity was not found
          *         or if a storage error occurs.
          */
         suspend fun delete(id: ID): Result<Unit>

         /**
          * Returns a [Flow] that emits the entity whenever it changes in the local store.
          *
          * The flow does not complete — it emits until cancelled.
          */
         fun observeById(id: ID): Flow<T>
     }
     ```
- **Notes**: `Flow` is from `kotlinx.coroutines`. No Room or Exposed imports — this is a pure Kotlin interface.

---

### Subtask T005 – Create RemoteDataSource interface

- **Purpose**: Defines the contract for remote API access. Implementations use Ktor or Retrofit per platform. `BaseRepository` uses this for the network leg of cache-first reads.
- **Parallel?**: Yes — independent of T004, T006, T008, T009.
- **Files**: Create `shared/src/commonMain/kotlin/com/vibely/shared/data/source/RemoteDataSource.kt`
- **Steps**:
  1. Create the file:
     ```kotlin
     package com.vibely.shared.data.source

     /**
      * Contract for a remote network data source that operates on entities of type [T]
      * identified by [ID].
      *
      * Implementations are platform-specific (e.g., Ktor HTTP client).
      */
     interface RemoteDataSource<T : Any, ID : Any> {

         /**
          * Fetches an entity by its [id] from the remote source.
          *
          * @return [Result.success] with the entity, [Result.failure] on network error or
          *         if the entity does not exist remotely.
          */
         suspend fun getById(id: ID): Result<T>

         /**
          * Saves (creates or updates) an entity on the remote source.
          *
          * @return [Result.success] with the saved entity as returned by the server,
          *         [Result.failure] on network or validation error.
          */
         suspend fun save(entity: T): Result<T>
     }
     ```
- **Notes**: No Ktor imports — pure Kotlin interface.

---

### Subtask T006 – Create SyncManager interface

- **Purpose**: Abstracts online/offline state detection and outbox-pattern queuing. `BaseRepository` uses this to decide whether to go online or queue locally. Concrete implementations vary by platform (Android: `ConnectivityManager`; JVM server: always online or network probe).
- **Parallel?**: Yes — independent of T004, T005, T008, T009.
- **Files**: Create `shared/src/commonMain/kotlin/com/vibely/shared/data/source/SyncManager.kt`
- **Steps**:
  1. Create the file:
     ```kotlin
     package com.vibely.shared.data.source

     /**
      * Manages network availability detection and offline-sync queuing.
      *
      * Platform implementations are registered via dependency injection.
      * Android uses [ConnectivityManager]; JVM server implementations can assume always-online
      * or use a network probe.
      */
     interface SyncManager {

         /**
          * Returns `true` if the device currently has network connectivity.
          */
         fun isOnline(): Boolean

         /**
          * Queues [entity] for synchronisation when connectivity is restored.
          *
          * This is a fire-and-forget operation: failures to queue must be logged but
          * must NOT propagate as exceptions to the caller.
          */
         suspend fun queueForSync(entity: Any)
     }
     ```
- **Notes**: Plain interface — not `expect/actual`. Research Decision 2 explains why (platform contract is identical, only implementation differs).

---

### Subtask T007 – Create BaseRepository abstract class

- **Purpose**: Provides the cache-first read and write-through write strategies shared by all feature repositories. Concrete subclasses only need to inject the three data sources.
- **Parallel?**: No — depends on T004 (`LocalDataSource`), T005 (`RemoteDataSource`), T006 (`SyncManager`).
- **Files**: Create `shared/src/commonMain/kotlin/com/vibely/shared/data/repository/BaseRepository.kt`
- **Steps**:
  1. Create the file implementing cache-first read and write-through write:
     ```kotlin
     package com.vibely.shared.data.repository

     import com.vibely.shared.data.source.LocalDataSource
     import com.vibely.shared.data.source.RemoteDataSource
     import com.vibely.shared.data.source.SyncManager
     import kotlinx.coroutines.flow.Flow
     import kotlinx.coroutines.flow.onStart

     /**
      * Abstract base class that implements a cache-first read strategy and a
      * write-through (online) / write-local-then-queue (offline) write strategy.
      *
      * Subclasses provide [localDataSource], [remoteDataSource], and [syncManager]
      * via constructor injection.
      *
      * **Nested transaction note**: If called from within an existing Exposed
      * transaction, the inner [newSuspendedTransaction] call re-uses the outer
      * transaction — no nested transaction is created.
      *
      * @param T The domain entity type.
      * @param ID The identifier type for [T].
      */
     abstract class BaseRepository<T : Any, ID : Any> {

         /** Local persistence data source. */
         protected abstract val localDataSource: LocalDataSource<T, ID>

         /** Remote network data source. */
         protected abstract val remoteDataSource: RemoteDataSource<T, ID>

         /** Sync state and offline queuing manager. */
         protected abstract val syncManager: SyncManager

         /**
          * Retrieves an entity by [id] using a cache-first strategy:
          * 1. Returns the local result immediately if found.
          * 2. Falls back to the remote source on local miss, then caches the result.
          * 3. Returns [Result.failure] if both sources fail.
          */
         suspend fun getById(id: ID): Result<T> {
             val local = localDataSource.getById(id)
             if (local.isSuccess) return local

             return remoteDataSource.getById(id).onSuccess { entity ->
                 localDataSource.insert(entity)
             }
         }

         /**
          * Saves [entity] using a write-through strategy when online, or a
          * write-local-then-queue strategy when offline:
          *
          * **Online path**: remote save → local insert → return remote result.
          * **Offline path**: local insert → queue for sync → return local result.
          * Failure to queue is logged and swallowed — it must not fail the caller.
          */
         suspend fun save(entity: T): Result<T> {
             return if (syncManager.isOnline()) {
                 remoteDataSource.save(entity).onSuccess { saved ->
                     localDataSource.insert(saved)
                 }
             } else {
                 localDataSource.insert(entity).also { result ->
                     result.onSuccess {
                         runCatching { syncManager.queueForSync(entity) }
                     }
                 }
             }
         }

         /**
          * Returns a [Flow] emitting the entity identified by [id] from the local store.
          * Triggers a background sync check on first subscription via [onSyncRequired].
          */
         fun observeById(id: ID): Flow<T> =
             localDataSource.observeById(id).onStart { onSyncRequired(id) }

         /**
          * Called when [observeById] begins collection. Subclasses may override to
          * trigger a background sync or refresh. Default implementation is a no-op.
          */
         protected open suspend fun onSyncRequired(id: ID) {
             // no-op by default — subclasses override to trigger background sync
         }
     }
     ```
- **Notes**:
  - `runCatching { syncManager.queueForSync(entity) }` swallows queuing failures as per spec requirement: failure to queue must not surface as an error to the caller.
  - `onSyncRequired` is `protected open` so subclasses can trigger background sync without coupling `BaseRepository` to any coroutine scope.
  - `Flow.onStart` from `kotlinx.coroutines`.

---

### Subtask T008 – Create DomainMapper interface

- **Purpose**: Enforces bidirectional conversion between persistence entities (Room/Exposed result rows) and domain models. Keeps domain models free of persistence annotations.
- **Parallel?**: Yes — independent of all other subtasks.
- **Files**: Create `shared/src/commonMain/kotlin/com/vibely/shared/mapper/DomainMapper.kt`
- **Steps**:
  1. Create the file:
     ```kotlin
     package com.vibely.shared.mapper

     /**
      * Bidirectional mapper between a persistence [Entity] and a [Domain] model.
      *
      * Implementations live in the data layer; domain models never import [Entity] types.
      *
      * @param Entity A flat, persistence-optimised struct (e.g., Room entity, Exposed ResultRow mapping).
      * @param Domain A rich domain model with business logic and validation.
      */
     interface DomainMapper<Entity, Domain> {

         /**
          * Converts a persistence [entity] to its [Domain] representation.
          */
         fun toDomain(entity: Entity): Domain

         /**
          * Converts a [domain] model to its persistence [Entity] representation.
          */
         fun toEntity(domain: Domain): Entity
     }
     ```
- **Notes**: Pure Kotlin interface — no framework deps.

---

### Subtask T009 – Create DtoMapper interface

- **Purpose**: Enforces bidirectional conversion between network DTOs (`@Serializable` data classes) and domain models. Keeps domain models free of serialization annotations.
- **Parallel?**: Yes — independent of all other subtasks.
- **Files**: Create `shared/src/commonMain/kotlin/com/vibely/shared/mapper/DtoMapper.kt`
- **Steps**:
  1. Create the file:
     ```kotlin
     package com.vibely.shared.mapper

     /**
      * Bidirectional mapper between a network [Dto] and a [Domain] model.
      *
      * [Dto] types are serialisation-optimised (annotated with `@Serializable`);
      * [Domain] types are framework-free. Mappers live in the data layer.
      *
      * @param Dto A serialisation-optimised struct for the network layer.
      * @param Domain A rich domain model with business logic and validation.
      */
     interface DtoMapper<Dto, Domain> {

         /**
          * Converts a network [dto] to its [Domain] representation.
          */
         fun toDomain(dto: Dto): Domain

         /**
          * Converts a [domain] model to its network [Dto] representation.
          */
         fun toDto(domain: Domain): Dto
     }
     ```
- **Notes**: Pure Kotlin interface — no `kotlinx.serialization` import needed here; the `@Serializable` annotation lives on concrete `Dto` classes.

---

## Test Strategy

No dedicated tests required for this WP. Verification is compile-time:

```bash
# Verify shared:commonMain compiles cleanly
./gradlew :shared:compileKotlinMetadata
```

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| `save` offline path swallows queue failure — might hide bugs | `runCatching { syncManager.queueForSync(entity) }` result should be logged (subclass responsibility). If KDoc is not clear enough, add a `// TODO: log queue failure` comment. |
| `observeById` triggers sync on every subscription — potential thundering herd | `onSyncRequired` is a no-op by default; subclasses control the sync trigger rate. |
| Missing `kotlinx.coroutines.flow` import causes compile error | Ensure `kotlinx-coroutines-core` is in `shared/build.gradle.kts` commonMain deps (should already be there). |

---

## Review Guidance

- [ ] `LocalDataSource`, `RemoteDataSource`, `SyncManager` in `com.vibely.shared.data.source` package
- [ ] `BaseRepository` in `com.vibely.shared.data.repository` package
- [ ] `DomainMapper`, `DtoMapper` in `com.vibely.shared.mapper` package
- [ ] All interfaces and the abstract class have KDoc on every method
- [ ] `BaseRepository.getById` implements cache-first: local → remote on miss → local insert
- [ ] `BaseRepository.save` implements write-through online, write-local-then-queue offline
- [ ] Queue failure in `save` offline path is swallowed (not propagated to caller)
- [ ] `BaseRepository.observeById` uses `onStart` to call `onSyncRequired`
- [ ] `onSyncRequired` is `protected open` (subclasses can override)
- [ ] Zero framework imports in all files (no Ktor, no Room, no Exposed, no Koin)
- [ ] `Result<T>` used for all fallible operations
- [ ] `./gradlew :shared:compileKotlinMetadata` passes

---

## Activity Log

> **CRITICAL**: Activity log entries MUST be in chronological order (oldest first, newest last).

- 2026-03-25T01:40:02Z – system – lane=planned – Prompt created.
