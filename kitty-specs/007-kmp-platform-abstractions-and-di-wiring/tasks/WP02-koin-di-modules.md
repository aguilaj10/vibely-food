---
work_package_id: WP02
title: Koin DI Modules in shared
lane: "done"
dependencies: [WP01]
base_branch: 007-kmp-platform-abstractions-and-di-wiring-WP01
base_commit: 60d6c8b76287b3c8e3ffb6521ec8955b5d4fa79a
created_at: '2026-03-24T23:11:38.818269+00:00'
subtasks:
- T007
- T008
- T009
- T010
phase: Phase 1 - Platform Abstractions
assignee: ''
agent: "claude"
shell_pid: "26433"
review_status: "approved"
reviewed_by: "Jonathan Sánchez Muñoz"
history:
- timestamp: '2026-03-24T22:28:04Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-004
- FR-005
---

# Work Package Prompt: WP02 – Koin DI Modules in `shared`

## ⚠️ IMPORTANT: Review Feedback Status

**Read this first if you are implementing this task!**

- **Has review feedback?**: Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section immediately (right below this notice).
- **You must address all feedback** before your work is complete.
- **Mark as acknowledged**: When you understand the feedback, update `review_status: acknowledged` in frontmatter.

---

## Review Feedback

*[This section is empty initially. Reviewers will populate it if the work is returned from review.]*

---

## Objectives & Success Criteria

- `shared/build.gradle.kts` includes Koin, DataStore, Room, and `core:common` in the appropriate source sets.
- `DatabaseStubs.kt` exists in `shared:jvmMain` with mandatory TODO header comment.
- `expect fun platformModule(): Module` is declared in `shared:commonMain`; three `actual` implementations exist for Android, JVM, and JS.
- `commonModule()` exists in `shared:commonMain` with only `UserPreferencesRepository`, `SecureStorage`, and `PlatformLogger` bindings — no use cases or repository implementations.
- `./gradlew :shared:build` passes on all targets.
- Zero Detekt violations.

**Implement with**: `spec-kitty implement WP02 --base WP01`

---

## Context & Constraints

- **Plan**: `kitty-specs/007-kmp-platform-abstractions-and-di-wiring/plan.md`
- **Spec**: `kitty-specs/007-kmp-platform-abstractions-and-di-wiring/spec.md`
- **Constitution**: `.kittify/memory/constitution.md` — `commonModule()` must NOT wire use cases or repository implementations; those are added when each feature is built. KDoc mandatory on all public symbols.
- **Dependency**: WP01 must be merged (on the same branch) before this WP. The `expect` classes from `core:common` must be compiled and on the classpath.
- **Gradle**: Use `libs.xxx` type-safe accessors only (per memory: `feedback_gradle_typesafe_config.md`).
- **Sequence within WP02**: T007 → T008 → T009 → T010.
- **Supporting classes**: `UserPreferencesRepository`, `VibelyLocalDatabase`, and `PendingEventDao` may need to be created as part of this WP if they don't already exist in the codebase.

---

## Subtasks & Detailed Guidance

### Subtask T007 – Update `shared/build.gradle.kts`

**Purpose**: Add all dependencies required by the Koin DI modules to the correct source sets.

**Steps**:

1. Open `shared/build.gradle.kts`.

2. Add to `commonMain.dependencies`:
   ```kotlin
   implementation(libs.koin.core)
   implementation(projects.core.common)
   ```

3. Add to `androidMain.dependencies`:
   ```kotlin
   implementation(libs.datastore.preferences)
   implementation(libs.room.runtime)
   ```

4. No changes needed for `jvmMain` or `jsMain` dependencies at this stage — `koin-core` from `commonMain` is sufficient for the stub modules.

5. Verify `libs.koin.core` exists in `libs.versions.toml`. If the alias is different (e.g., `koin-core`), use the correct accessor (`libs.koin.core` if alias is `koin-core` — Gradle replaces `-` with `.`).

**Files**: `shared/build.gradle.kts`

