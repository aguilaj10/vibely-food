---
work_package_id: WP05
title: Payment Bounded Context
lane: "doing"
dependencies:
- WP04
- WP03
- WP01
base_branch: 003-core-domain-models-WP05-merge-base
base_commit: e41d774a2f06e28d1c2de107608223c8ef6bc386
created_at: '2026-03-24T02:48:51.085611+00:00'
subtasks:
- T018
- T019
- T020
- T021
- T022
- T023
phase: Phase 3 - Payment
assignee: ''
agent: ''
shell_pid: "95460"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-24T00:40:45Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-010
- FR-011
- FR-012
- FR-013
- FR-014
---

# Work Package Prompt: WP05 – Payment Bounded Context

## ⚠️ IMPORTANT: Review Feedback Status

Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section.

---

## Review Feedback

*[Empty initially. Populated by reviewers if work is returned.]*

---

## Implement Command

```bash
spec-kitty implement WP05 --base WP04
```

---

## Objectives & Success Criteria

1. Five files exist in `core/domain/src/commonMain/kotlin/com/vibely/domain/payment/`: `PaymentMethod.kt`, `PaymentStatus.kt`, `Payment.kt`, `ReceiptLineItem.kt`, `Receipt.kt`
2. `Receipt` is an immutable `data class` (all `val`)
3. Kotest test asserts `payments.sumOf { it.amount.cents } == total.cents`
4. `PaymentMethod` and `PaymentStatus` when-exhaustiveness tests pass without `else` branch
5. `./gradlew :core:domain:build` passes with zero errors

---

## Context & Constraints

- **Feature**: 003-core-domain-models | **Data model**: `kitty-specs/003-core-domain-models/data-model.md`
- **WP04 provides**: `OrderId`, `TableId`, `SelectedModifier` — all imported by payment types
- **WP03 provides**: `CustomerId` — imported by `Receipt.customerId?`
- **WP01 provides**: `Money`, `Timestamp`
- **Constitution**: Cross-package imports allowed: `com.vibely.domain.common.*`, `com.vibely.domain.ordering.{OrderId, TableId, SelectedModifier}`, `com.vibely.domain.customer.CustomerId`. No framework imports.
- **`Receipt` is NOT a persisted entity** — it is derived by the service layer at close time and is an immutable value object.
- **`Receipt.total` is a stored snapshot**: The service layer passes it in; the domain model does not recompute it.

---

## Subtasks & Detailed Guidance

### Subtask T018 – Create `PaymentMethod.kt`

**Purpose**: Enumeration of accepted tender types.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/payment/PaymentMethod.kt`

**Target**:
```kotlin
package com.vibely.domain.payment

enum class PaymentMethod {
    CASH,
    CARD,
    DIGITAL_WALLET,
    VOUCHER,
}
```

**Validation**: No imports. All four values present.

---

### Subtask T019 – Create `PaymentStatus.kt`

**Purpose**: Enumeration of payment outcome states.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/payment/PaymentStatus.kt`

**Target**:
```kotlin
package com.vibely.domain.payment

enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED,
}
```

**Validation**: No imports. All four values present.

---

### Subtask T020 – Create `Payment.kt`

**Purpose**: Single tender record against an order. One order may have multiple `Payment`s (split payments).

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/payment/Payment.kt`

**Target**:
```kotlin
package com.vibely.domain.payment

import com.vibely.domain.common.Money
import com.vibely.domain.common.Timestamp
import com.vibely.domain.ordering.OrderId

@JvmInline
value class PaymentId(val value: String)

data class Payment(
    val id: PaymentId,
    val orderId: OrderId,
    val amount: Money,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val timestamp: Timestamp,
)
```

**Notes**:
- `amount: Money` — can be zero for fully comped orders (spec edge case).
- `PaymentId` is declared at the top of the same file per convention.

---

### Subtask T021 – Create `ReceiptLineItem.kt`

**Purpose**: A single display line on a receipt, derived from an `OrderItem`. Not a separate entity — a value object.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/payment/ReceiptLineItem.kt`

**Target**:
```kotlin
package com.vibely.domain.payment

import com.vibely.domain.common.Money
import com.vibely.domain.ordering.SelectedModifier

data class ReceiptLineItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Money,
    val modifiers: List<SelectedModifier>,
)
```

**Notes**:
- `modifiers` imports `SelectedModifier` from the ordering context — cross-bounded-context reference, intentional per data model.
- `ReceiptLineItem` has no ID — it is a value object derived from `OrderItem`.

---

### Subtask T022 – Create `Receipt.kt`

**Purpose**: Immutable summary document generated when an order is fully settled. Captures all line items, subtotal, tax, total, and applied payments at close time.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/payment/Receipt.kt`

**Target**:
```kotlin
package com.vibely.domain.payment

import com.vibely.domain.common.Money
import com.vibely.domain.common.Timestamp
import com.vibely.domain.customer.CustomerId
import com.vibely.domain.ordering.OrderId
import com.vibely.domain.ordering.TableId

