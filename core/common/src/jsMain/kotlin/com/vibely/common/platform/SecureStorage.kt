package com.vibely.common.platform

import kotlinx.browser.window

/**
 * Web/JS implementation of [SecureStorage] backed by [window.localStorage].
 * Suitable for this application's sensitivity level — no banking-sensitive data is stored.
 */
actual class SecureStorage actual constructor() {
    /** Saves [value] under [key] in [window.localStorage], replacing any existing entry. */
    actual fun save(
        key: String,
        value: String
    ) {
        window.localStorage.setItem(key, value)
    }

    /** Returns the value stored under [key] in [window.localStorage], or null if absent. */
    actual fun get(key: String): String? = window.localStorage.getItem(key)

    /** Removes the entry for [key] from [window.localStorage]. No-op if absent. */
    actual fun delete(key: String) {
        window.localStorage.removeItem(key)
    }

    /** Removes all entries from [window.localStorage]. */
    actual fun clear() {
        window.localStorage.clear()
    }
}
