---
work_package_id: WP04
title: composeApp Login UI & Navigation Routing
lane: "doing"
dependencies: []
base_branch: main
base_commit: 4146a99381d57875ac59f4fb1ed3756844aaa1ec
created_at: '2026-03-26T03:07:54.933425+00:00'
subtasks:
- T022
- T023
- T024
- T025
- T026
- T027
- T028
phase: Phase 3 - UI Layer
assignee: ''
agent: "claude-sonnet-4-6"
shell_pid: "14063"
review_status: "has_feedback"
reviewed_by: "Jonathan Sánchez Muñoz"
review_feedback_file: "/private/var/folders/lk/549xp1m52gg9ycr7sgpl0jcw0000gp/T/spec-kitty-review-feedback-WP04.md"
history:
- timestamp: '2026-03-26T01:25:51Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-009
- FR-010
- FR-011
- FR-012
- FR-013
- FR-014
- FR-015
- FR-016
- FR-017
- FR-018
- FR-019
- FR-020
- FR-021
- FR-026
- FR-027
- FR-028
---

# Work Package Prompt: WP04 – composeApp Login UI & Navigation Routing

## ⚠️ IMPORTANT: Review Feedback Status

Check `review_status` in frontmatter. If `has_feedback`, read the **Review Feedback** section below first.

---

## Review Feedback

**Reviewed by**: Jonathan Sánchez Muñoz
**Status**: ❌ Changes Requested
**Date**: 2026-03-26
**Feedback file**: `/private/var/folders/lk/549xp1m52gg9ycr7sgpl0jcw0000gp/T/spec-kitty-review-feedback-WP04.md`

**Issue 1 — S5 not satisfied: debug build shows Login screen on fresh install**

**Location**: `composeApp/src/main/kotlin/com/vibely/navigation/AppNavigation.kt`, line 44–45

**Problem**:
```kotlin
stored == null -> AppNavKey.Login
```
When there is no stored token (e.g. fresh debug install), the code unconditionally routes to `AppNavKey.Login` without calling `validateToken`. This means `DebugAuthMode.validateToken()` — which returns `Result.success(debugUser)` — is never invoked on startup, so the Login screen is always shown. S5 ("Debug build → FloorPlan shown; login never shown") is not satisfied.

**Fix**: Replace the null-token branch to attempt `validateToken("")`:
```kotlin
stored == null -> {
    // DebugAuthMode.validateToken succeeds regardless of token → FloorPlan.
    // ProductionAuthMode.validateToken("") fails (401) → Login.
    validateToken("").fold(
        onSuccess = { AppNavKey.FloorPlan },
        onFailure = { AppNavKey.Login },
    )
}
```
This requires no `BuildKonfig` coupling in the UI layer and correctly implements S1–S5:
- S1 (no token, production) → validateToken("") fails → Login ✅
- S5 (no token, debug) → validateToken("") succeeds → FloorPlan ✅
- S2/S3/S4 paths (stored != null) are unchanged ✅


## Objectives & Success Criteria

Deliver the complete user-facing authentication experience: startup token routing, login screen, and navigation wiring. After this WP the full user journey (S1–S5 from the spec) is demonstrable on Android.

- Cold start without stored token → Login screen shown (S1)
- Cold start with valid stored token → FloorPlan screen shown; login never shown (S2)
- Cold start with expired + valid refresh token → silent refresh; FloorPlan shown (S3)
- Cold start with expired + invalid refresh → Login shown (S4)
- Debug build (`AUTH_MODE=debug`) → FloorPlan shown; login never shown (S5)
- Loading indicator shown during startup token check — no blank frame (FR-013)
- Sign In button disabled while fields are empty or request is in progress (FR-015)
- Human-readable error on authentication failure (FR-017)
- Back stack cleared on successful login — pressing back does not return to login form (FR-018)

## Context & Constraints

**Implementation command** (depends on WP03):
```bash
spec-kitty implement WP04 --base WP03
```

