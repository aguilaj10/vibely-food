package com.vibely.feature.auth.usecase

import com.vibely.feature.auth.domain.auth.AuthMode
import com.vibely.feature.auth.domain.model.AuthToken
import com.vibely.feature.auth.domain.model.Credentials

/** Authenticates a staff member using the provided [Credentials]. */
class LoginUseCase(
    private val authMode: AuthMode
) {
    suspend operator fun invoke(credentials: Credentials): Result<AuthToken> = authMode.authenticate(credentials)
}
