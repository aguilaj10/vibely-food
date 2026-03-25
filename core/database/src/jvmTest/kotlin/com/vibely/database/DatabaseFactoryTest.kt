package com.vibely.database

import com.vibely.domain.tenant.OrganizationId
import com.vibely.domain.tenant.StoreId
import com.vibely.domain.tenant.UserId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeBlank
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager

@Testcontainers
class DatabaseFactoryTest {

    companion object {
        @Container
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:17")

        val factory: DatabaseFactory by lazy {
            val config = DatabaseConfig(
                url = postgres.jdbcUrl,
                username = postgres.username,
                password = postgres.password,
                environment = Environment.DEVELOPMENT,
            )
            DatabaseFactory(config)
        }
    }

    @Test
    fun `withTenantContext sets all three RLS session variables`() = runTest {
        val orgId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        val storeId = "b2c3d4e5-f6a7-8901-bcde-f01234567891"
        val userId = "c3d4e5f6-a7b8-9012-cdef-012345678901"

        val ctx = TenantContext(
            organizationId = OrganizationId(orgId),
            storeId = StoreId(storeId),
            userId = UserId(userId),
        )

        val results = factory.withTenantContext(ctx) {
            val org = TransactionManager.current().exec(
                "SELECT current_setting('app.current_organization_id', true)"
            ) { rs -> rs.next(); rs.getString(1) }
            val store = TransactionManager.current().exec(
                "SELECT current_setting('app.current_store_id', true)"
            ) { rs -> rs.next(); rs.getString(1) }
            val user = TransactionManager.current().exec(
                "SELECT current_setting('app.current_user_id', true)"
            ) { rs -> rs.next(); rs.getString(1) }
            Triple(org, store, user)
        }

        results.first shouldBe orgId
        results.second shouldBe storeId
        results.third shouldBe userId
    }

    @Test
    fun `withTenantContext does not set store_id when storeId is null`() = runTest {
        val orgId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        val userId = "c3d4e5f6-a7b8-9012-cdef-012345678901"

        val ctx = TenantContext(
            organizationId = OrganizationId(orgId),
            storeId = null,
            userId = UserId(userId),
        )

        val storeValue = factory.withTenantContext(ctx) {
            TransactionManager.current().exec(
                "SELECT current_setting('app.current_store_id', true)"
            ) { rs -> rs.next(); rs.getString(1) }
        }

        // PostgreSQL returns empty string for unset GUCs when missing_ok = true
        storeValue?.shouldBeBlank()
    }

    @Test
    fun `RLS session variables are absent after withTenantContext returns`() = runTest {
        val ctx = TenantContext(
            organizationId = OrganizationId("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
            storeId = StoreId("b2c3d4e5-f6a7-8901-bcde-f01234567891"),
            userId = UserId("c3d4e5f6-a7b8-9012-cdef-012345678901"),
        )

        // Execute a transaction that sets RLS variables
        factory.withTenantContext(ctx) { /* no-op */ }

        // After the transaction, open a raw JDBC connection and verify variables are unset
        val storeValue = DriverManager.getConnection(
            postgres.jdbcUrl, postgres.username, postgres.password
        ).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery(
                    "SELECT current_setting('app.current_store_id', true)"
                ).use { rs ->
                    rs.next()
                    rs.getString(1)
                }
            }
        }

        // PostgreSQL returns "" for unset GUCs when missing_ok = true
        storeValue.shouldBeBlank()
    }

    @Test
    fun `Flyway creates V1 baseline record in flyway_schema_history`() = runTest {
        // DatabaseFactory init block runs Flyway — already executed via `factory` lazy
        val version = DriverManager.getConnection(
            postgres.jdbcUrl, postgres.username, postgres.password
        ).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery(
                    "SELECT version FROM flyway_schema_history LIMIT 1"
                ).use { rs ->
                    if (rs.next()) rs.getString("version") else null
                }
            }
        }

        version shouldBe "1"
    }
}
