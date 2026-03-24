---
work_package_id: WP06
title: Inventory Bounded Context
lane: planned
dependencies:
- WP01
subtasks:
- T024
- T025
- T026
- T027
phase: Phase 5 - Inventory (parallel with WP02, WP03, WP07)
assignee: ''
agent: ''
shell_pid: ''
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-24T00:40:45Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-017
- FR-018
---

# Work Package Prompt: WP06 – Inventory Bounded Context

## ⚠️ IMPORTANT: Review Feedback Status

Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section.

---

## Review Feedback

*[Empty initially. Populated by reviewers if work is returned.]*

---

## Implement Command

```bash
spec-kitty implement WP06 --base WP01
```

This WP can run in parallel with WP02, WP03, and WP07 — all share `--base WP01`.

---

## Objectives & Success Criteria

1. Three files exist in `core/domain/src/commonMain/kotlin/com/vibely/domain/inventory/`: `UnitOfMeasure.kt`, `IngredientStock.kt`, `StockAlert.kt`
2. `IngredientStock.quantity` and `alertThreshold` are `Double` (not `Long`, not a custom type)
3. Kotest test asserts `quantity < alertThreshold == true` for a below-threshold stock item
4. `UnitOfMeasure` when-exhaustiveness test passes without `else` branch
5. `./gradlew :core:domain:build` passes with zero errors

---

## Context & Constraints

- **Feature**: 003-core-domain-models | **Data model**: `kitty-specs/003-core-domain-models/data-model.md`
- **WP01 provides**: `Timestamp` — imported by `StockAlert`
- **Constitution**: Zero framework imports. Only `com.vibely.domain.common.Timestamp` crosses into this package.
- **`Double` for quantity**: Kitchen quantities are inherently fractional (1.5 kg, 0.25 L). Floating-point imprecision at the gram level is operationally irrelevant. This is research.md Decision 5 — do not change to `Long` or introduce a custom `Quantity` type.
- **No cross-context references** to ordering or payment from inventory.

---

## Subtasks & Detailed Guidance

### Subtask T024 – Create `UnitOfMeasure.kt`

**Purpose**: Enumeration of measurement units for ingredients.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/inventory/UnitOfMeasure.kt`

**Target**:
```kotlin
package com.vibely.domain.inventory

enum class UnitOfMeasure {
    KILOGRAM,
    GRAM,
    LITRE,
    MILLILITRE,
    UNIT,
    PORTION,
}
```

**Validation**: No imports. All six values present.

---

### Subtask T025 – Create `IngredientStock.kt`

**Purpose**: Tracks current on-hand quantity of a raw ingredient along with the alert threshold.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/inventory/IngredientStock.kt`

**Target**:
```kotlin
package com.vibely.domain.inventory

@JvmInline
value class IngredientId(val value: String)

data class IngredientStock(
    val id: IngredientId,
    val name: String,
    val quantity: Double,        // current on-hand amount in the declared unit
    val unit: UnitOfMeasure,
    val alertThreshold: Double,  // quantity below which a StockAlert is warranted
)
```

**Notes**:
- Both `quantity` and `alertThreshold` are `Double` — fractional kitchen measurements (research.md Decision 5).
- No `init` block validating `quantity >= 0` — that enforcement is a service-layer concern.

---

### Subtask T026 – Create `StockAlert.kt`

**Purpose**: Records that an ingredient fell below its configured alert threshold at a point in time.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/inventory/StockAlert.kt`

**Target**:
```kotlin
package com.vibely.domain.inventory

import com.vibely.domain.common.Timestamp

@JvmInline
value class StockAlertId(val value: String)

data class StockAlert(
    val id: StockAlertId,
    val ingredientId: IngredientId,
    val quantityAtAlert: Double,   // quantity that triggered the alert (snapshot)
    val unit: UnitOfMeasure,
    val timestamp: Timestamp,
)
```

**Notes**:
- `quantityAtAlert` is a snapshot of the quantity at the time the alert was raised.
- `ingredientId` is a reference back to `IngredientStock.id` — not an embedded `IngredientStock` object.

---

### Subtask T027 – Write Kotest tests for the inventory context

**Purpose**: Verify construction, below-threshold comparison, and `UnitOfMeasure` when-exhaustiveness.

**File**: `core/domain/src/commonTest/kotlin/com/vibely/domain/inventory/InventoryTest.kt`

**Target**:
```kotlin
package com.vibely.domain.inventory

import com.vibely.domain.common.Timestamp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class InventoryTest : StringSpec({

    "IngredientStock can be constructed" {
        val stock = IngredientStock(
            id = IngredientId("ing-1"),
            name = "Tomatoes",
            quantity = 5.0,
            unit = UnitOfMeasure.KILOGRAM,
            alertThreshold = 2.0,
        )
        stock.name shouldBe "Tomatoes"
        stock.quantity shouldBe 5.0
        stock.alertThreshold shouldBe 2.0
        stock.unit shouldBe UnitOfMeasure.KILOGRAM
    }

    "below-threshold detection: quantity < alertThreshold" {
        val stock = IngredientStock(
            id = IngredientId("ing-2"),
            name = "Cream",
            quantity = 1.5,
            unit = UnitOfMeasure.LITRE,
            alertThreshold = 2.0,
        )
        (stock.quantity < stock.alertThreshold) shouldBe true
    }

    "above-threshold stock is not below threshold" {
        val stock = IngredientStock(
            id = IngredientId("ing-3"),
            name = "Flour",
            quantity = 10.0,
            unit = UnitOfMeasure.KILOGRAM,
            alertThreshold = 2.0,
        )
        (stock.quantity < stock.alertThreshold) shouldBe false
    }

    "StockAlert captures snapshot quantity" {
        val alert = StockAlert(
            id = StockAlertId("alert-1"),
            ingredientId = IngredientId("ing-2"),
            quantityAtAlert = 1.5,
            unit = UnitOfMeasure.LITRE,
            timestamp = Timestamp(1_700_000_000_000L),
        )
        alert.quantityAtAlert shouldBe 1.5
        alert.ingredientId.value shouldBe "ing-2"
    }

    "UnitOfMeasure is exhaustively handleable without else" {
        UnitOfMeasure.values().forEach { unit ->
            val label = when (unit) {
                UnitOfMeasure.KILOGRAM -> "kg"
                UnitOfMeasure.GRAM -> "g"
                UnitOfMeasure.LITRE -> "L"
                UnitOfMeasure.MILLILITRE -> "mL"
                UnitOfMeasure.UNIT -> "unit"
                UnitOfMeasure.PORTION -> "portion"
            }
            label.isNotEmpty() shouldBe true
        }
    }
})
```

**Validation**:
- [ ] All five test cases pass
- [ ] `when (unit)` compiles without `else` — proves SC-002

---

## Risks & Mitigations

- **`Double` for quantity is intentional**: Do not change to `Long` or introduce a wrapper type. This was explicitly decided in research.md Decision 5.
- **`StockAlert.ingredientId` type**: Must be `IngredientId` (from the same package), not a `String`.

---

## Review Guidance

- Verify `IngredientStock.quantity` and `alertThreshold` are `Double`
- Verify `StockAlert.ingredientId: IngredientId` (not `String`)
- Confirm `UnitOfMeasure` when-exhaustiveness test compiles without `else`
- Run `./gradlew :core:domain:build` — zero errors

---

## Activity Log

- 2026-03-24T00:40:45Z – system – lane=planned – Prompt created.
