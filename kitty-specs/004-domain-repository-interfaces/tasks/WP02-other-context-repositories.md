---
work_package_id: WP02
title: All Other Context Repositories
lane: "for_review"
dependencies: []
base_branch: main
base_commit: 15d6622ebdadda755d2ceb2ff7fe74b5e026541c
created_at: '2026-03-24T14:28:49.921537+00:00'
subtasks:
- T006
- T007
- T008
- T009
- T010
phase: Phase 1 - All Other Contexts
assignee: ''
agent: "claude-2"
shell_pid: "13000"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-24T14:22:26Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-007
- FR-008
- FR-009
- FR-010
- FR-011
- FR-012
- FR-013
---

# Work Package Prompt: WP02 – All Other Context Repositories

## ⚠️ IMPORTANT: Review Feedback Status

Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section.

---

## Review Feedback

*[Empty initially. Populated by reviewers if work is returned.]*

---

## Implement Command

```bash
spec-kitty agent workflow implement WP02 --agent <your-name>
```

---

## Objectives & Success Criteria

1. Five interface files exist across the correct packages in `core/domain/src/commonMain/kotlin/com/vibely/domain/`.
2. Every interface and every method carries KDoc — `./gradlew :core:domain:detekt` passes with zero violations.
3. All methods are `suspend`.
4. Single-entity operations return `Result<T>`; list operations return `List<T>`.
5. `PaymentRepository` has **no** `deleteById` — payments are append-only records.
6. Zero non-domain imports (only `com.vibely.domain.*` packages; no framework imports).
7. `./gradlew :core:domain:build` passes with zero errors and zero warnings.

---

## Context & Constraints

- **Feature**: 004-domain-repository-interfaces
- **Plan**: `kitty-specs/004-domain-repository-interfaces/plan.md`
- **Constitution**: `.kittify/memory/constitution.md` — `core:domain` must have zero framework dependencies; KDoc required on all public symbols.
- **Domain types**: All types (`CustomerId`, `Customer`, `IngredientId`, `IngredientStock`, `EmployeeId`, `Employee`, `Role`, `ShiftId`, `Shift`, `PaymentId`, `Payment`, `OrderId`) are already in `core:domain` from feature 003.
- **No new modules or build file changes** — all files go into the existing `commonMain` source set.
- **KDoc is critical**: Detekt enforces `UndocumentedPublicClass`, `UndocumentedPublicFunction`, `UndocumentedPublicProperty`. Missing KDoc will fail the pre-commit hook.
- **`PaymentRepository` is append-only**: Per FR-011, payments are never deleted. Do not add `deleteById`.
- **`findByPinHash` parameter is a hash**: KDoc must explicitly document that the parameter is a pre-hashed value, not a raw PIN.
- **`findBelowThreshold` condition**: KDoc must state `quantity < alertThreshold`.
- **`findOpenShifts` condition**: KDoc must state `clockOut == null`.

---

## Subtasks & Detailed Guidance

### Subtask T006 — Create `CustomerRepository`

**Purpose**: Define the data access contract for `Customer` aggregate roots. The `findByPhone` method enables returning-customer lookup during order placement without exposing storage details.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/customer/CustomerRepository.kt`

**Target implementation**:
```kotlin
package com.vibely.domain.customer

/**
 * Defines the data access contract for [Customer] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 */
interface CustomerRepository {

    /**
     * Retrieves a [Customer] by its unique identifier.
     *
     * @param id The [CustomerId] to look up.
     * @return [Result.success] containing the [Customer] if found,
     *         or [Result.failure] if no customer with the given [id] exists.
     */
    suspend fun findById(id: CustomerId): Result<Customer>

    /**
     * Retrieves a [Customer] by phone number.
     *
     * Used during order placement to look up returning customers.
     * The [phone] parameter should be a normalised phone string
     * (e.g., E.164 format: "+1234567890").
     *
     * @param phone The phone number string to look up.
     * @return [Result.success] containing the [Customer] if found,
     *         or [Result.failure] if no customer with the given phone exists.
     */
    suspend fun findByPhone(phone: String): Result<Customer>

