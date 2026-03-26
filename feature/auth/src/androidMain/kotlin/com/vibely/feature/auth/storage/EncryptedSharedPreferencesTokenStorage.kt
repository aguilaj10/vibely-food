package com.vibely.feature.auth.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vibely.feature.auth.domain.model.AuthToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Instant

private const val PREFS_FILE = "vibely_auth_token"
private const val KEY_ACCESS = "access_token"
private const val KEY_REFRESH = "refresh_token"
private const val KEY_EXPIRY = "expires_at"

/**
 * Android token storage backed by [EncryptedSharedPreferences] (AES-256-GCM).
 */
class EncryptedSharedPreferencesTokenStorage(
    private val context: Context,
) : TokenStorage {
    private val prefs by lazy {
        val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun saveToken(token: AuthToken) =
        withContext(Dispatchers.IO) {
            prefs
                .edit()
                .putString(KEY_ACCESS, token.accessToken)
                .putString(KEY_REFRESH, token.refreshToken)
                .putString(KEY_EXPIRY, token.expiresAt.toString())
                .apply()
        }

    override suspend fun getToken(): AuthToken? =
        withContext(Dispatchers.IO) {
            val access = prefs.getString(KEY_ACCESS, null) ?: return@withContext null
            val refresh = prefs.getString(KEY_REFRESH, null) ?: return@withContext null
            val expiry = prefs.getString(KEY_EXPIRY, null) ?: return@withContext null
            AuthToken(
                accessToken = access,
                refreshToken = refresh,
                expiresAt = Instant.parse(expiry),
            )
        }

    override suspend fun clearToken() =
        withContext(Dispatchers.IO) {
            prefs
                .edit()
                .remove(KEY_ACCESS)
                .remove(KEY_REFRESH)
                .remove(KEY_EXPIRY)
                .apply()
        }
}
