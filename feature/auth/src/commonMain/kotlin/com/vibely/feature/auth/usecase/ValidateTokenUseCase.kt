package com.vibely.feature.auth.usecase

import com.vibely.feature.auth.domain.auth.AuthMode
import com.vibely.feature.auth.domain.model.User

/** Validates the current access token and returns the authenticated [User]. */
class ValidateTokenUseCase(
    private val authMode: AuthMode
) {
    suspend operator fun invoke(accessToken: String): Result<User> = authMode.validateToken(accessToken)
}