    /**
     * Retrieves all customers.
     *
     * @return A list of all [Customer] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<Customer>

    /**
     * Persists a [Customer], inserting it if it does not exist or updating it if it does.
     *
     * @param customer The [Customer] to save.
     * @return [Result.success] containing the saved [Customer],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(customer: Customer): Result<Customer>

    /**
     * Removes the [Customer] with the given identifier.
     *
     * @param id The [CustomerId] of the customer to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no customer with the given [id] exists.
     */
    suspend fun deleteById(id: CustomerId): Result<Unit>
}
```

**Validation**:
- [ ] File exists at the correct path
- [ ] Interface has class-level KDoc
- [ ] `findByPhone` KDoc documents the expected phone format
- [ ] All methods are `suspend`
- [ ] `findById`, `findByPhone`, `save`, `deleteById` return `Result<T>`
- [ ] `findAll` returns `List<Customer>`

---

### Subtask T007 — Create `IngredientStockRepository`

**Purpose**: Define the data access contract for `IngredientStock` aggregate roots. The `findBelowThreshold` query drives the low-stock alert screen in the kitchen manager view.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/inventory/IngredientStockRepository.kt`

**Target implementation**:
```kotlin
package com.vibely.domain.inventory

/**
 * Defines the data access contract for [IngredientStock] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 */
interface IngredientStockRepository {

    /**
     * Retrieves an [IngredientStock] by its unique identifier.
     *
     * @param id The [IngredientId] to look up.
     * @return [Result.success] containing the [IngredientStock] if found,
     *         or [Result.failure] if no ingredient with the given [id] exists.
     */
    suspend fun findById(id: IngredientId): Result<IngredientStock>

    /**
     * Retrieves all ingredient stock records.
     *
     * @return A list of all [IngredientStock] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<IngredientStock>

    /**
     * Retrieves all ingredient stock records where current quantity is below the alert threshold.
     *
     * The condition is: [IngredientStock.quantity] < [IngredientStock.alertThreshold].
     * Used by the kitchen manager's low-stock alert screen.
     *
     * @return A list of [IngredientStock] records that are below threshold.
     *         Returns an empty list if all stock levels are sufficient.
     */
    suspend fun findBelowThreshold(): List<IngredientStock>

    /**
     * Persists an [IngredientStock] record, inserting it if it does not exist or updating it if it does.
     *
     * @param stock The [IngredientStock] to save.
     * @return [Result.success] containing the saved [IngredientStock],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(stock: IngredientStock): Result<IngredientStock>

    /**
     * Removes the [IngredientStock] record with the given identifier.
     *
     * @param id The [IngredientId] of the record to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no ingredient with the given [id] exists.
     */
    suspend fun deleteById(id: IngredientId): Result<Unit>
}
```

**Validation**:
- [ ] `findBelowThreshold` KDoc states the condition `quantity < alertThreshold`
- [ ] `findBelowThreshold` returns `List<IngredientStock>` (not `Result`)
- [ ] All other single-entity ops return `Result<T>`
- [ ] KDoc on interface and all methods

---

### Subtask T008 — Create `EmployeeRepository`

**Purpose**: Define the data access contract for `Employee` aggregate roots. The `findByPinHash` method supports clock-in authentication — the caller must hash the PIN before passing it in; the repository never receives a raw PIN.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/staff/EmployeeRepository.kt`

**Target implementation**:
```kotlin
package com.vibely.domain.staff

/**
 * Defines the data access contract for [Employee] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 */
interface EmployeeRepository {

    /**
     * Retrieves an [Employee] by its unique identifier.
     *
     * @param id The [EmployeeId] to look up.
     * @return [Result.success] containing the [Employee] if found,
     *         or [Result.failure] if no employee with the given [id] exists.
     */
    suspend fun findById(id: EmployeeId): Result<Employee>

