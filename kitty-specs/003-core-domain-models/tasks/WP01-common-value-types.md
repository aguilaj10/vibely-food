---
work_package_id: WP01
title: Common Value Types
lane: "doing"
dependencies: []
base_branch: main
base_commit: 65a2a0bd9549049fbf775380e44a373a6b734c6f
created_at: '2026-03-24T00:52:05.369493+00:00'
subtasks:
- T001
- T002
- T003
- T004
phase: Phase 1 - Foundation
assignee: ''
agent: "claude"
shell_pid: "78762"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-24T00:40:45Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-014
---

# Work Package Prompt: WP01 – Common Value Types

## ⚠️ IMPORTANT: Review Feedback Status

**Read this first if you are implementing this task!**

- **Has review feedback?**: Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section immediately.
- **Mark as acknowledged**: When you understand the feedback and begin addressing it, update `review_status: acknowledged`.

---

## Review Feedback

*[This section is empty initially. Reviewers will populate it if the work is returned from review.]*

---

## Implement Command

```bash
spec-kitty implement WP01
```

No `--base` needed (no dependencies).

---

## Objectives & Success Criteria

1. Three files exist under `core/domain/src/commonMain/kotlin/com/vibely/domain/common/`: `Money.kt`, `Timestamp.kt`, `Duration.kt`
2. `Money` has a `ZERO` companion constant and three arithmetic operators
3. Kotest tests pass on all three KMP targets (JVM, Android, JS) with ≥10 Money arithmetic combinations
4. `./gradlew :core:domain:build` passes with zero errors and zero warnings
5. Zero non-Kotlin imports in all three files

---

## Context & Constraints

- **Feature**: 003-core-domain-models | **Spec**: `kitty-specs/003-core-domain-models/spec.md` | **Plan**: `kitty-specs/003-core-domain-models/plan.md`
- **Constitution rule**: `core:domain` must have zero framework dependencies. `@JvmInline` is a Kotlin language feature, not a library import — it is permitted.
- **KMP targets**: `commonMain` — code must compile for Android, JVM, and JS without platform-specific branches.
- **No `kotlinx` imports**: `kotlinx.datetime` is forbidden here (see research.md Decision 3). All three types wrap `Long` for platform portability.
- **These three files unblock everything else.** WP02–WP07 all import from `com.vibely.domain.common`. Merge this WP first.

---

## Subtasks & Detailed Guidance

### Subtask T001 – Create `Money.kt`

**Purpose**: Define the monetary value type. All price fields, modifiers, receipts, and payment amounts use `Money`. Integer cents prevent floating-point rounding errors (SC-003).

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/common/Money.kt`

**Steps**:

1. Declare the package at the top: `package com.vibely.domain.common`
2. Annotate with `@JvmInline` and declare as `value class Money(val cents: Long)`
3. Add a `companion object` with `val ZERO: Money = Money(0)`
4. Add three operator functions inside the value class:
   ```kotlin
   operator fun plus(other: Money): Money = Money(cents + other.cents)
   operator fun minus(other: Money): Money = Money(cents - other.cents)
   operator fun times(factor: Int): Money = Money(cents * factor)
   ```
5. No other methods. Display/formatting belongs to the UI layer.

**Full file (target)**:
```kotlin
package com.vibely.domain.common

@JvmInline
value class Money(val cents: Long) {
    operator fun plus(other: Money): Money = Money(cents + other.cents)
    operator fun minus(other: Money): Money = Money(cents - other.cents)
    operator fun times(factor: Int): Money = Money(cents * factor)

    companion object {
        val ZERO: Money = Money(0)
    }
}
```

**Validation**:
- [ ] Compiles in `commonMain` with zero warnings
- [ ] No imports (only Kotlin built-ins used)
- [ ] `Money.ZERO.cents == 0L`

---

### Subtask T002 – Create `Timestamp.kt`

**Purpose**: Typed UTC timestamp wrapper. Avoids using raw `Long` where a timestamp is expected. No `kotlinx.datetime` — that would add a framework dependency (constitution violation).

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/common/Timestamp.kt`

**Steps**:

1. Package: `package com.vibely.domain.common`
2. `@JvmInline value class Timestamp(val epochMillis: Long)` — single field, no methods.
3. No arithmetic operators (addition/subtraction of timestamps produces a `Duration`, which belongs to the service layer).

**Full file (target)**:
```kotlin
package com.vibely.domain.common

@JvmInline
value class Timestamp(val epochMillis: Long)
```

**Validation**:
- [ ] Compiles in `commonMain` with zero warnings
- [ ] No imports

---

### Subtask T003 – Create `Duration.kt`

**Purpose**: Typed elapsed-time wrapper. Used by `Shift.breakDuration`. Same rationale as Timestamp — no `kotlinx.datetime.DateTimePeriod`.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/common/Duration.kt`

**Steps**:

1. Package: `package com.vibely.domain.common`
2. `@JvmInline value class Duration(val millis: Long)` — single field, no methods.

**Full file (target)**:
```kotlin
package com.vibely.domain.common

