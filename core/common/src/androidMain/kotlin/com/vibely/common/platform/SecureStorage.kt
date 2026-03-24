package com.vibely.common.platform

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Android implementation of [SecureStorage] backed by [EncryptedSharedPreferences]
 * and the Android Keystore system.
 *
 * The application [android.content.Context] is sourced from [ApplicationContextHolder],
 * which must be initialised in `VibelyApp.onCreate` before this class is first accessed.
 */
actual class SecureStorage actual constructor() {
    private val prefs by lazy {
        val context = ApplicationContextHolder.context
        val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        EncryptedSharedPreferences.create(
            context,
            "vibely_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Saves [value] under [key], replacing any existing entry. */
    actual fun save(
        key: String,
        value: String
    ) {
        prefs.edit().putString(key, value).apply()
    }

    /** Returns the value stored under [key], or null if absent. */
    actual fun get(key: String): String? = prefs.getString(key, null)

    /** Removes the entry for [key]. No-op if absent. */
    actual fun delete(key: String) {
        prefs.edit().remove(key).apply()
    }

    /** Removes all stored entries. */
    actual fun clear() {
        prefs.edit().clear().apply()
    }
}
