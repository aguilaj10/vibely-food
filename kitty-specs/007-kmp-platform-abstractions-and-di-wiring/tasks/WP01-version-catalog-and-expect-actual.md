---
work_package_id: WP01
title: Version Catalog + expect/actual in core:common
lane: "done"
dependencies: []
base_branch: main
base_commit: cfd0285a2c00faf5fb2c8e41971d0e5a47e238e7
created_at: '2026-03-24T22:35:51.553274+00:00'
subtasks:
- T001
- T002
- T003
- T004
- T005
- T006
phase: Phase 1 - Platform Abstractions
assignee: ''
agent: "claude"
shell_pid: "17885"
review_status: "approved"
reviewed_by: "Jonathan Sánchez Muñoz"
history:
- timestamp: '2026-03-24T22:28:04Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-001
- FR-002
- FR-003
- FR-007
---

# Work Package Prompt: WP01 – Version Catalog + expect/actual in `core:common`

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

- Remove all SQLDelight entries from `gradle/libs.versions.toml`; add DataStore KMP, Room KMP, security-crypto, and KSP.
- Add `security-crypto` to `core/common:androidMain` dependencies.
- Implement `PlatformCapabilities`, `PlatformLogger`, and `SecureStorage` as `expect class` declarations in `commonMain`, each with `actual` implementations for Android, JVM, and JS.
- Add `SecureStorageTest` and `PlatformCapabilitiesTest` to `core:common:jvmTest`.
- `./gradlew :core:common:jvmTest` passes with zero failures.
- Zero Detekt violations (KDoc on all public symbols).
- Zero platform imports in `commonMain`.

**Implement with**: `spec-kitty implement WP01`

---

## Context & Constraints

- **Plan**: `kitty-specs/007-kmp-platform-abstractions-and-di-wiring/plan.md`
- **Spec**: `kitty-specs/007-kmp-platform-abstractions-and-di-wiring/spec.md`
- **Constitution**: `.kittify/memory/constitution.md` — KDoc on all public symbols (Detekt-enforced); zero platform imports in `commonMain`; `expect/actual` is the only approved pattern for platform branching.
- **KMP convention**: `core:common` uses the `kmp-library` convention plugin, which already configures `android`, `jvm()`, and `js(IR) { browser() }` targets.
- **Testing**: Tests live in `jvmTest` source set (per memory: `feedback_testing_strategy.md` — JVM-only for domain/shared logic, not `commonTest`).
- **Gradle**: All build files must use `libs.xxx` type-safe accessors (per memory: `feedback_gradle_typesafe_config.md` — no string-based notation).
- **Sequence**: T001 → T002 → (T003 + T004 + T005 in parallel) → T006.

---

## Subtasks & Detailed Guidance

### Subtask T001 – Update `gradle/libs.versions.toml`

**Purpose**: Modernise the version catalog to drop SQLDelight and introduce the three new Android/KMP storage libraries plus KSP.

**Steps**:

1. Open `gradle/libs.versions.toml`.

2. **Remove** all entries that contain `sqldelight` (versions, libraries, plugins). Search for `sqldelight` (case-insensitive) and delete every match.

3. **Add** to `[versions]`:
   ```toml
   datastore = "1.1.3"
   room = "2.7.0"
   security-crypto = "1.1.0-alpha06"
   ksp = "2.3.20-2.0.1"
   ```
   The KSP version `2.3.20-2.0.1` must align with Kotlin `2.3.20` — verify the Kotlin version already in the catalog.

4. **Add** to `[libraries]`:
   ```toml
   datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
   room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
   room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
   security-crypto = { module = "androidx.security:security-crypto", version.ref = "security-crypto" }
   ```

5. **Add** to `[plugins]`:
   ```toml
   ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
   room = { id = "androidx.room", version.ref = "room" }
   ```

**Files**: `gradle/libs.versions.toml`

**Validation**:
- [ ] No `sqldelight` string remains anywhere in the file.
- [ ] All four new library aliases are present.
- [ ] Both new plugin aliases are present.
- [ ] KSP version suffix (`-2.0.1`) matches the Room KMP annotation processor requirement for Kotlin `2.3.20`.

---

### Subtask T002 – Update `core/common/build.gradle.kts`

**Purpose**: Wire `security-crypto` into the Android-specific source set so the Android `actual` for `SecureStorage` can import `EncryptedSharedPreferences`.

**Steps**:

1. Open `core/common/build.gradle.kts`.

2. Locate the `kotlin { sourceSets { ... } }` block. Find or create the `androidMain.dependencies { }` block.

