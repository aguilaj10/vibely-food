# Feature Specification: Core Domain Models

**Feature Branch**: `003-core-domain-models`
**Created**: 2026-03-24
**Status**: Draft
**Mission**: software-dev

## Overview

Define all pure Kotlin domain entities for the Vibely Food POS system. These models form the shared vocabulary of the entire application — every bounded context (ordering, customers, payments, inventory, staff, restaurant layout) expresses its data through these types.

The models carry no business logic and have zero framework dependencies. They are the single source of truth for what the system knows and tracks.

---

## User Scenarios & Testing

### User Story 1 – Place and Track an Order (Priority: P1)

A server takes a food order at a table, sends it to the kitchen, and tracks it through to delivery. The ordering bounded context — `MenuItem`, `Category`, `Order`, `OrderItem`, `Table`, `Section` — must be expressive enough to represent the full lifecycle of an order without ambiguity.

**Why this priority**: Every other feature in the POS depends on an order being representable. Nothing ships until this is solid.

**Independent Test**: Can be fully tested by constructing an `Order` with `OrderItem`s, transitioning it through all `OrderStatus` states, and asserting all fields are correctly represented at each step.

**Acceptance Scenarios**:

1. **Given** a menu with categories and items, **When** a server creates an order for a table, **Then** the order captures the table, the list of items with quantities and modifiers, and the initial status `OPEN`.
2. **Given** an open order, **When** it is sent to the kitchen, **Then** the status transitions to `IN_PROGRESS` and a timestamp is recorded.
3. **Given** an in-progress order, **When** it is delivered, **Then** the status transitions to `DELIVERED`.
4. **Given** an order in any state, **When** it is voided, **Then** the status transitions to `VOID` and the reason is captured.

---

### User Story 2 – Process a Payment (Priority: P1)

A cashier closes a bill by collecting one or more payments (cash, card, split). The payment bounded context — `Payment`, `PaymentMethod`, `PaymentStatus`, `Receipt` — must represent any combination of tender types and capture the outcome unambiguously.

**Why this priority**: Revenue recognition is the core purpose of a POS.

**Independent Test**: Can be fully tested by constructing a `Payment` against a closed order, splitting it across two `PaymentMethod` instances, and verifying a `Receipt` is producible from the resulting data.

**Acceptance Scenarios**:

1. **Given** a delivered order, **When** the customer pays by card, **Then** a `Payment` is created with `PaymentMethod.CARD`, the amount, and status `COMPLETED`.
2. **Given** a payment total split between cash and card, **When** both tenders are recorded, **Then** two `Payment` records exist and their amounts sum to the order total.
3. **Given** a payment attempt, **When** it fails (declined card), **Then** the `Payment` status is `FAILED` and the order remains open.

---

### User Story 3 – Manage Menu Items and Categories (Priority: P2)

A restaurant manager maintains the menu — adding items, setting prices, attaching modifiers, marking items as unavailable. The `MenuItem`, `Category`, `ModifierGroup`, and `Modifier` models must capture everything needed to display and price an item correctly.

**Why this priority**: Required before any order can be created; less urgent than the order lifecycle itself because initial data can be seeded.

**Independent Test**: Can be fully tested by constructing a `MenuItem` with a `ModifierGroup` containing optional and required `Modifier`s, and asserting the full description is representable.

**Acceptance Scenarios**:

1. **Given** a category "Burgers", **When** a menu item "Classic Burger" is created with a base price and an "Extras" modifier group, **Then** the item captures name, price, category, and modifier group.
2. **Given** a menu item, **When** it is marked unavailable, **Then** its `available` flag is `false` and ordering systems can filter it out.
3. **Given** a modifier group with `required = true` and 3 choices, **When** an order item is created without selecting one, **Then** the missing selection is detectable from the model.

---

### User Story 4 – Track Customer Profiles (Priority: P2)

A loyalty-aware POS associates orders with returning customers. The `Customer` model must hold contact details, preferences, and a loyalty balance — enough to greet a returning guest and apply a reward.

**Why this priority**: Supports upsell and retention; not required to take a first order.

**Independent Test**: Can be fully tested by constructing a `Customer` with a loyalty balance, creating an `Order` linked to them, and verifying the relationship is representable.

