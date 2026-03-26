---
work_package_id: WP03
title: feature/auth AuthMode + Use Cases + DI + Tests
lane: "doing"
dependencies: []
base_branch: main
base_commit: 13c6bd178129da3197f931792e31d2ac6880eded
created_at: '2026-03-26T02:20:54.664459+00:00'
subtasks:
- T015
- T016
- T017
- T018
- T019
- T020
- T021
phase: Phase 2 - Strategy Layer
assignee: ''
agent: "claude-sonnet-4-6"
shell_pid: "81584"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-26T01:25:51Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-001
- FR-002
- FR-003
- FR-004
- FR-005
- FR-022
- FR-023
- FR-024
- FR-025
- NFR-002
- NFR-003
---

# Work Package Prompt: WP03 – feature/auth AuthMode + Use Cases + DI + Tests

## ⚠️ IMPORTANT: Review Feedback Status

Check `review_status` in frontmatter. If `has_feedback`, read the **Review Feedback** section below first.

---

## Review Feedback

*[Empty — no feedback yet.]*

---

## Objectives & Success Criteria

Implement the full authentication strategy layer on top of WP01 and WP02: three `AuthMode` implementations, four use cases, Koin DI with build-time mode selection, and unit tests confirming all use-case paths work in isolation.

- `AuthMode` sealed interface compiles with no platform-specific imports
- `ProductionAuthMode` delegates all operations to `AuthApiClient` + `TokenStorage`
- `DebugAuthMode` returns success immediately; `validateToken` returns a `Role.OWNER` user
- `FakeAuthMode` is constructible with `FakeAuthMode()` — zero dependencies
- All four use cases are thin wrappers delegating to `AuthMode`
- `authModule` reads `BuildKonfig.AUTH_MODE` at startup — no `if (isDebug)` in business logic
- `./gradlew :feature:auth:jvmTest` passes with zero network or storage I/O

## Context & Constraints

**Implementation command** (depends on WP01 and WP02):
```bash
spec-kitty implement WP03 --base WP02
```
> WP01 and WP02 should both be merged before starting WP03.

**Relevant documents**:
- `kitty-specs/009-staff-authentication-and-login/spec.md` — FR-001 to FR-005, FR-022 to FR-025
- `kitty-specs/009-staff-authentication-and-login/data-model.md` — AuthMode interface, use case signatures
- `kitty-specs/009-staff-authentication-and-login/research.md` — R-005 (entity placement)
- `.kittify/memory/constitution.md` — `Result<T>` everywhere, fake impls for tests, no `if (isDebug)`

**Key constraints**:
- `AuthMode` and all use cases must be in `commonMain` with zero platform imports (NFR-002)
- `FakeAuthMode` must not require any test framework dependency (NFR-003)
- `authModule` is the **only** place `BuildKonfig.AUTH_MODE` is read — never in business logic
- Tests live in `feature/auth/src/jvmTest` (project convention, not `commonTest`)
- Use `Result<T>` for all fallible operations — never throw from use cases

---

## Subtasks & Detailed Guidance

### Subtask T015 — Define AuthMode sealed interface

**Purpose**: Establish the strategy contract that all three implementations (`ProductionAuthMode`, `DebugAuthMode`, `FakeAuthMode`) must satisfy.

**Steps**:
1. Create `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/domain/auth/AuthMode.kt`:
   ```kotlin
   package com.vibely.feature.auth.domain.auth

   import com.vibely.feature.auth.domain.model.AuthToken
   import com.vibely.feature.auth.domain.model.Credentials
   import com.vibely.feature.auth.domain.model.User

   /**
    * Strategy interface encapsulating all authentication operations.
    *
    * Three implementations exist:
    * - [ProductionAuthMode]: calls the real auth backend
    * - [DebugAuthMode]: auto-authenticates without network calls (debug builds)
    * - [FakeAuthMode]: configurable for testing (no I/O)
    *
    * The active implementation is selected by Koin at startup based on [BuildKonfig.AUTH_MODE].
    * Business logic must never branch on the implementation type.
    */
   sealed interface AuthMode {
       suspend fun authenticate(credentials: Credentials): Result<AuthToken>
       suspend fun refreshToken(refreshToken: String): Result<AuthToken>
       suspend fun validateToken(accessToken: String): Result<User>
       suspend fun logout(accessToken: String): Result<Unit>
   }
   ```

