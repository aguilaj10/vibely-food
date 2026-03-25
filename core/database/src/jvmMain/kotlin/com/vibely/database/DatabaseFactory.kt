package com.vibely.database

import com.vibely.common.DatabaseConstants
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Manages the HikariCP connection pool and provides tenant-scoped database access
 * via [withTenantContext].
 *
 * Constructed by the Koin DI container as a singleton. On construction:
 * 1. Builds the HikariCP pool from [config] and [DatabaseConstants].
 * 2. Connects Exposed to the pool.
 * 3. Runs Flyway baseline migration (idempotent — safe on repeated startup).
 *
 * @param config Immutable pool configuration.
 */
class DatabaseFactory(
    private val config: DatabaseConfig
) {
    private val dataSource: HikariDataSource = buildDataSource()
    internal val database: Database = Database.connect(dataSource)

    init {
        runFlyway()
    }

    private fun buildDataSource(): HikariDataSource {
        val hikariConfig =
            HikariConfig().apply {
                jdbcUrl = config.url
                username = config.username
                password = config.password
                maximumPoolSize = DatabaseConstants.MAX_POOL_SIZE
                minimumIdle = DatabaseConstants.MIN_IDLE
                connectionTimeout = DatabaseConstants.CONNECTION_TIMEOUT_MS
                idleTimeout = DatabaseConstants.IDLE_TIMEOUT_MS
                maxLifetime = DatabaseConstants.MAX_LIFETIME_MS
                connectionTestQuery = "SELECT 1"

                if (config.environment == Environment.DEVELOPMENT) {
                    leakDetectionThreshold = DatabaseConstants.LEAK_DETECTION_THRESHOLD_MS
                }

                addDataSourceProperty(
                    "preparedStatementCacheQueries",
                    DatabaseConstants.PREPARED_STATEMENT_CACHE_QUERIES.toString(),
                )
                addDataSourceProperty(
                    "preparedStatementCacheSizeMiB",
                    DatabaseConstants.PREPARED_STATEMENT_CACHE_SIZE_MIB.toString(),
                )
                addDataSourceProperty("binaryTransfer", "true")
                addDataSourceProperty("socketTimeout", "30")
                addDataSourceProperty("ApplicationName", "vibely-pos")
            }
        return HikariDataSource(hikariConfig)
    }

    private fun runFlyway() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .baselineVersion("1")
            .validateOnMigrate(true)
            .load()
            .migrate()
    }

    /**
     * Executes [block] within a database transaction that has the three PostgreSQL
     * RLS session variables set for the given [ctx].
     *
     * Session variables set (all via `SET LOCAL` — auto-cleared on transaction end):
     * - `app.current_organization_id` — always set
     * - `app.current_user_id` — always set
     * - `app.current_store_id` — set only when [TenantContext.storeId] is non-null
     *
     * **UUID format guard**: Each ID value is validated with [UUID.fromString] before
     * being interpolated into SQL. An [IllegalArgumentException] is thrown for invalid
     * UUIDs — this surfaces at the HTTP layer, not in the database.
     *
     * **Nested transaction behaviour**: Exposed re-uses the outer transaction when
     * [withTenantContext] is called from within an existing transaction. RLS variables
     * set by the outer call remain in effect.
     *
     * @param ctx Tenant context carrying the identity values.
     * @param block The database operations to execute within the tenant scope.
     * @return The result of [block].
     * @throws IllegalArgumentException if any non-null ID value is not a valid UUID.
     */
    suspend fun <T> withTenantContext(
        ctx: TenantContext,
        block: suspend () -> T
    ): T {
        val orgId = ctx.organizationId.value.also { validateUuid(it, "organizationId") }
        val userId = ctx.userId.value.also { validateUuid(it, "userId") }
        val storeId = ctx.storeId?.value?.also { validateUuid(it, "storeId") }

        return newSuspendedTransaction(Dispatchers.IO, database) {
            exec("SET LOCAL app.current_organization_id = '$orgId'")
            exec("SET LOCAL app.current_user_id = '$userId'")
            if (storeId != null) {
                exec("SET LOCAL app.current_store_id = '$storeId'")
            }
            block()
        }
    }

    private fun validateUuid(
        value: String,
        fieldName: String
    ) {
        try {
            UUID.fromString(value)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "TenantContext.$fieldName must be a valid UUID, got: $value",
                e,
            )
        }
    }
}
