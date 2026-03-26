---
work_package_id: WP01
title: core:network HTTP Client Foundation
lane: "doing"
dependencies: []
base_branch: main
base_commit: 5fbea05251ea8107af8036a6fe1a816d676ad1ed
created_at: '2026-03-26T01:43:10.837954+00:00'
subtasks:
- T001
- T002
- T003
- T004
- T005
- T006
- T007
phase: Phase 1 - Foundation (no dependencies)
assignee: ''
agent: "claude-sonnet-4-6"
shell_pid: "64904"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-26T01:25:51Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-002
- FR-022
- FR-023
- FR-024
- FR-025
---

# Work Package Prompt: WP01 – core:network HTTP Client Foundation

## ⚠️ IMPORTANT: Review Feedback Status

Check `review_status` in frontmatter. If `has_feedback`, read the **Review Feedback** section below first.

---

## Review Feedback

*[Empty — no feedback yet.]*

---

## Objectives & Success Criteria

Build the reusable Ktor HTTP client and auth API contract in `core:network`. This WP is self-contained and has **no upstream dependencies** — it can be implemented in parallel with WP02.

- `core:network` compiles for all three targets: Android (`androidMain`), JVM (`jvmMain`), JS (`jsMain`)
- All four HTTP DTOs are `@Serializable` and round-trip correctly
- `AuthApiClient` interface covers all four auth operations
- `KtorAuthApiClient` implements each operation against the correct endpoint
- Koin `networkModule` resolves `AuthApiClient` as a singleton
- No business logic in this module — only HTTP transport + contract

## Context & Constraints

**Implementation command** (no dependencies):
```bash
spec-kitty implement WP01
```

**Relevant documents**:
- `kitty-specs/009-staff-authentication-and-login/plan.md` — WP01 scope
- `kitty-specs/009-staff-authentication-and-login/contracts/auth-api.yaml` — OpenAPI 3.1 auth contract
- `kitty-specs/009-staff-authentication-and-login/data-model.md` — DTO shapes
- `.kittify/memory/constitution.md` — code quality, Koin, no framework in domain

**Key constraints**:
- `core:network` uses the `kmp-library` convention plugin (`android`, `jvm`, `js(IR) { browser() }` targets)
- All Ktor client engines are **platform-specific** — only `ktor-client-core` in `commonMain`
- `networkModule` is exported from this module; `VibelyApp` registers it in WP04
- Use `libs.xxx` type-safe catalog accessors everywhere — no string literals

---

## Subtasks & Detailed Guidance

### Subtask T001 — Add ktor-client-js to version catalog

**Purpose**: The existing catalog already has `ktor-client-okhttp` (Android/JVM) but is missing the JS engine entry. Add it so the JS target can select its engine.

**Steps**:
1. Open `gradle/libs.versions.toml`
2. In `[libraries]`, add after `ktor-client-okhttp`:
   ```toml
   ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktor" }
   ```
3. No version bump needed — reuses existing `ktor = "3.4.1"`.

**Files**: `gradle/libs.versions.toml`

**Notes**: `ktor-client-darwin` is already in the catalog (iOS). JS uses `ktor-client-js`.

---

### Subtask T002 — Update core/network/build.gradle.kts

**Purpose**: Wire all Ktor client dependencies and serialization into the three source sets.

**Steps**:
1. Open `core/network/build.gradle.kts`
2. Add a `sourceSets` block inside `kotlin { }`:
   ```kotlin
   sourceSets {
       commonMain.dependencies {
           implementation(libs.ktor.client.core)
           implementation(libs.ktor.client.content.negotiation)
           implementation(libs.ktor.client.logging)
           implementation(libs.ktor.serialization.kotlinx.json)
           implementation(libs.kotlinx.serialization.json)
           implementation(libs.kotlinx.datetime)
           implementation(libs.koin.core)
       }
       androidMain.dependencies {
           implementation(libs.ktor.client.okhttp)
       }
       val jvmMain by getting {
           dependencies {
               implementation(libs.ktor.client.okhttp)
           }
       }
       val jsMain by getting {
           dependencies {
               implementation(libs.ktor.client.js)
           }
       }
   }
   ```

**Files**: `core/network/build.gradle.kts`

**Notes**:
- `ktor-client-okhttp` is shared for Android and JVM — both use OkHttp engine.
- `ktor-serialization-kotlinx-json` is the content negotiation plugin (already in catalog).
- `koin-core` is added here so `networkModule` can be defined in this module.

---

### Subtask T003 — Implement HttpClientFactory with platform-specific engines

**Purpose**: Create a factory that returns a configured Ktor `HttpClient` with the correct engine per platform.

