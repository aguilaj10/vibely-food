---
work_package_id: WP07
title: Staff Bounded Context
lane: "done"
dependencies:
- WP01
base_branch: 003-core-domain-models-WP01
base_commit: 7c5a57dca0f4840ab52667f4afbc0d6562373dab
created_at: '2026-03-24T02:22:23.058662+00:00'
subtasks:
- T028
- T029
- T030
- T031
phase: Phase 6 - Staff (parallel with WP02, WP03, WP06)
assignee: ''
agent: "claude"
shell_pid: "89213"
review_status: "approved"
reviewed_by: "Jonathan Sánchez Muñoz"
history:
- timestamp: '2026-03-24T00:40:45Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-019
- FR-020
- FR-021
---

# Work Package Prompt: WP07 – Staff Bounded Context

## ⚠️ IMPORTANT: Review Feedback Status

Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section.

---

## Review Feedback

*[Empty initially. Populated by reviewers if work is returned.]*

---

## Implement Command

```bash
spec-kitty implement WP07 --base WP01
```

This WP can run in parallel with WP02, WP03, and WP06 — all share `--base WP01`.

---

## Objectives & Success Criteria

1. Three files exist in `core/domain/src/commonMain/kotlin/com/vibely/domain/staff/`: `Role.kt`, `Employee.kt`, `Shift.kt`
2. `Employee.pinHash` field is named `pinHash` (not `pin`)
3. `Shift.clockOut: Timestamp?` is nullable
4. Kotest tests pass: `role.name == "MANAGER"`, `clockOut == null`, `Role` when-exhaustiveness
5. `./gradlew :core:domain:build` passes with zero errors

---

## Context & Constraints

- **Feature**: 003-core-domain-models | **Data model**: `kitty-specs/003-core-domain-models/data-model.md`
- **WP01 provides**: `Timestamp`, `Duration` — imported by `Shift`
- **Constitution**: Zero framework imports. Only `com.vibely.domain.common.Timestamp` and `com.vibely.domain.common.Duration` cross into this package.
- **`pinHash` naming**: The field stores a hash string (bcrypt or similar), never the raw PIN. The name `pinHash` makes this explicit. Using `pin` would be misleading and could cause a future developer to store a raw PIN by mistake.
- **Authentication logic** (hashing algorithm, verification) belongs to feature 004, not to this domain model.

---

## Subtasks & Detailed Guidance

### Subtask T028 – Create `Role.kt`

**Purpose**: Enumeration of staff permission levels.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/staff/Role.kt`

**Target**:
```kotlin
package com.vibely.domain.staff

enum class Role {
    OWNER,
    MANAGER,
    CASHIER,
    SERVER,
    KITCHEN,
}
```

**Validation**: No imports. All five values present.

---

### Subtask T029 – Create `Employee.kt`

**Purpose**: A staff member identified by name and a hashed PIN, assigned a role.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/staff/Employee.kt`

**Target**:
```kotlin
package com.vibely.domain.staff

@JvmInline
value class EmployeeId(val value: String)

data class Employee(
    val id: EmployeeId,
    val name: String,
    val pinHash: String, // bcrypt or similar hash; raw PIN never stored in domain
    val role: Role,
)
```

**Notes**:
- `pinHash` is a `String` — the hashing algorithm (bcrypt, argon2, etc.) is an implementation detail of the authentication service (feature 004).
- `EmployeeId` is co-located at the top of the file per convention.

---

### Subtask T030 – Create `Shift.kt`

**Purpose**: A clock-in/clock-out record for one employee during one working period.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/staff/Shift.kt`

**Target**:
```kotlin
package com.vibely.domain.staff

import com.vibely.domain.common.Duration
import com.vibely.domain.common.Timestamp

@JvmInline
value class ShiftId(val value: String)

data class Shift(
    val id: ShiftId,
    val employeeId: EmployeeId,
    val clockIn: Timestamp,
    val clockOut: Timestamp?,      // null while the shift is still active
    val breakDuration: Duration,   // accumulated break time; Duration(0) if no breaks taken
)
```

**Notes**:
- `clockOut: Timestamp?` — nullable while the shift is ongoing.
- `breakDuration: Duration` — use `Duration(0)` for shifts with no breaks. This field is always present (not nullable) because "no break time" is `Duration(0)`, not `null`.
- `employeeId: EmployeeId` — reference only; do not embed `Employee`.

---

### Subtask T031 – Write Kotest tests for the staff context

**Purpose**: Verify construction, `clockOut == null` for active shifts, and `Role` when-exhaustiveness.

**File**: `core/domain/src/commonTest/kotlin/com/vibely/domain/staff/StaffTest.kt`

**Target**:
```kotlin
package com.vibely.domain.staff

