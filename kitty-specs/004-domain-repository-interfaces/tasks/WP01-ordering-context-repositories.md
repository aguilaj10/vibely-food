---
work_package_id: WP01
title: Ordering Context Repositories
lane: "doing"
dependencies: []
base_branch: main
base_commit: b5bb43f081f0923ed62e4c86282464845f98b90b
created_at: '2026-03-24T14:28:43.893055+00:00'
subtasks:
- T001
- T002
- T003
- T004
- T005
phase: Phase 1 - Ordering Context
assignee: ''
agent: "claude-1"
shell_pid: "12844"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-24T14:22:26Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-001
- FR-002
- FR-003
- FR-004
- FR-005
- FR-006
- FR-012
- FR-013
---

# Work Package Prompt: WP01 – Ordering Context Repositories

## ⚠️ IMPORTANT: Review Feedback Status

Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section.

---

## Review Feedback

*[Empty initially. Populated by reviewers if work is returned.]*

---

## Implement Command

```bash
spec-kitty agent workflow implement WP01 --agent <your-name>
```

---

## Objectives & Success Criteria

1. Five interface files exist in `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/`.
2. Every interface and every method carries KDoc — `./gradlew :core:domain:detekt` passes with zero violations.
3. All methods are `suspend`.
4. Single-entity operations return `Result<T>`; list operations return `List<T>`.
5. Zero non-domain imports (only `com.vibely.domain.ordering.*`, `com.vibely.domain.common.*`, `com.vibely.domain.customer.CustomerId`).
6. `./gradlew :core:domain:build` passes with zero errors and zero warnings.

---

## Context & Constraints

- **Feature**: 004-domain-repository-interfaces
- **Plan**: `kitty-specs/004-domain-repository-interfaces/plan.md`
- **Constitution**: `.kittify/memory/constitution.md` — `core:domain` must have zero framework dependencies; KDoc required on all public symbols.
- **Domain types**: All types (`CategoryId`, `Category`, `MenuItemId`, `MenuItem`, `SectionId`, `Section`, `TableId`, `TableStatus`, `Table`, `OrderId`, `Order`, `OrderStatus`, `CustomerId`) are already in `core:domain` from feature 003.
- **No new modules or build file changes** — all files go into the existing `commonMain` source set.
- **KDoc is critical**: Detekt enforces `UndocumentedPublicClass`, `UndocumentedPublicFunction`, `UndocumentedPublicProperty`. Missing KDoc will fail the pre-commit hook.

---

## Subtasks & Detailed Guidance

### Subtask T001 — Create `CategoryRepository`

**Purpose**: Define the data access contract for `Category` aggregate roots. Categories are the top-level grouping of menu items (e.g., "Starters", "Mains", "Desserts").

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/CategoryRepository.kt`

**Target implementation**:
```kotlin
package com.vibely.domain.ordering

/**
 * Defines the data access contract for [Category] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 * Implementations must not perform any work on the calling coroutine's thread directly.
 */
interface CategoryRepository {

    /**
     * Retrieves a [Category] by its unique identifier.
     *
     * @param id The [CategoryId] to look up.
     * @return [Result.success] containing the [Category] if found,
     *         or [Result.failure] if no category with the given [id] exists.
     */
    suspend fun findById(id: CategoryId): Result<Category>

    /**
     * Retrieves all categories regardless of availability.
     *
     * @return A list of all [Category] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<Category>

    /**
     * Retrieves all categories where [Category.available] is `true`.
     *
     * @return A list of available [Category] records. Returns an empty list if none are available.
     */
    suspend fun findAvailable(): List<Category>

    /**
     * Persists a [Category], inserting it if it does not exist or updating it if it does.
     *
     * @param category The [Category] to save.
     * @return [Result.success] containing the saved [Category],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(category: Category): Result<Category>

    /**
     * Removes the [Category] with the given identifier.
     *
     * @param id The [CategoryId] of the category to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no category with the given [id] exists.
     */
    suspend fun deleteById(id: CategoryId): Result<Unit>
}
```

**Validation**:
- [ ] File exists at the correct path
- [ ] Interface has class-level KDoc
- [ ] Every method has KDoc with `@param` and `@return` tags
- [ ] All methods are `suspend`
- [ ] `findById` returns `Result<Category>`
- [ ] `findAll` and `findAvailable` return `List<Category>`
- [ ] `save` returns `Result<Category>`
- [ ] `deleteById` returns `Result<Unit>`

---

### Subtask T002 — Create `MenuItemRepository`

**Purpose**: Define the data access contract for `MenuItem` aggregate roots. Menu items are the core sellable products, always belonging to a `Category`.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/MenuItemRepository.kt`