**Steps**:
1. Create `core/network/src/commonMain/kotlin/com/vibely/core/network/HttpClientFactory.kt`:
   ```kotlin
   package com.vibely.core.network

   import io.ktor.client.HttpClient
   import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
   import io.ktor.client.plugins.logging.LogLevel
   import io.ktor.client.plugins.logging.Logging
   import io.ktor.serialization.kotlinx.json.json
   import kotlinx.serialization.json.Json

   /**
    * Creates a platform-appropriate [HttpClient].
    * The engine is provided by [createHttpClient] which is implemented
    * in each platform's source set via expect/actual.
    */
   fun buildHttpClient(): HttpClient = createHttpClient {
       install(ContentNegotiation) {
           json(Json { ignoreUnknownKeys = true })
       }
       install(Logging) {
           level = LogLevel.INFO
       }
   }
   ```

2. Create `expect` declaration in `commonMain`:
   ```kotlin
   // core/network/src/commonMain/kotlin/com/vibely/core/network/PlatformHttpClient.kt
   package com.vibely.core.network

   import io.ktor.client.HttpClient
   import io.ktor.client.HttpClientConfig

   /** Platform-specific engine factory. */
   expect fun createHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient
   ```

3. Create `actual` for `androidMain`:
   ```kotlin
   // androidMain
   package com.vibely.core.network

   import io.ktor.client.HttpClient
   import io.ktor.client.HttpClientConfig
   import io.ktor.client.engine.okhttp.OkHttp

   actual fun createHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
       HttpClient(OkHttp, config)
   ```

4. Create `actual` for `jvmMain` (identical to Android):
   ```kotlin
   actual fun createHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
       HttpClient(OkHttp, config)
   ```

5. Create `actual` for `jsMain`:
   ```kotlin
   import io.ktor.client.engine.js.Js

   actual fun createHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
       HttpClient(Js, config)
   ```

**Files**:
- `core/network/src/commonMain/kotlin/com/vibely/core/network/HttpClientFactory.kt`
- `core/network/src/commonMain/kotlin/com/vibely/core/network/PlatformHttpClient.kt`
- `core/network/src/androidMain/kotlin/com/vibely/core/network/PlatformHttpClient.kt`
- `core/network/src/jvmMain/kotlin/com/vibely/core/network/PlatformHttpClient.kt`
- `core/network/src/jsMain/kotlin/com/vibely/core/network/PlatformHttpClient.kt`

---

### Subtask T004 — Define @Serializable HTTP DTOs

**Purpose**: Declare the request/response data classes that map to the OpenAPI contract in `contracts/auth-api.yaml`. These are transport-layer types — distinct from domain models.

**Steps**:
1. Create the DTOs in `core/network/src/commonMain/kotlin/com/vibely/core/network/auth/dto/`:

   ```kotlin
   // LoginRequest.kt
   @Serializable
   data class LoginRequest(val email: String, val password: String)

   // RefreshRequest.kt
   @Serializable
   data class RefreshRequest(val refreshToken: String)

   // TokenResponse.kt
   @Serializable
   data class TokenResponse(
       val accessToken: String,
       val refreshToken: String,
       val expiresAt: String, // ISO-8601 UTC — parsed to Instant by callers
   )

   // ValidateResponse.kt
   @Serializable
   data class ValidateResponse(
       val userId: String,
       val email: String,
       val role: String,
       val storeId: String,
   )
   ```

2. Each file has `package com.vibely.core.network.auth.dto` and `import kotlinx.serialization.Serializable`.

**Files**: `core/network/src/commonMain/kotlin/com/vibely/core/network/auth/dto/` (4 files)

**Parallel?**: Yes — can proceed alongside T003.

---

### Subtask T005 — Define AuthApiClient interface

**Purpose**: Define the contract that `KtorAuthApiClient` fulfills and that `ProductionAuthMode` (WP03) depends on. Keeping it as an interface enables testing without HTTP calls.

**Steps**:
1. Create `core/network/src/commonMain/kotlin/com/vibely/core/network/auth/AuthApiClient.kt`:
   ```kotlin
   package com.vibely.core.network.auth

   import com.vibely.core.network.auth.dto.LoginRequest
   import com.vibely.core.network.auth.dto.RefreshRequest
   import com.vibely.core.network.auth.dto.TokenResponse
   import com.vibely.core.network.auth.dto.ValidateResponse

   /**
    * HTTP contract for the Vibely authentication backend.
    * All operations return [Result] — never throw.
    */
   interface AuthApiClient {
       suspend fun login(request: LoginRequest): Result<TokenResponse>
       suspend fun refresh(request: RefreshRequest): Result<TokenResponse>
       suspend fun validate(accessToken: String): Result<ValidateResponse>
       suspend fun logout(accessToken: String): Result<Unit>
   }
   ```

**Files**: `core/network/src/commonMain/kotlin/com/vibely/core/network/auth/AuthApiClient.kt`

---

