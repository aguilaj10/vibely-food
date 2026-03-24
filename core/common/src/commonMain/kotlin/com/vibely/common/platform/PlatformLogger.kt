package com.vibely.common.platform

/**
 * Platform-aware structured logger.
 * Shared code calls this instead of importing platform logging APIs directly.
 */
expect class PlatformLogger() {
    /** Logs a debug-level message with the given [tag]. */
    fun debug(
        tag: String,
        message: String
    )

    /** Logs an info-level message with the given [tag]. */
    fun info(
        tag: String,
        message: String
    )

    /** Logs a warning-level message with the given [tag]. */
    fun warn(
        tag: String,
        message: String
    )

    /** Logs an error-level message with the given [tag] and optional [throwable]. */
    fun error(
        tag: String,
        message: String,
        throwable: Throwable? = null
    )
}
