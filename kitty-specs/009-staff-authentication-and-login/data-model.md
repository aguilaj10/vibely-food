# Data Model: Staff Authentication & Login

**Feature**: 009-staff-authentication-and-login
**Date**: 2026-03-26

---

## Entities

### AuthToken
Location: `feature/auth/commonMain`

| Field | Type | Description |
|---|---|---|
| `accessToken` | `String` | JWT access token |
| `refreshToken` | `String` | Opaque refresh token |
| `expiresAt` | `Instant` | UTC expiry timestamp (`kotlinx.datetime.Instant`) |

```kotlin
data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
)
```

### Credentials
Location: `feature/auth/commonMain`

| Field | Type | Description |
|---|---|---|
| `email` | `String` | Staff email address |
| `password` | `String` | Plaintext password (in-memory only, never persisted) |

```kotlin
data class Credentials(
    val email: String,
    val password: String,
)
```

### User (auth context)
Location: `feature/auth/commonMain`

| Field | Type | Source | Description |
|---|---|---|---|
| `id` | `UserId` | `core:domain` | UUID-backed staff identifier |
| `email` | `String` | — | Authenticated staff email |
| `role` | `Role` | `core:domain` | `OWNER / MANAGER / CASHIER / WAITER / KITCHEN / VIEWER` |
| `storeId` | `StoreId` | `core:domain` | The store this session is scoped to |

```kotlin
data class User(
    val id: UserId,
    val email: String,
    val role: Role,
    val storeId: StoreId,
)
```

---

## Interfaces

### AuthMode (sealed)
Location: `feature/auth/commonMain`

```kotlin
sealed interface AuthMode {
    suspend fun authenticate(credentials: Credentials): Result<AuthToken>
    suspend fun refreshToken(refreshToken: String): Result<AuthToken>
    suspend fun validateToken(accessToken: String): Result<User>
    suspend fun logout(accessToken: String): Result<Unit>
}
```

Implementations:
- `ProductionAuthMode` — calls `AuthApiClient`; persists token on success
- `DebugAuthMode` — returns success immediately; pre-configured `User(role = Role.OWNER)`
- `FakeAuthMode` — publicly settable `Result` properties; no network or storage I/O

### TokenStorage
Location: `feature/auth/commonMain`

```kotlin
interface TokenStorage {
    suspend fun saveToken(token: AuthToken)
    suspend fun getToken(): AuthToken?
    suspend fun clearToken()
}
```

Platform implementations:
| Platform | Class | Mechanism |
|---|---|---|
| Android (`androidMain`) | `EncryptedSharedPreferencesTokenStorage` | `EncryptedSharedPreferences` + AES-256-GCM `MasterKey` |
| JVM (`jvmMain`) | `Pkcs12KeystoreTokenStorage` | PKCS12 `KeyStore` in `~/.vibely/token.ks` |
| Web/JS (`jsMain`) | `SessionStorageTokenStorage` | `kotlinx.browser.sessionStorage` |

---

## HTTP DTOs
Location: `core/network/commonMain`

### LoginRequest
```kotlin
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)
```

### TokenResponse
```kotlin
@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String, // ISO-8601 UTC timestamp
)
```

### RefreshRequest
```kotlin
@Serializable
data class RefreshRequest(
    val refreshToken: String,
)
```

### ValidateResponse
```kotlin
@Serializable
data class ValidateResponse(
    val userId: String,
    val email: String,
    val role: String,
    val storeId: String,
)
```

---

## Use Cases
Location: `feature/auth/commonMain`

| Class | Input | Output | Delegates to |
|---|---|---|---|
| `LoginUseCase` | `Credentials` | `Result<AuthToken>` | `AuthMode.authenticate` |
| `LogoutUseCase` | `accessToken: String` | `Result<Unit>` | `AuthMode.logout` |
| `RefreshTokenUseCase` | `refreshToken: String` | `Result<AuthToken>` | `AuthMode.refreshToken` |
| `ValidateTokenUseCase` | `accessToken: String` | `Result<User>` | `AuthMode.validateToken` |

---

## Navigation Keys
Location: `composeApp/src/main/kotlin/`

```kotlin
@Serializable
sealed interface AppNavKey : NavKey {
    @Serializable data object Login : AppNavKey
    @Serializable data object FloorPlan : AppNavKey
}
```

---

## UI State

### LoginUiState
Location: `composeApp/src/main/kotlin/`

```kotlin
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
```

Derived property: `isSubmitEnabled = email.isNotBlank() && password.isNotBlank() && !isLoading`

---

## Startup Flow

```
App cold start
    │
    ├─ TokenStorage.getToken() == null ──────────────────────► Show LoginScreen
    │
    ├─ Token exists → AuthMode.validateToken()
    │       │
    │       ├─ Result.success ──────────────────────────────► Navigate to FloorPlan
    │       │
    │       └─ Result.failure (expired)
    │               │
    │               └─ AuthMode.refreshToken()
    │                       │
    │                       ├─ Result.success → saveToken() ► Navigate to FloorPlan
    │                       └─ Result.failure → clearToken() ► Show LoginScreen
```