3. Add:
   ```kotlin
   androidMain.dependencies {
       implementation(libs.security.crypto)
   }
   ```
   Use the type-safe accessor `libs.security.crypto` (Gradle derives the accessor from the alias `security-crypto` by replacing `-` with `.`).

4. Do NOT add DataStore or Room here — those belong in `shared`, not `core:common`.

**Files**: `core/common/build.gradle.kts`

**Validation**:
- [ ] `libs.security.crypto` appears in `androidMain.dependencies`.
- [ ] No other new dependencies added to this file.

---

### Subtask T003 – Implement `PlatformCapabilities` (expect + 3 actuals)

**Purpose**: Provide a platform-aware capability flags class so shared code can gate behavior on platform support without platform imports.

**Files**:
- `core/common/src/commonMain/kotlin/com/vibely/common/platform/PlatformCapabilities.kt`
- `core/common/src/androidMain/kotlin/com/vibely/common/platform/PlatformCapabilities.kt`
- `core/common/src/jvmMain/kotlin/com/vibely/common/platform/PlatformCapabilities.kt`
- `core/common/src/jsMain/kotlin/com/vibely/common/platform/PlatformCapabilities.kt`

**Steps**:

1. **commonMain** — `expect class`:
   ```kotlin
   package com.vibely.common.platform

   /**
    * Describes the runtime capabilities available on the current platform.
    * Shared code uses these flags to gate platform-specific behaviour.
    */
   expect class PlatformCapabilities() {
       /** True if the platform can persist data to local storage. */
       val supportsLocalCache: Boolean
       /** True if the platform supports background work after the UI is hidden. */
       val supportsBackgroundSync: Boolean
       /** True if the platform can display push or local notifications. */
       val supportsNotifications: Boolean
   }
   ```

2. **androidMain** — `actual class` with values `true / true / true`:
   ```kotlin
   package com.vibely.common.platform

   /**
    * Android implementation of [PlatformCapabilities].
    * All capabilities are available on Android.
    */
   actual class PlatformCapabilities actual constructor() {
       actual val supportsLocalCache: Boolean = true
       actual val supportsBackgroundSync: Boolean = true
       actual val supportsNotifications: Boolean = true
   }
   ```

3. **jvmMain** — `actual class` with values `false / true / false`:
   ```kotlin
   package com.vibely.common.platform

   /**
    * JVM server implementation of [PlatformCapabilities].
    * The JVM target does not persist data locally and cannot send notifications.
    */
   actual class PlatformCapabilities actual constructor() {
       actual val supportsLocalCache: Boolean = false
       actual val supportsBackgroundSync: Boolean = true
       actual val supportsNotifications: Boolean = false
   }
   ```

4. **jsMain** — `actual class` with values `true / false / false`:
   ```kotlin
   package com.vibely.common.platform

   /**
    * Web/JS implementation of [PlatformCapabilities].
    * The browser can persist data via storage APIs but cannot run background tasks.
    */
   actual class PlatformCapabilities actual constructor() {
       actual val supportsLocalCache: Boolean = true
       actual val supportsBackgroundSync: Boolean = false
       actual val supportsNotifications: Boolean = false
   }
   ```

**Validation**:
- [ ] All four files compile.
- [ ] No `import android.*`, `import java.*`, or JS-specific imports in `commonMain`.
- [ ] KDoc on the class and every property in all four files.

---

### Subtask T004 – Implement `PlatformLogger` (expect + 3 actuals)

**Purpose**: Provide a structured, platform-aware logger so shared code never imports platform logging APIs directly.

**Files**:
- `core/common/src/commonMain/kotlin/com/vibely/common/platform/PlatformLogger.kt`
- `core/common/src/androidMain/kotlin/com/vibely/common/platform/PlatformLogger.kt`
- `core/common/src/jvmMain/kotlin/com/vibely/common/platform/PlatformLogger.kt`
- `core/common/src/jsMain/kotlin/com/vibely/common/platform/PlatformLogger.kt`

**Steps**:

1. **commonMain** — `expect class`:
   ```kotlin
   package com.vibely.common.platform

   /**
    * Platform-aware structured logger.
    * Shared code calls this instead of importing platform logging APIs directly.
    */
   expect class PlatformLogger() {
       /** Logs a debug-level message with the given [tag]. */
       fun debug(tag: String, message: String)
       /** Logs an info-level message with the given [tag]. */
       fun info(tag: String, message: String)
       /** Logs a warning-level message with the given [tag]. */
       fun warn(tag: String, message: String)
       /** Logs an error-level message with the given [tag] and optional [throwable]. */
       fun error(tag: String, message: String, throwable: Throwable? = null)
   }
   ```