**Relevant documents**:
- `kitty-specs/009-staff-authentication-and-login/spec.md` — FR-009 to FR-021, FR-026 to FR-028, scenarios S1–S5
- `kitty-specs/009-staff-authentication-and-login/data-model.md` — `AppNavKey`, `LoginUiState`, startup flow diagram
- `kitty-specs/009-staff-authentication-and-login/research.md` — R-002 (Navigation3 API), R-003 (composeApp source set reality)
- `.kittify/memory/constitution.md` — `Result<T>`, Koin, no platform branching

**Key constraints**:
- `composeApp` uses the `android-app` convention plugin — it is **Android-only**. There is no `commonMain` source set. All files go under `composeApp/src/main/kotlin/com/vibely/`.
- Navigation3 API: `NavBackStack<AppNavKey>`, `rememberNavBackStack()`, `NavDisplay` — **not** Navigation Compose 2 (`NavController`, `NavHost`, string routes).
- `navigation3-runtime` provides `NavBackStack`, `NavKey`, `rememberNavBackStack` — in `dependencies {}`.
- `navigation3-ui` provides `NavDisplay` — in `dependencies {}` (Android-only module, fine here).
- `FloorPlan` navigates to a **placeholder** composable for now (the floor plan screen is Phase 2).

---

## Subtasks & Detailed Guidance

### Subtask T022 — Update composeApp/build.gradle.kts

**Purpose**: Add navigation3, feature:auth, core:network, and koin-compose dependencies so the app can use nav keys, view models, and composable injection.

**Steps**:
1. Open `composeApp/build.gradle.kts`. Currently has:
   ```kotlin
   dependencies {
       implementation(projects.core.common)
       implementation(projects.shared)
       implementation(libs.koin.android)
   }
   ```
2. Extend `dependencies {}`:
   ```kotlin
   dependencies {
       implementation(projects.core.common)
       implementation(projects.core.network)
       implementation(projects.feature.auth)
       implementation(projects.shared)
       implementation(libs.koin.android)
       implementation(libs.koin.compose)
       implementation(libs.navigation3.runtime)
       implementation(libs.navigation3.ui)
       implementation(libs.compose.ui)
       implementation(libs.compose.material3)
       implementation(libs.compose.foundation)
       implementation(libs.compose.runtime)
   }
   ```
3. Verify that `libs.compose.*` entries are already in the catalog under the `compose-ui` bundle (they are — see `gradle/libs.versions.toml`). Reference them individually or via `libs.bundles.compose.ui`.

**Files**: `composeApp/build.gradle.kts`

---

### Subtask T023 — Create AppNavKey sealed interface

**Purpose**: Define screen identity types for the navigation back stack. Each destination is a `@Serializable data object` so Navigation3 can serialize/restore them.

**Steps**:
1. Create `composeApp/src/main/kotlin/com/vibely/navigation/AppNavKey.kt`:
   ```kotlin
   package com.vibely.navigation

   import androidx.navigation3.runtime.NavKey
   import kotlinx.serialization.Serializable

   /**
    * Type-safe navigation destination identities for Vibely POS.
    * Each object represents one screen in the app's back stack.
    */
   @Serializable
   sealed interface AppNavKey : NavKey {

       /** The login form — shown when the user has no valid session. */
       @Serializable
       data object Login : AppNavKey

       /**
        * The table floor plan — the primary post-login destination.
        * The actual content is implemented in Phase 2.
        */
       @Serializable
       data object FloorPlan : AppNavKey
   }
   ```

**Files**: `composeApp/src/main/kotlin/com/vibely/navigation/AppNavKey.kt`

**Notes**: `NavKey` is from `androidx.navigation3:navigation3-runtime`. `@Serializable` is from `kotlinx-serialization-core` (transitive via `navigation3-runtime`).

---

### Subtask T024 — Implement LoginViewModel

**Purpose**: Hold login form state and coordinate the login action. Exposed as a `StateFlow<LoginUiState>` consumed by `LoginScreen`.

