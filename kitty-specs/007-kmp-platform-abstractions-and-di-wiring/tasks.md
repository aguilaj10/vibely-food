# Work Packages: KMP Platform Abstractions and DI Wiring

**Inputs**: `kitty-specs/007-kmp-platform-abstractions-and-di-wiring/`
**Prerequisites**: plan.md ✅, spec.md ✅

**Tests**: jvmTest — `SecureStorageTest` and `PlatformCapabilitiesTest` in WP01 (T006).

**Organization**: Fine-grained subtasks (`Txxx`) roll up into work packages (`WPxx`). All three WPs are sequential: WP01 → WP02 → WP03.

---

## Work Package WP01: Version Catalog + expect/actual in `core:common` (Priority: P0) 🎯 MVP

**Goal**: Remove SQLDelight from the version catalog, add DataStore KMP / Room KMP / security-crypto / KSP, implement all 3 `expect` declarations and their 9 `actual` implementations in `core:common`, and add two jvmTest classes for validation.
**Independent Test**: `./gradlew :core:common:jvmTest` passes; all three `expect` classes compile across Android, JVM, and JS targets with zero Detekt violations.
**Prompt**: `/tasks/WP01-version-catalog-and-expect-actual.md`
**Requirements Refs**: FR-001, FR-002, FR-003, FR-007

### Included Subtasks
- [ ] T001 Update `gradle/libs.versions.toml` — remove all SQLDelight entries, add DataStore/Room/security-crypto/KSP versions, libraries, and plugins
- [ ] T002 Update `core/common/build.gradle.kts` — add `security-crypto` to `androidMain` dependencies
- [ ] T003 [P] Implement `PlatformCapabilities` — `expect class` in `commonMain` + 3 `actual` implementations (Android/JVM/JS)
- [ ] T004 [P] Implement `PlatformLogger` — `expect class` in `commonMain` + 3 `actual` implementations (Android/JVM/JS)
- [ ] T005 [P] Implement `SecureStorage` — `expect class` in `commonMain` + 3 `actual` implementations (Android/JVM/JS)
- [ ] T006 Add `SecureStorageTest` and `PlatformCapabilitiesTest` in `core:common:jvmTest`

### Implementation Notes
- T001 must be committed before T002–T005 (adds the library/plugin aliases referenced in Gradle scripts).
- T003, T004, T005 are fully independent source files — safe to implement concurrently once T001 lands.
- All `expect` declarations and every `actual` class/function must have KDoc (Detekt-enforced).
- Zero platform imports (`android.*`, `java.*`, JS-specific) in `commonMain` source set.
- `EncryptedSharedPreferences` constructor requires an `Activity` or `Application` `Context` — the `SecureStorage` Android actual must accept `Context` via constructor injection or obtain it from the Koin Android context.

### Parallel Opportunities
- T003, T004, T005 touch separate files in separate source sets — safe to parallelize across engineers or agents.

### Dependencies
- None — this is the first work package.

### Risks & Mitigations
- `security-crypto 1.1.0-alpha06` requires `minSdk 23`; project `minSdk` is 26 — compatible.
- KSP plugin version (`2.3.20-2.0.1`) must align with Kotlin `2.3.20` — confirm in T001.
- Detekt `UndocumentedPublicClass/Function/Property` will hard-fail CI if KDoc is absent — every public symbol needs KDoc.

---

## Work Package WP02: Koin DI Modules in `shared` (Priority: P0)

**Goal**: Implement `expect fun platformModule()` with three platform actuals, the minimal `commonModule()`, the temporary `DatabaseStubs.kt` for JVM, and all required supporting classes (`UserPreferencesRepository`, `VibelyLocalDatabase`, `PendingEventDao`) so the Koin graph fully resolves on every target.
**Independent Test**: `./gradlew :shared:build` (all targets) passes; all Koin module definitions compile without unresolved symbols.
**Prompt**: `/tasks/WP02-koin-di-modules.md`
**Requirements Refs**: FR-004, FR-005

### Included Subtasks
- [ ] T007 Update `shared/build.gradle.kts` — add `koin-core`, DataStore, Room, and `core:common` dependencies to the correct source sets
- [ ] T008 Create `DatabaseStubs.kt` in `shared:jvmMain` — `DatabaseConfig` data class and empty `DatabaseFactory` class with mandatory TODO header
- [ ] T009 Implement `PlatformModule.kt` — `expect fun platformModule()` in `commonMain` + Android / JVM / JS `actual` implementations
- [ ] T010 Implement `CommonModule.kt` — minimal `commonModule()` in `shared:commonMain`