2. **androidMain** — delegates to `android.util.Log`:
   ```kotlin
   package com.vibely.common.platform

   import android.util.Log

   /**
    * Android implementation of [PlatformLogger] backed by [android.util.Log].
    */
   actual class PlatformLogger actual constructor() {
       actual fun debug(tag: String, message: String) { Log.d(tag, message) }
       actual fun info(tag: String, message: String) { Log.i(tag, message) }
       actual fun warn(tag: String, message: String) { Log.w(tag, message) }
       actual fun error(tag: String, message: String, throwable: Throwable?) {
           Log.e(tag, message, throwable)
       }
   }
   ```

3. **jvmMain** — delegates to `System.out.println` with level prefix:
   ```kotlin
   package com.vibely.common.platform

   /**
    * JVM implementation of [PlatformLogger] backed by stdout with level prefixes.
    */
   actual class PlatformLogger actual constructor() {
       actual fun debug(tag: String, message: String) = println("[DEBUG] [$tag] $message")
       actual fun info(tag: String, message: String) = println("[INFO]  [$tag] $message")
       actual fun warn(tag: String, message: String) = println("[WARN]  [$tag] $message")
       actual fun error(tag: String, message: String, throwable: Throwable?) {
           println("[ERROR] [$tag] $message")
           throwable?.printStackTrace()
       }
   }
   ```

4. **jsMain** — delegates to `console`:
   ```kotlin
   package com.vibely.common.platform

   /**
    * Web/JS implementation of [PlatformLogger] backed by the browser console.
    */
   actual class PlatformLogger actual constructor() {
       actual fun debug(tag: String, message: String) = console.log("[$tag] $message")
       actual fun info(tag: String, message: String) = console.log("[$tag] $message")
       actual fun warn(tag: String, message: String) = console.warn("[$tag] $message")
       actual fun error(tag: String, message: String, throwable: Throwable?) {
           console.error("[$tag] $message${throwable?.let { ": $it" } ?: ""}")
       }
   }
   ```

**Validation**:
- [ ] All four files compile.
- [ ] Android actual imports only `android.util.Log` — no other platform imports.
- [ ] KDoc on every function in all four files.

---

### Subtask T005 – Implement `SecureStorage` (expect + 3 actuals)

**Purpose**: Provide a platform-aware, key-value secure storage contract so sensitive values (tokens, session keys) are stored with the appropriate mechanism per platform.

**Files**:
- `core/common/src/commonMain/kotlin/com/vibely/common/platform/SecureStorage.kt`
- `core/common/src/androidMain/kotlin/com/vibely/common/platform/SecureStorage.kt`
- `core/common/src/jvmMain/kotlin/com/vibely/common/platform/SecureStorage.kt`
- `core/common/src/jsMain/kotlin/com/vibely/common/platform/SecureStorage.kt`

**Steps**:

1. **commonMain** — `expect class`:
   ```kotlin
   package com.vibely.common.platform

   /**
    * Platform-aware key-value storage for sensitive values (tokens, session keys).
    * Shared code depends only on this contract; platform implementations vary.
    */
   expect class SecureStorage() {
       /** Saves [value] under [key], replacing any existing entry. */
       fun save(key: String, value: String)
       /** Returns the value stored under [key], or null if absent. */
       fun get(key: String): String?
       /** Removes the entry for [key]. No-op if absent. */
       fun delete(key: String)
       /** Removes all stored entries. */
       fun clear()
   }
   ```

