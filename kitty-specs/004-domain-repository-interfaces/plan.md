# Implementation Plan: Domain Repository Interfaces

**Branch**: `004-domain-repository-interfaces` | **Date**: 2026-03-24 | **Spec**: [spec.md](spec.md)

---

## Summary

Add 10 pure Kotlin `interface` files to `core/domain/src/commonMain/kotlin/` — one per aggregate root across all six bounded contexts. Each interface declares `suspend` operations returning `Result<T>` (fallible) or `List<T>` (queries where empty is valid). Every public interface and method carries KDoc (Detekt enforces this). Zero framework imports. No implementations shipped in this feature.

---

## Technical Context

**Language/Version**: Kotlin 2.3.20 (multiplatform — Android, JVM, JS targets)
**Primary Dependencies**: None — `core:domain` has zero external dependencies by constitution
**Storage**: N/A — interfaces only; no persistence implementation in this feature
**Testing**: `./gradlew :core:domain:build` + Detekt + KtLint — no runtime tests needed for interface files
**Target Platform**: KMP `commonMain` (shared across Android, JVM, JS)
**Project Type**: KMP library module (`core:domain`)
**Performance Goals**: N/A — pure interfaces, zero overhead
**Async contract**: `suspend` throughout; no `Flow` (reactive updates deferred to a future feature)
**Error handling**: `Result<T>` for all operations that may fail; `List<T>` for list queries (empty list is valid, not an error)
**Constraints**: Zero non-Kotlin/non-domain imports; KDoc on every public symbol (Detekt-enforced)

---

## Constitution Check

| Rule | Status | Notes |
|------|--------|-------|
| `core:domain` must have zero framework dependencies | ✅ PASS | Only imports from `com.vibely.domain.*` and Kotlin stdlib |
| Use `Result<T>` for fallible operations | ✅ PASS | All `findById`, `save`, `deleteById` return `Result<T>` |
| Use fake implementations for testing | ✅ N/A | Interfaces enable fakes; no fakes required in this WP |
| No `expect/actual` in shared code | ✅ N/A | Pure `commonMain` interfaces, no platform branching |
| No `if (isDebug)` / `BuildConfig` in business logic | ✅ N/A | No logic in this feature |
| KDoc on all public classes, functions, properties | ✅ REQUIRED | Every interface and method must have KDoc — Detekt enforces `UndocumentedPublicClass`, `UndocumentedPublicFunction` |
| Always use typed IDs | ✅ CONFIRMED | All parameters use typed `@JvmInline` ID classes from feature 003 |

**Gate result**: PASS — no violations. KDoc is the most critical implementation constraint.

---

## Project Structure

### Documentation (this feature)

```
kitty-specs/004-domain-repository-interfaces/
├── plan.md       # This file
└── spec.md       # Feature specification
```

No `research.md` — no unknowns to resolve (all types and patterns are established).
No `data-model.md` — no new entities.
No `contracts/` — these interfaces ARE the contracts.

### Source Code

```
core/domain/src/commonMain/kotlin/com/vibely/domain/
├── ordering/
│   ├── CategoryRepository.kt         # CategoryRepository interface
│   ├── MenuItemRepository.kt         # MenuItemRepository interface
│   ├── SectionRepository.kt          # SectionRepository interface
│   ├── TableRepository.kt            # TableRepository interface
│   └── OrderRepository.kt            # OrderRepository interface
├── customer/
│   └── CustomerRepository.kt         # CustomerRepository interface
├── inventory/
│   └── IngredientStockRepository.kt  # IngredientStockRepository interface
├── staff/
│   ├── EmployeeRepository.kt         # EmployeeRepository interface
│   └── ShiftRepository.kt            # ShiftRepository interface
└── payment/
    └── PaymentRepository.kt          # PaymentRepository interface
```

No changes to `build.gradle.kts`, no new modules, no new source sets.

---

## Interface Contracts

### `CategoryRepository` — `com.vibely.domain.ordering`

```kotlin
interface CategoryRepository {
    suspend fun findById(id: CategoryId): Result<Category>
    suspend fun findAll(): List<Category>
    suspend fun findAvailable(): List<Category>          // available == true
    suspend fun save(category: Category): Result<Category>
    suspend fun deleteById(id: CategoryId): Result<Unit>
}
```

### `MenuItemRepository` — `com.vibely.domain.ordering`

```kotlin
interface MenuItemRepository {
    suspend fun findById(id: MenuItemId): Result<MenuItem>
    suspend fun findAll(): List<MenuItem>
    suspend fun findAvailable(): List<MenuItem>           // available == true
    suspend fun findByCategoryId(categoryId: CategoryId): List<MenuItem>
    suspend fun save(menuItem: MenuItem): Result<MenuItem>
    suspend fun deleteById(id: MenuItemId): Result<Unit>
}
```

### `SectionRepository` — `com.vibely.domain.ordering`

```kotlin
interface SectionRepository {
    suspend fun findById(id: SectionId): Result<Section>
    suspend fun findAll(): List<Section>
    suspend fun save(section: Section): Result<Section>
    suspend fun deleteById(id: SectionId): Result<Unit>
}
```

