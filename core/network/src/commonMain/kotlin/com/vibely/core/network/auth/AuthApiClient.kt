package com.vibely.core.network.auth

import com.vibely.core.network.auth.dto.LoginRequest
import com.vibely.core.network.auth.dto.RefreshRequest
import com.vibely.core.network.auth.dto.TokenResponse
import com.vibely.core.network.auth.dto.ValidateResponse

/**
 * HTTP contract for the Vibely authentication backend.
 * All operations return [Result] — never throw.
 */
interface AuthApiClient {
    suspend fun login(request: LoginRequest): Result<TokenResponse>

    suspend fun refresh(request: RefreshRequest): Result<TokenResponse>

    suspend fun validate(accessToken: String): Result<ValidateResponse>

    suspend fun logout(accessToken: String): Result<Unit>
}
