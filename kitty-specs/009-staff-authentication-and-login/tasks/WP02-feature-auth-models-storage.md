---
work_package_id: WP02
title: feature/auth Models + Platform Storage
lane: "doing"
dependencies: []
base_branch: main
base_commit: 3e353041e274e41309f7869b8a79975361408ce9
created_at: '2026-03-26T01:43:18.097159+00:00'
subtasks:
- T008
- T009
- T010
- T011
- T012
- T013
- T014
phase: Phase 1 - Foundation (no dependencies)
assignee: ''
agent: "claude-sonnet-4-6"
shell_pid: "77606"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-26T01:25:51Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-006
- FR-007
- FR-008
- NFR-001
- NFR-002
---

# Work Package Prompt: WP02 – feature/auth Models + Platform Storage

## ⚠️ IMPORTANT: Review Feedback Status

Check `review_status` in frontmatter. If `has_feedback`, read the **Review Feedback** section below first.

---

## Review Feedback

*[Empty — no feedback yet.]*

---

## Objectives & Success Criteria

Configure the `feature/auth` module, define auth domain models, the `TokenStorage` interface, and all three platform implementations. This WP is **independent of WP01** — it can be implemented in parallel.

- `feature/auth` compiles for Android, JVM, and JS
- `BuildKonfig.AUTH_MODE` is accessible from `commonMain` code after a build with `AUTH_MODE=debug`
- `AuthToken`, `Credentials`, `User` are usable in `commonMain` with no platform imports
- `TokenStorage` interface has exactly three operations
- All three platform implementations instantiate, save, retrieve, and clear a token without errors

## Context & Constraints

**Implementation command** (no dependencies):
```bash
spec-kitty implement WP02
```

**Relevant documents**:
- `kitty-specs/009-staff-authentication-and-login/plan.md` — WP02 scope
- `kitty-specs/009-staff-authentication-and-login/data-model.md` — entity definitions
- `kitty-specs/009-staff-authentication-and-login/research.md` — R-001 (buildkonfig), R-004 (sessionStorage), R-005 (domain entities), R-006 (PKCS12)
- `.kittify/memory/constitution.md` — `expect/actual` requirement, `Result<T>`, `Dispatchers.IO`

**Key constraints**:
- `TokenStorage` operations must run on `Dispatchers.IO` (NFR-001)
- `User` references `UserId`, `StoreId`, `Role` from `:core:domain` — import only, do not redefine
- `buildkonfig` generates object `BuildKonfig` (note: NOT `BuildConfig`)
- `kotlinx.browser.sessionStorage` is available in `jsMain` stdlib — no extra dep

---

## Subtasks & Detailed Guidance

### Subtask T008 — Add buildkonfig 0.17.1 to version catalog

**Purpose**: Register the `buildkonfig` Gradle plugin in the central version catalog so `feature/auth/build.gradle.kts` can apply it with a type-safe alias.

**Steps**:
1. Open `gradle/libs.versions.toml`
2. In `[versions]`, add:
   ```toml
   buildkonfig = "0.17.1"
   ```
3. In `[plugins]`, add:
   ```toml
   buildkonfig = { id = "com.codingfeline.buildkonfig", version.ref = "buildkonfig" }
   ```
4. No `[libraries]` entry needed — `buildkonfig` is a Gradle plugin, not a runtime library.

**Files**: `gradle/libs.versions.toml`

---

### Subtask T009 — Update feature/auth/build.gradle.kts

**Purpose**: Apply the `buildkonfig` plugin, configure `AUTH_MODE` injection, and declare all source-set dependencies.