### `TableRepository` — `com.vibely.domain.ordering`

```kotlin
interface TableRepository {
    suspend fun findById(id: TableId): Result<Table>
    suspend fun findAll(): List<Table>
    suspend fun findByStatus(status: TableStatus): List<Table>
    suspend fun findBySectionId(sectionId: SectionId): List<Table>
    suspend fun save(table: Table): Result<Table>
    suspend fun deleteById(id: TableId): Result<Unit>
}
```

### `OrderRepository` — `com.vibely.domain.ordering`

```kotlin
interface OrderRepository {
    suspend fun findById(id: OrderId): Result<Order>
    suspend fun findByTableId(tableId: TableId): List<Order>
    suspend fun findByCustomerId(customerId: CustomerId): List<Order>
    suspend fun findByStatus(status: OrderStatus): List<Order>
    suspend fun save(order: Order): Result<Order>
    suspend fun deleteById(id: OrderId): Result<Unit>
}
```

### `CustomerRepository` — `com.vibely.domain.customer`

```kotlin
interface CustomerRepository {
    suspend fun findById(id: CustomerId): Result<Customer>
    suspend fun findByPhone(phone: String): Result<Customer>
    suspend fun findAll(): List<Customer>
    suspend fun save(customer: Customer): Result<Customer>
    suspend fun deleteById(id: CustomerId): Result<Unit>
}
```

### `IngredientStockRepository` — `com.vibely.domain.inventory`

```kotlin
interface IngredientStockRepository {
    suspend fun findById(id: IngredientId): Result<IngredientStock>
    suspend fun findAll(): List<IngredientStock>
    suspend fun findBelowThreshold(): List<IngredientStock>   // quantity < alertThreshold
    suspend fun save(stock: IngredientStock): Result<IngredientStock>
    suspend fun deleteById(id: IngredientId): Result<Unit>
}
```

### `EmployeeRepository` — `com.vibely.domain.staff`

```kotlin
interface EmployeeRepository {
    suspend fun findById(id: EmployeeId): Result<Employee>
    suspend fun findByPinHash(pinHash: String): Result<Employee>
    suspend fun findAll(): List<Employee>
    suspend fun findByRole(role: Role): List<Employee>
    suspend fun save(employee: Employee): Result<Employee>
    suspend fun deleteById(id: EmployeeId): Result<Unit>
}
```

### `ShiftRepository` — `com.vibely.domain.staff`

```kotlin
interface ShiftRepository {
    suspend fun findById(id: ShiftId): Result<Shift>
    suspend fun findByEmployeeId(employeeId: EmployeeId): List<Shift>
    suspend fun findOpenShifts(): List<Shift>   // clockOut == null
    suspend fun save(shift: Shift): Result<Shift>
    suspend fun deleteById(id: ShiftId): Result<Unit>
}
```

### `PaymentRepository` — `com.vibely.domain.payment`

```kotlin
interface PaymentRepository {
    suspend fun findById(id: PaymentId): Result<Payment>
    suspend fun findByOrderId(orderId: OrderId): List<Payment>
    suspend fun save(payment: Payment): Result<Payment>
}
```

---

## Implementation Phases

All 10 interfaces depend only on feature 003 types (already on `main`). They have no inter-dependencies and can be parallelised freely.

### Phase 1 — Ordering Context (WP01)

Deliver the 5 ordering interfaces: `CategoryRepository`, `MenuItemRepository`, `SectionRepository`, `TableRepository`, `OrderRepository`.

**Deliverables**: 5 files in `com.vibely.domain.ordering`

**Key constraint**: KDoc on every interface and every method.

### Phase 2 — All Other Contexts (WP02)

Deliver the remaining 5 interfaces: `CustomerRepository`, `IngredientStockRepository`, `EmployeeRepository`, `ShiftRepository`, `PaymentRepository`.

**Deliverables**: 5 files across `customer`, `inventory`, `staff`, `payment` packages

**Parallelisable with WP01** — no dependency between them.

---

## Parallelisation

```
WP01 (ordering: 5 interfaces) ─┐
                                ├──► merge to main
WP02 (other 5 interfaces)     ─┘
```

---

## Testing Strategy

Interface files contain no executable logic — there is nothing to unit test.

Build gates serve as the full test:
1. `./gradlew :core:domain:compileKotlinJvm` — verifies contracts are well-formed Kotlin
2. `./gradlew :core:domain:detekt` — verifies KDoc present on all public symbols
3. `./gradlew :core:domain:ktlintCheck` — verifies formatting

---

## Definition of Done

- [ ] All 10 interface files exist under `core/domain/src/commonMain/`
- [ ] `./gradlew :core:domain:build` passes with zero errors and zero warnings
- [ ] `./gradlew :core:domain:detekt` passes — KDoc on every public interface and method
- [ ] `./gradlew :core:domain:ktlintCheck` passes — clean formatting
- [ ] Zero non-Kotlin/non-domain imports in any interface file
- [ ] All `findById`, `save`, `deleteById` return `Result<T>`
- [ ] All list queries return `List<T>`
- [ ] All methods are `suspend`