**Files**: `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/domain/auth/AuthMode.kt`

---

### Subtask T016 — Implement ProductionAuthMode

**Purpose**: Production implementation that calls `AuthApiClient` (from `core:network`) and persists/clears the token via `TokenStorage`.

**Steps**:
1. Create `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/domain/auth/ProductionAuthMode.kt`:
   ```kotlin
   package com.vibely.feature.auth.domain.auth

   import com.vibely.core.network.auth.AuthApiClient
   import com.vibely.core.network.auth.dto.LoginRequest
   import com.vibely.core.network.auth.dto.RefreshRequest
   import com.vibely.domain.staff.Role
   import com.vibely.domain.tenant.StoreId
   import com.vibely.domain.tenant.UserId
   import com.vibely.feature.auth.domain.model.AuthToken
   import com.vibely.feature.auth.domain.model.Credentials
   import com.vibely.feature.auth.domain.model.User
   import com.vibely.feature.auth.storage.TokenStorage
   import kotlinx.datetime.Instant

   /**
    * Production [AuthMode] — delegates to the auth backend via [AuthApiClient].
    * Persists the token on successful login/refresh; clears it on logout.
    */
   class ProductionAuthMode(
       private val apiClient: AuthApiClient,
       private val storage: TokenStorage,
   ) : AuthMode {

       override suspend fun authenticate(credentials: Credentials): Result<AuthToken> =
           apiClient.login(LoginRequest(credentials.email, credentials.password))
               .map { dto ->
                   AuthToken(dto.accessToken, dto.refreshToken, Instant.parse(dto.expiresAt))
               }
               .onSuccess { token -> storage.saveToken(token) }

       override suspend fun refreshToken(refreshToken: String): Result<AuthToken> =
           apiClient.refresh(RefreshRequest(refreshToken))
               .map { dto ->
                   AuthToken(dto.accessToken, dto.refreshToken, Instant.parse(dto.expiresAt))
               }
               .onSuccess { token -> storage.saveToken(token) }

       override suspend fun validateToken(accessToken: String): Result<User> =
           apiClient.validate(accessToken).map { dto ->
               User(
                   id = UserId(dto.userId),
                   email = dto.email,
                   role = Role.valueOf(dto.role),
                   storeId = StoreId(dto.storeId),
               )
           }

       override suspend fun logout(accessToken: String): Result<Unit> =
           apiClient.logout(accessToken).onSuccess { storage.clearToken() }
   }
   ```

**Files**: `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/domain/auth/ProductionAuthMode.kt`

**Notes**: `Result.map`, `Result.onSuccess` keep the `Result<T>` chain clean without throwing. `Role.valueOf` can throw if the server returns an unknown role — this is an acceptable crash (server contract violation).

---

### Subtask T017 — Implement DebugAuthMode

**Purpose**: Auto-authenticate without any network or storage I/O. Used in debug builds so developers never see the login screen.

**Steps**:
1. Create `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/domain/auth/DebugAuthMode.kt`:
   ```kotlin
   package com.vibely.feature.auth.domain.auth

   import com.vibely.domain.staff.Role
   import com.vibely.domain.tenant.StoreId
   import com.vibely.domain.tenant.UserId
   import com.vibely.feature.auth.domain.model.AuthToken
   import com.vibely.feature.auth.domain.model.Credentials
   import com.vibely.feature.auth.domain.model.User
   import kotlinx.datetime.Clock
   import kotlinx.datetime.DateTimeUnit
   import kotlinx.datetime.plus

   private val DEBUG_USER = User(
       id = UserId("00000000-0000-0000-0000-000000000001"),
       email = "debug@vibely.local",
       role = Role.OWNER,
       storeId = StoreId("00000000-0000-0000-0000-000000000001"),
   )

   /**
    * Debug-only [AuthMode] — succeeds immediately without any I/O.
    * Active when [BuildKonfig.AUTH_MODE] == "debug".
    */
   class DebugAuthMode : AuthMode {

       override suspend fun authenticate(credentials: Credentials): Result<AuthToken> =
           Result.success(debugToken())

       override suspend fun refreshToken(refreshToken: String): Result<AuthToken> =
           Result.success(debugToken())

       override suspend fun validateToken(accessToken: String): Result<User> =
           Result.success(DEBUG_USER)

       override suspend fun logout(accessToken: String): Result<Unit> =
           Result.success(Unit)

       private fun debugToken() = AuthToken(
           accessToken = "debug-access-token",
           refreshToken = "debug-refresh-token",
           expiresAt = Clock.System.now().plus(24, DateTimeUnit.HOUR),
       )
   }
   ```

