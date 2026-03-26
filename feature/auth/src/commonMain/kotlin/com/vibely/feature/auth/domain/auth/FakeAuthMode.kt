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
 *
 * Lives in `commonMain` (not a test source set) so it can be used from any context
 * without test-framework dependencies (NFR-003).
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

    override suspend fun refreshToken(refreshToken: String): Result<AuthToken> = refreshTokenResult

    override suspend fun validateToken(accessToken: String): Result<User> = validateTokenResult

    override suspend fun logout(accessToken: String): Result<Unit> = logoutResult
}