**Target implementation**:
```kotlin
package com.vibely.domain.ordering

/**
 * Defines the data access contract for [MenuItem] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 */
interface MenuItemRepository {

    /**
     * Retrieves a [MenuItem] by its unique identifier.
     *
     * @param id The [MenuItemId] to look up.
     * @return [Result.success] containing the [MenuItem] if found,
     *         or [Result.failure] if no item with the given [id] exists.
     */
    suspend fun findById(id: MenuItemId): Result<MenuItem>

    /**
     * Retrieves all menu items regardless of availability.
     *
     * @return A list of all [MenuItem] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<MenuItem>

    /**
     * Retrieves all menu items where [MenuItem.available] is `true`.
     *
     * @return A list of available [MenuItem] records. Returns an empty list if none are available.
     */
    suspend fun findAvailable(): List<MenuItem>

    /**
     * Retrieves all menu items belonging to the given category.
     *
     * @param categoryId The [CategoryId] to filter by.
     * @return A list of [MenuItem] records in the given category.
     *         Returns an empty list if the category has no items.
     */
    suspend fun findByCategoryId(categoryId: CategoryId): List<MenuItem>

    /**
     * Persists a [MenuItem], inserting it if it does not exist or updating it if it does.
     *
     * @param menuItem The [MenuItem] to save.
     * @return [Result.success] containing the saved [MenuItem],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(menuItem: MenuItem): Result<MenuItem>

    /**
     * Removes the [MenuItem] with the given identifier.
     *
     * @param id The [MenuItemId] of the item to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no item with the given [id] exists.
     */
    suspend fun deleteById(id: MenuItemId): Result<Unit>
}
```

**Validation**:
- [ ] `findByCategoryId` returns `List<MenuItem>` (not `Result`) — empty list is valid
- [ ] All other single-entity ops return `Result<T>`
- [ ] KDoc on all methods including `@param` and `@return`

---

### Subtask T003 — Create `SectionRepository`

**Purpose**: Define the data access contract for `Section` aggregate roots. Sections represent physical areas of the restaurant (e.g., "Terrace", "Main Hall") and group tables.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/SectionRepository.kt`

**Target implementation**:
```kotlin
package com.vibely.domain.ordering

/**
 * Defines the data access contract for [Section] aggregate roots.
 *
 * Sections represent physical areas of the restaurant that group tables.
 * All operations are suspending to allow non-blocking execution.
 */
interface SectionRepository {

    /**
     * Retrieves a [Section] by its unique identifier.
     *
     * @param id The [SectionId] to look up.
     * @return [Result.success] containing the [Section] if found,
     *         or [Result.failure] if no section with the given [id] exists.
     */
    suspend fun findById(id: SectionId): Result<Section>

    /**
     * Retrieves all sections.
     *
     * @return A list of all [Section] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<Section>

    /**
     * Persists a [Section], inserting it if it does not exist or updating it if it does.
     *
     * @param section The [Section] to save.
     * @return [Result.success] containing the saved [Section],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(section: Section): Result<Section>

    /**
     * Removes the [Section] with the given identifier.
     *
     * @param id The [SectionId] of the section to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no section with the given [id] exists.
     */
    suspend fun deleteById(id: SectionId): Result<Unit>
}
```

---

### Subtask T004 — Create `TableRepository`

**Purpose**: Define the data access contract for `Table` aggregate roots, including filtered queries by status and section — both are needed by the ordering flow.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/TableRepository.kt`

**Target implementation**:
```kotlin
package com.vibely.domain.ordering

/**
 * Defines the data access contract for [Table] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 */
interface TableRepository {

    /**
     * Retrieves a [Table] by its unique identifier.
     *
     * @param id The [TableId] to look up.
     * @return [Result.success] containing the [Table] if found,
     *         or [Result.failure] if no table with the given [id] exists.
     */
    suspend fun findById(id: TableId): Result<Table>

    /**
     * Retrieves all tables regardless of status.
     *
     * @return A list of all [Table] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<Table>

    /**
     * Retrieves all tables with the given [TableStatus].
     *
     * Useful for showing free tables during order placement or reserved tables
     * in the floor plan view.
     *
     * @param status The [TableStatus] to filter by (FREE, OCCUPIED, or RESERVED).
     * @return A list of [Table] records with the given status.
     *         Returns an empty list if no tables have that status.
     */
    suspend fun findByStatus(status: TableStatus): List<Table>

    /**
     * Retrieves all tables belonging to the given section.
     *
     * @param sectionId The [SectionId] to filter by.
     * @return A list of [Table] records in the given section.
     *         Returns an empty list if the section has no tables.
     */
    suspend fun findBySectionId(sectionId: SectionId): List<Table>

    /**
     * Persists a [Table], inserting it if it does not exist or updating it if it does.
     *
     * @param table The [Table] to save.
     * @return [Result.success] containing the saved [Table],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(table: Table): Result<Table>

    /**
     * Removes the [Table] with the given identifier.
     *
     * @param id The [TableId] of the table to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no table with the given [id] exists.
     */
    suspend fun deleteById(id: TableId): Result<Unit>
}
```

