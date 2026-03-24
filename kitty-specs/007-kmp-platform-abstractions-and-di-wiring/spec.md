# Feature Specification: KMP Platform Abstractions and DI Wiring

**Feature**: 007-kmp-platform-abstractions-and-di-wiring
**Date**: 2026-03-24
**Mission**: software-dev
**Status**: Draft

---

## Overview

Establish the platform abstraction layer and dependency injection wiring for the Kotlin Multiplatform project. This feature provides a clean boundary between shared business logic and platform-specific capabilities, enabling the rest of the codebase to depend on abstractions rather than concrete platform APIs.

Covers two plan sections in one feature because they are tightly coupled: the `expect/actual` declarations (0.1.2) are the contracts that the DI modules (0.1.4) must satisfy.

---

## Goals

- Define platform contracts (`PlatformCapabilities`, `PlatformLogger`, `SecureStorage`) as `expect` declarations in shared code
- Provide concrete `actual` implementations for Android, JVM, and Web/JS
- Wire all platform-specific dependencies through Koin so the rest of the app never imports platform APIs directly
- Keep `commonModule()` minimal — only infrastructure that exists right now; use cases and repositories are added as features are built

---

## Actors

- **Android app**: needs `EncryptedSharedPreferences`-backed secure storage, Android logger, local database context
- **JVM server**: needs env-var-backed config, in-memory secure storage (tokens managed via env), JVM logger
- **Web/JS client**: needs `localStorage`-backed storage, console logger, capability flags for browser environment

---

## Functional Requirements

### FR-001 — `PlatformCapabilities` expect/actual
The system must declare a `PlatformCapabilities` contract in `commonMain` exposing three boolean flags:
- `supportsLocalCache` — whether the platform can persist data locally
- `supportsBackgroundSync` — whether the platform supports background work
- `supportsNotifications` — whether the platform can display push/local notifications

Each platform must provide an `actual` implementation with values appropriate to its environment:
- Android: `true / true / true`
- JVM: `false / true / false`
- Web/JS: `true / false / false`

### FR-002 — `PlatformLogger` expect/actual
The system must declare a `PlatformLogger` contract in `commonMain` with at minimum:
- `fun debug(tag: String, message: String)`
- `fun info(tag: String, message: String)`
- `fun warn(tag: String, message: String)`
- `fun error(tag: String, message: String, throwable: Throwable? = null)`

Each platform must provide an `actual` implementation:
- Android: delegates to Android `Log`
- JVM: delegates to `println` / SLF4J-compatible output
- Web/JS: delegates to `console.log / console.warn / console.error`

### FR-003 — `SecureStorage` expect/actual
The system must declare a `SecureStorage` contract in `commonMain`:
- `fun save(key: String, value: String)`
- `fun get(key: String): String?`
- `fun delete(key: String)`
- `fun clear()`

Platform implementations:
- **Android**: `EncryptedSharedPreferences` backed by Android Keystore. Simple, performant, no over-engineering — the app does not handle banking-sensitive data.
- **JVM**: In-memory `ConcurrentHashMap`. Auth tokens for the server process are provided via environment variables at startup; no persistent local storage needed.
- **Web/JS**: `localStorage`. Acceptable for the app's sensitivity level; no additional encryption layer.

### FR-004 — `expect fun platformModule(): Module`
The system must declare `platformModule()` as an `expect` top-level function in `commonMain`, returning a Koin `Module`. Each platform must provide an `actual` that binds:

**Android `actual`:**
- `DataStore<Preferences>` — from `androidContext().dataStore`
- `VibelyLocalDatabase` — Room database instance
- `PendingEventDao` — from the Room database
- `UserPreferencesRepository` — using the DataStore singleton
- `SecureStorage` — `AndroidSecureStorage` backed by `EncryptedSharedPreferences`
- `PlatformLogger` — `AndroidLogger`

**JVM `actual`:**
- `DatabaseConfig` — reads `DATABASE_URL`, `DB_USER`, `DB_PASSWORD` from environment variables, with safe defaults for local development
- `DatabaseFactory` — HikariCP-backed factory using `DatabaseConfig`
- `SecureStorage` — `JvmSecureStorage` (in-memory `ConcurrentHashMap`)
- `PlatformLogger` — `JvmLogger`

**Web/JS `actual`:**
- `SecureStorage` — `WebSecureStorage` backed by `localStorage`
- `PlatformLogger` — `WebLogger` delegating to `console`
- `PlatformCapabilities` — Web/JS capability values

### FR-005 — `commonModule()`
The system must provide a single `commonModule()` function in `commonMain` that wires platform-agnostic infrastructure available at this stage:
- `UserPreferencesRepository` — bound as singleton, receives `DataStore` from `platformModule`
- `SecureStorage` — re-exported from `platformModule` binding
- `PlatformLogger` — re-exported from `platformModule` binding

