package com.vibely.common

/**
 * Placeholder constants for API client configuration.
 *
 * [API_VERSION] is the only known value. All other values are `TODO()` placeholders
 * that will be replaced when the network layer is built.
 */
object ApiConstants {
    /** Base URL of the Vibely API. Source: environment configuration at runtime. */
    val BASE_URL: String
        get() = TODO("Provide via environment config — e.g. https://api.vibely.app")

    /** API version prefix appended to all request paths. Known value: "v1". */
    const val API_VERSION: String = "v1"

    /** Maximum time (ms) to wait for an API response before the request is cancelled. Source: UX/SLA requirements. */
    val TIMEOUT_MS: Long
        get() = TODO("Determine from UX requirements — typically 30_000L")

    /** Maximum number of retry attempts for transient failures. Source: reliability requirements. */
    val MAX_RETRIES: Int
        get() = TODO("Determine from reliability requirements — typically 3")

    /** Base backoff interval (ms) between retry attempts. Source: reliability requirements. */
    val RETRY_BACKOFF_MS: Long
        get() = TODO("Determine from reliability requirements — typically 1_000L with exponential multiplier")
}