**Validation**:
- [ ] `libs.koin.core` in `commonMain.dependencies`.
- [ ] `projects.core.common` in `commonMain.dependencies`.
- [ ] `libs.datastore.preferences` in `androidMain.dependencies`.
- [ ] `libs.room.runtime` in `androidMain.dependencies`.
- [ ] No string-based dependency notation.

---

### Subtask T008 – Create `DatabaseStubs.kt` in `shared:jvmMain`

**Purpose**: Provide temporary `DatabaseConfig` and `DatabaseFactory` stubs so the JVM `platformModule` actual compiles before `core:database` is implemented in Phase 1.1.

**Steps**:

1. Create `shared/src/jvmMain/kotlin/com/vibely/shared/di/DatabaseStubs.kt`:
   ```kotlin
   package com.vibely.shared.di

   // TODO: Remove this file when Phase 1.1 (core:database) is implemented.
   // These stubs exist solely to allow the JVM platformModule to compile before
   // the real database layer is introduced in core:database.

   /**
    * Configuration for the JVM database connection.
    * Values are read from environment variables at startup.
    *
    * This is a temporary stub — replace with the real implementation in Phase 1.1.
    */
   data class DatabaseConfig(
       /** JDBC URL, e.g. `jdbc:postgresql://localhost:5432/vibely`. */
       val url: String,
       /** Database username. */
       val username: String,
       /** Database password. */
       val password: String,
   )

   /**
    * Factory for creating database connections using [config].
    *
    * This is a temporary stub — replace with the real HikariCP-backed
    * implementation in Phase 1.1 (core:database).
    */
   class DatabaseFactory(val config: DatabaseConfig) {
       // Intentionally empty — full implementation in Phase 1.1.
   }
   ```

**Files**: `shared/src/jvmMain/kotlin/com/vibely/shared/di/DatabaseStubs.kt`

**Validation**:
- [ ] File exists at the correct path.
- [ ] TODO comment is present as the first line after the package declaration.
- [ ] Both classes have KDoc.
- [ ] `DatabaseFactory` body is empty (no implementation).

---

### Subtask T009 – Implement `PlatformModule.kt` (expect fun + 3 actuals)

**Purpose**: Declare `expect fun platformModule(): Module` in `commonMain` and provide three `actual` implementations that bind all platform-specific dependencies into Koin.

**Files**:
- `shared/src/commonMain/kotlin/com/vibely/shared/di/PlatformModule.kt`
- `shared/src/androidMain/kotlin/com/vibely/shared/di/PlatformModule.kt`
- `shared/src/jvmMain/kotlin/com/vibely/shared/di/PlatformModule.kt`
- `shared/src/jsMain/kotlin/com/vibely/shared/di/PlatformModule.kt`

**Supporting classes to create if absent**:
- `UserPreferencesRepository` — DataStore-backed, in `shared:commonMain` or `shared:androidMain`
- `VibelyLocalDatabase` — Room database class, in `shared:androidMain`
- `PendingEventDao` — Room DAO interface, in `shared:androidMain`

**Steps**:

1. **commonMain** — `expect fun`:
   ```kotlin
   package com.vibely.shared.di

   import org.koin.core.module.Module

   /** Returns the Koin [Module] for platform-specific dependency bindings. */
   expect fun platformModule(): Module
   ```

2. **androidMain** — `actual fun`:

   First, ensure `VibelyLocalDatabase` and `PendingEventDao` exist. Create minimal stubs if absent:

   ```kotlin
   // shared/src/androidMain/kotlin/com/vibely/shared/db/PendingEventDao.kt
   package com.vibely.shared.db

   import androidx.room.Dao

   /** DAO for pending event queue entries. Full implementation in Phase 1.1. */
   @Dao
   interface PendingEventDao
   ```

   ```kotlin
   // shared/src/androidMain/kotlin/com/vibely/shared/db/VibelyLocalDatabase.kt
   package com.vibely.shared.db

   import androidx.room.Database
   import androidx.room.RoomDatabase

   /** Room database for local storage. Full implementation in Phase 1.1. */
   @Database(entities = [], version = 1)
   abstract class VibelyLocalDatabase : RoomDatabase() {
       abstract fun pendingEventDao(): PendingEventDao
   }
   ```

   Create `UserPreferencesRepository` if absent:
   ```kotlin
   // shared/src/commonMain/kotlin/com/vibely/shared/repository/UserPreferencesRepository.kt
   package com.vibely.shared.repository

   import androidx.datastore.core.DataStore
   import androidx.datastore.preferences.core.Preferences

   /**
    * Repository for user preferences backed by [DataStore].
    * Provides typed access to persisted user configuration.
    *
    * Full implementation is added as user preference features are built.
    */
   class UserPreferencesRepository(
       private val dataStore: DataStore<Preferences>,
   )
   ```

   Then the Android `platformModule` actual:
   ```kotlin
   package com.vibely.shared.di

   import androidx.room.Room
   import com.vibely.common.platform.PlatformLogger
   import com.vibely.common.platform.SecureStorage
   import com.vibely.shared.db.VibelyLocalDatabase
   import com.vibely.shared.repository.UserPreferencesRepository
   import org.koin.android.ext.koin.androidContext
   import org.koin.dsl.module

   actual fun platformModule() = module {
       single { androidContext().dataStore }
       single {
           Room.databaseBuilder(
               androidContext(),
               VibelyLocalDatabase::class.java,
               "vibely_local.db",
           ).build()
       }
       single { get<VibelyLocalDatabase>().pendingEventDao() }
       single { UserPreferencesRepository(get()) }
       single { SecureStorage() }
       single { PlatformLogger() }
   }
   ```

   Note: `androidContext().dataStore` requires a DataStore extension property to be defined. Create it:
   ```kotlin
   // shared/src/androidMain/kotlin/com/vibely/shared/di/DataStoreExt.kt
   package com.vibely.shared.di

   import android.content.Context
   import androidx.datastore.core.DataStore
   import androidx.datastore.preferences.core.Preferences
   import androidx.datastore.preferences.preferencesDataStore

   private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vibely_prefs")

   /** Returns the application [DataStore] instance. */
   val Context.vibelyDataStore: DataStore<Preferences> get() = dataStore
   ```
   Update the `platformModule` to use `androidContext().vibelyDataStore`.

3. **jvmMain** — `actual fun`:
   ```kotlin
   package com.vibely.shared.di

   import com.vibely.common.platform.PlatformLogger
   import com.vibely.common.platform.SecureStorage
   import org.koin.dsl.module

   actual fun platformModule() = module {
       single {
           DatabaseConfig(
               url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/vibely",
               username = System.getenv("DB_USER") ?: "postgres",
               password = System.getenv("DB_PASSWORD") ?: "postgres",
           )
       }
       single { DatabaseFactory(get()) }
       single { SecureStorage() }
       single { PlatformLogger() }
   }
   ```

4. **jsMain** — `actual fun`:
   ```kotlin
   package com.vibely.shared.di

   import com.vibely.common.platform.PlatformCapabilities
   import com.vibely.common.platform.PlatformLogger
   import com.vibely.common.platform.SecureStorage
   import org.koin.dsl.module

   actual fun platformModule() = module {
       single { SecureStorage() }
       single { PlatformLogger() }
       single { PlatformCapabilities() }
   }
   ```

**Validation**:
- [ ] `expect fun platformModule()` has KDoc.
- [ ] All three actuals compile against their respective source sets.
- [ ] JVM actual reads only from `System.getenv()` — no hardcoded real credentials.
- [ ] Android actual does NOT import anything from `jvmMain` or `jsMain`.

---

### Subtask T010 – Implement `CommonModule.kt` in `shared:commonMain`

**Purpose**: Provide a single `commonModule()` function for platform-agnostic Koin bindings. Kept intentionally minimal — use cases and repository implementations are added when each feature is built.

**Files**: `shared/src/commonMain/kotlin/com/vibely/shared/di/CommonModule.kt`

**Steps**:

1. Create `CommonModule.kt`:
   ```kotlin
   package com.vibely.shared.di

   import com.vibely.common.platform.PlatformLogger
   import com.vibely.common.platform.SecureStorage
   import com.vibely.shared.repository.UserPreferencesRepository
   import org.koin.core.module.Module
   import org.koin.dsl.module

   /**
    * Koin module containing platform-agnostic infrastructure bindings.
    *
    * Repository and use-case bindings are added here incrementally as each
    * feature is implemented. Do NOT add use cases or repository implementations
    * in advance — only wire infrastructure that exists right now.
    */
   fun commonModule(): Module = module {
       single { UserPreferencesRepository(get()) }
       single<SecureStorage> { get() }
       single<PlatformLogger> { get() }
   }
   ```

   The `get()` calls for `SecureStorage` and `PlatformLogger` resolve the instances already registered by `platformModule()` — this re-exports them under the common interface.

**Files**: `shared/src/commonMain/kotlin/com/vibely/shared/di/CommonModule.kt`

**Validation**:
- [ ] `commonModule()` has KDoc.
- [ ] Only three bindings: `UserPreferencesRepository`, `SecureStorage`, `PlatformLogger`.
- [ ] No use case or repository implementation bindings.
- [ ] No `import android.*`, `import java.*`, or JS-specific imports.

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| `VibelyLocalDatabase` or `PendingEventDao` missing | Create minimal Room stubs as part of T009 |
| `UserPreferencesRepository` missing | Create stub class with DataStore constructor param in T009 |
| `dataStore` extension property conflicts | Use `preferencesDataStore` delegate with unique name `vibely_prefs` |
| JVM dev fallback credentials committed | Use only generic `localhost` placeholders — never real credentials |
| Koin alias mismatch in catalog | Verify `libs.koin.core` accessor matches the actual alias in `libs.versions.toml` |

---

## Review Guidance

- Confirm `DatabaseStubs.kt` has the mandatory `// TODO: Remove this file when Phase 1.1 (core:database) is implemented.` comment.
- Confirm `commonModule()` has exactly 3 bindings and no use cases.
- Confirm JVM `platformModule` actual reads only from `System.getenv()`.
- Confirm all public symbols have KDoc.
- Run `./gradlew :shared:build` and verify all targets compile.