**Steps**:
1. Create `composeApp/src/main/kotlin/com/vibely/feature/auth/LoginViewModel.kt`:
   ```kotlin
   package com.vibely.feature.auth

   import androidx.lifecycle.ViewModel
   import androidx.lifecycle.viewModelScope
   import com.vibely.feature.auth.domain.model.Credentials
   import com.vibely.feature.auth.usecase.LoginUseCase
   import com.vibely.navigation.AppNavKey
   import kotlinx.coroutines.flow.MutableSharedFlow
   import kotlinx.coroutines.flow.MutableStateFlow
   import kotlinx.coroutines.flow.StateFlow
   import kotlinx.coroutines.flow.asSharedFlow
   import kotlinx.coroutines.flow.asStateFlow
   import kotlinx.coroutines.flow.update
   import kotlinx.coroutines.launch

   data class LoginUiState(
       val email: String = "",
       val password: String = "",
       val isLoading: Boolean = false,
       val errorMessage: String? = null,
   ) {
       val isSubmitEnabled: Boolean
           get() = email.isNotBlank() && password.isNotBlank() && !isLoading
   }

   /**
    * ViewModel for the login screen.
    * Exposes [uiState] for the UI and [navigationEvents] for one-shot navigation.
    */
   class LoginViewModel(
       private val loginUseCase: LoginUseCase,
   ) : ViewModel() {

       private val _uiState = MutableStateFlow(LoginUiState())
       val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

       private val _navigationEvents = MutableSharedFlow<AppNavKey>()
       val navigationEvents = _navigationEvents.asSharedFlow()

       fun onEmailChange(email: String) {
           _uiState.update { it.copy(email = email, errorMessage = null) }
       }

       fun onPasswordChange(password: String) {
           _uiState.update { it.copy(password = password, errorMessage = null) }
       }

       fun onSignIn() {
           val state = _uiState.value
           if (!state.isSubmitEnabled) return
           viewModelScope.launch {
               _uiState.update { it.copy(isLoading = true, errorMessage = null) }
               val result = loginUseCase(Credentials(state.email, state.password))
               result
                   .onSuccess { _navigationEvents.emit(AppNavKey.FloorPlan) }
                   .onFailure { error ->
                       _uiState.update {
                           it.copy(
                               isLoading = false,
                               errorMessage = error.message ?: "Sign in failed. Please try again.",
                           )
                       }
                   }
           }
       }
   }
   ```

2. Register in Koin (as part of T028 update, or inline here):
   ```kotlin
   // In authFeatureModule or as part of composeApp Koin setup
   viewModel { LoginViewModel(get()) }
   ```

**Files**: `composeApp/src/main/kotlin/com/vibely/feature/auth/LoginViewModel.kt`

**Parallel?**: Yes — can proceed alongside T025 once T023 is done.

**Notes**: `ViewModel` from `androidx.lifecycle:lifecycle-viewmodel-ktx` — add to `composeApp` deps if not already present (check `koin-compose` — it may pull it transitively). `navigationEvents` as `SharedFlow` keeps navigation one-shot and avoids re-emission on recomposition.

---

### Subtask T025 — Implement LoginScreen composable

**Purpose**: The login form UI. Collects email and password, shows loading state during sign-in, and displays human-readable errors on failure.