2. **androidMain** — `EncryptedSharedPreferences` backed by Android Keystore:
   - The Android actual requires a `Context` to create `EncryptedSharedPreferences`. Since `SecureStorage` is instantiated via Koin (which has access to `androidContext()`), obtain `Context` as a constructor parameter via Koin injection rather than directly in this `actual`. However, because the `expect` declares a no-arg constructor, the Android actual must accept `Context` via a mechanism compatible with the `expect` signature. The recommended approach is to use a **singleton** initialised by the Koin Android actual (see WP02/T009), where the Android actual of `platformModule()` calls `SecureStorage(get())` with `Context` injected.
   - Therefore, change the `expect` constructor to accept an optional `Context`-equivalent if needed, OR define the Android actual with a secondary constructor. The cleanest pattern for KMP is to make `SecureStorage` in `androidMain` an `actual class` that stores the `Context` in a companion object or is initialised through Koin. **Use the pattern below** where Koin passes the context at construction:

   Since `expect class SecureStorage()` must have a no-arg constructor, the Android actual must source `Context` from elsewhere. Use a `lateinit` companion pattern or annotate the class to receive context through Koin. The simplest compliant approach:

   ```kotlin
   package com.vibely.common.platform

   import android.content.Context
   import androidx.security.crypto.EncryptedSharedPreferences
   import androidx.security.crypto.MasterKey

   /**
    * Android implementation of [SecureStorage] backed by [EncryptedSharedPreferences]
    * and the Android Keystore system.
    *
    * The [Context] is provided via the Koin Android application context.
    */
   actual class SecureStorage actual constructor() {

       private val prefs by lazy {
           val context = ApplicationContextHolder.context
           val masterKey = MasterKey.Builder(context)
               .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
               .build()
           EncryptedSharedPreferences.create(
               context,
               "vibely_secure_prefs",
               masterKey,
               EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
               EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
           )
       }

       actual fun save(key: String, value: String) {
           prefs.edit().putString(key, value).apply()
       }

       actual fun get(key: String): String? = prefs.getString(key, null)

       actual fun delete(key: String) {
           prefs.edit().remove(key).apply()
       }

       actual fun clear() {
           prefs.edit().clear().apply()
       }
   }
   ```

   Also create `ApplicationContextHolder.kt` in `androidMain`:
   ```kotlin
   package com.vibely.common.platform

   import android.content.Context

   /**
    * Holds the application [Context] for use by platform-specific implementations
    * that cannot receive [Context] through their constructor signature.
    * Must be initialised in [VibelyApp.onCreate] before Koin starts.
    */
   object ApplicationContextHolder {
       lateinit var context: Context
           private set

       /** Initialises the holder. Call once from [Application.onCreate]. */
       fun init(context: Context) {
           this.context = context.applicationContext
       }
   }
   ```

   `VibelyApp.onCreate()` in WP03 (T011) will call `ApplicationContextHolder.init(this)` before `startKoin`.

3. **jvmMain** — `ConcurrentHashMap` (in-memory, non-persistent):
   ```kotlin
   package com.vibely.common.platform

   import java.util.concurrent.ConcurrentHashMap

   /**
    * JVM implementation of [SecureStorage] backed by an in-memory [ConcurrentHashMap].
    * Data is NOT persisted across process restarts. Auth tokens are managed via
    * environment variables on the JVM server target.
    */
   actual class SecureStorage actual constructor() {

       private val store = ConcurrentHashMap<String, String>()

       actual fun save(key: String, value: String) { store[key] = value }
       actual fun get(key: String): String? = store[key]
       actual fun delete(key: String) { store.remove(key) }
       actual fun clear() { store.clear() }
   }
   ```

4. **jsMain** — `window.localStorage`:
   ```kotlin
   package com.vibely.common.platform

   /**
    * Web/JS implementation of [SecureStorage] backed by [window.localStorage].
    * Suitable for this application's sensitivity level.
    */
   actual class SecureStorage actual constructor() {

       actual fun save(key: String, value: String) {
           kotlinx.browser.window.localStorage.setItem(key, value)
       }

       actual fun get(key: String): String? =
           kotlinx.browser.window.localStorage.getItem(key)

       actual fun delete(key: String) {
           kotlinx.browser.window.localStorage.removeItem(key)
       }

       actual fun clear() {
           kotlinx.browser.window.localStorage.clear()
       }
   }
   ```

**Validation**:
- [ ] All four files compile.
- [ ] JVM actual uses only `java.util.concurrent.ConcurrentHashMap` — no Android/JS imports.
- [ ] JS actual uses `kotlinx.browser.window.localStorage` — no Java/Android imports.
- [ ] KDoc on the class and every function in all four files.
- [ ] Android actual does not crash at instantiation if `ApplicationContextHolder.context` is unset — this is guaranteed by WP03/T011 calling `init()` before Koin.

---

### Subtask T006 – Add `SecureStorageTest` and `PlatformCapabilitiesTest` in `core:common:jvmTest`

**Purpose**: Validate JVM `actual` behaviour for `SecureStorage` (save/get/delete/clear round-trips) and assert that `PlatformCapabilities` JVM values match the spec.

**Files**:
- `core/common/src/jvmTest/kotlin/com/vibely/common/platform/SecureStorageTest.kt`
- `core/common/src/jvmTest/kotlin/com/vibely/common/platform/PlatformCapabilitiesTest.kt`

**Steps**:

