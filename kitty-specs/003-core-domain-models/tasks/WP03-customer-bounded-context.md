---
work_package_id: WP03
title: Customer Bounded Context
lane: "for_review"
dependencies:
- WP01
base_branch: 003-core-domain-models-WP01
base_commit: 7c5a57dca0f4840ab52667f4afbc0d6562373dab
created_at: '2026-03-24T02:22:23.060169+00:00'
subtasks:
- T011
- T012
phase: Phase 4 - Customer (parallel with WP02, WP06, WP07)
assignee: ''
agent: "claude-wp03"
shell_pid: "83169"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-24T00:40:45Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-015
- FR-016
---

# Work Package Prompt: WP03 – Customer Bounded Context

## ⚠️ IMPORTANT: Review Feedback Status

Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section.

---

## Review Feedback

*[Empty initially. Populated by reviewers if work is returned.]*

---

## Implement Command

```bash
spec-kitty implement WP03 --base WP01
```

This WP can run in parallel with WP02, WP06, and WP07 — all three share the same `--base WP01` starting point.

---

## Objectives & Success Criteria

1. `core/domain/src/commonMain/kotlin/com/vibely/domain/customer/Customer.kt` exists with `CustomerId` and `Customer`
2. `CustomerTest.kt` in `commonTest` passes — nullable fields accept `null`, equality by content works
3. `./gradlew :core:domain:build` passes with zero errors
4. Zero non-Kotlin imports

---

## Context & Constraints

- **Why this WP must be merged before WP04**: `Order.customerId: CustomerId?` in the ordering context imports `CustomerId` from `com.vibely.domain.customer`. WP04 cannot compile until `CustomerId` is available on the base branch.
- **Small but critical**: This WP is intentionally small (one entity, one test). Its purpose is to define `CustomerId` as a compilation prerequisite for `Order.kt`.
- **Constitution**: Zero framework imports. No `kotlinx`, no Android SDK.
- **Nullable fields**: `phone` and `email` are nullable — anonymous walk-in customers are valid (spec FR-016, edge cases).

---

## Subtasks & Detailed Guidance

### Subtask T011 – Create `Customer.kt`

**Purpose**: Define the `Customer` entity with its typed ID. This is the sole entity in the customer bounded context.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/customer/Customer.kt`

**Target**:
```kotlin
package com.vibely.domain.customer

@JvmInline
value class CustomerId(val value: String)

data class Customer(
    val id: CustomerId,
    val name: String,
    val phone: String?,      // E.164 format; nullable for walk-ins
    val email: String?,      // nullable
    val loyaltyPoints: Int,  // current balance; non-negative by convention
)
```

**Notes**:
- `loyaltyPoints` is `Int`, non-negative by convention — enforcement is a service-layer concern.
- `phone` format (E.164) is documented in the data model; the domain model stores the string as-is.
- No imports needed (no `Money`, `Timestamp`, or cross-context references).

**Validation**:
- [ ] No imports
- [ ] Compiles in `commonMain`

---

### Subtask T012 – Write `CustomerTest.kt`

**Purpose**: Confirm construction, nullability, equality, and loyalty points field accessibility.

**File**: `core/domain/src/commonTest/kotlin/com/vibely/domain/customer/CustomerTest.kt`

**Target**:
```kotlin
package com.vibely.domain.customer

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class CustomerTest : StringSpec({
    "Customer can be constructed with all fields" {
        val customer = Customer(
            id = CustomerId("cust-001"),
            name = "Ana García",
            phone = "+34600000001",
            email = "ana@example.com",
            loyaltyPoints = 150,
        )
        customer.id.value shouldBe "cust-001"
        customer.name shouldBe "Ana García"
        customer.phone shouldBe "+34600000001"
        customer.email shouldBe "ana@example.com"
        customer.loyaltyPoints shouldBe 150
    }

    "Customer phone and email can be null for anonymous walk-in" {
        val walkIn = Customer(
            id = CustomerId("cust-anon"),
            name = "Walk-in",
            phone = null,
            email = null,
            loyaltyPoints = 0,
        )
        walkIn.phone.shouldBeNull()
        walkIn.email.shouldBeNull()
        walkIn.loyaltyPoints shouldBe 0
    }

    "Customer equality is value-based" {
        val c1 = Customer(CustomerId("x"), "Name", null, null, 0)
        val c2 = Customer(CustomerId("x"), "Name", null, null, 0)
        c1 shouldBe c2
    }

    "CustomerId wraps a string" {
        CustomerId("abc-123").value shouldBe "abc-123"
    }
})
```

**Validation**:
- [ ] All four test cases pass
- [ ] `./gradlew :core:domain:jvmTest` (or equivalent) passes

---

## Risks & Mitigations

- **WP04 dependency**: WP04 will not compile until this WP is merged. Ensure WP03 is reviewed and merged before starting WP04.
- **`loyaltyPoints` type**: Use `Int`, not `Long` — loyalty points are whole numbers that won't exceed `Int.MAX_VALUE` for a restaurant POS.

---

## Review Guidance

- Verify `CustomerId` is an `@JvmInline value class`, not a `data class` or type alias
- Verify `phone` and `email` are `String?` (nullable), not `String`
- Confirm zero imports in `Customer.kt`
- All four test cases pass

---

## Activity Log

- 2026-03-24T00:40:45Z – system – lane=planned – Prompt created.
- 2026-03-24T02:22:24Z – claude-wp03 – shell_pid=83169 – lane=doing – Assigned agent via workflow command
- 2026-03-24T02:30:30Z – claude-wp03 – shell_pid=83169 – lane=for_review – Ready for review: Customer entity, CustomerId value class, 4 jvmTest cases pass