    /**
     * Retrieves an [Employee] by their PIN hash.
     *
     * **The [pinHash] parameter must be a pre-hashed value — never a raw PIN.**
     * The caller is responsible for hashing the PIN before invoking this method.
     * Used during clock-in authentication to identify the employee without
     * storing or transmitting the raw PIN.
     *
     * @param pinHash The hashed PIN string to look up.
     * @return [Result.success] containing the matching [Employee] if found,
     *         or [Result.failure] if no employee with the given hash exists.
     */
    suspend fun findByPinHash(pinHash: String): Result<Employee>

    /**
     * Retrieves all employees.
     *
     * @return A list of all [Employee] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<Employee>

    /**
     * Retrieves all employees with the given [Role].
     *
     * @param role The [Role] to filter by.
     * @return A list of [Employee] records with the given role.
     *         Returns an empty list if no employees have that role.
     */
    suspend fun findByRole(role: Role): List<Employee>

    /**
     * Persists an [Employee], inserting it if it does not exist or updating it if it does.
     *
     * @param employee The [Employee] to save.
     * @return [Result.success] containing the saved [Employee],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(employee: Employee): Result<Employee>

    /**
     * Removes the [Employee] with the given identifier.
     *
     * @param id The [EmployeeId] of the employee to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no employee with the given [id] exists.
     */
    suspend fun deleteById(id: EmployeeId): Result<Unit>
}
```

**Validation**:
- [ ] `findByPinHash` KDoc explicitly states the parameter is a **pre-hashed** value, not a raw PIN
- [ ] `findByPinHash` and `findById` return `Result<Employee>`
- [ ] `findAll` and `findByRole` return `List<Employee>`
- [ ] KDoc on interface and all methods

---

### Subtask T009 — Create `ShiftRepository`

**Purpose**: Define the data access contract for `Shift` aggregate roots. The `findOpenShifts` method identifies who is currently clocked in (shifts where `clockOut` is null). The `findByEmployeeId` method enables shift history queries per employee.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/staff/ShiftRepository.kt`

**Target implementation**:
```kotlin
package com.vibely.domain.staff

/**
 * Defines the data access contract for [Shift] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 */
interface ShiftRepository {

    /**
     * Retrieves a [Shift] by its unique identifier.
     *
     * @param id The [ShiftId] to look up.
     * @return [Result.success] containing the [Shift] if found,
     *         or [Result.failure] if no shift with the given [id] exists.
     */
    suspend fun findById(id: ShiftId): Result<Shift>

    /**
     * Retrieves all shifts for a given employee.
     *
     * @param employeeId The [EmployeeId] to filter by.
     * @return A list of [Shift] records for the given employee.
     *         Returns an empty list if the employee has no shifts.
     */
    suspend fun findByEmployeeId(employeeId: EmployeeId): List<Shift>

    /**
     * Retrieves all currently open shifts.
     *
     * An open shift is one where [Shift.clockOut] is `null`, indicating
     * the employee has clocked in but has not yet clocked out.
     *
     * @return A list of open [Shift] records. Returns an empty list if no shifts are open.
     */
    suspend fun findOpenShifts(): List<Shift>

    /**
     * Persists a [Shift], inserting it if it does not exist or updating it if it does.
     *
     * @param shift The [Shift] to save.
     * @return [Result.success] containing the saved [Shift],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(shift: Shift): Result<Shift>

    /**
     * Removes the [Shift] with the given identifier.
     *
     * @param id The [ShiftId] of the shift to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no shift with the given [id] exists.
     */
    suspend fun deleteById(id: ShiftId): Result<Unit>
}
```

**Validation**:
- [ ] `findOpenShifts` KDoc states the condition is `clockOut == null`
- [ ] `findByEmployeeId` and `findOpenShifts` return `List<Shift>`
- [ ] `findById`, `save`, `deleteById` return `Result<T>`
- [ ] KDoc on interface and all methods

---

### Subtask T010 — Create `PaymentRepository`

**Purpose**: Define the data access contract for `Payment` aggregate roots. Payments are **append-only** records — once created, they are never deleted. There is no `deleteById` on this interface. The `findByOrderId` method enables receipt reconstruction by fetching all payments associated with an order.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/payment/PaymentRepository.kt`

**⚠️ Important**: This interface intentionally has **no `deleteById`** method. Do not add one. Payments are immutable transaction records per FR-011.

**Target implementation**:
```kotlin
package com.vibely.domain.payment

