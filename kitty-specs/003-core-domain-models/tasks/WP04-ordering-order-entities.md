---
work_package_id: WP04
title: Ordering — Order Entities
lane: "done"
dependencies:
- WP01
- WP02
- WP03
base_branch: 003-core-domain-models-WP04-merge-base
base_commit: 84862d647a5efe01a955991441ce43d2b9eb7424
created_at: '2026-03-24T02:46:05.076401+00:00'
subtasks:
- T013
- T014
- T015
- T016
- T017
phase: Phase 2 - Ordering Bounded Context (Part 2)
assignee: ''
agent: "claude"
shell_pid: "93507"
review_status: "approved"
reviewed_by: "Jonathan Sánchez Muñoz"
history:
- timestamp: '2026-03-24T00:40:45Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-005
- FR-006
- FR-007
---

# Work Package Prompt: WP04 – Ordering — Order Entities

## ⚠️ IMPORTANT: Review Feedback Status

Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section.

---

## Review Feedback

*[Empty initially. Populated by reviewers if work is returned.]*

---

## Implement Command

```bash
spec-kitty implement WP04 --base WP03
```

> Note: WP04 depends on both WP02 and WP03. Use `--base WP03` (the most recent merge). WP02 will already be included in the merged history by the time WP03 is merged.

---

## Objectives & Success Criteria

1. Four files exist: `OrderStatus.kt`, `SelectedModifier.kt`, `OrderItem.kt`, `Order.kt` in `ordering/`
2. `Order.customerId: CustomerId?` correctly imports from `com.vibely.domain.customer`
3. Kotest tests pass: full `Order` construction with two `OrderItem`s (one with a modifier), `OrderStatus` exhaustiveness in `when`
4. `./gradlew :core:domain:build` passes with zero errors and zero warnings
5. Zero non-Kotlin imports (only `com.vibely.domain.common.*` and `com.vibely.domain.customer.CustomerId` allowed as cross-package imports)

---

## Context & Constraints

- **Feature**: 003-core-domain-models | **Data model**: `kitty-specs/003-core-domain-models/data-model.md`
- **WP02 provides**: `CategoryId`, `MenuItemId`, `ModifierId`, `ModifierGroupId`, `SectionId`, `TableId`, `Table`, `TableStatus`, `Section`, `MenuItem`, `ModifierGroup`, `Modifier`, `Category`
- **WP03 provides**: `CustomerId` — imported by `Order.kt`
- **WP01 provides**: `Money`, `Timestamp`
- **Snapshot pattern**: `SelectedModifier` captures `name` and `priceAdjustment` **at order time** — not a live reference. This ensures historical orders remain correct after menu price changes (research.md Decision 8).
- **Constitution**: Only `com.vibely.domain.common.*` and `com.vibely.domain.customer.CustomerId` are cross-package imports. No framework imports.

---

## Subtasks & Detailed Guidance

### Subtask T013 – Create `OrderStatus.kt`

**Purpose**: Enumeration of all order lifecycle states. Must be exhaustively handleable in `when` expressions — no `else` branch needed (SC-002).

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/OrderStatus.kt`

**Target**:
```kotlin
package com.vibely.domain.ordering

enum class OrderStatus {
    OPEN,
    IN_PROGRESS,
    DELIVERED,
    CLOSED,
    VOID,
}
```

**Validation**:
- [ ] All five values present
- [ ] No imports

---

### Subtask T014 – Create `SelectedModifier.kt`

**Purpose**: Immutable snapshot of a modifier at the time of ordering. Captures `name` and `priceAdjustment` so the order record remains accurate even after the modifier's price changes later.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/SelectedModifier.kt`

**Target**:
```kotlin
package com.vibely.domain.ordering

import com.vibely.domain.common.Money

data class SelectedModifier(
    val modifierId: ModifierId,       // traceability reference back to the original modifier
    val name: String,                 // snapshot of modifier name at order time
    val priceAdjustment: Money,       // snapshot of price at order time
)
```

**Notes**:
- `modifierId` is kept for traceability (e.g., reporting which modifier was chosen), but the display name and price are snapshots.
- No `ModifierId` ID at the top of this file — `SelectedModifier` is a value object, not an entity. It has no identity of its own.

---

### Subtask T015 – Create `OrderItem.kt`

**Purpose**: One line in an order. Captures the menu item reference, quantity, price snapshot, selected modifiers, and an optional special instruction.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/OrderItem.kt`

**Target**:
```kotlin
package com.vibely.domain.ordering

import com.vibely.domain.common.Money

data class OrderItem(
    val menuItemId: MenuItemId,
    val menuItemName: String,              // snapshot of name at order time
    val quantity: Int,                     // must be ≥ 1 (enforced by service layer)
    val unitPrice: Money,                  // snapshot of base price at order time
    val selectedModifiers: List<SelectedModifier>,
    val note: String?,                     // optional special instruction
)
```

**Notes**:
- `menuItemName` is a snapshot — name may change in the menu after the order is placed.
- `unitPrice` is a snapshot of `MenuItem.basePrice` at order time.
- `selectedModifiers` may be empty (no customisation selected).

---

### Subtask T016 – Create `Order.kt`

**Purpose**: The central transaction record linking table, customer, items, status, and timestamps. This is the most referenced entity in the system.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/Order.kt`

**Target**:
```kotlin
package com.vibely.domain.ordering

import com.vibely.domain.common.Timestamp
import com.vibely.domain.customer.CustomerId

@JvmInline
value class OrderId(val value: String)

data class Order(
    val id: OrderId,
    val tableId: TableId,
    val customerId: CustomerId?,   // null for anonymous orders
    val items: List<OrderItem>,    // may be empty while order is being built
    val status: OrderStatus,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val voidReason: String?,       // populated only when status == VOID
)
```