@JvmInline
value class Duration(val millis: Long)
```

**Validation**:
- [ ] Compiles in `commonMain` with zero warnings
- [ ] No imports

---

### Subtask T004 – Write Kotest tests for common types

**Purpose**: Verify SC-003 (Money arithmetic exactness) and basic construction of all three value types.

**Files**:
- `core/domain/src/commonTest/kotlin/com/vibely/domain/common/MoneyTest.kt`
- `core/domain/src/commonTest/kotlin/com/vibely/domain/common/TimestampTest.kt`
- `core/domain/src/commonTest/kotlin/com/vibely/domain/common/DurationTest.kt`

**MoneyTest.kt — required test cases** (minimum 10 Money arithmetic combinations for SC-003):

```kotlin
package com.vibely.domain.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MoneyTest : StringSpec({
    "Money.ZERO has cents == 0" {
        Money.ZERO.cents shouldBe 0L
    }
    "plus combines cents exactly" {
        Money(1099) + Money(501) shouldBe Money(1600)
    }
    "plus with zero is identity" {
        Money(999) + Money.ZERO shouldBe Money(999)
    }
    "minus subtracts cents exactly" {
        Money(1600) - Money(501) shouldBe Money(1099)
    }
    "minus to zero" {
        Money(500) - Money(500) shouldBe Money.ZERO
    }
    "times scales by factor" {
        Money(333) * 3 shouldBe Money(999)
    }
    "times by one is identity" {
        Money(750) * 1 shouldBe Money(750)
    }
    "times by zero yields ZERO" {
        Money(750) * 0 shouldBe Money.ZERO
    }
    "sum of three prices is exact" {
        Money(100) + Money(200) + Money(300) shouldBe Money(600)
    }
    "sum of ten cent amounts — classic floating-point trap" {
        // 10 × 0.10 in floating point ≠ 1.00; in integer cents it must be exact
        (1..10).fold(Money.ZERO) { acc, _ -> acc + Money(10) } shouldBe Money(100)
    }
    "large amounts fit in Long" {
        Money(Long.MAX_VALUE / 2) + Money(1L) shouldBe Money(Long.MAX_VALUE / 2 + 1L)
    }
})
```

**TimestampTest.kt**:
```kotlin
package com.vibely.domain.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TimestampTest : StringSpec({
    "Timestamp stores epochMillis" {
        Timestamp(1_700_000_000_000L).epochMillis shouldBe 1_700_000_000_000L
    }
    "two Timestamps with same millis are equal" {
        Timestamp(42L) shouldBe Timestamp(42L)
    }
})
```

**DurationTest.kt**:
```kotlin
package com.vibely.domain.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DurationTest : StringSpec({
    "Duration stores millis" {
        Duration(3_600_000L).millis shouldBe 3_600_000L
    }
    "Duration zero" {
        Duration(0L).millis shouldBe 0L
    }
})
```

**Validation**:
- [ ] `./gradlew :core:domain:jvmTest` (or equivalent) passes
- [ ] ≥10 Money combinations tested (SC-003 satisfied)

---

## Risks & Mitigations

- **`@JvmInline` and JS target**: Value classes are supported on all three KMP targets in Kotlin 2.x. No risk.
- **`Duration` naming clash**: Kotlin stdlib has `kotlin.time.Duration`. Our `Duration` is in a different package (`com.vibely.domain.common.Duration`), so there is no clash as long as files don't import `kotlin.time.Duration`. These files have no imports, so no conflict.
- **Kotest version**: Ensure `io.kotest:kotest-assertions-core` is in `commonTest` dependencies (should already be configured per the project's `libs.versions.toml`).

---

## Review Guidance

- Verify zero imports in `Money.kt`, `Timestamp.kt`, `Duration.kt`
- Confirm `Money.ZERO` is a companion constant, not a top-level function
- Count Money test cases — must be ≥10 to satisfy SC-003
- Run `./gradlew :core:domain:build` — zero errors, zero warnings
- Check `./gradlew :core:domain:detekt` — zero violations

---

## Activity Log

- 2026-03-24T00:40:45Z – system – lane=planned – Prompt created.
- 2026-03-24T00:52:06Z – claude – shell_pid=61888 – lane=doing – Assigned agent via workflow command
- 2026-03-24T01:13:17Z – claude – shell_pid=61888 – lane=for_review – Ready for review: Money/Timestamp/Duration value classes + 15 Kotest tests. Build + ktlint + detekt all pass on JVM and JS targets.
- 2026-03-24T01:53:17Z – claude – shell_pid=61888 – lane=for_review – Ready for review: Money/Timestamp/Duration value classes with jvmTest tests passing. useJUnit() added to build.gradle.kts for jvmTest discovery.
- 2026-03-24T01:55:12Z – claude – shell_pid=78762 – lane=doing – Started review via workflow command
