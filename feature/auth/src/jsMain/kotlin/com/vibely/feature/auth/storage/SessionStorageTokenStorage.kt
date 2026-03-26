package com.vibely.feature.auth.storage

import com.vibely.feature.auth.domain.model.AuthToken
import kotlinx.browser.sessionStorage
import kotlinx.datetime.Instant

private const val KEY_ACCESS = "vibely_access_token"
private const val KEY_REFRESH = "vibely_refresh_token"
private const val KEY_EXPIRY = "vibely_expires_at"

/**
 * Web token storage backed by [sessionStorage].
 * Tokens are automatically cleared when the browser tab is closed.
 *
 * All functions are non-blocking — sessionStorage is synchronous in the browser.
 * The suspend modifier is kept for interface compatibility.
 */
class SessionStorageTokenStorage : TokenStorage {
    override suspend fun saveToken(token: AuthToken) {
        sessionStorage.setItem(KEY_ACCESS, token.accessToken)
        sessionStorage.setItem(KEY_REFRESH, token.refreshToken)
        sessionStorage.setItem(KEY_EXPIRY, token.expiresAt.toString())
    }

    override suspend fun getToken(): AuthToken? {
        val access = sessionStorage.getItem(KEY_ACCESS) ?: return null
        val refresh = sessionStorage.getItem(KEY_REFRESH) ?: return null
        val expiry = sessionStorage.getItem(KEY_EXPIRY) ?: return null
        return AuthToken(access, refresh, Instant.parse(expiry))
    }

    override suspend fun clearToken() {
        sessionStorage.removeItem(KEY_ACCESS)
        sessionStorage.removeItem(KEY_REFRESH)
        sessionStorage.removeItem(KEY_EXPIRY)
    }
}