import com.vibely.domain.ordering.OrderId

/**
 * Defines the data access contract for [Payment] aggregate roots.
 *
 * Payments are append-only records. Once persisted, a payment is never deleted.
 * All operations are suspending to allow non-blocking execution.
 */
interface PaymentRepository {

    /**
     * Retrieves a [Payment] by its unique identifier.
     *
     * @param id The [PaymentId] to look up.
     * @return [Result.success] containing the [Payment] if found,
     *         or [Result.failure] if no payment with the given [id] exists.
     */
    suspend fun findById(id: PaymentId): Result<Payment>

    /**
     * Retrieves all payments associated with the given order.
     *
     * An order may have multiple payments (e.g., split payment between cash and card).
     * Used to reconstruct how an order was settled when generating a receipt.
     *
     * @param orderId The [OrderId] to filter by.
     * @return A list of [Payment] records for the given order.
     *         Returns an empty list if the order has no payments.
     */
    suspend fun findByOrderId(orderId: OrderId): List<Payment>

    /**
     * Persists a [Payment]. Payments are append-only — this method only inserts;
     * updating an existing payment is not supported.
     *
     * @param payment The [Payment] to save.
     * @return [Result.success] containing the saved [Payment],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(payment: Payment): Result<Payment>
}
```

**Validation**:
- [ ] **No `deleteById` method** — this is intentional and correct
- [ ] `import com.vibely.domain.ordering.OrderId` is present (only cross-context import)
- [ ] `findByOrderId` returns `List<Payment>` (not `Result`)
- [ ] `findById` and `save` return `Result<T>`
- [ ] `save` KDoc notes the append-only semantics
- [ ] KDoc on interface and all methods

---

## Risks & Mitigations

- **Missing KDoc on any public symbol** → Detekt will fail. Fix: verify every `interface` and every `fun` has a KDoc block before committing.
- **Adding `deleteById` to `PaymentRepository`** → violates FR-011. Payments are immutable; the interface must not expose deletion.
- **Raw PIN in `findByPinHash` KDoc** → KDoc must clearly state the parameter is a pre-hashed value; missing this note is a security documentation gap.
- **Wrong return type** (`List` vs `Result`) → review spec FR-012/FR-013. Single-entity lookups = `Result<T>`, list queries = `List<T>`.
- **ktlint multiline-expression-wrapping** → if any method signature spans multiple lines, ktlint may require the opening paren to be on a new line. Run `./gradlew :core:domain:ktlintFormat` before committing.
- **Wrong package for `OrderId` import in `PaymentRepository`** → must be `import com.vibely.domain.ordering.OrderId`, not any other path.

---

## Review Guidance

- Confirm all 5 files exist at the exact paths listed.
- Run `./gradlew :core:domain:build` — must pass with zero errors.
- Run `./gradlew :core:domain:detekt` — must pass (KDoc check).
- Verify `PaymentRepository.kt` has **no `deleteById`**.
- Verify `PaymentRepository.kt` has `import com.vibely.domain.ordering.OrderId` and no other cross-package imports.
- Verify `EmployeeRepository.kt`'s `findByPinHash` KDoc clearly states the parameter is pre-hashed.
- Verify `IngredientStockRepository.kt`'s `findBelowThreshold` KDoc states `quantity < alertThreshold`.
- Verify `ShiftRepository.kt`'s `findOpenShifts` KDoc states `clockOut == null`.
- Verify no file imports anything from `android`, `ktor`, `koin`, or any framework.

---

## Activity Log

- 2026-03-24T14:22:26Z – system – lane=planned – Prompt generated via /spec-kitty.tasks
- 2026-03-24T14:28:50Z – claude-2 – shell_pid=13000 – lane=doing – Assigned agent via workflow command
- 2026-03-24T14:35:42Z – claude-2 – shell_pid=13000 – lane=for_review – Ready for review: 5 repository interfaces (customer, inventory, staff, payment) with full KDoc
