# Research: Staff Authentication & Login

**Feature**: 009-staff-authentication-and-login
**Date**: 2026-03-26
**Status**: Complete — all unknowns resolved

---

## R-001 — BuildKonfig Plugin for AUTH_MODE Injection

**Decision**: Use `com.codingfeline.buildkonfig` version **0.17.1** applied to `feature/auth`.

**Rationale**: The spec requires `AuthMode` selection via a compile-time `AUTH_MODE` value in `commonMain` without platform-specific branching. BuildKonfig generates a `BuildKonfig` object in `commonMain` from Gradle build-time values, satisfying FR-005 cleanly.

**Integration**:

```toml
# gradle/libs.versions.toml
buildkonfig = "0.17.1"
...
buildkonfig = { id = "com.codingfeline.buildkonfig", version.ref = "buildkonfig" }
```

```kotlin
// feature/auth/build.gradle.kts — added to the plugin block
alias(libs.plugins.buildkonfig)

// new top-level block
buildkonfig {
    packageName = "com.vibely.feature.auth"
    val authMode = System.getenv("AUTH_MODE") ?: "production"
    defaultConfigs {
        buildConfigField(com.codingfeline.buildkonfig.compiler.generator.BuildKonfigGenerator.FieldSpec.Type.STRING, "AUTH_MODE", authMode)
    }
}
```

The Koin `authModule` then reads `BuildKonfig.AUTH_MODE` to bind the correct `AuthMode` implementation. Business logic never sees this value.

**Alternatives considered**:
- Custom `expect/actual` for the constant — more boilerplate, equivalent result
- Koin runtime flag — defers selection to startup, adds runtime complexity; rejected

---

## R-002 — Navigation3 API (confirmed)

**Decision**: Use `androidx.navigation3:navigation3-runtime:1.0.1` (commonMain) + `navigation3-ui:1.0.1` (Android/JVM only).

Already in `gradle/libs.versions.toml`. Rationale and API confirmed in prior session research.

Key points:
- `NavBackStack<T : NavKey>` replaces `NavController`
- `rememberNavBackStack()` replaces `rememberNavController()`
- `NavDisplay` replaces `NavHost` — available in `navigation3-ui` (Android/JVM only)
- Screen identities are `@Serializable data object` types implementing a sealed `AppNavKey`
- No string routes

Since `composeApp` is currently Android-only (`android-app` convention plugin), `navigation3-ui` goes in regular `dependencies {}` block (not `androidMain` source set). `AppNavKey` and `navigation3-runtime` will be added when the module is promoted to KMP.

---

## R-003 — composeApp Source Set Reality

**Decision**: UI composables (LoginScreen, AppNavigation, AppNavKey, LoginViewModel) live in `composeApp/src/main/kotlin/` (Android main source set).

**Rationale**: `composeApp` uses `android-app` convention plugin — it is Android-only. The comment in `android-app.gradle.kts` notes: "The Desktop target will be wired to the 'shared' KMP module in a future feature iteration." There is no `commonMain` source set today.

When `composeApp` is eventually promoted to KMP, the nav keys and composables move to `commonMain` with no functional change.

---

## R-004 — Web TokenStorage Implementation

**Decision**: Use `kotlinx.browser.sessionStorage` (from `kotlin-stdlib-js`) for the JS `TokenStorage` implementation.

**Rationale**: Restaurant POS on a shared kiosk — tokens clearing on tab close is the correct UX. IndexedDB + SubtleCrypto is disproportionate for non-sensitive business data. `sessionStorage` maps directly onto the three-operation `TokenStorage` interface with 10–15 lines of Kotlin/JS code.

```kotlin
// jsMain
import kotlinx.browser.sessionStorage

class SessionStorageTokenStorage : TokenStorage {
    override suspend fun saveToken(token: AuthToken) {
        sessionStorage.setItem(KEY_ACCESS, token.accessToken)
        sessionStorage.setItem(KEY_REFRESH, token.refreshToken)
        sessionStorage.setItem(KEY_EXPIRY, token.expiresAt.toString())
    }
    override suspend fun getToken(): AuthToken? { ... }
    override suspend fun clearToken() {
        sessionStorage.removeItem(KEY_ACCESS)
        sessionStorage.removeItem(KEY_REFRESH)
        sessionStorage.removeItem(KEY_EXPIRY)
    }
}
```

`kotlinx.browser` is part of the Kotlin stdlib for JS — no additional dependency.

---

## R-005 — Auth Domain Entities vs core:domain

**Decision**: `AuthToken`, `Credentials`, and `User` (auth context) live in `feature/auth/commonMain`. They are auth-specific types; domain identity types (`UserId`, `StoreId`, `Role`) are imported from `core:domain`.

**Rationale**: `core:domain` is the canonical home for domain-level identifiers and value objects shared across features. Auth-specific aggregates are feature-scoped. This avoids polluting `core:domain` with auth machinery.

`User` in the auth context:
```kotlin
data class User(
    val id: UserId,      // core:domain
    val email: String,
    val role: Role,      // core:domain — OWNER, MANAGER, CASHIER, WAITER, KITCHEN, VIEWER
    val storeId: StoreId // core:domain
)
```

Note: `Role` enum has 6 values (`OWNER, MANAGER, CASHIER, WAITER, KITCHEN, VIEWER`) — the spec lists only 4 for context but the full enum is already defined in `core:domain`.

---

## R-006 — JVM TokenStorage (PKCS12 Keystore)

**Decision**: Use JVM's built-in `KeyStore` with PKCS12 format, stored in `~/.vibely/token.ks`.

**Rationale**: No additional dependency required (standard JVM security APIs). Token bytes are stored as a `SecretKeyEntry` with AES-GCM encryption managed by the keystore.

Key operations:
- `KeyStore.getInstance("PKCS12")`
- Store path: `System.getProperty("user.home") + "/.vibely/token.ks"`
- Keystore password: derived from machine ID or fixed dev secret (resolved during implementation)
- All operations run on `Dispatchers.IO` per NFR-001
