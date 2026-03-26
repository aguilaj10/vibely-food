# Staff Authentication & Login

**Feature:** 009-staff-authentication-and-login
**Mission:** software-dev
**Source:** IMPLEMENTATION_PLAN.md §1.2.1, §1.2.2, §1.2.3

---

## Overview

Vibely POS staff must authenticate before accessing any POS functionality. This feature implements the complete authentication subsystem: the strategy layer that supports production JWT auth, debug auto-login, and fake responses for testing; platform-specific secure token storage; and the login screen that connects these pieces to the user.

After successful authentication, all staff — regardless of role — land on the table floor plan of their store's first configured area.

---

## Actors

- **Staff member** — any employee with a Vibely account (`OWNER`, `MANAGER`, `CASHIER`, or `WAITER`). Uses the login screen on production builds.
- **Developer** — uses debug builds where authentication is bypassed automatically; never sees the login form.
- **Test suite** — uses `FakeAuthMode` with configurable responses; no real network calls.

---

## User Scenarios

### S1 — Cold start, no stored token
A staff member opens the app for the first time (or after logging out). No token is found in storage. The login screen is shown immediately.

1. Staff enters email and password.
2. Taps **Sign In**.
3. App contacts the auth backend.
4. On success: token is persisted and the app navigates to the floor plan of the first configured area.
5. On failure: an error message appears inline; the form stays on screen.

### S2 — Cold start, valid stored token
Staff opens the app and a valid, non-expired token exists in storage.

1. App validates the stored token silently on startup.
2. Login screen is never shown.
3. App navigates directly to the floor plan.

### S3 — Cold start, expired token (silent refresh succeeds)
Staff opens the app; the stored token has expired but the refresh token is still valid.

1. App validates the stored token → receives an expiry failure.
2. App attempts a silent token refresh using the stored refresh token.
3. Refresh succeeds: new token is persisted.
4. App navigates to the floor plan — login screen never shown.

### S4 — Cold start, expired token (silent refresh fails)
Both the access token and the refresh token are expired or revoked.

1. App validates the stored token → expiry failure.
2. App attempts silent refresh → refresh also fails.
3. Stored token is cleared.
4. Login screen is shown.

### S5 — Debug build (developer)
A developer runs the app with `AUTH_MODE=debug`.

1. App cold-starts.
2. `DebugAuthMode` auto-authenticates with a pre-configured test user (`Role.OWNER`).
3. Login screen is never shown; app navigates directly to the floor plan.

### S6 — Logout
A staff member logs out from within the app (logout trigger is out of scope for this feature, but the `LogoutUseCase` must be available for other features to call).

1. Caller invokes `LogoutUseCase`.
2. Token is cleared from storage and revoked on the server.
3. App navigates back to the login screen, clearing the back stack.

---

## Functional Requirements

### Authentication Strategy

**FR-001** The system must provide a single `AuthMode` interface that encapsulates `authenticate`, `refreshToken`, `validateToken`, and `logout` operations.

**FR-002** The system must provide a `ProductionAuthMode` that calls the auth API, persists the token on success, and propagates failures as `Result.failure`.

**FR-003** The system must provide a `DebugAuthMode` that returns success immediately without network calls, using a pre-configured test user with role `OWNER` (not `ADMIN`).

**FR-004** The system must provide a `FakeAuthMode` with publicly settable result properties, allowing tests to configure any combination of success and failure responses without network or storage I/O.

**FR-005** The active `AuthMode` implementation must be selected at startup via a build configuration value (`AUTH_MODE`). Business logic must never contain `if (isDebug)` or `if (isFake)` branches.

### Token Storage

**FR-006** Tokens must be stored using platform-appropriate encrypted storage:
- **Android** — `EncryptedSharedPreferences` with AES-256-GCM keys managed by `MasterKey`.
- **JVM (Desktop)** — PKCS12 keystore file in `~/.vibely/`, encrypted with AES-GCM.
- **Web** — `sessionStorage` via `kotlinx.browser.sessionStorage`; tokens clear automatically when the browser tab closes.

**FR-007** `TokenStorage` must expose exactly three operations: `saveToken`, `getToken`, and `clearToken`. No other storage concerns belong in this interface.

**FR-008** Token storage implementations must be registered in Koin as platform-specific singletons via `actual platformModule`.

### Startup Token Lifecycle

**FR-009** On every cold start the app must attempt to validate the stored token before showing any screen.

**FR-010** If validation fails due to expiry, the app must attempt a silent token refresh before routing to the login screen.

**FR-011** If the silent refresh succeeds, the new token must be persisted and the app must navigate directly to the floor plan without showing the login screen.

**FR-012** If both validation and refresh fail, any stored token must be cleared and the login screen must be shown.

**FR-013** Token validation and refresh on startup must complete before the first screen is rendered. A loading indicator is acceptable during this check; a blank screen is not.

### Login Screen

**FR-014** The login screen must present an email field, a password field, and a **Sign In** button.

**FR-015** The **Sign In** button must be disabled while either field is empty or while a login request is in progress.