1. **`SecureStorageTest.kt`**:
   ```kotlin
   package com.vibely.common.platform

   import io.kotest.matchers.nulls.shouldBeNull
   import io.kotest.matchers.shouldBe
   import kotlin.test.Test

   class SecureStorageTest {

       private val storage = SecureStorage()

       @Test
       fun `save then get returns stored value`() {
           storage.save("key1", "value1")
           storage.get("key1") shouldBe "value1"
       }

       @Test
       fun `get returns null for missing key`() {
           storage.get("missing") shouldBeNull()
       }

       @Test
       fun `delete removes the entry`() {
           storage.save("key2", "value2")
           storage.delete("key2")
           storage.get("key2") shouldBeNull()
       }

       @Test
       fun `delete is no-op for absent key`() {
           storage.delete("nonexistent") // should not throw
       }

       @Test
       fun `clear removes all entries`() {
           storage.save("a", "1")
           storage.save("b", "2")
           storage.clear()
           storage.get("a") shouldBeNull()
           storage.get("b") shouldBeNull()
       }

       @Test
       fun `save overwrites existing entry`() {
           storage.save("k", "old")
           storage.save("k", "new")
           storage.get("k") shouldBe "new"
       }
   }
   ```

2. **`PlatformCapabilitiesTest.kt`**:
   ```kotlin
   package com.vibely.common.platform

   import io.kotest.matchers.shouldBe
   import kotlin.test.Test

   class PlatformCapabilitiesTest {

       private val caps = PlatformCapabilities()

       @Test
       fun `JVM supportsLocalCache is false`() {
           caps.supportsLocalCache shouldBe false
       }

       @Test
       fun `JVM supportsBackgroundSync is true`() {
           caps.supportsBackgroundSync shouldBe true
       }

       @Test
       fun `JVM supportsNotifications is false`() {
           caps.supportsNotifications shouldBe false
       }
   }
   ```

3. Ensure Kotest assertions (`io.kotest.matchers`) are already in the `jvmTest` dependencies of `core/common/build.gradle.kts`. If not, add:
   ```kotlin
   jvmTest.dependencies {
       implementation(libs.kotest.assertions.core)
   }
   ```
   (Alias `kotest.assertions.core` should already be in `libs.versions.toml` from prior features; if not, add `kotest-assertions-core = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }`.)

**Run command**: `./gradlew :core:common:jvmTest`

**Validation**:
- [ ] All 9 test methods pass.
- [ ] No test uses mocks — all tests use the real JVM `actual` implementations.
- [ ] Each test uses Kotest `shouldBe` / `shouldBeNull` assertions (no JUnit `assertEquals`).

---

## Test Strategy

Tests are in `core:common:jvmTest` (T006). Run with:
```bash
./gradlew :core:common:jvmTest
```

Full multi-target compile check:
```bash
./gradlew :core:common:build
```

Detekt check:
```bash
./gradlew :core:common:detekt
```

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| `security-crypto` minSdk conflicts | `minSdk 26` ≥ required `minSdk 23` — safe |
| KSP version mismatch with Kotlin `2.3.20` | Use exactly `ksp = "2.3.20-2.0.1"` |
| `ApplicationContextHolder` unset on Android | `VibelyApp.init()` in WP03/T011 must precede `startKoin` |
| Detekt KDoc failures | Add KDoc to every `expect` declaration and every `actual` class/function |
| `kotlinx.browser` not on JS classpath | Should be included via `kotlin-stdlib-js` — verify compilation |

---

## Review Guidance

- Confirm zero `import android.*`, `import java.*`, or JS-specific imports in all `commonMain` files.
- Confirm all 9 jvmTests pass.
- Confirm KDoc is present on all public symbols (Detekt gate).
- Check `libs.versions.toml` has no remaining `sqldelight` strings.
- Check `core/common/build.gradle.kts` uses `libs.security.crypto` (type-safe accessor).

---

## Activity Log

- 2026-03-24T22:28:04Z – system – lane=planned – Prompt created.
- 2026-03-24T22:35:52Z – claude-sonnet-4-6 – shell_pid=9181 – lane=doing – Assigned agent via workflow command
- 2026-03-24T22:50:57Z – claude-sonnet-4-6 – shell_pid=9181 – lane=for_review – All 6 subtasks complete. 17 files changed: version catalog updated, 3 expect classes + 10 actual implementations + 2 jvmTest classes. 9/9 tests passing. ktlint clean. Pre-commit hooks passed.
- 2026-03-24T22:52:51Z – claude – shell_pid=17885 – lane=doing – Started review via workflow command
- 2026-03-24T22:58:05Z – claude – shell_pid=17885 – lane=done – Review passed: all 6 subtasks verified. 9/9 tests pass. Zero platform imports in commonMain. Type-safe Gradle accessors used. KDoc present on all public symbols. ApplicationContextHolder pattern correctly documents init-before-Koin requirement. Import isolation verified across all 4 source sets. ktlint clean.
