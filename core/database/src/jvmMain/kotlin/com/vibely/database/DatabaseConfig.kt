package com.vibely.database

/**
 * Identifies the deployment environment, controlling pool diagnostics and logging.
 */
enum class Environment {
    /** Development: leak detection enabled, verbose logging. */
    DEVELOPMENT,

    /** Staging: production-like configuration with additional validation. */
    STAGING,

    /** Production: maximum performance, minimal overhead. */
    PRODUCTION,
}

/**
 * Immutable configuration for the HikariCP connection pool.
 *
 * @property url JDBC URL pointing to PgBouncer (not PostgreSQL directly).
 * @property username Database username.
 * @property password Database password.
 * @property environment Deployment environment — controls diagnostics.
 */
data class DatabaseConfig(
    val url: String,
    val username: String,
    val password: String,
    val environment: Environment,
) {
    companion object {
        /**
         * Creates a [DatabaseConfig] from environment variables, falling back to
         * safe defaults suitable for local development.
         *
         * | Env Var       | Default                                   |
         * |---|---|
         * | `DB_URL`      | `jdbc:postgresql://localhost:5432/vibely` |
         * | `DB_USER`     | `vibely`                                  |
         * | `DB_PASSWORD` | `password`                                |
         * | `APP_ENV`     | `DEVELOPMENT`                             |
         */
        fun fromEnvironment(): DatabaseConfig =
            DatabaseConfig(
                url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/vibely",
                username = System.getenv("DB_USER") ?: "vibely",
                password = System.getenv("DB_PASSWORD") ?: "password",
                environment =
                    System
                        .getenv("APP_ENV")
                        ?.let { runCatching { Environment.valueOf(it) }.getOrNull() }
                        ?: Environment.DEVELOPMENT,
            )
    }
}