**Steps**:
1. Create `composeApp/src/main/kotlin/com/vibely/feature/auth/LoginScreen.kt`:
   ```kotlin
   package com.vibely.feature.auth

   import androidx.compose.foundation.layout.*
   import androidx.compose.material3.*
   import androidx.compose.runtime.*
   import androidx.compose.ui.Alignment
   import androidx.compose.ui.Modifier
   import androidx.compose.ui.text.input.PasswordVisualTransformation
   import androidx.compose.ui.unit.dp
   import org.koin.compose.viewmodel.koinViewModel

   /**
    * Login screen shown when the user has no valid session.
    *
    * @param onNavigate Called with the next [AppNavKey] when login succeeds.
    */
   @Composable
   fun LoginScreen(
       onNavigate: (com.vibely.navigation.AppNavKey) -> Unit,
       viewModel: LoginViewModel = koinViewModel(),
   ) {
       val state by viewModel.uiState.collectAsState()

       // Consume navigation events without re-triggering on recomposition
       LaunchedEffect(viewModel) {
           viewModel.navigationEvents.collect { destination ->
               onNavigate(destination)
           }
       }

       Column(
           modifier = Modifier
               .fillMaxSize()
               .padding(horizontal = 32.dp),
           verticalArrangement = Arrangement.Center,
           horizontalAlignment = Alignment.CenterHorizontally,
       ) {
           Text(
               text = "Vibely POS",
               style = MaterialTheme.typography.headlineMedium,
               modifier = Modifier.padding(bottom = 32.dp),
           )

           OutlinedTextField(
               value = state.email,
               onValueChange = viewModel::onEmailChange,
               label = { Text("Email") },
               singleLine = true,
               modifier = Modifier.fillMaxWidth(),
           )

           Spacer(modifier = Modifier.height(12.dp))

           OutlinedTextField(
               value = state.password,
               onValueChange = viewModel::onPasswordChange,
               label = { Text("Password") },
               singleLine = true,
               visualTransformation = PasswordVisualTransformation(),
               modifier = Modifier.fillMaxWidth(),
           )

           Spacer(modifier = Modifier.height(24.dp))

           Button(
               onClick = viewModel::onSignIn,
               enabled = state.isSubmitEnabled,
               modifier = Modifier.fillMaxWidth(),
           ) {
               if (state.isLoading) {
                   CircularProgressIndicator(
                       modifier = Modifier.size(18.dp),
                       strokeWidth = 2.dp,
                       color = MaterialTheme.colorScheme.onPrimary,
                   )
               } else {
                   Text("Sign In")
               }
           }

           state.errorMessage?.let { message ->
               Spacer(modifier = Modifier.height(12.dp))
               Text(
                   text = message,
                   color = MaterialTheme.colorScheme.error,
                   style = MaterialTheme.typography.bodyMedium,
               )
           }
       }
   }
   ```

**Files**: `composeApp/src/main/kotlin/com/vibely/feature/auth/LoginScreen.kt`

**Parallel?**: Yes — can proceed alongside T024 once T023 is done.

---

### Subtask T026 — Implement AppNavigation composable (startup routing)

**Purpose**: The root composable that owns the `NavBackStack`. On first composition it validates the stored token (and attempts a silent refresh if expired) before deciding whether to show `LoginScreen` or `FloorPlanScreen`. A loading indicator is shown during this check.

