package com.vibely.common.platform

/**
 * JVM implementation of [PlatformLogger] backed by stdout with level prefixes.
 */
actual class PlatformLogger actual constructor() {
    /** Logs a debug-level message to stdout. */
    actual fun debug(
        tag: String,
        message: String
    ) = println("[DEBUG] [$tag] $message")

    /** Logs an info-level message to stdout. */
    actual fun info(
        tag: String,
        message: String
    ) = println("[INFO]  [$tag] $message")

    /** Logs a warning-level message to stdout. */
    actual fun warn(
        tag: String,
        message: String
    ) = println("[WARN]  [$tag] $message")

    /** Logs an error-level message to stdout, printing the [throwable] stack trace if present. */
    actual fun error(
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        println("[ERROR] [$tag] $message")
        throwable?.printStackTrace()
    }
}