**Steps**:
1. Open `feature/auth/build.gradle.kts`. Currently it only has the `kmp-library` plugin applied.
2. Add `alias(libs.plugins.buildkonfig)` to the `plugins {}` block.
3. Add a `sourceSets {}` block inside `kotlin {}`:
   ```kotlin
   kotlin {
       android {
           namespace = "com.vibely.feature.auth"
       }

       sourceSets {
           commonMain.dependencies {
               implementation(projects.core.domain)
               implementation(projects.core.network)
               implementation(libs.koin.core)
               implementation(libs.kotlinx.coroutines.core)
               implementation(libs.kotlinx.datetime)
           }
           androidMain.dependencies {
               implementation(libs.security.crypto)
           }
           jvmTest.dependencies {
               implementation(libs.kotest.assertions.core)
               implementation(libs.kotlinx.coroutines.test)
               implementation(libs.turbine)
           }
       }
   }
   ```
4. Add the `buildkonfig {}` block **after** the `kotlin {}` block:
   ```kotlin
   buildkonfig {
       packageName = "com.vibely.feature.auth"
       val authMode = System.getenv("AUTH_MODE") ?: "production"
       defaultConfigs {
           buildConfigField(
               com.codingfeline.buildkonfig.compiler.generator.BuildKonfigGenerator.FieldSpec.Type.STRING,
               "AUTH_MODE",
               authMode,
           )
       }
   }
   ```
   > Tip: The `Type.STRING` reference is verbose. You may import the type or use the string shorthand `STRING` if the plugin DSL supports it in 0.17.1.
5. Keep `jvmTest` task configuration from the existing `core:domain` pattern:
   ```kotlin
   tasks.withType<Test>().configureEach { useJUnit() }
   ```

**Files**: `feature/auth/build.gradle.kts`

**Notes**: `projects.core.network` is added now even though `ProductionAuthMode` (WP03) is the actual consumer. Having the dep in `commonMain` allows `AuthApiClient` to be referenced in `AuthMode` method signatures if needed.

---

### Subtask T010 — Define domain models: AuthToken, Credentials, User

**Purpose**: Create the auth-domain data classes in `commonMain`. These are distinct from HTTP DTOs (which live in `core:network`) and carry domain semantics.

**Steps**:
1. Create package `com.vibely.feature.auth.domain.model` in `commonMain`:

   ```kotlin
   // AuthToken.kt
   package com.vibely.feature.auth.domain.model

   import kotlinx.datetime.Instant

   /**
    * A valid authentication session with both access and refresh tokens.
    */
   data class AuthToken(
       val accessToken: String,
       val refreshToken: String,
       val expiresAt: Instant,
   )
   ```

   ```kotlin
   // Credentials.kt
   package com.vibely.feature.auth.domain.model

   /**
    * User-supplied login credentials. Never persisted.
    */
   data class Credentials(
       val email: String,
       val password: String,
   )
   ```

   ```kotlin
   // User.kt
   package com.vibely.feature.auth.domain.model

   import com.vibely.domain.staff.Role
   import com.vibely.domain.tenant.StoreId
   import com.vibely.domain.tenant.UserId

   /**
    * Authenticated staff member identity returned by [ValidateTokenUseCase].
    */
   data class User(
       val id: UserId,
       val email: String,
       val role: Role,
       val storeId: StoreId,
   )
   ```

**Files**:
- `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/domain/model/AuthToken.kt`
- `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/domain/model/Credentials.kt`
- `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/domain/model/User.kt`

**Parallel?**: Yes — can proceed alongside T013 and T014.

---

### Subtask T011 — Define TokenStorage interface

**Purpose**: Declare the three-operation storage contract. Platform implementations (T012–T014) implement this interface; `ProductionAuthMode` (WP03) consumes it.

**Steps**:
1. Create `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/storage/TokenStorage.kt`:
   ```kotlin
   package com.vibely.feature.auth.storage

   import com.vibely.feature.auth.domain.model.AuthToken

   /**
    * Platform-appropriate encrypted storage for authentication tokens.
    *
    * All operations are suspend and must run on [kotlinx.coroutines.Dispatchers.IO].
    */
   interface TokenStorage {
       /** Persist [token] to encrypted platform storage. */
       suspend fun saveToken(token: AuthToken)

       /** Retrieve the stored [AuthToken], or null if none exists. */
       suspend fun getToken(): AuthToken?

       /** Remove all stored token data. */
       suspend fun clearToken()
   }
   ```

