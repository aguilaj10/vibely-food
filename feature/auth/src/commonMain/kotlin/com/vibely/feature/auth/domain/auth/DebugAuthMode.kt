package com.vibely.feature.auth.domain.auth

import com.vibely.domain.staff.Role
import com.vibely.domain.tenant.StoreId
import com.vibely.domain.tenant.UserId
import com.vibely.feature.auth.domain.model.AuthToken
import com.vibely.feature.auth.domain.model.Credentials
import com.vibely.feature.auth.domain.model.User
import kotlin.time.Instant

private val DEBUG_USER =
    User(
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
    override suspend fun authenticate(credentials: Credentials): Result<AuthToken> = Result.success(debugToken())

    override suspend fun refreshToken(refreshToken: String): Result<AuthToken> = Result.success(debugToken())

    override suspend fun validateToken(accessToken: String): Result<User> = Result.success(DEBUG_USER)

    override suspend fun logout(accessToken: String): Result<Unit> = Result.success(Unit)

    private fun debugToken() =
        AuthToken(
            accessToken = "debug-access-token",
            refreshToken = "debug-refresh-token",
            // Far-future expiry — debug tokens never expire
            expiresAt = Instant.parse("2099-12-31T00:00:00Z"),
        )
}