**Steps**:
1. Create `composeApp/src/main/kotlin/com/vibely/navigation/AppNavigation.kt`:
   ```kotlin
   package com.vibely.navigation

   import androidx.compose.foundation.layout.Box
   import androidx.compose.foundation.layout.fillMaxSize
   import androidx.compose.material3.CircularProgressIndicator
   import androidx.compose.runtime.*
   import androidx.compose.ui.Alignment
   import androidx.compose.ui.Modifier
   import androidx.navigation3.runtime.rememberNavBackStack
   import androidx.navigation3.ui.NavDisplay
   import com.vibely.feature.auth.LoginScreen
   import com.vibely.feature.auth.usecase.RefreshTokenUseCase
   import com.vibely.feature.auth.usecase.ValidateTokenUseCase
   import com.vibely.feature.auth.storage.TokenStorage
   import org.koin.compose.koinInject

   /**
    * Root composable that owns the [NavBackStack] and handles startup token routing.
    *
    * On cold start:
    * 1. Retrieve the stored token.
    * 2. Validate it → on success, navigate to [AppNavKey.FloorPlan].
    * 3. On failure (expired), attempt a silent refresh → on success, navigate to FloorPlan.
    * 4. On refresh failure, clear stored token → navigate to [AppNavKey.Login].
    *
    * A loading indicator is shown while the check is in progress (FR-013).
    */
   @Composable
   fun AppNavigation(
       tokenStorage: TokenStorage = koinInject(),
       validateToken: ValidateTokenUseCase = koinInject(),
       refreshToken: RefreshTokenUseCase = koinInject(),
   ) {
       var startDestination: AppNavKey? by remember { mutableStateOf(null) }

       LaunchedEffect(Unit) {
           val stored = tokenStorage.getToken()
           startDestination = when {
               stored == null -> AppNavKey.Login
               else -> {
                   validateToken(stored.accessToken)
                       .fold(
                           onSuccess = { AppNavKey.FloorPlan },
                           onFailure = {
                               // Validation failed — attempt silent refresh
                               refreshToken(stored.refreshToken)
                                   .fold(
                                       onSuccess = { AppNavKey.FloorPlan },
                                       onFailure = {
                                           tokenStorage.clearToken()
                                           AppNavKey.Login
                                       },
                                   )
                           },
                       )
               }
           }
       }

       val destination = startDestination
       if (destination == null) {
           // Token check in progress — show loading indicator
           Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
               CircularProgressIndicator()
           }
           return
       }

       val backStack = rememberNavBackStack(destination)

       NavDisplay(
           backStack = backStack,
           onBack = { backStack.removeLastOrNull() },
       ) { key ->
           when (key) {
               AppNavKey.Login -> LoginScreen(
                   onNavigate = { nextKey ->
                       // Clear login from back stack before navigating to floor plan (FR-018)
                       backStack.clear()
                       backStack.add(nextKey)
                   },
               )
               AppNavKey.FloorPlan -> FloorPlanPlaceholder()
           }
       }
   }

   /** Temporary placeholder for the floor plan screen (Phase 2). */
   @Composable
   private fun FloorPlanPlaceholder() {
       Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
           androidx.compose.material3.Text("Floor Plan — coming in Phase 2")
       }
   }
   ```

**Files**: `composeApp/src/main/kotlin/com/vibely/navigation/AppNavigation.kt`

**Notes**:
- `NavDisplay` trailing lambda signature: `content: @Composable (key: AppNavKey) -> Unit`
- `backStack.clear()` then `backStack.add(AppNavKey.FloorPlan)` ensures Login is removed from the stack before navigating (FR-018)
- `LaunchedEffect(Unit)` fires once per composition; `startDestination` being `null` guards the loading state (FR-013)

---

### Subtask T027 — Create MainActivity with NavDisplay wiring

**Purpose**: The Android entry-point Activity. Sets the Compose content to `AppNavigation`, applies the Material3 theme.

**Steps**:
1. Create `composeApp/src/main/kotlin/com/vibely/MainActivity.kt`:
   ```kotlin
   package com.vibely

   import android.os.Bundle
   import androidx.activity.ComponentActivity
   import androidx.activity.compose.setContent
   import androidx.activity.enableEdgeToEdge
   import androidx.compose.material3.MaterialTheme
   import androidx.compose.material3.Surface
   import com.vibely.navigation.AppNavigation

   /**
    * Single-activity entry point for Vibely POS.
    * Hosts the Compose content and delegates all navigation to [AppNavigation].
    */
   class MainActivity : ComponentActivity() {
       override fun onCreate(savedInstanceState: Bundle?) {
           super.onCreate(savedInstanceState)
           enableEdgeToEdge()
           setContent {
               MaterialTheme {
                   Surface {
                       AppNavigation()
                   }
               }
           }
       }
   }
   ```

