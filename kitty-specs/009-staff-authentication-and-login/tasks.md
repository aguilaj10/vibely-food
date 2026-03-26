---
description: "Work package task list for Staff Authentication & Login"
---

# Work Packages: Staff Authentication & Login

**Inputs**: Design documents from `/kitty-specs/009-staff-authentication-and-login/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/auth-api.yaml ✓

**Organization**: 28 fine-grained subtasks (T001–T028) across 4 work packages.
WP01 and WP02 have no dependencies and can be implemented in parallel.

---

## Work Package WP01: core:network HTTP Client Foundation (Priority: P0)

**Goal**: Build the reusable Ktor HTTP client and the auth API contract so `ProductionAuthMode` (WP03) has a real backend to call. Entirely self-contained — no upstream WP dependency.
**Independent Test**: `core:network` module compiles for all three targets (Android, JVM, JS). DTOs are correctly `@Serializable`. Koin `networkModule` resolves `AuthApiClient`.
**Prompt**: `tasks/WP01-core-network-http-client.md`
**Estimated size**: ~360 lines

### Included Subtasks
- [x] T001 Add `ktor-client-js` library entry to `gradle/libs.versions.toml`
- [x] T002 Update `core/network/build.gradle.kts` with Ktor client deps for all source sets
- [x] T003 Implement `HttpClientFactory` with platform-specific engine selection (`expect/actual`)
- [x] T004 [P] Define `@Serializable` HTTP DTOs: `LoginRequest`, `RefreshRequest`, `TokenResponse`, `ValidateResponse`
- [x] T005 Define `AuthApiClient` interface (login, refresh, validate, logout)
- [x] T006 Implement `KtorAuthApiClient` — four HTTP calls against `/auth/*` endpoints
- [x] T007 Define Koin `networkModule` (singleton `HttpClient` + `AuthApiClient` binding)

**Requirement Refs**: FR-002, FR-022, FR-023, FR-024, FR-025

### Implementation Notes
- Use `expect/actual` in `HttpClientFactory` to select OkHttp (Android/JVM) vs JS engine.
- All HTTP calls use `Bearer` header for authenticated endpoints (validate, logout).
- DTOs live in `commonMain` under `com.vibely.core.network.auth.dto`.
- `networkModule` is a top-level Koin module exported from `core:network`.

### Parallel Opportunities
- T004 (DTOs) can proceed in parallel with T003 (HttpClientFactory).

### Dependencies
- None (starting WP — can run in parallel with WP02).

### Risks & Mitigations
- JS Ktor engine requires `ktor-client-js` — must be in `jsMain` dependencies, not `commonMain`.
- `HttpClientFactory` expect/actual: ensure both `androidMain`, `jvmMain`, and `jsMain` have `actual` implementations.

---

## Work Package WP02: feature/auth Models + Platform Storage (Priority: P0)

**Goal**: Set up the `feature/auth` module build configuration, define auth domain models, the `TokenStorage` interface, and all three platform implementations. Independent of WP01 — pure Kotlin domain + platform storage.
**Independent Test**: `feature/auth` compiles for Android, JVM, and JS. `TokenStorage` implementations instantiate without errors. `AuthToken`, `Credentials`, and `User` are usable from `commonMain`.
**Prompt**: `tasks/WP02-feature-auth-models-storage.md`
**Estimated size**: ~380 lines

### Included Subtasks
- [x] T008 Add `buildkonfig 0.17.1` version + plugin entry to `gradle/libs.versions.toml`
- [x] T009 Update `feature/auth/build.gradle.kts` — apply buildkonfig, add all source-set dependencies
- [x] T010 [P] Define domain models: `AuthToken`, `Credentials`, `User` in `feature/auth/commonMain`
- [x] T011 Define `TokenStorage` interface in `feature/auth/commonMain`
- [x] T012 Implement `EncryptedSharedPreferencesTokenStorage` (`androidMain`)
- [x] T013 [P] Implement `Pkcs12KeystoreTokenStorage` (`jvmMain`)
- [x] T014 [P] Implement `SessionStorageTokenStorage` (`jsMain`)

**Requirement Refs**: FR-006, FR-007, FR-008, NFR-001, NFR-002

### Implementation Notes
- `User` imports `UserId`, `StoreId`, `Role` from `:core:domain` (already exists).
- `EncryptedSharedPreferences`: use `MasterKey.Builder` + `AES256_GCM` scheme. Run all ops on `Dispatchers.IO`.
- PKCS12: `KeyStore.getInstance("PKCS12")`. Store path: `~/.vibely/token.ks`. All ops on `Dispatchers.IO`.
- `sessionStorage`: use `kotlinx.browser.sessionStorage` — stdlib, no extra dep needed.
- `buildkonfig` config reads `System.getenv("AUTH_MODE") ?: "production"`.

### Parallel Opportunities
- T010 (models), T013 (JVM storage), T014 (JS storage) can run in parallel once T009 is done.

### Dependencies
- None (can run in parallel with WP01).

### Risks & Mitigations
- `security-crypto 1.1.0-alpha06` is alpha — already in catalog, already used by `core:common`.
- PKCS12 keystore password: use a fixed dev-only constant for now; document as a TODO for production hardening.

---

## Work Package WP03: feature/auth AuthMode + Use Cases + DI + Tests (Priority: P0)

**Goal**: Implement the full authentication strategy layer: three `AuthMode` implementations, four use cases, Koin DI with `BuildKonfig`-driven mode selection, and unit tests covering all use-case paths.
**Independent Test**: `./gradlew :feature:auth:jvmTest` passes with zero network or storage I/O. All use-case success and failure paths covered via `FakeAuthMode`.
**Prompt**: `tasks/WP03-feature-auth-mode-usecases-di-tests.md`
**Estimated size**: ~430 lines

### Included Subtasks
- [x] T015 Define `AuthMode` sealed interface (`commonMain`)
- [x] T016 Implement `ProductionAuthMode` — delegates to `AuthApiClient` + `TokenStorage`
- [x] T017 [P] Implement `DebugAuthMode` — returns hardcoded `Role.OWNER` user; no I/O
- [x] T018 [P] Implement `FakeAuthMode` — publicly settable `Result` fields; no I/O
- [x] T019 Implement `LoginUseCase`, `LogoutUseCase`, `RefreshTokenUseCase`, `ValidateTokenUseCase`
- [x] T020 Define `authModule` Koin (reads `BuildKonfig.AUTH_MODE`) + `authPlatformModule` (TokenStorage platform bindings)
- [x] T021 Unit tests: success + failure path for all four use cases using `FakeAuthMode`

**Requirement Refs**: FR-001, FR-002, FR-003, FR-004, FR-005, FR-022, FR-023, FR-024, FR-025, NFR-002, NFR-003

### Implementation Notes
- `AuthMode` is `sealed interface` (not enum or abstract class) — implementations are plain classes.
- `ProductionAuthMode` maps `TokenResponse` DTO (from `core:network`) to `AuthToken` domain model.
- `DebugAuthMode` pre-builds `User(id = UserId("debug"), email = "debug@vibely.local", role = Role.OWNER, storeId = StoreId("debug"))`.
- `authModule` uses `when (BuildKonfig.AUTH_MODE)` to select implementation — this is the only allowed `BuildKonfig` usage.
- Tests live in `feature/auth/src/jvmTest` (per project convention).

### Parallel Opportunities
- T017 (`DebugAuthMode`) and T018 (`FakeAuthMode`) can be written in parallel once T015 (interface) is done.

### Dependencies
- Depends on **WP01** (needs `AuthApiClient` for `ProductionAuthMode`).
- Depends on **WP02** (needs `AuthToken`, `Credentials`, `User`, `TokenStorage`).

### Risks & Mitigations
- `BuildKonfig` object name is `BuildKonfig` (not `BuildConfig`) — easy to mis-type.
- `authPlatformModule` must be registered separately per platform via `actual platformModule`.

---

## Work Package WP04: composeApp Login UI & Navigation Routing (Priority: P1) 🎯 MVP

**Goal**: Deliver the complete user-facing login experience: startup routing (silent token validation + refresh), login screen, and navigation wiring. This is the MVP — the first end-to-end user story (S1–S5) becomes demonstrable after this WP.
**Independent Test**: App launches on Android; cold start without token → Login screen is shown. Cold start with valid token → FloorPlan destination is navigated to. Debug build → Login screen never shown.
**Prompt**: `tasks/WP04-compose-app-login-ui-navigation.md`
**Estimated size**: ~390 lines

### Included Subtasks
- [x] T022 Update `composeApp/build.gradle.kts` — add navigation3, feature:auth, koin-compose deps
- [x] T023 Create `AppNavKey` sealed interface with `Login` and `FloorPlan` `@Serializable data object`s
- [x] T024 Implement `LoginViewModel` — `StateFlow<LoginUiState>`, `onEmailChange`, `onPasswordChange`, `onSignIn`
- [x] T025 Implement `LoginScreen` composable — email/password fields, Sign In button, loading, error display
- [x] T026 Implement `AppNavigation` composable — startup token lifecycle (validate → refresh → route to Login or FloorPlan)
- [x] T027 Create `MainActivity` — sets content, wires `NavDisplay` with `AppNavigation`
- [x] T028 Update `VibelyApp.kt` — add `authModule()`, `authPlatformModule()`, `networkModule()` to `startKoin`

**Requirement Refs**: FR-009, FR-010, FR-011, FR-012, FR-013, FR-014, FR-015, FR-016, FR-017, FR-018, FR-019, FR-020, FR-021, FR-026, FR-027, FR-028

### Implementation Notes
- `AppNavKey` uses `NavKey` from `navigation3-runtime`; `@Serializable` from `kotlinx.serialization`.
- `AppNavigation` shows a `CircularProgressIndicator` while the token check is in flight (FR-013).
- `LoginViewModel` navigates by emitting a `SharedFlow<AppNavKey>` or using a navigation callback.
- On successful login, `LoginScreen` must be popped from the back stack (FR-018).
- `FloorPlan` NavKey navigates to a placeholder screen for now (the floor plan is Phase 2).

### Parallel Opportunities
- T024 (`LoginViewModel`) and T025 (`LoginScreen`) can proceed in parallel once T023 (nav keys) is done.

### Dependencies
- Depends on **WP03** (needs `LoginUseCase`, `ValidateTokenUseCase`, `RefreshTokenUseCase`).

### Risks & Mitigations
- Navigation3 is `1.0.1` stable but new — refer to research.md for confirmed API. Avoid Navigation Compose 2 patterns.
- `NavDisplay` is in `navigation3-ui`, not `navigation3-runtime`. Both must be declared.
- `AppNavigation` coroutine must be launched with `LaunchedEffect(Unit)` not in a `remember` lambda.

---

## Dependency & Execution Summary

```
WP01 ─────────────────────────┐
(core:network, no deps)       ├──► WP03 (authMode + usecases) ──► WP04 (UI + routing)
WP02 ─────────────────────────┘
(auth models + storage, no deps)
```

- **WP01 and WP02 are fully independent**: implement in parallel.
- **WP03** requires both WP01 and WP02.
- **WP04** requires WP03.
- **MVP scope**: All four WPs — the complete feature is small enough to be the MVP.

---

## Subtask Index

| ID | Summary | WP | Parallel? |
|---|---|---|---|
| T001 | Add ktor-client-js to version catalog | WP01 | No |
| T002 | Update core/network build.gradle.kts | WP01 | No |
| T003 | HttpClientFactory with expect/actual engines | WP01 | No |
| T004 | HTTP DTOs (4 data classes, @Serializable) | WP01 | Yes |
| T005 | AuthApiClient interface | WP01 | No |
| T006 | KtorAuthApiClient implementation | WP01 | No |
| T007 | Koin networkModule | WP01 | No |
| T008 | buildkonfig in version catalog | WP02 | No |
| T009 | feature/auth build.gradle.kts setup | WP02 | No |
| T010 | AuthToken, Credentials, User models | WP02 | Yes |
| T011 | TokenStorage interface | WP02 | No |
| T012 | EncryptedSharedPreferencesTokenStorage (Android) | WP02 | No |
| T013 | Pkcs12KeystoreTokenStorage (JVM) | WP02 | Yes |
| T014 | SessionStorageTokenStorage (JS) | WP02 | Yes |
| T015 | AuthMode sealed interface | WP03 | No |
| T016 | ProductionAuthMode | WP03 | No |
| T017 | DebugAuthMode | WP03 | Yes |
| T018 | FakeAuthMode | WP03 | Yes |
| T019 | 4 use cases | WP03 | No |
| T020 | authModule + authPlatformModule Koin | WP03 | No |
| T021 | Unit tests (jvmTest, FakeAuthMode) | WP03 | No |
| T022 | composeApp build.gradle.kts deps | WP04 | No |
| T023 | AppNavKey sealed interface | WP04 | No |
| T024 | LoginViewModel | WP04 | Yes |
| T025 | LoginScreen composable | WP04 | Yes |
| T026 | AppNavigation composable (startup routing) | WP04 | No |
| T027 | MainActivity + NavDisplay wiring | WP04 | No |
| T028 | VibelyApp.kt Koin module registration | WP04 | No |