---

## Activity Log

- 2026-03-24T22:28:04Z – system – lane=planned – Prompt created.
- 2026-03-24T23:11:46Z – claude – shell_pid=26433 – lane=doing – Assigned agent via workflow command
- 2026-03-24T23:25:58Z – claude – shell_pid=26433 – lane=for_review – All 4 subtasks complete. 13 files changed: koin-android added to catalog, compileSdk bumped to 36, shared module fully wired with platformModule (3 actuals) + commonModule (3 bindings). :shared:build passes all targets. Detekt clean.
- 2026-03-24T23:26:05Z – claude – shell_pid=26433 – lane=for_review – All 4 subtasks complete. 13 files: koin-android added to catalog, compileSdk 35→36, shared fully wired with platformModule (3 actuals: Android/JVM/JS) + commonModule (3 bindings). :shared:build passes all targets. Detekt clean.
- 2026-03-24T23:26:11Z – claude – shell_pid=26433 – lane=for_review – All 4 subtasks complete
- 2026-03-24T23:30:29Z – claude – shell_pid=26433 – lane=done – Review passed: All success criteria met. Build passes all 3 targets; detekt clean. T007 ✓ (libs.koin.android correctly added beyond spec for androidContext()); T008 ✓ (TODO + @file:Suppress + KDoc); T009 ✓ (3 actuals, JVM reads only System.getenv, no cross-target imports); T010 ✓ (exactly 3 bindings, no use-cases). Two correct spec deviations: (1) UserPreferencesRepository made no-arg stub in commonMain — DataStore is Android-only, spec guidance would have broken commonMain compilation; (2) SecureStorage/PlatformLogger instantiated directly in commonModule rather than re-exported from platformModule — avoids Koin self-reference cycle. WP03 exists as dependent; should rebase after merge.