2. Register `MainActivity` in `composeApp/src/main/AndroidManifest.xml`. If the manifest doesn't exist yet, create it:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <manifest xmlns:android="http://schemas.android.com/apk/res/android">
       <application
           android:name=".VibelyApp"
           android:label="Vibely POS"
           android:theme="@style/Theme.AppCompat">
           <activity
               android:name=".MainActivity"
               android:exported="true">
               <intent-filter>
                   <action android:name="android.intent.action.MAIN" />
                   <category android:name="android.intent.category.LAUNCHER" />
               </intent-filter>
           </activity>
       </application>
   </manifest>
   ```

**Files**:
- `composeApp/src/main/kotlin/com/vibely/MainActivity.kt`
- `composeApp/src/main/AndroidManifest.xml`

**Notes**: Add `androidx.activity:activity-compose` to `composeApp/build.gradle.kts` if it's not already a transitive dep. Check after first build.

---

### Subtask T028 — Update VibelyApp.kt Koin module registration

**Purpose**: Register `authModule`, `authPlatformModule`, and `networkModule` in the application's Koin startup so all auth dependencies are available at runtime.

**Steps**:
1. Open `composeApp/src/main/kotlin/com/vibely/VibelyApp.kt` (currently exists with `commonModule` and `platformModule`).
2. Add imports and register the new modules:
   ```kotlin
   import com.vibely.core.network.di.networkModule
   import com.vibely.feature.auth.di.authModule
   import com.vibely.feature.auth.di.authPlatformModule

   class VibelyApp : Application() {
       override fun onCreate() {
           super.onCreate()
           ApplicationContextHolder.init(this)
           startKoin {
               androidContext(this@VibelyApp)
               modules(
                   commonModule(),
                   platformModule(),
                   networkModule(),
                   authModule(),
                   authPlatformModule(),
               )
           }
       }
   }
   ```

**Files**: `composeApp/src/main/kotlin/com/vibely/VibelyApp.kt`

**Notes**: Order matters for Koin — `networkModule()` must come before `authModule()` since `authModule` depends on `AuthApiClient` from `networkModule`.

---

## Risks & Mitigations

- **Navigation3 API confusion with Navigation Compose 2**: Always use `rememberNavBackStack`, `NavBackStack`, `NavDisplay`. Never use `rememberNavController`, `NavController`, `NavHost`, string routes.
- **`NavDisplay` content lambda signature**: In Navigation3 1.0.1 the `content` parameter receives the current key and provides a composable. Verify the exact signature from the dependency source if the IDE shows an error.
- **`backStack.clear()`**: Check the Navigation3 1.0.1 API — the method may be `clear()`, `removeAll()`, or require a loop. If `clear()` isn't available, use `while (backStack.isNotEmpty()) backStack.removeLastOrNull()`.
- **Missing `AndroidManifest.xml`**: If `composeApp` was set up without a manifest (only `VibelyApp.kt` exists), the manifest must be created. Check if an existing manifest is present at `composeApp/src/main/AndroidManifest.xml`.
- **`LoginViewModel` + `viewModel {}`**: Koin `viewModel { }` DSL requires `koin-android` or `koin-compose`. Both are present in the deps.

## Review Guidance

- Install and launch the debug APK (`AUTH_MODE=debug`): app opens directly to floor plan placeholder
- Install production APK (no `AUTH_MODE` env): cold start shows Login screen; enter credentials → floor plan
- Pressing back from floor plan should exit the app (not return to login) — back stack correctly cleared
- Loading spinner visible during startup token check (even briefly with a fast network)
- Sign In button disabled while email/password fields are empty
- Error message shown below Sign In button on failed login (human-readable, not stack trace)
- KDoc on `AppNavKey`, `AppNavigation`, `LoginViewModel`, `LoginScreen`, `MainActivity`

## Activity Log

- 2026-03-26T01:25:51Z – system – lane=planned – Prompt created.
- 2026-03-26T03:08:01Z – claude-sonnet-4-6 – shell_pid=3036 – lane=doing – Assigned agent via workflow command
- 2026-03-26T03:18:47Z – claude-sonnet-4-6 – shell_pid=3036 – lane=for_review – Ready for review: Login UI + Navigation3 routing complete. AppNavKey, LoginViewModel, LoginScreen, AppNavigation, MainActivity all implemented. VibelyApp updated with networkModule/authModule/authPlatformModule. ktlintCheck passes.
- 2026-03-26T03:19:33Z – claude-sonnet-4-6 – shell_pid=10511 – lane=doing – Started review via workflow command
- 2026-03-26T03:22:28Z – claude-sonnet-4-6 – shell_pid=10511 – lane=planned – Moved to planned
- 2026-03-26T03:24:51Z – claude-sonnet-4-6 – shell_pid=14063 – lane=doing – Started implementation via workflow command