### Implementation Notes
- T007 must land before any other task in this WP (Koin and Room not on classpath yet).
- T008 must exist before T009's JVM actual (it references `DatabaseConfig`/`DatabaseFactory`).
- Android actual of `platformModule()` must bind: `DataStore<Preferences>`, `VibelyLocalDatabase`, `PendingEventDao`, `UserPreferencesRepository`, `SecureStorage`, `PlatformLogger`.
- JVM actual reads: `DATABASE_URL`, `DB_USER`, `DB_PASSWORD` via `System.getenv()`; local dev fallbacks to `jdbc:postgresql://localhost:5432/vibely`, `postgres`, `postgres`.
- JS actual binds: `SecureStorage`, `PlatformLogger`, `PlatformCapabilities`.
- `commonModule()` binds ONLY: `UserPreferencesRepository`, `SecureStorage` (re-export), `PlatformLogger` (re-export). No use cases or repository implementations.
- `UserPreferencesRepository`, `VibelyLocalDatabase`, `PendingEventDao` must be created as part of this WP if they don't exist.

### Parallel Opportunities
- Android, JVM, and JS `actual` blocks within T009 touch separate source sets — can be authored in parallel.

### Dependencies
- Depends on WP01.

### Risks & Mitigations
- `UserPreferencesRepository` requires `DataStore<Preferences>` — must be created with DataStore-backed implementation; its constructor takes a `DataStore` param.
- `VibelyLocalDatabase` (Room) entity/DAO setup is minimal in this feature — enough to compile; full implementation in Phase 1.1.
- JVM dev fallback credentials must be generic (`localhost`) — never commit real credentials.

---

## Work Package WP03: Entry Points and `app-web` Scaffold (Priority: P1)

**Goal**: Wire Koin initialisation at all three platform entry points and scaffold the new `app-web` Kotlin/JS browser module so the full DI graph starts cleanly on Android, JVM, and Web/JS.
**Independent Test**: `./gradlew :composeApp:assembleDebug :server:build :app-web:build` all pass; Koin starts without missing-binding exceptions on each target.
**Prompt**: `/tasks/WP03-entry-points-and-app-web.md`
**Requirements Refs**: FR-006

### Included Subtasks
- [ ] T011 Create `VibelyApp.kt` in `composeApp:androidMain` — `Application` subclass calling `startKoin`; register `android:name` in `AndroidManifest.xml`
- [ ] T012 Update `server/Main.kt` — call `startKoin` before Ktor engine; add `koin-core` and Ktor Netty to `server/build.gradle.kts`
- [ ] T013 Scaffold `app-web` module — `build.gradle.kts`, JS entry point `Main.kt`, register in `settings.gradle.kts`

### Implementation Notes
- T011, T012, T013 touch three independent modules — safe to implement concurrently.
- T011: Check for any existing `Application` subclass in `composeApp:androidMain` before creating a new one to avoid duplication.
- T012: Ktor server body is intentionally a stub (`// Ktor configuration follows in Phase 1.2`).
- T013: `app-web` uses `js(IR) { browser { binaries.executable() } }`; include `koin-core` and `projects.shared` in `jsMain.dependencies`.

### Parallel Opportunities
- All three subtasks are fully independent — parallel implementation safe.

### Dependencies
- Depends on WP02.

### Risks & Mitigations
- If an existing `Application` class exists in `composeApp`, adapt it rather than create a duplicate.
- `settings.gradle.kts` `include(":app-web")` must follow the same naming convention as other app modules.

---

## Dependency & Execution Summary

- **Sequence**: WP01 → WP02 → WP03
- **Parallelization**: Within WP01, T003/T004/T005 are parallel (after T001). Within WP03, T011/T012/T013 are parallel.
- **MVP Scope**: WP01 + WP02 establish the platform abstractions and fully wired DI graph. WP03 connects entry points — required for runtime verification on each platform.

---

## Subtask Index (Reference)

| Subtask ID | Summary | Work Package | Priority | Parallel? |
|------------|---------|--------------|----------|-----------|
| T001 | Update `libs.versions.toml` — remove SQLDelight, add DataStore/Room/security-crypto/KSP | WP01 | P0 | No |
| T002 | Update `core/common/build.gradle.kts` | WP01 | P0 | No |
| T003 | `PlatformCapabilities` expect + 3 actuals | WP01 | P0 | Yes (after T001) |
| T004 | `PlatformLogger` expect + 3 actuals | WP01 | P0 | Yes (after T001) |
| T005 | `SecureStorage` expect + 3 actuals | WP01 | P0 | Yes (after T001) |
| T006 | `SecureStorageTest` + `PlatformCapabilitiesTest` in jvmTest | WP01 | P0 | No |
| T007 | Update `shared/build.gradle.kts` | WP02 | P0 | No |
| T008 | `DatabaseStubs.kt` in `shared:jvmMain` | WP02 | P0 | No |
| T009 | `platformModule()` expect + 3 actuals | WP02 | P0 | Partial |
| T010 | `commonModule()` in `shared:commonMain` | WP02 | P0 | No |
| T011 | `VibelyApp.kt` + AndroidManifest update | WP03 | P1 | Yes |
| T012 | `server/Main.kt` Koin wiring | WP03 | P1 | Yes |
| T013 | `app-web` module scaffold | WP03 | P1 | Yes |