### Subtask T006 — Implement KtorAuthApiClient

**Purpose**: Implement `AuthApiClient` using the `HttpClient` from T003. Each method makes one HTTP call to the corresponding `/auth/*` endpoint.

**Steps**:
1. Create `core/network/src/commonMain/kotlin/com/vibely/core/network/auth/KtorAuthApiClient.kt`:

   ```kotlin
   package com.vibely.core.network.auth

   import com.vibely.core.network.auth.dto.*
   import io.ktor.client.HttpClient
   import io.ktor.client.call.body
   import io.ktor.client.request.*
   import io.ktor.http.ContentType
   import io.ktor.http.contentType

   /**
    * Ktor-backed implementation of [AuthApiClient].
    *
    * @param client The shared [HttpClient] instance from Koin.
    * @param baseUrl The auth backend base URL (e.g., "https://api.vibely.com").
    */
   class KtorAuthApiClient(
       private val client: HttpClient,
       private val baseUrl: String,
   ) : AuthApiClient {

       override suspend fun login(request: LoginRequest): Result<TokenResponse> =
           runCatching {
               client.post("$baseUrl/auth/login") {
                   contentType(ContentType.Application.Json)
                   setBody(request)
               }.body()
           }

       override suspend fun refresh(request: RefreshRequest): Result<TokenResponse> =
           runCatching {
               client.post("$baseUrl/auth/refresh") {
                   contentType(ContentType.Application.Json)
                   setBody(request)
               }.body()
           }

       override suspend fun validate(accessToken: String): Result<ValidateResponse> =
           runCatching {
               client.post("$baseUrl/auth/validate") {
                   bearerAuth(accessToken)
               }.body()
           }

       override suspend fun logout(accessToken: String): Result<Unit> =
           runCatching {
               client.post("$baseUrl/auth/logout") {
                   bearerAuth(accessToken)
               }
               Unit
           }
   }
   ```

2. `baseUrl` is injected by Koin (T007) from a build-time constant or environment variable. Use a placeholder for now: `"https://api.vibely.com"` — this will be updated when the real backend is configured.

**Files**: `core/network/src/commonMain/kotlin/com/vibely/core/network/auth/KtorAuthApiClient.kt`

**Notes**: `runCatching` converts exceptions to `Result.failure` — satisfies the `Result<T>` constitution requirement.

---

### Subtask T007 — Define Koin networkModule

**Purpose**: Export a Koin module from `core:network` that downstream modules (`feature:auth`) can include. Provides `HttpClient` and `AuthApiClient` as singletons.

**Steps**:
1. Create `core/network/src/commonMain/kotlin/com/vibely/core/network/di/NetworkModule.kt`:
   ```kotlin
   package com.vibely.core.network.di

   import com.vibely.core.network.auth.AuthApiClient
   import com.vibely.core.network.auth.KtorAuthApiClient
   import com.vibely.core.network.buildHttpClient
   import org.koin.dsl.module

   /**
    * Koin module providing the shared [io.ktor.client.HttpClient] and [AuthApiClient].
    *
    * Register this in your application's Koin setup alongside other feature modules.
    */
   fun networkModule() = module {
       single { buildHttpClient() }
       single<AuthApiClient> {
           KtorAuthApiClient(
               client = get(),
               baseUrl = "https://api.vibely.com", // TODO: inject from BuildConfig in future
           )
       }
   }
   ```

**Files**: `core/network/src/commonMain/kotlin/com/vibely/core/network/di/NetworkModule.kt`

---

## Risks & Mitigations

- **`jsMain` engine**: `ktor-client-js` must be in `jsMain.dependencies`, not `commonMain`. If placed in `commonMain` it breaks Android/JVM compilation.
- **`expect/actual` completeness**: The Kotlin compiler will error if any platform is missing an `actual` implementation. Implement all three before attempting to build.
- **`baseUrl` hardcoding**: Acknowledged technical debt. Use `"https://api.vibely.com"` as placeholder; document with `// TODO`.

## Review Guidance

- All three build targets (`androidMain`, `jvmMain`, `jsMain`) must compile: `./gradlew :core:network:compileCommonMainKotlinMetadata :core:network:compileKotlinAndroid :core:network:compileKotlinJvm :core:network:compileKotlinJs`
- `AuthApiClient` must be a pure interface with no platform imports
- `KtorAuthApiClient` uses `runCatching` (never throws)
- `networkModule()` is a function (not `val`) so each call returns a new module instance (Koin convention)
- KDoc on `AuthApiClient`, `KtorAuthApiClient`, `networkModule`, `buildHttpClient`

## Activity Log

- 2026-03-26T01:25:51Z – system – lane=planned – Prompt created.
- 2026-03-26T01:43:11Z – claude-sonnet-4-6 – shell_pid=64904 – lane=doing – Assigned agent via workflow command