data class Receipt(
    val orderId: OrderId,
    val tableId: TableId,
    val customerId: CustomerId?,    // null for anonymous orders
    val lineItems: List<ReceiptLineItem>,
    val subtotal: Money,            // sum of (unitPrice + modifier adjustments) × quantity
    val taxAmount: Money,           // snapshot — computed by service layer
    val total: Money,               // subtotal + taxAmount (snapshot, not recomputed)
    val payments: List<Payment>,    // all payments applied to this order
    val closedAt: Timestamp,
)
```

**Notes**:
- `total` is stored as a snapshot — the service layer computes `subtotal + taxAmount` and passes it in. The domain model does NOT enforce `total == subtotal + taxAmount`; that is a service-layer convention (spec plan.md Phase 3 key constraint).
- All fields are `val` — `Receipt` is immutable.
- `Receipt` is derived and in-memory; it is NOT a separately persisted entity.

---

### Subtask T023 – Write Kotest tests for the payment context

**Purpose**: Verify `Receipt` construction and the payment sum convention, plus `PaymentMethod`/`PaymentStatus` when-exhaustiveness.

**File**: `core/domain/src/commonTest/kotlin/com/vibely/domain/payment/PaymentTest.kt`

**Target**:
```kotlin
package com.vibely.domain.payment

import com.vibely.domain.common.Money
import com.vibely.domain.common.Timestamp
import com.vibely.domain.customer.CustomerId
import com.vibely.domain.ordering.ModifierId
import com.vibely.domain.ordering.OrderId
import com.vibely.domain.ordering.SelectedModifier
import com.vibely.domain.ordering.TableId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PaymentTest : StringSpec({

    val now = Timestamp(1_700_000_000_000L)

    "Receipt sums payments equal to total" {
        val payment1 = Payment(
            id = PaymentId("pay-1"),
            orderId = OrderId("order-1"),
            amount = Money(1500),
            method = PaymentMethod.CASH,
            status = PaymentStatus.COMPLETED,
            timestamp = now,
        )
        val payment2 = Payment(
            id = PaymentId("pay-2"),
            orderId = OrderId("order-1"),
            amount = Money(500),
            method = PaymentMethod.CARD,
            status = PaymentStatus.COMPLETED,
            timestamp = now,
        )
        val lineItem = ReceiptLineItem(
            name = "Classic Burger",
            quantity = 1,
            unitPrice = Money(1099),
            modifiers = listOf(
                SelectedModifier(ModifierId("mod-1"), "Extra cheese", Money(150)),
            ),
        )
        val receipt = Receipt(
            orderId = OrderId("order-1"),
            tableId = TableId("table-3"),
            customerId = CustomerId("cust-1"),
            lineItems = listOf(lineItem),
            subtotal = Money(1800),
            taxAmount = Money(200),
            total = Money(2000),
            payments = listOf(payment1, payment2),
            closedAt = now,
        )

        receipt.payments.sumOf { it.amount.cents } shouldBe receipt.total.cents
        receipt.lineItems.size shouldBe 1
    }

    "Receipt allows null customerId for anonymous order" {
        val receipt = Receipt(
            orderId = OrderId("order-anon"),
            tableId = TableId("table-1"),
            customerId = null,
            lineItems = emptyList(),
            subtotal = Money.ZERO,
            taxAmount = Money.ZERO,
            total = Money.ZERO,
            payments = emptyList(),
            closedAt = now,
        )
        receipt.customerId shouldBe null
    }

    "PaymentMethod is exhaustively handleable without else" {
        PaymentMethod.values().forEach { method ->
            val label = when (method) {
                PaymentMethod.CASH -> "cash"
                PaymentMethod.CARD -> "card"
                PaymentMethod.DIGITAL_WALLET -> "digital wallet"
                PaymentMethod.VOUCHER -> "voucher"
            }
            label.isNotEmpty() shouldBe true
        }
    }

    "PaymentStatus is exhaustively handleable without else" {
        PaymentStatus.values().forEach { status ->
            val label = when (status) {
                PaymentStatus.PENDING -> "pending"
                PaymentStatus.COMPLETED -> "completed"
                PaymentStatus.FAILED -> "failed"
                PaymentStatus.REFUNDED -> "refunded"
            }
            label.isNotEmpty() shouldBe true
        }
    }

    "Payment amount can be zero for comped order" {
        val comped = Payment(
            id = PaymentId("pay-comp"),
            orderId = OrderId("order-comp"),
            amount = Money.ZERO,
            method = PaymentMethod.VOUCHER,
            status = PaymentStatus.COMPLETED,
            timestamp = now,
        )
        comped.amount shouldBe Money.ZERO
    }
})
```

**Validation**:
- [ ] All five test cases pass
- [ ] `when (method)` and `when (status)` compile without `else` — proves SC-002

---

## Risks & Mitigations

- **Cross-context imports**: `Receipt.kt` imports from `ordering` and `customer` packages. Both WP03 and WP04 must be merged before this WP compiles.
- **`ReceiptLineItem` has no ID**: It is a value object. Do not add a `ReceiptLineItemId`.
- **`Receipt.total` is NOT computed inside the domain model**: Do not add a computed property or `init` check. The service layer is responsible.

---

## Review Guidance

- Verify `Receipt` is `data class` with all `val` fields (no `var`)
- Verify `ReceiptLineItem` has no ID field
- Confirm `PaymentMethod` and `PaymentStatus` when-exhaustiveness tests compile without `else`
- Check `payments.sumOf { it.amount.cents } == total.cents` test passes
- Run `./gradlew :core:domain:build` — zero errors

---

## Activity Log

- 2026-03-24T00:40:45Z – system – lane=planned – Prompt created.
