---
description: "Work package task list for feature 004 — Domain Repository Interfaces"
---

# Work Packages: Domain Repository Interfaces

**Inputs**: `kitty-specs/004-domain-repository-interfaces/`
**Prerequisites**: plan.md ✅, spec.md ✅
**Feature dependency**: Feature 003 (core-domain-models) merged to `main` ✅

**Organization**: 10 interfaces across 6 bounded contexts, split into 2 fully parallel WPs of 5 interfaces each.
**Tests**: No runtime tests needed — build + Detekt + KtLint serve as the verification gates.

## Subtask Format: `[Txxx] [P?] Description`
- **[P]** = safe to parallelize (different files, no inter-dependency)

---

## Work Package WP01: Ordering Context Repositories (Priority: P0) 🎯 MVP

**Goal**: Define the 5 repository interfaces that cover the ordering bounded context — catalog entities (`Category`, `MenuItem`, `Section`, `Table`) and the central `Order` aggregate.
**Independent Test**: `./gradlew :core:domain:build` passes with zero errors; `./gradlew :core:domain:detekt` passes (KDoc present on all 5 interfaces and all their methods).
**Prompt**: `tasks/WP01-ordering-context-repositories.md`
**Estimated prompt size**: ~380 lines

### Included Subtasks
- [x] T001 [P] Create `CategoryRepository` in `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/CategoryRepository.kt`
- [x] T002 [P] Create `MenuItemRepository` in `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/MenuItemRepository.kt`
- [x] T003 [P] Create `SectionRepository` in `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/SectionRepository.kt`
- [x] T004 [P] Create `TableRepository` in `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/TableRepository.kt`
- [x] T005 [P] Create `OrderRepository` in `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/OrderRepository.kt`

### Implementation Notes
- All files go in the existing `com.vibely.domain.ordering` package — same package as the aggregate roots.
- All methods must be `suspend`.
- Single-entity operations return `Result<T>`; list operations return `List<T>`.
- Every interface and every method must have KDoc — Detekt enforces `UndocumentedPublicClass` and `UndocumentedPublicFunction`.
- No imports beyond `com.vibely.domain.ordering.*`, `com.vibely.domain.common.*`, and `com.vibely.domain.customer.CustomerId` (for `OrderRepository`).

### Parallel Opportunities
- All 5 files are independent and can be written in any order.

### Dependencies
- None — depends only on feature 003 types already on `main`.

### Requirement Refs
- FR-001, FR-002, FR-003, FR-004, FR-005, FR-006, FR-012, FR-013

### Risks & Mitigations
- Missing KDoc → Detekt fails. Fix: add KDoc to every public symbol before committing.
- String parameters (e.g., `findByPhone`) must document what format is expected in KDoc.

---

## Work Package WP02: All Other Context Repositories (Priority: P0)

**Goal**: Define the remaining 5 repository interfaces across the customer, inventory, staff, and payment bounded contexts.
**Independent Test**: `./gradlew :core:domain:build` passes with zero errors; `./gradlew :core:domain:detekt` passes (KDoc present on all 5 interfaces and their methods).
**Prompt**: `tasks/WP02-other-context-repositories.md`
**Estimated prompt size**: ~380 lines

### Included Subtasks
- [x] T006 [P] Create `CustomerRepository` in `core/domain/src/commonMain/kotlin/com/vibely/domain/customer/CustomerRepository.kt`
- [x] T007 [P] Create `IngredientStockRepository` in `core/domain/src/commonMain/kotlin/com/vibely/domain/inventory/IngredientStockRepository.kt`
- [x] T008 [P] Create `EmployeeRepository` in `core/domain/src/commonMain/kotlin/com/vibely/domain/staff/EmployeeRepository.kt`
- [x] T009 [P] Create `ShiftRepository` in `core/domain/src/commonMain/kotlin/com/vibely/domain/staff/ShiftRepository.kt`
- [x] T010 [P] Create `PaymentRepository` in `core/domain/src/commonMain/kotlin/com/vibely/domain/payment/PaymentRepository.kt`

### Implementation Notes
- Each file goes in the package of its aggregate root — no new packages.
- Same rules as WP01: `suspend`, `Result<T>` for fallible ops, `List<T>` for list queries, full KDoc.
- `PaymentRepository` has no `deleteById` — payments are append-only records (spec FR-011).
- `EmployeeRepository.findByPinHash` accepts a pre-hashed string; KDoc must document this.

### Parallel Opportunities
- All 5 files are independent; WP02 can run concurrently with WP01.

### Dependencies
- None — no dependency on WP01.

### Requirement Refs
- FR-007, FR-008, FR-009, FR-010, FR-011, FR-012, FR-013

### Risks & Mitigations
- `findByPinHash` — KDoc must make clear the parameter is a hash, not a raw PIN.
- `findBelowThreshold` — KDoc must document the comparison: `quantity < alertThreshold`.
- `findOpenShifts` — KDoc must document the condition: `clockOut == null`.
