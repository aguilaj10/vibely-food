package com.vibely.feature.auth.storage

import com.vibely.feature.auth.domain.model.AuthToken

/**
 * Platform-appropriate encrypted storage for authentication tokens.
 *
 * All operations are suspend and must run on [kotlinx.coroutines.Dispatchers.IO].
 */
interface TokenStorage {
    /** Persist [token] to encrypted platform storage. */
    suspend fun saveToken(token: AuthToken)

    /** Retrieve the stored [AuthToken], or null if none exists. */
    suspend fun getToken(): AuthToken?

    /** Remove all stored token data. */
    suspend fun clearToken()
}