**Acceptance Scenarios**:

1. **Given** a new customer registration, **When** their profile is created, **Then** it captures name, phone/email, and an initial loyalty balance of zero.
2. **Given** an existing customer, **When** an order is linked to them, **Then** the order holds a reference to the customer.

---

### User Story 5 – Monitor Inventory (Priority: P3)

Kitchen staff track ingredient stock to know when to 86 a dish. The `IngredientStock` and `StockAlert` models capture current quantity, unit, and threshold levels.

**Why this priority**: Important for operations; does not block ordering or payment flows.

**Independent Test**: Can be fully tested by constructing an `IngredientStock` item, reducing its quantity below the alert threshold, and asserting a `StockAlert` can be derived from that state.

**Acceptance Scenarios**:

1. **Given** an ingredient "Tomatoes" with quantity 5 kg and alert threshold 2 kg, **When** the quantity drops to 1.5 kg, **Then** the model represents a below-threshold state.
2. **Given** an out-of-stock ingredient, **When** a menu item requires it, **Then** the relationship between `IngredientStock` and `MenuItem` is representable.

---

### User Story 6 – Manage Staff and Shifts (Priority: P3)

Managers schedule employees and track who is clocked in. The `Employee`, `Role`, and `Shift` models carry identity, permission level, and time-tracking data.

**Why this priority**: Required for authentication and access control in later features; models can be defined now.

**Independent Test**: Can be fully tested by constructing an `Employee` with a `Role` and a `Shift` record, and asserting all scheduling fields are present.

**Acceptance Scenarios**:

1. **Given** an employee "Ana" with role `MANAGER`, **When** she clocks in, **Then** a `Shift` is created with start time and her employee reference.
2. **Given** an employee with role `CASHIER`, **When** their role is inspected, **Then** the permission level is lower than `MANAGER`.

---

### Edge Cases

- An `Order` with zero items must be representable (e.g., a reserved or partially created order).
- A `Payment` amount of zero must be representable for fully comped orders.
- A `MenuItem` with no modifier groups must be valid.
- An `Order` with no linked `Customer` must be valid (anonymous orders).
- `OrderStatus` and `PaymentStatus` must be enumerable so consuming code can exhaustively handle every state without casting or string comparison.
- Monetary amounts must use exact representation to prevent rounding errors when summing multiple line items or split payments.

---

## Requirements

### Functional Requirements

**Ordering Bounded Context**

- **FR-001**: The system MUST represent a `MenuItem` with name, description, base price, availability flag, category reference, and zero or more modifier groups.
- **FR-002**: The system MUST represent a `Category` with name, display order, and availability flag.
- **FR-003**: The system MUST represent a `ModifierGroup` with name, whether selection is required, minimum and maximum number of selections, and a list of `Modifier` options.
- **FR-004**: The system MUST represent a `Modifier` with name, price adjustment (positive, negative, or zero), and availability flag.
- **FR-005**: The system MUST represent an `Order` with a unique identifier, table reference, optional customer reference, list of `OrderItem`s, status, creation timestamp, and last-updated timestamp.
- **FR-006**: The system MUST represent an `OrderItem` with a `MenuItem` reference, quantity, selected modifiers, unit price at time of order, and an optional special-instruction note.
- **FR-007**: The system MUST represent `OrderStatus` as an enumeration with at minimum: `OPEN`, `IN_PROGRESS`, `DELIVERED`, `CLOSED`, `VOID`.
- **FR-008**: The system MUST represent a `Table` with identifier, section reference, seating capacity, and occupancy status (`FREE`, `OCCUPIED`, `RESERVED`).
- **FR-009**: The system MUST represent a `Section` with name and an ordered list of table references.

**Payment Bounded Context**

- **FR-010**: The system MUST represent a `Payment` with a unique identifier, order reference, amount, payment method, status, and timestamp.
- **FR-011**: The system MUST represent `PaymentMethod` as an enumeration with at minimum: `CASH`, `CARD`, `DIGITAL_WALLET`, `VOUCHER`.
- **FR-012**: The system MUST represent `PaymentStatus` as an enumeration with at minimum: `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`.
- **FR-013**: The system MUST represent a `Receipt` capturing order line items, subtotal, tax amount, total, and a summary of payments applied.
- **FR-014**: All monetary amounts MUST use an exact integer-based representation (smallest currency unit) to prevent floating-point rounding errors.