**Files**: `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/domain/auth/DebugAuthMode.kt`

**Parallel?**: Yes — can proceed alongside T018 once T015 is done.

---

### Subtask T018 — Implement FakeAuthMode

**Purpose**: Test-only `AuthMode` with publicly settable `Result` fields. No test framework required — plain class, usable from any source set.

**Steps**:
1. Create `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/domain/auth/FakeAuthMode.kt`:
   ```kotlin
   package com.vibely.feature.auth.domain.auth

   import com.vibely.feature.auth.domain.model.AuthToken
   import com.vibely.feature.auth.domain.model.Credentials
   import com.vibely.feature.auth.domain.model.User

   /**
    * Fake [AuthMode] for tests. Configure each operation's result before calling.
    *
    * All results default to [Result.failure] with [IllegalStateException].
    * Override before use:
    * ```kotlin
    * val fake = FakeAuthMode()
    * fake.authenticateResult = Result.success(token)
    * ```
    */
   class FakeAuthMode : AuthMode {

       var authenticateResult: Result<AuthToken> =
           Result.failure(IllegalStateException("FakeAuthMode.authenticateResult not set"))

       var refreshTokenResult: Result<AuthToken> =
           Result.failure(IllegalStateException("FakeAuthMode.refreshTokenResult not set"))

       var validateTokenResult: Result<User> =
           Result.failure(IllegalStateException("FakeAuthMode.validateTokenResult not set"))

       var logoutResult: Result<Unit> =
           Result.failure(IllegalStateException("FakeAuthMode.logoutResult not set"))

       /** Number of times [authenticate] has been called — useful for call-count assertions. */
       var authenticateCallCount: Int = 0
           private set

       override suspend fun authenticate(credentials: Credentials): Result<AuthToken> {
           authenticateCallCount++
           return authenticateResult
       }

       override suspend fun refreshToken(refreshToken: String): Result<AuthToken> =
           refreshTokenResult

       override suspend fun validateToken(accessToken: String): Result<User> =
           validateTokenResult

       override suspend fun logout(accessToken: String): Result<Unit> =
           logoutResult
   }
   ```

**Files**: `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/domain/auth/FakeAuthMode.kt`

**Parallel?**: Yes — can proceed alongside T017.

**Notes**: `FakeAuthMode` lives in `commonMain` (not `testFixtures` or a test source set) so it can be used from any source set without framework dependencies (NFR-003).

---

### Subtask T019 — Implement four use cases

**Purpose**: Create thin delegation wrappers — each use case holds one `AuthMode` reference and delegates to one operation. They exist to keep the UI layer decoupled from `AuthMode` directly.

**Steps**:
1. Create `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/usecase/`:

   ```kotlin
   // LoginUseCase.kt
   package com.vibely.feature.auth.usecase

   import com.vibely.feature.auth.domain.auth.AuthMode
   import com.vibely.feature.auth.domain.model.AuthToken
   import com.vibely.feature.auth.domain.model.Credentials

   /** Authenticates a staff member using the provided [Credentials]. */
   class LoginUseCase(private val authMode: AuthMode) {
       suspend operator fun invoke(credentials: Credentials): Result<AuthToken> =
           authMode.authenticate(credentials)
   }
   ```

   ```kotlin
   // LogoutUseCase.kt
   class LogoutUseCase(private val authMode: AuthMode) {
       suspend operator fun invoke(accessToken: String): Result<Unit> =
           authMode.logout(accessToken)
   }
   ```

   ```kotlin
   // RefreshTokenUseCase.kt
   class RefreshTokenUseCase(private val authMode: AuthMode) {
       suspend operator fun invoke(refreshToken: String): Result<AuthToken> =
           authMode.refreshToken(refreshToken)
   }
   ```

   ```kotlin
   // ValidateTokenUseCase.kt
   class ValidateTokenUseCase(private val authMode: AuthMode) {
       suspend operator fun invoke(accessToken: String): Result<User> =
           authMode.validateToken(accessToken)
   }
   ```

   Each file has the appropriate package and `User` import from `com.vibely.feature.auth.domain.model`.

