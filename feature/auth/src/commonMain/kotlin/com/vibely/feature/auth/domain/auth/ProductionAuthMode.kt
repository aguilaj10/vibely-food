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
import kotlin.time.Instant

/**
 * Production [AuthMode] — delegates to the auth backend via [AuthApiClient].
 * Persists the token on successful login/refresh; clears it on logout.
 */
class ProductionAuthMode(
    private val apiClient: AuthApiClient,
    private val storage: TokenStorage,
) : AuthMode {
    override suspend fun authenticate(credentials: Credentials): Result<AuthToken> =
        apiClient
            .login(LoginRequest(credentials.email, credentials.password))
            .map { dto ->
                AuthToken(dto.accessToken, dto.refreshToken, Instant.parse(dto.expiresAt))
            }.onSuccess { token -> storage.saveToken(token) }

    override suspend fun refreshToken(refreshToken: String): Result<AuthToken> =
        apiClient
            .refresh(RefreshRequest(refreshToken))
            .map { dto ->
                AuthToken(dto.accessToken, dto.refreshToken, Instant.parse(dto.expiresAt))
            }.onSuccess { token -> storage.saveToken(token) }

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
