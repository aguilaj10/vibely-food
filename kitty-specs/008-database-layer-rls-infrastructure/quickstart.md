# Quickstart: Database Layer Infrastructure with RLS

**Feature**: 008-database-layer-rls-infrastructure
**Date**: 2026-03-25

---

## Prerequisites

- PostgreSQL running locally (or via Docker)
- Schema applied from `docs/database-schema.sql`
- Environment variables set (or defaults used for development)

```bash
export DB_URL="jdbc:postgresql://localhost:5432/vibely"
export DB_USER="vibely"
export DB_PASSWORD="password"
export APP_ENV="DEVELOPMENT"
```

---

## Scenario 1: Execute a tenant-scoped query

```kotlin
// 1. Build context from authenticated JWT claims
val ctx = TenantContext(
    organizationId = OrganizationId("a1b2c3d4-..."),
    storeId = StoreId("e5f6g7h8-..."),
    userId = UserId("i9j0k1l2-..."),
)

// 2. Run any Exposed query inside withTenantContext
val orders = databaseFactory.withTenantContext(ctx) {
    OrdersTable.selectAll().toList()
}
// RLS policies automatically filter to ctx.organizationId + ctx.storeId
```

---

## Scenario 2: Org-level operation (no store)

```kotlin
val ctx = TenantContext(
    organizationId = OrganizationId("a1b2c3d4-..."),
    storeId = null,   // org-level — only organization_id RLS policy applies
    userId = UserId("i9j0k1l2-..."),
)

val org = databaseFactory.withTenantContext(ctx) {
    OrganizationsTable.selectAll().firstOrNull()
}
```

---

## Scenario 3: Implement a concrete repository

```kotlin
class OrderRepositoryImpl(
    override val localDataSource: LocalDataSource<Order, OrderId>,
    override val remoteDataSource: RemoteDataSource<Order, OrderId>,
    override val syncManager: SyncManager,
) : BaseRepository<Order, OrderId>()

// Usage
val result: Result<Order> = orderRepo.getById(OrderId("..."))
result.onSuccess { order -> /* use order */ }
result.onFailure { error -> /* handle */ }
```

---

## Scenario 4: Implement a mapper

```kotlin
object OrderMapper : DomainMapper<OrderEntity, Order> {
    override fun toDomain(entity: OrderEntity): Order = Order(
        id = OrderId(entity.id),
        // ...
    )
    override fun toEntity(domain: Order): OrderEntity = OrderEntity(
        id = domain.id.value,
        // ...
    )
}
```

---

## Verify RLS is working (integration test)

```kotlin
// In DatabaseFactoryTest using Testcontainers
val result = databaseFactory.withTenantContext(ctx) {
    exec("SELECT current_setting('app.current_organization_id', true)")
}
result shouldBe ctx.organizationId.value
```

---

## Flyway baseline check

On first startup against a fresh database (empty `flyway_schema_history`):
```
Flyway Community Edition ... by Redgate
Database: jdbc:postgresql://localhost:5432/vibely (PostgreSQL 17)
Successfully baselined schema with version: 1
Current version of schema "public": 1
Schema "public" is up to date. No migration necessary.
```

On subsequent startups:
```
Current version of schema "public": 1
Schema "public" is up to date. No migration necessary.
```