**Validation**:
- [ ] `findByStatus` and `findBySectionId` return `List<Table>` (not `Result`)
- [ ] KDoc for `findByStatus` documents the three possible `TableStatus` values

---

### Subtask T005 — Create `OrderRepository`

**Purpose**: Define the data access contract for `Order` aggregate roots — the central transaction record of the POS. This interface supports all querying needs for the kitchen display, cashier view, and customer history.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/OrderRepository.kt`

**Import note**: `OrderRepository` references `CustomerId` from the customer bounded context. This is the only cross-context import and is intentional per the data model.

**Target implementation**:
```kotlin
package com.vibely.domain.ordering

import com.vibely.domain.customer.CustomerId

/**
 * Defines the data access contract for [Order] aggregate roots.
 *
 * Orders are the central transaction records linking tables, customers,
 * items, and status. All operations are suspending to allow non-blocking execution.
 */
interface OrderRepository {

    /**
     * Retrieves an [Order] by its unique identifier.
     *
     * @param id The [OrderId] to look up.
     * @return [Result.success] containing the [Order] if found,
     *         or [Result.failure] if no order with the given [id] exists.
     */
    suspend fun findById(id: OrderId): Result<Order>

    /**
     * Retrieves all orders associated with a given table.
     *
     * A table may have multiple orders over time (e.g., re-opened after a void).
     *
     * @param tableId The [TableId] to filter by.
     * @return A list of [Order] records for the given table.
     *         Returns an empty list if the table has no orders.
     */
    suspend fun findByTableId(tableId: TableId): List<Order>

    /**
     * Retrieves all orders placed by a given customer.
     *
     * @param customerId The [CustomerId] to filter by.
     * @return A list of [Order] records for the given customer.
     *         Returns an empty list if the customer has no orders.
     */
    suspend fun findByCustomerId(customerId: CustomerId): List<Order>

    /**
     * Retrieves all orders with the given [OrderStatus].
     *
     * Used by the kitchen display to show OPEN and IN_PROGRESS orders,
     * and by the cashier to show orders ready for payment (DELIVERED).
     *
     * @param status The [OrderStatus] to filter by.
     * @return A list of [Order] records with the given status.
     *         Returns an empty list if no orders have that status.
     */
    suspend fun findByStatus(status: OrderStatus): List<Order>

    /**
     * Persists an [Order], inserting it if it does not exist or updating it if it does.
     *
     * @param order The [Order] to save.
     * @return [Result.success] containing the saved [Order],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(order: Order): Result<Order>

    /**
     * Removes the [Order] with the given identifier.
     *
     * @param id The [OrderId] of the order to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no order with the given [id] exists.
     */
    suspend fun deleteById(id: OrderId): Result<Unit>
}
```

**Validation**:
- [ ] `import com.vibely.domain.customer.CustomerId` is present (only cross-context import)
- [ ] `findByTableId`, `findByCustomerId`, `findByStatus` return `List<Order>`
- [ ] `findById`, `save`, `deleteById` return `Result<T>`
- [ ] KDoc on interface and all methods

---

## Risks & Mitigations

- **Missing KDoc on any public symbol** → Detekt will fail. Fix: verify every `interface` and every `fun` has a KDoc block before committing.
- **Wrong return type** (`List` vs `Result`) → review spec FR-012/FR-013. Single-entity lookups = `Result<T>`, list queries = `List<T>`.
- **ktlint multiline-expression-wrapping** → if any method signature spans multiple lines, ktlint may require the opening paren to be on a new line. Run `./gradlew :core:domain:ktlintFormat` before committing.

---

## Review Guidance

- Confirm all 5 files exist at the exact paths listed.
- Run `./gradlew :core:domain:build` — must pass with zero errors.
- Run `./gradlew :core:domain:detekt` — must pass (KDoc check).
- Verify `OrderRepository.kt` has `import com.vibely.domain.customer.CustomerId` and no other cross-package imports.
- Verify no file imports anything from `android`, `ktor`, `koin`, or any framework.

---

## Activity Log

- 2026-03-24T14:22:26Z – system – lane=planned – Prompt generated via /spec-kitty.tasks
- 2026-03-24T14:28:44Z – claude-1 – shell_pid=12844 – lane=doing – Assigned agent via workflow command
