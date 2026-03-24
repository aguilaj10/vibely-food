---
work_package_id: WP01
title: Enum Corrections in core:domain
lane: "done"
dependencies: []
base_branch: main
base_commit: bad7276d4456b3157e3dc17e1ebc211024d6bac0
created_at: '2026-03-24T15:16:00.298036+00:00'
subtasks:
- T001
- T002
- T003
- T004
- T005
- T006
- T007
phase: Phase 1 - Enum Corrections
assignee: ''
agent: "claude"
shell_pid: "40058"
review_status: "approved"
reviewed_by: "Jonathan Sánchez Muñoz"
history:
- timestamp: '2026-03-24T15:10:01Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-001
- FR-005
- FR-006
- FR-007
---

# Work Package Prompt: WP01 – Enum Corrections in core:domain

## ⚠️ IMPORTANT: Review Feedback Status

Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section.

---

## Review Feedback

*[Empty initially. Populated by reviewers if work is returned.]*

---

## Implement Command

```bash
spec-kitty implement WP01
```

---

## Objectives & Success Criteria

1. Four enum files in `core:domain` exactly match the database schema types in `docs/database-schema.sql`.
2. Three test files updated so all existing tests compile and pass with the new enum values.
3. `./gradlew :core:domain:build` passes — zero errors, zero warnings.
4. `./gradlew :core:domain:detekt` passes — KDoc present on all updated enum classes.
5. No other files modified beyond the seven listed below.

---

## Context & Constraints

- **Feature**: 006-core-common-constants-and-permissions
- **Plan**: `kitty-specs/006-core-common-constants-and-permissions/plan.md`
- **Constitution**: `.kittify/memory/constitution.md` — `core:domain` must have zero framework dependencies; KDoc required on all public symbols.
- **DB schema**: `docs/database-schema.sql` is the authoritative source of truth. Do not invent values.
- **KDoc is critical**: Detekt enforces `UndocumentedPublicClass`. Each enum class must have a class-level KDoc comment.
- **Zero non-Kotlin imports**: No Android, Ktor, Koin, or framework imports allowed in `core:domain`.
- **No build file changes**: All changes are in `commonMain` source files and `jvmTest` test files only.
- **`PaymentMethod.SPLIT` must not exist**: `SPLIT` has been removed from the DB schema; `BANK_TRANSFER` replaces it. Split payments are modelled as multiple `Payment` records.

---

## Subtasks & Detailed Guidance

### Subtask T001 — Update `Role.kt`

**Purpose**: Align the `Role` enum to the database `user_role` type.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/staff/Role.kt`

**Change**:
- Rename `SERVER` → `WAITER`
- Add `VIEWER`

**Target implementation**:
```kotlin
package com.vibely.domain.staff

/**
 * Represents the role assigned to a user within a store.
 *
 * Values match the database [user_role] type exactly.
 */
enum class Role {
    OWNER,
    MANAGER,
    CASHIER,
    WAITER,
    KITCHEN,
    VIEWER,
}
```

**Validation**:
- [ ] Exactly 6 values: OWNER, MANAGER, CASHIER, WAITER, KITCHEN, VIEWER
- [ ] No `SERVER` value present
- [ ] Class-level KDoc present

---

### Subtask T002 — Update `OrderStatus.kt`

**Purpose**: Replace the misaligned order status values with the DB schema values.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/OrderStatus.kt`

**Change** (full replacement — none of the old values survive):
| Old | New |
|-----|-----|
| OPEN | DRAFT |
| IN_PROGRESS | PENDING |
| *(missing)* | PREPARING |
| DELIVERED | READY |
| CLOSED | COMPLETED |
| VOID | CANCELLED |

**Target implementation**:
```kotlin
package com.vibely.domain.ordering

/**
 * Represents the lifecycle state of an order.
 *
 * Values match the database [order_status] type exactly.
 */
enum class OrderStatus {
    DRAFT,
    PENDING,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED,
}
```

**Validation**:
- [ ] Exactly 6 values: DRAFT, PENDING, PREPARING, READY, COMPLETED, CANCELLED
- [ ] None of OPEN, IN_PROGRESS, DELIVERED, CLOSED, VOID present
- [ ] Class-level KDoc present

---

### Subtask T003 — Update `TableStatus.kt`

**Purpose**: Rename `FREE` to `AVAILABLE` and add `CLEANING`.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/TableStatus.kt`

**Change**:
- Rename `FREE` → `AVAILABLE`
- Add `CLEANING`
- Keep `OCCUPIED`, `RESERVED` unchanged

**Target implementation**:
```kotlin
package com.vibely.domain.ordering

/**
 * Represents the occupancy state of a restaurant table.
 *
 * Values match the database [table_status] type exactly.
 */
enum class TableStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    CLEANING,
}
```

**Validation**:
- [ ] Exactly 4 values: AVAILABLE, OCCUPIED, RESERVED, CLEANING
- [ ] No `FREE` value present
- [ ] Class-level KDoc present

---

### Subtask T004 — Update `PaymentMethod.kt`

**Purpose**: Replace `VOUCHER` with `BANK_TRANSFER` to align to the updated DB schema.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/payment/PaymentMethod.kt`

