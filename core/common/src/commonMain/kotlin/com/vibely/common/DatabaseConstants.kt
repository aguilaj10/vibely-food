package com.vibely.common

/**
 * Connection pool and database performance constants for HikariCP configuration.
 *
 * All values are sourced from the implementation plan and follow HikariCP best
 * practices for PostgreSQL with PgBouncer in session pooling mode.
 */
object DatabaseConstants {
    /**
     * Maximum connection pool size: 2x CPU count, minimum 10.
     *
     * Uses the formula: (core_count * 2) for SSD-backed servers (no spindle overhead).
     * Declared as `val` (not `const`) because [Runtime.availableProcessors] is a
     * runtime call.
     */
    val MAX_POOL_SIZE: Int = (Runtime.getRuntime().availableProcessors() * 2).coerceAtLeast(10)

    /** Minimum idle connections kept alive in the pool. */
    const val MIN_IDLE: Int = 10

    /** Maximum time (ms) to wait for a connection from the pool before throwing. */
    const val CONNECTION_TIMEOUT_MS: Long = 30_000L

    /** Maximum time (ms) a connection may remain idle in the pool before eviction. */
    const val IDLE_TIMEOUT_MS: Long = 600_000L

    /**
     * Maximum lifetime (ms) of a connection in the pool.
     *
     * Set to 30 minutes — slightly below PostgreSQL's default `wait_timeout` to
     * prevent connections from being killed mid-use.
     */
    const val MAX_LIFETIME_MS: Long = 1_800_000L

    /** Number of prepared statement cache entries per connection. */
    const val PREPARED_STATEMENT_CACHE_QUERIES: Int = 256

    /** Size limit (MiB) for the prepared statement cache per connection. */
    const val PREPARED_STATEMENT_CACHE_SIZE_MIB: Int = 5

    /**
     * Leak detection threshold (ms) — enabled only in DEVELOPMENT environment.
     *
     * HikariCP logs a warning when a connection is held for longer than this duration
     * without being returned to the pool.
     */
    const val LEAK_DETECTION_THRESHOLD_MS: Long = 60_000L
}
