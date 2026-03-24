@file:Suppress("ForbiddenComment")

package com.vibely.shared.di

// TODO: Remove this file when Phase 1.1 (core:database) is implemented.
// These stubs exist solely to allow the JVM platformModule to compile before
// the real database layer is introduced in core:database.

/**
 * Configuration for the JVM database connection.
 * Values are read from environment variables at startup.
 *
 * This is a temporary stub — replace with the real implementation in Phase 1.1.
 */
data class DatabaseConfig(
    /** JDBC URL, e.g. `jdbc:postgresql://localhost:5432/vibely`. */
    val url: String,
    /** Database username. */
    val username: String,
    /** Database password. */
    val password: String,
)

/**
 * Factory for creating database connections using [config].
 *
 * This is a temporary stub — replace with the real HikariCP-backed
 * implementation in Phase 1.1 (core:database).
 */
class DatabaseFactory(
    /** Configuration used to create database connections. */
    val config: DatabaseConfig,
) {
    // Intentionally empty — full implementation in Phase 1.1.
}
