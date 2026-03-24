package com.vibely.common.platform

import android.content.Context

/**
 * Holds the application [Context] for use by platform-specific implementations
 * that cannot receive [Context] through their constructor signature.
 *
 * Must be initialised in `VibelyApp.onCreate` before Koin starts, via [init].
 */
object ApplicationContextHolder {
    /** The application context. Throws [UninitializedPropertyAccessException] if not yet initialised. */
    lateinit var context: Context
        private set

    /**
     * Initialises the holder with the application context.
     * Call once from [android.app.Application.onCreate] before starting Koin.
     */
    fun init(context: Context) {
        this.context = context.applicationContext
    }
}
