package com.vibely.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.vibely.feature.auth.LoginScreen
import com.vibely.feature.auth.storage.TokenStorage
import com.vibely.feature.auth.usecase.RefreshTokenUseCase
import com.vibely.feature.auth.usecase.ValidateTokenUseCase
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
            stored == null -> {
                // DebugAuthMode.validateToken succeeds regardless of token → FloorPlan.
                // ProductionAuthMode.validateToken("") fails (401) → Login (S1).
                validateToken("").fold(
                    onSuccess = { AppNavKey.FloorPlan },
                    onFailure = { AppNavKey.Login },
                )
            }
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
        // Token check in progress — show loading indicator (FR-013)
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
        Text("Floor Plan — coming in Phase 2")
    }
}