**Files**: `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/usecase/` (4 files)

---

### Subtask T020 — Define authModule + authPlatformModule Koin

**Purpose**: Wire everything together in Koin. `authModule` reads `BuildKonfig.AUTH_MODE` to select the right `AuthMode`; `authPlatformModule` binds the platform-specific `TokenStorage` singleton.

**Steps**:
1. Create `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/di/AuthModule.kt`:
   ```kotlin
   package com.vibely.feature.auth.di

   import com.vibely.feature.auth.BuildKonfig
   import com.vibely.feature.auth.domain.auth.AuthMode
   import com.vibely.feature.auth.domain.auth.DebugAuthMode
   import com.vibely.feature.auth.domain.auth.FakeAuthMode
   import com.vibely.feature.auth.domain.auth.ProductionAuthMode
   import com.vibely.feature.auth.usecase.*
   import org.koin.dsl.module

   /**
    * Core auth domain Koin module.
    * Reads [BuildKonfig.AUTH_MODE] to select the [AuthMode] implementation.
    * Register alongside [authPlatformModule] and [networkModule].
    */
   fun authModule() = module {
       single<AuthMode> {
           when (BuildKonfig.AUTH_MODE) {
               "debug" -> DebugAuthMode()
               "fake" -> FakeAuthMode()
               else -> ProductionAuthMode(apiClient = get(), storage = get())
           }
       }
       factory { LoginUseCase(get()) }
       factory { LogoutUseCase(get()) }
       factory { RefreshTokenUseCase(get()) }
       factory { ValidateTokenUseCase(get()) }
   }
   ```

2. Create `expect` declaration for the platform module in `commonMain`:
   ```kotlin
   // feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/di/AuthPlatformModule.kt
   package com.vibely.feature.auth.di

   import org.koin.core.module.Module

   /** Platform-specific TokenStorage binding. Implemented per platform via expect/actual. */
   expect fun authPlatformModule(): Module
   ```

3. Create `actual` for `androidMain`:
   ```kotlin
   // androidMain
   package com.vibely.feature.auth.di

   import com.vibely.feature.auth.storage.EncryptedSharedPreferencesTokenStorage
   import com.vibely.feature.auth.storage.TokenStorage
   import org.koin.android.ext.koin.androidContext
   import org.koin.core.module.Module
   import org.koin.dsl.module

   actual fun authPlatformModule(): Module = module {
       single<TokenStorage> { EncryptedSharedPreferencesTokenStorage(androidContext()) }
   }
   ```

4. Create `actual` for `jvmMain`:
   ```kotlin
   actual fun authPlatformModule(): Module = module {
       single<TokenStorage> { Pkcs12KeystoreTokenStorage() }
   }
   ```

5. Create `actual` for `jsMain`:
   ```kotlin
   actual fun authPlatformModule(): Module = module {
       single<TokenStorage> { SessionStorageTokenStorage() }
   }
   ```

**Files**:
- `feature/auth/src/commonMain/.../di/AuthModule.kt`
- `feature/auth/src/commonMain/.../di/AuthPlatformModule.kt`
- `feature/auth/src/androidMain/.../di/AuthPlatformModule.kt`
- `feature/auth/src/jvmMain/.../di/AuthPlatformModule.kt`
- `feature/auth/src/jsMain/.../di/AuthPlatformModule.kt`

**Notes**: `factory` (not `single`) for use cases — use cases are stateless and cheap to construct. `single` for `AuthMode` and `TokenStorage` — they maintain state or hold expensive resources.

---

### Subtask T021 — Unit tests for all four use cases

**Purpose**: Confirm every use-case success and failure path using `FakeAuthMode`. Zero network or storage I/O.