**Change**:
- Rename `VOUCHER` → `BANK_TRANSFER`
- Keep `CASH`, `CARD`, `DIGITAL_WALLET` unchanged
- **`SPLIT` must not be added** — split payments are multiple Payment records, not a payment method

**Target implementation**:
```kotlin
package com.vibely.domain.payment

/**
 * Represents the payment instrument used to settle an order.
 *
 * Values match the database [payment_method_type] type exactly.
 * Split payments (orders settled with multiple instruments) are recorded
 * as separate [Payment] records, each with its own [PaymentMethod].
 */
enum class PaymentMethod {
    CASH,
    CARD,
    DIGITAL_WALLET,
    BANK_TRANSFER,
}
```

**Validation**:
- [ ] Exactly 4 values: CASH, CARD, DIGITAL_WALLET, BANK_TRANSFER
- [ ] No `VOUCHER` or `SPLIT` present
- [ ] Class-level KDoc present and mentions split payment semantics

---

### Subtask T005 — Fix `StaffTest.kt` [P]

**Purpose**: Update test references from old `Role` values to new values.

**File**: `core/domain/src/jvmTest/kotlin/com/vibely/domain/staff/StaffTest.kt`

**Steps**:
1. Search for any reference to `Role.SERVER` and replace with `Role.WAITER`
2. Add any test assertions involving `Role.VIEWER` if tests enumerate all role values
3. Ensure the test still compiles and passes

**Validation**:
- [ ] No reference to `Role.SERVER` in the file
- [ ] `./gradlew :core:domain:jvmTest` passes

---

### Subtask T006 — Fix `OrderTest.kt` [P]

**Purpose**: Update test references from old `OrderStatus` values to new values.

**File**: `core/domain/src/jvmTest/kotlin/com/vibely/domain/ordering/OrderTest.kt`

**Steps**:
1. Replace all old enum references:
   - `OrderStatus.OPEN` → `OrderStatus.DRAFT`
   - `OrderStatus.IN_PROGRESS` → `OrderStatus.PENDING`
   - `OrderStatus.DELIVERED` → `OrderStatus.READY`
   - `OrderStatus.CLOSED` → `OrderStatus.COMPLETED`
   - `OrderStatus.VOID` → `OrderStatus.CANCELLED`
2. Add `OrderStatus.PREPARING` to any test that enumerates all status values
3. Ensure the test still compiles and passes

**Validation**:
- [ ] No reference to OPEN, IN_PROGRESS, DELIVERED, CLOSED, VOID in the file
- [ ] `./gradlew :core:domain:jvmTest` passes

---

### Subtask T007 — Fix `PaymentTest.kt` [P]

**Purpose**: Update test references from old `PaymentMethod` values.

**File**: `core/domain/src/jvmTest/kotlin/com/vibely/domain/payment/PaymentTest.kt`

**Steps**:
1. Replace `PaymentMethod.VOUCHER` → `PaymentMethod.BANK_TRANSFER`
2. Remove any reference to `PaymentMethod.SPLIT` (it does not exist)
3. Ensure the test still compiles and passes

**Validation**:
- [ ] No reference to `VOUCHER` or `SPLIT` in the file
- [ ] `./gradlew :core:domain:jvmTest` passes

---

## Risks & Mitigations

- **Enum values used outside test files**: Before editing, run `grep -r "Role\.\|OrderStatus\.\|TableStatus\.\|PaymentMethod\." core/domain/src/` to find all usages. Only test files and the enum files themselves should appear; if other files reference old values, update them too.
- **KDoc missing on enum class**: Detekt will fail. Ensure every updated enum has a class-level `/** … */` block.
- **ktlint formatting**: Run `./gradlew :core:domain:ktlintFormat` if the build reports formatting issues.

---

## Review Guidance

- Confirm all four enum files have exactly the values listed in `plan.md` — no extra, no missing.
- Confirm `docs/database-schema.sql` enum types match the Kotlin enum values exactly.
- Run `./gradlew :core:domain:build` — must produce zero errors and zero warnings.
- Run `./gradlew :core:domain:detekt` — must pass.
- Verify no framework imports (android, ktor, koin) in any modified file.
- Verify `PaymentMethod` has no `SPLIT` value.

---

## Activity Log

- 2026-03-24T15:10:01Z – system – lane=planned – Prompt generated via /spec-kitty.tasks
- 2026-03-24T15:16:01Z – claude-1 – shell_pid=30214 – lane=doing – Assigned agent via workflow command
- 2026-03-24T15:33:08Z – claude-1 – shell_pid=30214 – lane=for_review – Ready for review: corrected Role/OrderStatus/TableStatus/PaymentMethod enums with exhaustive jvmTest coverage; build + detekt + ktlint green
- 2026-03-24T15:33:17Z – claude – shell_pid=40058 – lane=doing – Started review via workflow command
- 2026-03-24T15:33:38Z – claude – shell_pid=40058 – lane=done – Review passed: all 5 domain enums corrected to match DB schema (Role/OrderStatus/TableStatus/PaymentMethod), KDoc added to every enum, split-payment semantics documented in PaymentMethod, exhaustive jvmTest coverage, build + ktlint + detekt green
