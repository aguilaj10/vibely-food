# Work Packages: Core Domain Models

**Inputs**: Design documents from `kitty-specs/003-core-domain-models/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅

**Tests**: Kotest assertions in `commonTest` — explicitly required by plan.md and SC-003 (10+ Money arithmetic tests).

**Organization**: 31 subtasks (`T001`–`T031`) rolled into 7 work packages. Each WP targets a single bounded context or foundation layer. WP02, WP03, WP06, WP07 are all parallelizable after WP01 completes.

**Parallelization Map**:
```
WP01 (common) ──► WP02 (ordering catalog) ──┐
              └──► WP03 (customer)          ├──► WP04 (order entities) ──► WP05 (payment)
              └──► WP06 (inventory) [end]   ┘
              └──► WP07 (staff) [end]
```

---

## Work Package WP01: Common Value Types (Priority: P0) 🎯 MVP

**Goal**: Define `Money`, `Timestamp`, and `Duration` — the three foundation value classes that every other domain file imports.
**Independent Test**: `Money(1099) + Money(501) == Money(1600)`; `Money.ZERO.cents == 0L`; 10+ arithmetic combinations pass without rounding errors.
**Prompt**: `tasks/WP01-common-value-types.md`
**Estimated size**: ~280 lines

### Included Subtasks
- [x] T001 Create `common/Money.kt` — `@JvmInline value class Money(val cents: Long)` with `ZERO` constant and `+`, `−`, `×(Int)` operators
- [x] T002 Create `common/Timestamp.kt` — `@JvmInline value class Timestamp(val epochMillis: Long)`
- [x] T003 Create `common/Duration.kt` — `@JvmInline value class Duration(val millis: Long)`
- [x] T004 [P] Write Kotest tests in `commonTest/kotlin/com/vibely/domain/common/` for Money arithmetic (≥10 combinations per SC-003), Timestamp construction, Duration construction

### Implementation Notes
- All three files live in `core/domain/src/commonMain/kotlin/com/vibely/domain/common/`
- Zero non-Kotlin imports; `@JvmInline` is a Kotlin language feature, not a library import
- `Money` companion object holds `ZERO: Money = Money(0)`
- Operators: `operator fun plus(other: Money)`, `operator fun minus(other: Money)`, `operator fun times(factor: Int)` — no division (avoids rounding policy)
- Tests live in `core/domain/src/commonTest/kotlin/com/vibely/domain/common/`

### Parallel Opportunities
- T001–T003 can be written concurrently (independent files); T004 must follow T001–T003.

### Dependencies
- None (first work package).

### Risks & Mitigations
- `@JvmInline` requires Kotlin 1.5+; project uses 2.3.20 — no risk.
- Ensure `operator fun times(factor: Int)` is defined on `Money`, NOT `Int.times(Money)`, to keep the type on the left.

**Requirement Refs**: FR-014, SC-003

---

## Work Package WP02: Ordering — Catalog Entities (Priority: P1) 🎯 MVP

**Goal**: Define all menu and layout catalog types: `Category`, `Modifier`, `ModifierGroup`, `MenuItem`, `Section`, `Table` (including `TableStatus` enum).
**Independent Test**: Construct a `MenuItem` with a required `ModifierGroup` containing two `Modifier`s; assert all fields accessible and `available == true`.
**Prompt**: `tasks/WP02-ordering-catalog-entities.md`
**Estimated size**: ~370 lines

### Included Subtasks
- [ ] T005 Create `ordering/Category.kt` — `CategoryId` + `Category`
- [ ] T006 [P] Create `ordering/Modifier.kt` — `ModifierId` + `Modifier`
- [ ] T007 [P] Create `ordering/ModifierGroup.kt` — `ModifierGroupId` + `ModifierGroup` (contains `List<Modifier>`)
- [ ] T008 [P] Create `ordering/MenuItem.kt` — `MenuItemId` + `MenuItem` (references `CategoryId`, `List<ModifierGroup>`)
- [ ] T009 [P] Create `ordering/Section.kt` — `SectionId` + `Section` (contains `List<TableId>`)
- [ ] T010 [P] Create `ordering/Table.kt` — `TableId` + `Table` + `TableStatus` enum

### Implementation Notes
- All files in `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/`
- Each file declares its `@JvmInline value class` ID at the top of the same file (e.g., `CategoryId` in `Category.kt`)
- `TableStatus`: `FREE | OCCUPIED | RESERVED`
- `ModifierGroup.options: List<Modifier>` — embeds the full modifier objects (not IDs), so all info is co-located
- `MenuItem.modifierGroups: List<ModifierGroup>` — same embedding pattern
- `Section.tables: List<TableId>` — reference by ID only (display-order list, not embedding)

### Parallel Opportunities
- T005–T010 are all independent files; all can be written in parallel.

### Dependencies
- Depends on WP01 (imports `Money` for `Modifier.priceAdjustment` and `MenuItem.basePrice`).

### Risks & Mitigations
- `Section.tables` holds `TableId` not `Table` — keep this as ID reference only; do not accidentally embed `Table`.
- `ModifierGroup.minSelections`/`maxSelections` are plain `Int`; enforcement that `min ≤ max` belongs to the service layer, not the domain model.

**Requirement Refs**: FR-001, FR-002, FR-003, FR-004, FR-008, FR-009

---

## Work Package WP03: Customer Bounded Context (Priority: P1)

**Goal**: Define `Customer` with its `CustomerId` — required before `Order` can be written (Order holds `customerId: CustomerId?`).
**Independent Test**: Construct a `Customer`; assert `phone == null`, `email == null`, `loyaltyPoints == 0` for an anonymous walk-in.
**Prompt**: `tasks/WP03-customer-bounded-context.md`
**Estimated size**: ~220 lines

### Included Subtasks
- [ ] T011 Create `customer/Customer.kt` — `CustomerId` + `Customer`
- [ ] T012 Write Kotest tests in `commonTest/kotlin/com/vibely/domain/customer/CustomerTest.kt` — assert nullable fields, loyalty points, all fields accessible

### Implementation Notes
- File in `core/domain/src/commonMain/kotlin/com/vibely/domain/customer/`
- `CustomerId` is the only customer ID; it is referenced by `Order.customerId: CustomerId?` in the ordering context
- `Customer.loyaltyPoints: Int` — non-negative by convention, not enforced in this model
- `Customer.phone: String?` — E.164 format (enforcement is service layer concern)
- `Customer.email: String?` — nullable

### Parallel Opportunities
- Can run fully in parallel with WP02, WP06, WP07.

### Dependencies
- Depends on WP01 (no common types used, but WP01 establishes the module; WP03 can run concurrently with WP01 if the module compiles independently — order: after WP01 is merged).

### Risks & Mitigations
- This WP is intentionally small (1 entity, 1 test). Its primary purpose is to define `CustomerId` so WP04 can reference it without a circular dependency.

**Requirement Refs**: FR-015, FR-016

---

## Work Package WP04: Ordering — Order Entities (Priority: P1) 🎯 MVP

**Goal**: Define `OrderStatus`, `SelectedModifier`, `OrderItem`, and `Order` — completing the ordering bounded context.
**Independent Test**: Construct a full `Order` with two `OrderItem`s (one with a selected modifier); assert all fields accessible including `customerId == null` for anonymous order.
**Prompt**: `tasks/WP04-ordering-order-entities.md`
**Estimated size**: ~380 lines

### Included Subtasks
- [ ] T013 Create `ordering/OrderStatus.kt` — `OrderStatus` enum (`OPEN | IN_PROGRESS | DELIVERED | CLOSED | VOID`)
- [ ] T014 Create `ordering/SelectedModifier.kt` — snapshot value object (captures `modifierId`, `name`, `priceAdjustment` at order time)
- [ ] T015 Create `ordering/OrderItem.kt` — `OrderItem` (references `MenuItemId`, `Money`, `List<SelectedModifier>`)
- [ ] T016 Create `ordering/Order.kt` — `OrderId` + `Order` (references `TableId`, `CustomerId?`, `List<OrderItem>`, `Timestamp`)
- [ ] T017 Write Kotest tests in `commonTest/kotlin/com/vibely/domain/ordering/` — full Order construction, two OrderItems with modifiers, OrderStatus enum exhaustiveness

### Implementation Notes
- `SelectedModifier` is a snapshot — copies `name: String` and `priceAdjustment: Money` from the modifier at order time; not a live reference
- `OrderItem.note: String?` — optional special instruction
- `Order.customerId: CustomerId?` — imports from `com.vibely.domain.customer.CustomerId`
- `Order.voidReason: String?` — only populated when `status == VOID`
- `Order.items: List<OrderItem>` — allowed to be empty (order built incrementally)
- `OrderStatus` is used in `when` exhaustiveness tests — no `else` branch (SC-002)

### Parallel Opportunities
- T013–T016 can be written in parallel; T017 must follow all four.

### Dependencies
- Depends on WP01 (Money, Timestamp).
- Depends on WP02 (MenuItemId, TableId, SectionId).
- Depends on WP03 (CustomerId).

### Risks & Mitigations
- `Order.kt` imports `CustomerId` from a different bounded context package (`com.vibely.domain.customer`). This is expected and intentional per the data model.
- Do NOT embed a `Customer` object — only the `CustomerId?` reference.

**Requirement Refs**: FR-005, FR-006, FR-007

---

## Work Package WP05: Payment Bounded Context (Priority: P1) 🎯 MVP

**Goal**: Define `PaymentMethod`, `PaymentStatus`, `Payment`, `ReceiptLineItem`, and `Receipt`.
**Independent Test**: Construct a `Receipt` with two `ReceiptLineItem`s and two `Payment`s; assert `payments.sumOf { it.amount.cents } == total.cents`.
**Prompt**: `tasks/WP05-payment-bounded-context.md`
**Estimated size**: ~380 lines

### Included Subtasks
- [ ] T018 Create `payment/PaymentMethod.kt` — `PaymentMethod` enum (`CASH | CARD | DIGITAL_WALLET | VOUCHER`)
- [ ] T019 Create `payment/PaymentStatus.kt` — `PaymentStatus` enum (`PENDING | COMPLETED | FAILED | REFUNDED`)
- [ ] T020 Create `payment/Payment.kt` — `PaymentId` + `Payment` (references `OrderId`, `Money`, `Timestamp`)
- [ ] T021 Create `payment/ReceiptLineItem.kt` — value object (`name: String`, `quantity: Int`, `unitPrice: Money`, `modifiers: List<SelectedModifier>`)
- [ ] T022 Create `payment/Receipt.kt` — immutable value object (`orderId`, `tableId`, `customerId?`, `lineItems`, `subtotal`, `taxAmount`, `total`, `payments`, `closedAt`)
- [ ] T023 Write Kotest tests in `commonTest/kotlin/com/vibely/domain/payment/` — Receipt construction, payment sum assertion, PaymentMethod/PaymentStatus exhaustiveness

### Implementation Notes
- `Receipt` is a `data class` — all `val`, no mutable state
- `Receipt.total == receipt.subtotal + receipt.taxAmount` is a convention, not enforced by the model
- `ReceiptLineItem.modifiers: List<SelectedModifier>` — imports from `com.vibely.domain.ordering.SelectedModifier`
- `Payment.id: PaymentId` — declared at top of `Payment.kt`
- Both `PaymentMethod` and `PaymentStatus` are used in `when` exhaustiveness tests — no `else` branch (SC-002)

### Parallel Opportunities
- T018–T022 can be written in parallel; T023 must follow all five.

### Dependencies
- Depends on WP04 (OrderId, TableId, SelectedModifier from ordering context).
- Depends on WP03 (CustomerId).
- Depends on WP01 (Money, Timestamp).

### Risks & Mitigations
- `ReceiptLineItem` imports `SelectedModifier` from the ordering package — cross-context import, expected per data model.
- `Receipt.total` is stored as a snapshot field, not computed inside the model. Tests should assert equality by construction, not re-derive it.

**Requirement Refs**: FR-010, FR-011, FR-012, FR-013, FR-014

---

## Work Package WP06: Inventory Bounded Context (Priority: P3)

**Goal**: Define `UnitOfMeasure`, `IngredientStock`, and `StockAlert`.
**Independent Test**: Construct `IngredientStock` with `quantity = 1.5` and `alertThreshold = 2.0`; assert `quantity < alertThreshold == true`.
**Prompt**: `tasks/WP06-inventory-bounded-context.md`
**Estimated size**: ~300 lines

### Included Subtasks
- [ ] T024 Create `inventory/UnitOfMeasure.kt` — enum (`KILOGRAM | GRAM | LITRE | MILLILITRE | UNIT | PORTION`)
- [ ] T025 [P] Create `inventory/IngredientStock.kt` — `IngredientId` + `IngredientStock` (Double quantity, alertThreshold)
- [ ] T026 [P] Create `inventory/StockAlert.kt` — `StockAlertId` + `StockAlert` (references `IngredientId`, `UnitOfMeasure`, `Timestamp`)
- [ ] T027 Write Kotest tests in `commonTest/kotlin/com/vibely/domain/inventory/`

### Implementation Notes
- `IngredientStock.quantity: Double` — fractional kitchen quantities (1.5 kg); see research.md Decision 5
- `IngredientStock.alertThreshold: Double` — quantity below which a StockAlert is warranted
- `StockAlert.quantityAtAlert: Double` — snapshot of quantity that triggered the alert
- `UnitOfMeasure` is exhaustively handleable in `when` (SC-002)

### Parallel Opportunities
- Can run fully in parallel with WP02, WP03, WP07.
- T024–T026 can be written concurrently.

### Dependencies
- Depends on WP01 (Timestamp in StockAlert).

### Risks & Mitigations
- `Double` for quantity is a deliberate decision (research.md Decision 5) — do not convert to `Long` or introduce a custom type.

**Requirement Refs**: FR-017, FR-018

---

## Work Package WP07: Staff Bounded Context (Priority: P3)

**Goal**: Define `Role`, `Employee`, and `Shift`.
**Independent Test**: Construct `Employee` with `role = Role.MANAGER`; assert `role.name == "MANAGER"`. Construct `Shift` with no clock-out; assert `clockOut == null`.
**Prompt**: `tasks/WP07-staff-bounded-context.md`
**Estimated size**: ~300 lines

### Included Subtasks
- [ ] T028 Create `staff/Role.kt` — enum (`OWNER | MANAGER | CASHIER | SERVER | KITCHEN`)
- [ ] T029 [P] Create `staff/Employee.kt` — `EmployeeId` + `Employee` (name, pinHash, role)
- [ ] T030 [P] Create `staff/Shift.kt` — `ShiftId` + `Shift` (employeeId, clockIn, clockOut?, breakDuration)
- [ ] T031 Write Kotest tests in `commonTest/kotlin/com/vibely/domain/staff/`

### Implementation Notes
- `Employee.pinHash: String` — stores a hash string, NOT the raw PIN (algorithm is a service-layer concern)
- `Shift.clockOut: Timestamp?` — nullable while shift is still active
- `Shift.breakDuration: Duration` — accumulated break time; `Duration(0)` if no breaks taken
- `Role` is exhaustively handleable in `when` (SC-002)

### Parallel Opportunities
- Can run fully in parallel with WP02, WP03, WP06.
- T028–T030 can be written concurrently.

### Dependencies
- Depends on WP01 (Timestamp, Duration).

### Risks & Mitigations
- `Employee.pinHash` must be named `pinHash`, not `pin` — ensures no implementation accidentally stores a raw PIN.

**Requirement Refs**: FR-019, FR-020, FR-021
