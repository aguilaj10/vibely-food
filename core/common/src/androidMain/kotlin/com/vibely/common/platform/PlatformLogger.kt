package com.vibely.common.platform

import android.util.Log

/**
 * Android implementation of [PlatformLogger] backed by [android.util.Log].
 */
actual class PlatformLogger actual constructor() {
    /** Logs a debug-level message via [Log.d]. */
    actual fun debug(
        tag: String,
        message: String
    ) {
        Log.d(tag, message)
    }

    /** Logs an info-level message via [Log.i]. */
    actual fun info(
        tag: String,
        message: String
    ) {
        Log.i(tag, message)
    }

    /** Logs a warning-level message via [Log.w]. */
    actual fun warn(
        tag: String,
        message: String
    ) {
        Log.w(tag, message)
    }

    /** Logs an error-level message via [Log.e], including optional [throwable]. */
    actual fun error(
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        Log.e(tag, message, throwable)
    }
}
