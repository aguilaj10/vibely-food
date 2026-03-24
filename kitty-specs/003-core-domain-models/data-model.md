# Data Model: Core Domain Models

**Feature**: 003-core-domain-models
**Date**: 2026-03-24
**Source**: spec.md FR-001–FR-021 + research.md decisions

All types live in `core/domain/src/commonMain/kotlin/` under the `com.vibely.domain` namespace. Every type is pure Kotlin — zero framework imports.

---

## Common Value Types (`com.vibely.domain.common`)

```
Money
  cents: Long                        // integer cents; e.g. 1099 = €10.99
  companion ZERO: Money              // Money(0)
  operator +, −, ×(Int)             // intrinsic arithmetic; not business logic
  [No other fields — display/formatting in UI layer]

Timestamp
  epochMillis: Long                  // UTC milliseconds since Unix epoch

Duration
  millis: Long                       // elapsed milliseconds
```

All three are `@JvmInline value class`.

---

## Ordering Bounded Context (`com.vibely.domain.ordering`)

### ID Types (all `@JvmInline value class`)

```
CategoryId(value: String)
MenuItemId(value: String)
ModifierGroupId(value: String)
ModifierId(value: String)
SectionId(value: String)
TableId(value: String)
OrderId(value: String)
```

### Enumerations

```
TableStatus
  FREE | OCCUPIED | RESERVED

OrderStatus
  OPEN | IN_PROGRESS | DELIVERED | CLOSED | VOID
```

### Entities and Value Objects

```
Category
  id:           CategoryId
  name:         String
  displayOrder: Int
  available:    Boolean

Modifier
  id:              ModifierId
  name:            String
  priceAdjustment: Money         // positive = surcharge, negative = discount, 0 = free
  available:       Boolean

ModifierGroup
  id:            ModifierGroupId
  name:          String
  required:      Boolean         // must the customer select at least one option?
  minSelections: Int             // inclusive lower bound (0 if not required)
  maxSelections: Int             // inclusive upper bound
  options:       List<Modifier>

MenuItem
  id:             MenuItemId
  name:           String
  description:    String
  basePrice:      Money
  available:      Boolean
  categoryId:     CategoryId
  modifierGroups: List<ModifierGroup>

Section
  id:     SectionId
  name:   String
  tables: List<TableId>          // ordered for display

Table
  id:       TableId
  sectionId: SectionId
  capacity:  Int
  status:    TableStatus

SelectedModifier                 // snapshot — captured at order time, immutable
  modifierId:       ModifierId   // reference for traceability
  name:             String       // snapshot of name at order time
  priceAdjustment:  Money        // snapshot of price at order time

OrderItem
  menuItemId:        MenuItemId
  menuItemName:      String      // snapshot of name at order time
  quantity:          Int         // must be ≥ 1
  unitPrice:         Money       // snapshot of base price at order time
  selectedModifiers: List<SelectedModifier>
  note:              String?     // optional special instruction

Order
  id:          OrderId
  tableId:     TableId
  customerId:  CustomerId?       // null for anonymous orders
  items:       List<OrderItem>   // may be empty while order is being built
  status:      OrderStatus
  createdAt:   Timestamp
  updatedAt:   Timestamp
  voidReason:  String?           // populated only when status = VOID
```

---

## Payment Bounded Context (`com.vibely.domain.payment`)

### ID Types (all `@JvmInline value class`)

```
PaymentId(value: String)
```

### Enumerations

```
PaymentMethod
  CASH | CARD | DIGITAL_WALLET | VOUCHER

PaymentStatus
  PENDING | COMPLETED | FAILED | REFUNDED
```

### Entities and Value Objects

```
Payment
  id:        PaymentId
  orderId:   OrderId
  amount:    Money
  method:    PaymentMethod
  status:    PaymentStatus
  timestamp: Timestamp

ReceiptLineItem                  // value object — derived, not stored separately
  name:              String
  quantity:          Int
  unitPrice:         Money
  modifiers:         List<SelectedModifier>

Receipt                          // immutable value object produced at close time
  orderId:    OrderId
  tableId:    TableId
  customerId: CustomerId?
  lineItems:  List<ReceiptLineItem>
  subtotal:   Money              // sum of (unitPrice + modifier adjustments) × quantity
  taxAmount:  Money              // tax computed by service layer; stored as snapshot
  total:      Money              // subtotal + taxAmount
  payments:   List<Payment>      // all payments applied to this order
  closedAt:   Timestamp
```

