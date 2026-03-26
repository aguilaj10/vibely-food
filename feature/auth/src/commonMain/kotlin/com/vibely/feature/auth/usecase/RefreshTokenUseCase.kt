package com.vibely.feature.auth.usecase

import com.vibely.feature.auth.domain.auth.AuthMode
import com.vibely.feature.auth.domain.model.AuthToken

/** Refreshes the current session using the stored refresh token. */
class RefreshTokenUseCase(
    private val authMode: AuthMode
) {
    suspend operator fun invoke(refreshToken: String): Result<AuthToken> = authMode.refreshToken(refreshToken)
}
