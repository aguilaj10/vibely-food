package com.vibely.common

/**
 * Placeholder constants for database connection pool configuration.
 *
 * All values are `TODO()` placeholders. Actual values will be provided by
 * environment-driven configuration when the infrastructure layer is built.
 */
object DatabaseConstants {
    /** Maximum number of connections in the pool. Source: infrastructure config. */
    val MAX_POOL_SIZE: Int
        get() = TODO("Determine from load testing — typically 10–20 for a single-store POS")

    /** Minimum number of idle connections maintained in the pool. Source: infrastructure config. */
    val MIN_IDLE: Int
        get() = TODO("Determine from baseline load — typically 2–5")

    /** Maximum time (ms) to wait for a connection from the pool before throwing. Source: infrastructure config. */
    val CONNECTION_TIMEOUT_MS: Long
        get() = TODO("Determine from SLA requirements — typically 30_000L")

    /** Maximum time (ms) a connection may sit idle before being evicted. Source: infrastructure config. */
    val IDLE_TIMEOUT_MS: Long
        get() = TODO("Determine from DB server keepalive settings — typically 600_000L")

    /** Maximum lifetime (ms) of a connection in the pool before it is retired. Source: infrastructure config. */
    val MAX_LIFETIME_MS: Long
        get() = TODO("Typically slightly less than DB server wait_timeout — e.g. 1_800_000L")

    /** Number of prepared statements cached per connection. Source: infrastructure config. */
    val PREPARED_STATEMENT_CACHE_SIZE: Int
        get() = TODO("Determine from query diversity analysis — typically 250")
}
