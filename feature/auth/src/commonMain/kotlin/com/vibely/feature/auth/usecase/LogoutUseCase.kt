package com.vibely.feature.auth.usecase

import com.vibely.feature.auth.domain.auth.AuthMode

/** Logs out the currently authenticated staff member. */
class LogoutUseCase(
    private val authMode: AuthMode
) {
    suspend operator fun invoke(accessToken: String): Result<Unit> = authMode.logout(accessToken)
}