`commonModule()` must NOT wire any use cases or repository implementations at this stage. Those bindings are added in the feature that implements each use case.

### FR-006 — App initialisation entry points
The system must provide Koin startup wiring for each platform entry point:
- **Android**: `VibelyApp : Application` calls `startKoin { androidContext(this); modules(commonModule(), platformModule()) }`
- **JVM**: `main()` function calls `startKoin { modules(commonModule(), platformModule()) }` before starting the Ktor server
- **Web/JS**: Koin is started at the JS entry point before any UI is rendered

### FR-007 — No direct platform imports in `commonMain`
All `commonMain` code must depend only on the `expect` declarations. No `import android.*`, `import java.*`, or JS-specific imports are permitted in `commonMain` source sets.

---

## User Scenarios & Testing

### Scenario 1 — Android app starts up
Given the Android app launches, when `VibelyApp.onCreate()` runs, then Koin is initialised with both modules and any component can inject `SecureStorage`, `PlatformLogger`, or `UserPreferencesRepository` without referencing Android APIs directly.

### Scenario 2 — JVM server starts up
Given the JVM server starts, when `main()` runs, then Koin is initialised, `DatabaseConfig` reads from environment variables, and `DatabaseFactory` is available for injection by repository implementations.

### Scenario 3 — Web/JS client starts
Given the web client loads, when the Koin entry point runs, then `SecureStorage` (backed by `localStorage`) and `PlatformLogger` (backed by `console`) are available for injection.

### Scenario 4 — Token saved and retrieved on Android
Given the app is running on Android, when `SecureStorage.save("auth_token", value)` is called and then `SecureStorage.get("auth_token")` is called, then the original value is returned and persists across process restarts.

### Scenario 5 — Token saved and retrieved on JVM
Given the server is running, when `SecureStorage.save("session_key", value)` is called and then `SecureStorage.get("session_key")` is called, then the original value is returned. Data does NOT persist across restarts (in-memory).

### Scenario 6 — PlatformCapabilities reflects correct flags per platform
Given a component reads `PlatformCapabilities`, when running on Android the flags are `true/true/true`, on JVM `false/true/false`, on Web/JS `true/false/false`.

### Scenario 7 — Logger forwards to platform output
Given a component calls `PlatformLogger.info("TAG", "message")`, then on Android the message appears in Logcat, on JVM in stdout, on Web/JS in the browser console.

---

## Success Criteria

- SC-001: All three platforms compile without errors after the `expect/actual` declarations are introduced
- SC-002: Koin graph resolves successfully at startup on Android, JVM, and Web/JS — no missing binding exceptions at runtime
- SC-003: `commonMain` source set contains zero platform-specific imports
- SC-004: `SecureStorage` round-trip (save → get → delete) passes on all three platforms
- SC-005: `PlatformLogger` routes output to the correct platform sink on all three platforms
- SC-006: `PlatformCapabilities` returns the documented values for each platform
- SC-007: `commonModule()` contains no use case or repository bindings

---

## Key Entities

| Entity | Location | Description |
|--------|----------|-------------|
| `PlatformCapabilities` | `core:common` `commonMain` | expect class — platform feature flags |
| `PlatformLogger` | `core:common` `commonMain` | expect class — structured log output |
| `SecureStorage` | `core:common` `commonMain` | expect class — key-value secure storage |
| `AndroidSecureStorage` | `core:common` `androidMain` | actual — EncryptedSharedPreferences |
| `JvmSecureStorage` | `core:common` `jvmMain` | actual — in-memory ConcurrentHashMap |
| `WebSecureStorage` | `core:common` `jsMain` | actual — localStorage |
| `platformModule()` | `shared` (all source sets) | expect fun — Koin module per platform |
| `commonModule()` | `shared` `commonMain` | shared Koin bindings |
| `VibelyApp` | `app-android` | Android Application subclass — Koin init |

---

## Assumptions

- `UserPreferencesRepository` and `VibelyLocalDatabase` are already declared in the plan (DataStore/Room); their full implementation is part of this feature.
- `DatabaseFactory` and `DatabaseConfig` are stubbed in JVM `platformModule` now so the DI graph is connected; their full implementation follows in the database layer feature.
- The Web/JS target is a Kotlin/JS browser target; `window.localStorage` is available.
- `androidx.security:security-crypto` must be added to `libs.versions.toml` for `EncryptedSharedPreferences` on Android.

---

## Out of Scope

- Use case bindings in `commonModule()` — deferred until each use case is implemented
- Repository implementation bindings — deferred until `core:database` is implemented (Phase 1.1)
- Biometric or hardware-backed key storage — not needed for this app's sensitivity level
- Web/JS `localStorage` encryption — not warranted; app does not store banking-sensitive data