**Files**: `feature/auth/src/commonMain/kotlin/com/vibely/feature/auth/storage/TokenStorage.kt`

---

### Subtask T012 — Implement EncryptedSharedPreferencesTokenStorage (Android)

**Purpose**: Store `AuthToken` fields as individual string entries in `EncryptedSharedPreferences` using AES-256-GCM.

**Steps**:
1. Create `feature/auth/src/androidMain/kotlin/com/vibely/feature/auth/storage/EncryptedSharedPreferencesTokenStorage.kt`:
   ```kotlin
   package com.vibely.feature.auth.storage

   import android.content.Context
   import androidx.security.crypto.EncryptedSharedPreferences
   import androidx.security.crypto.MasterKey
   import com.vibely.feature.auth.domain.model.AuthToken
   import kotlinx.coroutines.Dispatchers
   import kotlinx.coroutines.withContext
   import kotlinx.datetime.Instant

   private const val PREFS_FILE = "vibely_auth_token"
   private const val KEY_ACCESS = "access_token"
   private const val KEY_REFRESH = "refresh_token"
   private const val KEY_EXPIRY = "expires_at"

   /**
    * Android token storage backed by [EncryptedSharedPreferences] (AES-256-GCM).
    */
   class EncryptedSharedPreferencesTokenStorage(
       private val context: Context,
   ) : TokenStorage {

       private val prefs by lazy {
           val masterKey = MasterKey.Builder(context)
               .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
               .build()
           EncryptedSharedPreferences.create(
               context,
               PREFS_FILE,
               masterKey,
               EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
               EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
           )
       }

       override suspend fun saveToken(token: AuthToken) = withContext(Dispatchers.IO) {
           prefs.edit()
               .putString(KEY_ACCESS, token.accessToken)
               .putString(KEY_REFRESH, token.refreshToken)
               .putString(KEY_EXPIRY, token.expiresAt.toString())
               .apply()
       }

       override suspend fun getToken(): AuthToken? = withContext(Dispatchers.IO) {
           val access = prefs.getString(KEY_ACCESS, null) ?: return@withContext null
           val refresh = prefs.getString(KEY_REFRESH, null) ?: return@withContext null
           val expiry = prefs.getString(KEY_EXPIRY, null) ?: return@withContext null
           AuthToken(
               accessToken = access,
               refreshToken = refresh,
               expiresAt = Instant.parse(expiry),
           )
       }

       override suspend fun clearToken() = withContext(Dispatchers.IO) {
           prefs.edit()
               .remove(KEY_ACCESS).remove(KEY_REFRESH).remove(KEY_EXPIRY)
               .apply()
       }
   }
   ```

**Files**: `feature/auth/src/androidMain/kotlin/com/vibely/feature/auth/storage/EncryptedSharedPreferencesTokenStorage.kt`

**Notes**: `context` is injected by Koin (`androidContext()` is available in `authPlatformModule`). The `lazy` initializer ensures the `MasterKey` and prefs are created once on first access.

---

### Subtask T013 — Implement Pkcs12KeystoreTokenStorage (JVM)

**Purpose**: Store token fields in a PKCS12 Java `KeyStore` file in `~/.vibely/token.ks` for the desktop JVM target.

