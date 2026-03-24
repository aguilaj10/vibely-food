package com.vibely.common.platform

/**
 * Platform-aware key-value storage for sensitive values (tokens, session keys).
 * Shared code depends only on this contract; platform implementations vary.
 */
expect class SecureStorage() {
    /** Saves [value] under [key], replacing any existing entry. */
    fun save(
        key: String,
        value: String
    )

    /** Returns the value stored under [key], or null if absent. */
    fun get(key: String): String?

    /** Removes the entry for [key]. No-op if absent. */
    fun delete(key: String)

    /** Removes all stored entries. */
    fun clear()
}