---

## Customer Bounded Context (`com.vibely.domain.customer`)

### ID Types (all `@JvmInline value class`)

```
CustomerId(value: String)
```

### Entities

```
Customer
  id:            CustomerId
  name:          String
  phone:         String?         // E.164 format; nullable for walk-ins
  email:         String?         // nullable
  loyaltyPoints: Int             // current balance; non-negative
```

---

## Inventory Bounded Context (`com.vibely.domain.inventory`)

### ID Types (all `@JvmInline value class`)

```
IngredientId(value: String)
StockAlertId(value: String)
```

### Enumerations

```
UnitOfMeasure
  KILOGRAM | GRAM | LITRE | MILLILITRE | UNIT | PORTION
```

### Entities

```
IngredientStock
  id:             IngredientId
  name:           String
  quantity:       Double         // current on-hand amount in the declared unit
  unit:           UnitOfMeasure
  alertThreshold: Double         // quantity below which a StockAlert is warranted

StockAlert
  id:               StockAlertId
  ingredientId:     IngredientId
  quantityAtAlert:  Double       // quantity that triggered the alert
  unit:             UnitOfMeasure
  timestamp:        Timestamp
```

---

## Staff Bounded Context (`com.vibely.domain.staff`)

### ID Types (all `@JvmInline value class`)

```
EmployeeId(value: String)
ShiftId(value: String)
```

### Enumerations

```
Role
  OWNER | MANAGER | CASHIER | SERVER | KITCHEN
```

### Entities

```
Employee
  id:      EmployeeId
  name:    String
  pinHash: String                // bcrypt or similar hash; raw PIN never stored
  role:    Role

Shift
  id:            ShiftId
  employeeId:    EmployeeId
  clockIn:       Timestamp
  clockOut:      Timestamp?      // null while the shift is still active
  breakDuration: Duration        // accumulated break time; zero if no breaks taken
```

---

## Entity Relationship Summary

```
Section ──< Table
Category ──< MenuItem ──< ModifierGroup ──< Modifier
Order >── Table
Order >── Customer (optional)
Order ──< OrderItem >── (snapshot of MenuItem)
OrderItem ──< SelectedModifier (snapshot of Modifier)
Payment >── Order
Receipt >── Order
Receipt ──< ReceiptLineItem
Receipt ──< Payment
StockAlert >── IngredientStock
Shift >── Employee
```

---

## Source File Layout

```
core/domain/src/commonMain/kotlin/com/vibely/domain/
├── common/
│   ├── Money.kt
│   ├── Timestamp.kt
│   └── Duration.kt
├── ordering/
│   ├── Category.kt
│   ├── Modifier.kt
│   ├── ModifierGroup.kt
│   ├── MenuItem.kt
│   ├── Section.kt
│   ├── Table.kt
│   ├── TableStatus.kt
│   ├── OrderStatus.kt
│   ├── SelectedModifier.kt
│   ├── OrderItem.kt
│   └── Order.kt
├── payment/
│   ├── PaymentMethod.kt
│   ├── PaymentStatus.kt
│   ├── Payment.kt
│   ├── ReceiptLineItem.kt
│   └── Receipt.kt
├── customer/
│   └── Customer.kt
├── inventory/
│   ├── UnitOfMeasure.kt
│   ├── IngredientStock.kt
│   └── StockAlert.kt
└── staff/
    ├── Role.kt
    ├── Employee.kt
    └── Shift.kt
```

**Total source files**: 27 (including 3 common value types, 12 ID value classes inlined at the top of each entity file)

---

## ID Value Class Convention

Each entity file declares its ID at the top, co-located with the entity:

```
// Order.kt
@JvmInline value class OrderId(val value: String)

data class Order(
  id: OrderId,
  ...
)
```

This keeps IDs discoverable alongside their entity without requiring a separate `ids/` package.