**Notes**:
- `customerId: CustomerId?` — cross-context import from `com.vibely.domain.customer`. This is intentional and documented in the data model.
- `items: List<OrderItem>` — allowed to be empty (spec edge case: order may be reserved or partially created).
- `voidReason: String?` — only meaningful when `status == VOID`. Domain model does not enforce this; service layer does.

---

### Subtask T017 – Write Kotest tests for the ordering context

**Purpose**: Verify full `Order` construction, snapshot correctness, `OrderStatus` exhaustiveness in `when`, and `customerId == null` for anonymous orders.

**Files**:
- `core/domain/src/commonTest/kotlin/com/vibely/domain/ordering/OrderTest.kt`

**Target**:
```kotlin
package com.vibely.domain.ordering

import com.vibely.domain.common.Money
import com.vibely.domain.common.Timestamp
import com.vibely.domain.customer.CustomerId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class OrderTest : StringSpec({

    val now = Timestamp(1_700_000_000_000L)

    "Order can be constructed with all required fields" {
        val modifier = SelectedModifier(
            modifierId = ModifierId("mod-1"),
            name = "Extra cheese",
            priceAdjustment = Money(150),
        )
        val item1 = OrderItem(
            menuItemId = MenuItemId("item-1"),
            menuItemName = "Classic Burger",
            quantity = 1,
            unitPrice = Money(1099),
            selectedModifiers = listOf(modifier),
            note = "No onions",
        )
        val item2 = OrderItem(
            menuItemId = MenuItemId("item-2"),
            menuItemName = "Fries",
            quantity = 2,
            unitPrice = Money(399),
            selectedModifiers = emptyList(),
            note = null,
        )
        val order = Order(
            id = OrderId("order-001"),
            tableId = TableId("table-5"),
            customerId = CustomerId("cust-001"),
            items = listOf(item1, item2),
            status = OrderStatus.OPEN,
            createdAt = now,
            updatedAt = now,
            voidReason = null,
        )

        order.id.value shouldBe "order-001"
        order.items.size shouldBe 2
        order.status shouldBe OrderStatus.OPEN
        order.voidReason.shouldBeNull()
        order.items[0].selectedModifiers[0].priceAdjustment shouldBe Money(150)
    }

    "Order allows null customerId for anonymous order" {
        val order = Order(
            id = OrderId("order-anon"),
            tableId = TableId("table-1"),
            customerId = null,
            items = emptyList(),
            status = OrderStatus.OPEN,
            createdAt = now,
            updatedAt = now,
            voidReason = null,
        )
        order.customerId.shouldBeNull()
        order.items shouldBe emptyList()
    }

    "Order with VOID status can carry voidReason" {
        val order = Order(
            id = OrderId("order-void"),
            tableId = TableId("table-2"),
            customerId = null,
            items = emptyList(),
            status = OrderStatus.VOID,
            createdAt = now,
            updatedAt = now,
            voidReason = "Customer left",
        )
        order.status shouldBe OrderStatus.VOID
        order.voidReason shouldBe "Customer left"
    }

    "OrderStatus is exhaustively handleable without else" {
        OrderStatus.values().forEach { status ->
            val label = when (status) {
                OrderStatus.OPEN -> "open"
                OrderStatus.IN_PROGRESS -> "in progress"
                OrderStatus.DELIVERED -> "delivered"
                OrderStatus.CLOSED -> "closed"
                OrderStatus.VOID -> "void"
                // No else branch — exhaustiveness enforced by compiler (SC-002)
            }
            label.isNotEmpty() shouldBe true
        }
    }

    "SelectedModifier captures snapshot values" {
        val snap = SelectedModifier(
            modifierId = ModifierId("mod-x"),
            name = "Jalapeños",
            priceAdjustment = Money(50),
        )
        snap.name shouldBe "Jalapeños"
        snap.priceAdjustment.cents shouldBe 50L
    }
})
```

**Validation**:
- [ ] All five test cases pass
- [ ] `when (status) { ... }` block compiles without `else` — proves SC-002

---

## Risks & Mitigations

- **Cross-context import**: `Order.kt` imports `CustomerId` from `com.vibely.domain.customer`. WP03 must be merged before this WP compiles.
- **`Order.items` empty list**: Spec explicitly allows it. Do not add an `init` block validating that items is non-empty.
- **`SelectedModifier` has no ID**: It is a value object (snapshot), not an entity. Do not add a `SelectedModifierId` — that would be incorrect per the data model.

---

## Review Guidance

- Verify `Order.customerId: CustomerId?` imports from `com.vibely.domain.customer` (not a local redefinition)
- Verify `SelectedModifier` has no ID field — it is a snapshot value object
- Confirm `OrderStatus` when-exhaustiveness test compiles without `else` branch
- Check `./gradlew :core:domain:build` passes
- Confirm `./gradlew :core:domain:detekt` passes

---

## Activity Log

- 2026-03-24T00:40:45Z – system – lane=planned – Prompt created.
- 2026-03-24T02:46:07Z – claude – shell_pid=93507 – lane=doing – Assigned agent via workflow command
- 2026-03-24T02:48:16Z – claude – shell_pid=93507 – lane=for_review – Ready for review: OrderStatus, SelectedModifier, OrderItem, Order with OrderId, 5 jvmTest cases, build clean
- 2026-03-24T02:48:38Z – claude – shell_pid=93507 – lane=done – Review passed: Order, OrderItem, SelectedModifier, OrderStatus — snapshot pattern correct, CustomerId? from customer context, JvmInline with import, 5 jvmTest cases, build and ktlint clean
