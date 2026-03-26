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
