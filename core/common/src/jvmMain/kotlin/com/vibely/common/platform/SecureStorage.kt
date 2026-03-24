package com.vibely.common.platform

import java.util.concurrent.ConcurrentHashMap

/**
 * JVM implementation of [SecureStorage] backed by an in-memory [ConcurrentHashMap].
 *
 * Data is NOT persisted across process restarts. Auth tokens for the JVM server
 * process are managed via environment variables at startup; this storage is used
 * for transient session-scoped values only.
 */
actual class SecureStorage actual constructor() {
    private val store = ConcurrentHashMap<String, String>()

    /** Saves [value] under [key], replacing any existing entry. */
    actual fun save(
        key: String,
        value: String
    ) {
        store[key] = value
    }

    /** Returns the value stored under [key], or null if absent. */
    actual fun get(key: String): String? = store[key]

    /** Removes the entry for [key]. No-op if absent. */
    actual fun delete(key: String) {
        store.remove(key)
    }

    /** Removes all stored entries. */
    actual fun clear() {
        store.clear()
    }
}