import com.vibely.domain.common.Duration
import com.vibely.domain.common.Timestamp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class StaffTest : StringSpec({

    val clockInTime = Timestamp(1_700_000_000_000L)

    "Employee can be constructed with role MANAGER" {
        val employee = Employee(
            id = EmployeeId("emp-1"),
            name = "Ana García",
            pinHash = "\$2b\$12\$hashedValueHere",
            role = Role.MANAGER,
        )
        employee.role shouldBe Role.MANAGER
        employee.role.name shouldBe "MANAGER"
        employee.pinHash.isNotEmpty() shouldBe true
    }

    "Active shift has null clockOut" {
        val shift = Shift(
            id = ShiftId("shift-1"),
            employeeId = EmployeeId("emp-1"),
            clockIn = clockInTime,
            clockOut = null,
            breakDuration = Duration(0L),
        )
        shift.clockOut.shouldBeNull()
        shift.breakDuration.millis shouldBe 0L
    }

    "Completed shift has non-null clockOut" {
        val clockOutTime = Timestamp(1_700_003_600_000L) // 1 hour later
        val shift = Shift(
            id = ShiftId("shift-2"),
            employeeId = EmployeeId("emp-2"),
            clockIn = clockInTime,
            clockOut = clockOutTime,
            breakDuration = Duration(900_000L), // 15 minutes break
        )
        shift.clockOut shouldBe clockOutTime
        shift.breakDuration.millis shouldBe 900_000L
    }

    "Role is exhaustively handleable without else" {
        Role.values().forEach { role ->
            val label = when (role) {
                Role.OWNER -> "owner"
                Role.MANAGER -> "manager"
                Role.CASHIER -> "cashier"
                Role.SERVER -> "server"
                Role.KITCHEN -> "kitchen"
            }
            label.isNotEmpty() shouldBe true
        }
    }

    "EmployeeId wraps a string" {
        EmployeeId("emp-abc").value shouldBe "emp-abc"
    }
})
```

**Validation**:
- [ ] All five test cases pass
- [ ] `when (role)` compiles without `else` — proves SC-002
- [ ] `clockOut.shouldBeNull()` passes for active shift

---

## Risks & Mitigations

- **`pinHash` naming**: Must be `pinHash`, not `pin`. Reviewers should reject any PR where this field is named `pin`.
- **`breakDuration: Duration` not nullable**: Zero break time is `Duration(0L)`, not `null`. This makes accumulation logic simpler in the service layer.
- **`Shift.clockOut` is nullable**: Do not accidentally make it non-nullable. An active shift has no clock-out time yet.

---

## Review Guidance

- Verify `Employee.pinHash: String` is named exactly `pinHash` (not `pin`, not `hashedPin`)
- Verify `Shift.clockOut: Timestamp?` is nullable
- Verify `Shift.breakDuration: Duration` is NOT nullable (zero = `Duration(0)`)
- Confirm `Role` when-exhaustiveness test compiles without `else`
- Run `./gradlew :core:domain:build` — zero errors

---

## Activity Log

- 2026-03-24T00:40:45Z – system – lane=planned – Prompt created.
- 2026-03-24T02:22:23Z – claude-wp07 – shell_pid=83169 – lane=doing – Assigned agent via workflow command
- 2026-03-24T02:35:27Z – claude-wp07 – shell_pid=83169 – lane=for_review – Ready for review: Role enum, Employee with pinHash, Shift with nullable clockOut, 5 jvmTest cases pass
- 2026-03-24T02:38:19Z – claude – shell_pid=89213 – lane=doing – Started review via workflow command
- 2026-03-24T02:45:16Z – claude – shell_pid=89213 – lane=done – Review passed: Role enum, Employee with pinHash, Shift with nullable clockOut + non-nullable breakDuration, 5 jvmTest cases, build clean