**FR-016** While a login request is in progress, a loading indicator must replace the button label.

**FR-017** If authentication fails, an error message must appear on the login screen. The message must be human-readable (not a raw exception message or HTTP status code).

**FR-018** On successful login, the login screen must be removed from the back stack before navigating to the floor plan, so that pressing back does not return to the login form.

**FR-019** In debug builds, the login screen must never be shown. The app must navigate directly to the floor plan.

### Post-Login Destination

**FR-020** After successful authentication (login, silent refresh, or debug bypass), the app must navigate to the table floor plan of the store's first configured area.

**FR-021** The auth feature must navigate to the floor plan by routing to an `AppNavKey` that carries the destination identity; it must not directly instantiate or depend on the floor plan composable.

### Use Cases

**FR-022** `LoginUseCase` must delegate to `AuthMode.authenticate` and return `Result<AuthToken>`.

**FR-023** `LogoutUseCase` must delegate to `AuthMode.logout` and return `Result<Unit>`.

**FR-024** `RefreshTokenUseCase` must delegate to `AuthMode.refreshToken` and return `Result<AuthToken>`.

**FR-025** `ValidateTokenUseCase` must delegate to `AuthMode.validateToken` and return `Result<User>`.

### Navigation

**FR-026** Navigation must use Navigation3 (`1.0.1`). Screen identities must be `@Serializable` data objects implementing a sealed `AppNavKey` interface. No string routes.

**FR-027** The back stack must be a `NavBackStack<AppNavKey>` managed with `rememberNavBackStack`. `NavDisplay` is the rendering composable.

**FR-028** `navigation3-runtime` must be declared in `commonMain`. `navigation3-ui` (which provides `NavDisplay`) must be declared in `androidMain` and `jvmMain` source sets only.

---

## Non-Functional Requirements

**NFR-001** Token storage operations must not block the main thread. All `TokenStorage` implementations must execute on an IO dispatcher.

**NFR-002** The `AuthMode` interface and all use cases must be declared in `commonMain` with no platform-specific imports.

**NFR-003** `FakeAuthMode` must not require a test framework to construct or configure. It is a plain class usable from any source set.

---

## Key Entities

| Entity | Description |
|---|---|
| `AuthToken` | Access token string, refresh token string, expiry timestamp |
| `Credentials` | Email and password supplied by the user |
| `RefreshToken` | Wrapper around the refresh token string |
| `User` | Authenticated user identity: `UserId`, email, `Role`, `StoreId` |
| `AuthMode` | Sealed interface — Production / Debug / Fake implementations |
| `TokenStorage` | Platform interface — Android / JVM / Web implementations |

---

## Success Criteria

**SC-001** A staff member on a production build can sign in with valid credentials and reach the floor plan in under 3 seconds on a standard network connection.

**SC-002** A staff member whose session has expired is refreshed silently at least 95% of the time when the refresh token is still valid, without seeing the login screen.

**SC-003** A developer running a debug build never sees the login screen; the app always opens directly on the floor plan.

**SC-004** All authentication use cases are testable in isolation using `FakeAuthMode` with zero network or storage I/O — confirmed by the test suite running without Docker or emulator.

**SC-005** An invalid password attempt shows a user-readable error within 5 seconds; the form remains interactive.

**SC-006** After logout, pressing back does not navigate to any authenticated screen.

---

## Scope Boundaries

**In scope:**
- `AuthMode` sealed interface and three implementations
- `TokenStorage` interface and three platform implementations
- `LoginUseCase`, `LogoutUseCase`, `RefreshTokenUseCase`, `ValidateTokenUseCase`
- Koin DI modules (`authModule`, `authFeatureModule`, platform `TokenStorage` bindings)
- `LoginViewModel` and `LoginScreen`
- `AppNavKey` sealed interface and startup routing (`AppNavigation`)
- Navigation3 dependency declarations in version catalog and build files

**Out of scope:**
- The floor plan / table status screen itself (Phase 2)
- Password reset or account creation flows
- Multi-factor authentication
- Mid-session token refresh (automatic background refresh during active use)
- Any role-based access control beyond delivering the authenticated `User` to callers

---

## Assumptions

- The auth backend exposes `/auth/login`, `/auth/refresh`, `/auth/validate`, and `/auth/logout` endpoints. The exact contract is defined when the `core:network` module is implemented.
- `BuildConfig.AUTH_MODE` is generated at build time from the `AUTH_MODE` environment variable. The mechanism for injecting build config into KMP `commonMain` is resolved during implementation.
- "First configured area" is determined by the floor plan feature (Phase 2). This feature navigates to `AppNavKey.FloorPlan` (or equivalent); the floor plan feature resolves which area to display.
- Token expiry is detected by the server returning a 401 response; the client does not inspect token timestamps directly.

---

## Activity Log

- 2026-03-26T00:55:03Z – system – Spec created from IMPLEMENTATION_PLAN §1.2.1, §1.2.2, §1.2.3 via `/spec-kitty.specify`.
