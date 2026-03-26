package com.vibely.feature.auth.storage

import com.vibely.feature.auth.domain.model.AuthToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Instant

private const val KEYSTORE_FILE = ".vibely/token.ks"
private const val ENTRY_ACCESS = "access_token"
private const val ENTRY_REFRESH = "refresh_token"
private const val ENTRY_EXPIRY = "expires_at"

// TODO: replace with machine-derived or env-provided password for production
private val KEYSTORE_PASSWORD = "vibely-dev-only".toCharArray()

/**
 * JVM token storage backed by a PKCS12 [KeyStore] file in the user home directory.
 */
class Pkcs12KeystoreTokenStorage : TokenStorage {
    private val keystoreFile: File
        get() = File(System.getProperty("user.home"), KEYSTORE_FILE)

    private fun loadOrCreate(): KeyStore {
        val ks = KeyStore.getInstance("PKCS12")
        if (keystoreFile.exists()) {
            keystoreFile.inputStream().use { ks.load(it, KEYSTORE_PASSWORD) }
        } else {
            ks.load(null, KEYSTORE_PASSWORD)
        }
        return ks
    }

    private fun KeyStore.storeEntry(
        alias: String,
        value: String
    ) {
        setEntry(
            alias,
            KeyStore.SecretKeyEntry(SecretKeySpec(value.toByteArray(), "AES")),
            KeyStore.PasswordProtection(KEYSTORE_PASSWORD),
        )
    }

    private fun KeyStore.loadEntry(alias: String): String? {
        val entry =
            getEntry(alias, KeyStore.PasswordProtection(KEYSTORE_PASSWORD))
                as? KeyStore.SecretKeyEntry ?: return null
        return String(entry.secretKey.encoded)
    }

    private fun persist(ks: KeyStore) {
        keystoreFile.parentFile?.mkdirs()
        keystoreFile.outputStream().use { ks.store(it, KEYSTORE_PASSWORD) }
    }

    override suspend fun saveToken(token: AuthToken) =
        withContext(Dispatchers.IO) {
            val ks = loadOrCreate()
            ks.storeEntry(ENTRY_ACCESS, token.accessToken)
            ks.storeEntry(ENTRY_REFRESH, token.refreshToken)
            ks.storeEntry(ENTRY_EXPIRY, token.expiresAt.toString())
            persist(ks)
        }

    override suspend fun getToken(): AuthToken? =
        withContext(Dispatchers.IO) {
            if (!keystoreFile.exists()) return@withContext null
            val ks = loadOrCreate()
            val access = ks.loadEntry(ENTRY_ACCESS) ?: return@withContext null
            val refresh = ks.loadEntry(ENTRY_REFRESH) ?: return@withContext null
            val expiry = ks.loadEntry(ENTRY_EXPIRY) ?: return@withContext null
            AuthToken(access, refresh, Instant.parse(expiry))
        }

    override suspend fun clearToken() =
        withContext(Dispatchers.IO) {
            keystoreFile.delete()
            Unit
        }
}