**Steps**:
1. Create `feature/auth/src/jvmMain/kotlin/com/vibely/feature/auth/storage/Pkcs12KeystoreTokenStorage.kt`:
   ```kotlin
   package com.vibely.feature.auth.storage

   import com.vibely.feature.auth.domain.model.AuthToken
   import kotlinx.coroutines.Dispatchers
   import kotlinx.coroutines.withContext
   import kotlinx.datetime.Instant
   import java.io.File
   import java.security.KeyStore
   import javax.crypto.spec.SecretKeySpec

   private const val KEYSTORE_FILE = ".vibely/token.ks"
   private const val ENTRY_ACCESS = "access_token"
   private const val ENTRY_REFRESH = "refresh_token"
   private const val ENTRY_EXPIRY = "expires_at"
   // TODO: replace with machine-derived or env-provided password for production
   private val KEYSTORE_PASSWORD = "vibely-dev-only".toCharArray()

   /**
    * JVM token storage backed by a PKCS12 [KeyStore] file in the user home directory.
    */
   class Pkcs12KeystoreTokenStorage : TokenStorage {

       private val keystoreFile: File
           get() = File(System.getProperty("user.home"), KEYSTORE_FILE)

       private fun loadOrCreate(): KeyStore {
           val ks = KeyStore.getInstance("PKCS12")
           if (keystoreFile.exists()) {
               keystoreFile.inputStream().use { ks.load(it, KEYSTORE_PASSWORD) }
           } else {
               ks.load(null, KEYSTORE_PASSWORD)
           }
           return ks
       }

       private fun KeyStore.storeEntry(alias: String, value: String) {
           setEntry(
               alias,
               KeyStore.SecretKeyEntry(SecretKeySpec(value.toByteArray(), "AES")),
               KeyStore.PasswordProtection(KEYSTORE_PASSWORD),
           )
       }

       private fun KeyStore.getEntry(alias: String): String? {
           val entry = getEntry(alias, KeyStore.PasswordProtection(KEYSTORE_PASSWORD))
               as? KeyStore.SecretKeyEntry ?: return null
           return String(entry.secretKey.encoded)
       }

       private fun save(ks: KeyStore) {
           keystoreFile.parentFile?.mkdirs()
           keystoreFile.outputStream().use { ks.store(it, KEYSTORE_PASSWORD) }
       }

       override suspend fun saveToken(token: AuthToken) = withContext(Dispatchers.IO) {
           val ks = loadOrCreate()
           ks.storeEntry(ENTRY_ACCESS, token.accessToken)
           ks.storeEntry(ENTRY_REFRESH, token.refreshToken)
           ks.storeEntry(ENTRY_EXPIRY, token.expiresAt.toString())
           save(ks)
       }

       override suspend fun getToken(): AuthToken? = withContext(Dispatchers.IO) {
           if (!keystoreFile.exists()) return@withContext null
           val ks = loadOrCreate()
           val access = ks.getEntry(ENTRY_ACCESS) ?: return@withContext null
           val refresh = ks.getEntry(ENTRY_REFRESH) ?: return@withContext null
           val expiry = ks.getEntry(ENTRY_EXPIRY) ?: return@withContext null
           AuthToken(access, refresh, Instant.parse(expiry))
       }

       override suspend fun clearToken() = withContext(Dispatchers.IO) {
           keystoreFile.delete()
       }
   }
   ```

**Files**: `feature/auth/src/jvmMain/kotlin/com/vibely/feature/auth/storage/Pkcs12KeystoreTokenStorage.kt`

**Parallel?**: Yes — can proceed alongside T010 and T014.

---

### Subtask T014 — Implement SessionStorageTokenStorage (JS)

**Purpose**: Store token fields in the browser's `sessionStorage`. Tokens are automatically cleared when the tab closes — correct POS behaviour.