**Customer Bounded Context**

- **FR-015**: The system MUST represent a `Customer` with unique identifier, name, optional phone number, optional email address, and a loyalty points balance.
- **FR-016**: The system MUST allow an `Order` to reference an optional `Customer` so that anonymous orders are valid.

**Inventory Bounded Context**

- **FR-017**: The system MUST represent an `IngredientStock` item with name, current quantity, unit of measure, and alert threshold quantity.
- **FR-018**: The system MUST represent a `StockAlert` with an ingredient reference, the quantity at the time of the alert, and a timestamp.

**Staff Bounded Context**

- **FR-019**: The system MUST represent an `Employee` with unique identifier, name, PIN credential, and assigned `Role`.
- **FR-020**: The system MUST represent `Role` as an enumeration with at minimum: `OWNER`, `MANAGER`, `CASHIER`, `SERVER`, `KITCHEN`.
- **FR-021**: The system MUST represent a `Shift` with employee reference, clock-in timestamp, optional clock-out timestamp, and total break duration.

### Key Entities

- **MenuItem**: A dish or drink available for order; priced and categorised; may carry customisation options.
- **Category**: A logical grouping of menu items (e.g., "Starters", "Mains", "Beverages").
- **ModifierGroup**: A set of add-on or choice options that can be applied to an order item (e.g., "Extras", "Cooking preference").
- **Modifier**: A single selectable option within a modifier group; carries a price adjustment.
- **Order**: The central transaction record linking a table, ordered items, an optional customer, and a status lifecycle.
- **OrderItem**: One line in an order; records what was ordered, how many, which modifiers were selected, and the price locked at order time.
- **Table**: A physical seating location inside a section; tracks current occupancy.
- **Section**: A named area of the restaurant (e.g., "Terrace", "Bar", "Main Floor").
- **Payment**: A single tender record against an order; one order may have multiple payments.
- **Receipt**: An immutable summary document generated when an order is fully settled.
- **Customer**: A registered guest; carries contact details and a loyalty points balance.
- **IngredientStock**: Tracks the current on-hand quantity of a raw ingredient.
- **StockAlert**: A record that an ingredient fell below its configured alert threshold.
- **Employee**: A staff member identified by name and PIN; assigned a role that governs access.
- **Shift**: A clock-in / clock-out record for one employee during one working period.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: All 15 domain entities are defined in the `core:domain` module with zero non-Kotlin framework imports — verified at compile time.
- **SC-002**: All status enumerations (`OrderStatus`, `PaymentStatus`, `TableStatus`, `Role`) are exhaustively handleable in `when` expressions without an `else` branch.
- **SC-003**: Monetary totals computed by summing any combination of order line items and payments produce exact results with no rounding error — verified by unit tests covering at least 10 distinct price combinations.
- **SC-004**: Every entity can be constructed and compared for equality in unit tests using only its primary constructor — no factories, builders, or framework annotations required.
- **SC-005**: All 6 bounded contexts (ordering, payments, customers, inventory, staff, layout) are represented; no entity from any context is missing its core fields as defined in FR-001 through FR-021.

---

## Assumptions

- Entities use value-based equality (equivalent to Kotlin `data class`) so tests can assert equality by content.
- `Money` is stored as a `Long` count of the smallest currency unit (e.g., cents); display formatting is a concern of the UI layer.
- Identifiers are opaque strings (wrapping a UUID); the precise type is an implementation decision resolved during planning.
- Tax rates and tax calculation are NOT modelled here — tax configuration belongs to a separate concern.
- Multi-currency support is out of scope; all amounts share a single configured currency.
- `Receipt` is an immutable value object derived from a closed order, not a separately persisted entity.
- Staff `PIN` is stored in hashed form; the hashing algorithm is an implementation detail.

---

## Out of Scope

- Business logic, validation rules, and state-machine enforcement — those belong to use-case layers in later features.
- Persistence mapping, database schema, and ORM annotations.
- API contracts, serialisation, and wire format annotations.
- Authentication and authorisation mechanisms (deferred to feature 004).
- Reporting aggregates, read models, and derived statistics.
