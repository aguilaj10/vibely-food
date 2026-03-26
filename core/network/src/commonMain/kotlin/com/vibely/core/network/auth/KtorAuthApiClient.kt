package com.vibely.core.network.auth

import com.vibely.core.network.auth.dto.LoginRequest
import com.vibely.core.network.auth.dto.RefreshRequest
import com.vibely.core.network.auth.dto.TokenResponse
import com.vibely.core.network.auth.dto.ValidateResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Ktor-backed implementation of [AuthApiClient].
 *
 * @param client The shared [HttpClient] instance from Koin.
 * @param baseUrl The auth backend base URL (e.g., "https://api.vibely.com").
 */
class KtorAuthApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
) : AuthApiClient {
    override suspend fun login(request: LoginRequest): Result<TokenResponse> =
        runCatching {
            client
                .post("$baseUrl/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
        }

    override suspend fun refresh(request: RefreshRequest): Result<TokenResponse> =
        runCatching {
            client
                .post("$baseUrl/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
        }

    override suspend fun validate(accessToken: String): Result<ValidateResponse> =
        runCatching {
            client
                .post("$baseUrl/auth/validate") {
                    bearerAuth(accessToken)
                }.body()
        }

    override suspend fun logout(accessToken: String): Result<Unit> =
        runCatching {
            client.post("$baseUrl/auth/logout") {
                bearerAuth(accessToken)
            }
            Unit
        }
}