**Steps**:
1. Create `feature/auth/src/jsMain/kotlin/com/vibely/feature/auth/storage/SessionStorageTokenStorage.kt`:
   ```kotlin
   package com.vibely.feature.auth.storage

   import com.vibely.feature.auth.domain.model.AuthToken
   import kotlinx.browser.sessionStorage
   import kotlinx.datetime.Instant

   private const val KEY_ACCESS = "vibely_access_token"
   private const val KEY_REFRESH = "vibely_refresh_token"
   private const val KEY_EXPIRY = "vibely_expires_at"

   /**
    * Web token storage backed by [sessionStorage].
    * Tokens are automatically cleared when the browser tab is closed.
    *
    * All functions are non-blocking — sessionStorage is synchronous in the browser.
    * The suspend modifier is kept for interface compatibility.
    */
   class SessionStorageTokenStorage : TokenStorage {

       override suspend fun saveToken(token: AuthToken) {
           sessionStorage.setItem(KEY_ACCESS, token.accessToken)
           sessionStorage.setItem(KEY_REFRESH, token.refreshToken)
           sessionStorage.setItem(KEY_EXPIRY, token.expiresAt.toString())
       }

       override suspend fun getToken(): AuthToken? {
           val access = sessionStorage.getItem(KEY_ACCESS) ?: return null
           val refresh = sessionStorage.getItem(KEY_REFRESH) ?: return null
           val expiry = sessionStorage.getItem(KEY_EXPIRY) ?: return null
           return AuthToken(access, refresh, Instant.parse(expiry))
       }

       override suspend fun clearToken() {
           sessionStorage.removeItem(KEY_ACCESS)
           sessionStorage.removeItem(KEY_REFRESH)
           sessionStorage.removeItem(KEY_EXPIRY)
       }
   }
   ```

**Files**: `feature/auth/src/jsMain/kotlin/com/vibely/feature/auth/storage/SessionStorageTokenStorage.kt`

**Parallel?**: Yes — can proceed alongside T010 and T013.

**Notes**: `kotlinx.browser` is part of the Kotlin JS stdlib — no additional dependency. The `suspend` keyword is kept for interface compatibility; the operations are synchronous and non-blocking in practice.

---

## Risks & Mitigations

- **`buildkonfig` DSL type reference**: The `Type.STRING` reference is verbose. Check buildkonfig 0.17.1 release notes — some versions support a simpler `STRING` import from the plugin DSL. If compilation fails, use the fully qualified form shown in T009.
- **`MasterKey` (alpha)**: `security-crypto 1.1.0-alpha06` is already declared in `core:common`'s `androidMain` — reuse the same version reference.
- **PKCS12 password**: Using a fixed dev-only password in T013. Mark with `// TODO` and flag for production hardening before the feature ships.
- **`withContext(Dispatchers.IO)` in JS**: `Dispatchers.IO` does not exist in the JS target. `SessionStorageTokenStorage` (T014) intentionally omits it since `sessionStorage` is synchronous. Do NOT add `withContext` to the JS implementation.

## Review Guidance

- Run `./gradlew :feature:auth:compileCommonMainKotlinMetadata` — must pass with `BuildKonfig` generated
- Run `AUTH_MODE=debug ./gradlew :feature:auth:compileCommonMainKotlinMetadata` — `BuildKonfig.AUTH_MODE` must be `"debug"`
- `TokenStorage` interface has exactly 3 operations (FR-007)
- `User.role` type is `Role` from `com.vibely.domain.staff.Role` (not a local copy)
- All Android/JVM storage ops wrapped in `withContext(Dispatchers.IO)` (NFR-001)
- KDoc on `TokenStorage`, `AuthToken`, `Credentials`, `User`, and all three storage classes

## Activity Log

- 2026-03-26T01:25:51Z – system – lane=planned – Prompt created.
- 2026-03-26T01:43:19Z – claude-sonnet-4-6 – shell_pid=65169 – lane=doing – Assigned agent via workflow command
- 2026-03-26T02:06:48Z – claude-sonnet-4-6 – shell_pid=65169 – lane=for_review – Ready for review: buildkonfig config, AuthToken/Credentials/User models, TokenStorage interface + Android/JVM/JS impls. All 7 subtasks complete.
- 2026-03-26T02:13:05Z – claude-sonnet-4-6 – shell_pid=77606 – lane=doing – Started review via workflow command
