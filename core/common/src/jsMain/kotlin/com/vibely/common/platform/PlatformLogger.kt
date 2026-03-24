package com.vibely.common.platform

/**
 * Web/JS implementation of [PlatformLogger] backed by the browser console.
 */
actual class PlatformLogger actual constructor() {
    /** Logs a debug-level message via [console.log]. */
    actual fun debug(
        tag: String,
        message: String
    ) = console.log("[$tag] $message")

    /** Logs an info-level message via [console.log]. */
    actual fun info(
        tag: String,
        message: String
    ) = console.log("[$tag] $message")

    /** Logs a warning-level message via [console.warn]. */
    actual fun warn(
        tag: String,
        message: String
    ) = console.warn("[$tag] $message")

    /** Logs an error-level message via [console.error], appending [throwable] message if present. */
    actual fun error(
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        console.error("[$tag] $message${throwable?.let { ": $it" } ?: ""}")
    }
}