**Steps**:
1. Create test classes in `feature/auth/src/jvmTest/kotlin/com/vibely/feature/auth/usecase/`:

   ```kotlin
   // LoginUseCaseTest.kt
   package com.vibely.feature.auth.usecase

   import com.vibely.domain.staff.Role
   import com.vibely.domain.tenant.StoreId
   import com.vibely.domain.tenant.UserId
   import com.vibely.feature.auth.domain.auth.FakeAuthMode
   import com.vibely.feature.auth.domain.model.AuthToken
   import com.vibely.feature.auth.domain.model.Credentials
   import com.vibely.feature.auth.domain.model.User
   import io.kotest.matchers.result.shouldBeFailure
   import io.kotest.matchers.result.shouldBeSuccess
   import io.kotest.matchers.shouldBe
   import kotlinx.coroutines.test.runTest
   import kotlinx.datetime.Clock
   import kotlin.test.Test

   class LoginUseCaseTest {
       private val fake = FakeAuthMode()
       private val useCase = LoginUseCase(fake)
       private val credentials = Credentials("staff@vibely.com", "secret")

       @Test fun `success — returns token from AuthMode`() = runTest {
           val token = AuthToken("access", "refresh", Clock.System.now())
           fake.authenticateResult = Result.success(token)
           useCase(credentials).shouldBeSuccess() shouldBe token
       }

       @Test fun `failure — propagates failure from AuthMode`() = runTest {
           val error = RuntimeException("invalid credentials")
           fake.authenticateResult = Result.failure(error)
           useCase(credentials).shouldBeFailure() shouldBe error
       }

       @Test fun `delegates — calls AuthMode exactly once`() = runTest {
           fake.authenticateResult = Result.success(AuthToken("a", "r", Clock.System.now()))
           useCase(credentials)
           fake.authenticateCallCount shouldBe 1
       }
   }
   ```

2. Create similar tests for `LogoutUseCaseTest`, `RefreshTokenUseCaseTest`, `ValidateTokenUseCaseTest`:
   - Each tests success path (returns the fake's result) and failure path (propagates the error).
   - `ValidateTokenUseCaseTest` uses a `User` fixture.
   - No Koin in tests — direct construction via `UseCase(FakeAuthMode())`.

3. Run: `./gradlew :feature:auth:jvmTest`

**Files**: `feature/auth/src/jvmTest/kotlin/com/vibely/feature/auth/usecase/` (4 test files)

**Notes**:
- Use `kotlinx.coroutines.test.runTest` for coroutine tests (in `kotlinx-coroutines-test`)
- `io.kotest:kotest-assertions-core` for `shouldBeSuccess`, `shouldBeFailure`, `shouldBe`
- Do **not** use Mockk/Mockito — constitution forbids mocks

---

## Risks & Mitigations

- **`BuildKonfig` import path**: Import is `com.vibely.feature.auth.BuildKonfig` (the `packageName` set in T009). IDE may not suggest this until after the first build generates the source.
- **`Role.valueOf` in ProductionAuthMode**: If the server returns an unknown role string, this throws. Accept this — it's a server contract violation. Log the error if desired.
- **`FakeAuthMode` in `commonMain`**: Some developers expect test fakes in `testFixtures`. Here it's in `commonMain` by design (NFR-003). Add a KDoc note explaining this is intentional.
- **`expect/actual` for `authPlatformModule`**: The JS `actual` must be in `jsMain` even if the JS app doesn't use auth yet — Kotlin requires all `actual` declarations to be present or the build fails.

## Review Guidance

- `./gradlew :feature:auth:jvmTest` — all tests pass; zero network calls
- `AuthMode` is `sealed interface` with no platform-specific imports in `commonMain`
- `FakeAuthMode` can be instantiated with `FakeAuthMode()` from any context
- `authModule()` has exactly one `when (BuildKonfig.AUTH_MODE)` — no other places in the codebase read this value
- `factory` for use cases, `single` for `AuthMode` and `TokenStorage`
- All platform `actual authPlatformModule()` implementations present (Android + JVM + JS)
- KDoc on `AuthMode`, `FakeAuthMode`, `authModule`, `authPlatformModule`, and all use cases

## Activity Log

- 2026-03-26T01:25:51Z – system – lane=planned – Prompt created.
- 2026-03-26T02:20:55Z – claude-sonnet-4-6 – shell_pid=81584 – lane=doing – Assigned agent via workflow command
