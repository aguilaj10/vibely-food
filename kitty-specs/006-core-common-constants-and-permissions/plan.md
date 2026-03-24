# Implementation Plan: Core Common — Constants and Permissions

**Branch**: `006-core-common-constants-and-permissions` | **Date**: 2026-03-24 | **Spec**: [spec.md](spec.md)

---

## Summary

Two parallel work packages delivering pure-Kotlin value types and constants with zero dependencies:

1. **WP01** — Correct four existing enumerations in `core:domain` to match the database schema, and update all test references to renamed values.
2. **WP02** — Populate `core:common` with a new `Permission` type, a role-to-permission mapping, and three placeholder constant objects (`DatabaseConstants`, `ApiConstants`, `SyncConstants`).

The database schema (`docs/database-schema.sql`) is the authoritative source for all enum values. No research, no API contracts, no data model documents needed.

---

## Technical Context

**Language/Version**: Kotlin 2.3.20 (KMP — Android, JVM, JS targets)
**Primary Dependencies**: None — both `core:domain` and `core:common` have zero external dependencies by constitution
**Storage**: N/A — pure value types only
**Testing**: Kotest assertions in `jvmTest` — existing tests updated, no new tests required beyond build/Detekt gates
**Target Platform**: KMP `commonMain` (shared across Android, JVM, JS)
**Project Type**: KMP library modules
**Performance Goals**: N/A — pure value types, zero overhead
**Constraints**: Zero non-Kotlin imports; KDoc on every public symbol (Detekt-enforced)

**Affected Modules**:
- `core:domain` — existing enums updated (`Role`, `OrderStatus`, `TableStatus`, `PaymentMethod`)
- `core:common` — new types added (`Permission`, `RolePermissions`, `DatabaseConstants`, `ApiConstants`, `SyncConstants`); module already scaffolded and registered in `settings.gradle.kts`

---

## Constitution Check

| Rule | Status | Notes |
|------|--------|-------|
| `core:domain` must have zero framework dependencies | ✅ PASS | Enum corrections are pure Kotlin |
| `core:common` must have zero framework dependencies | ✅ PASS | Permission model and constants are pure Kotlin |
| KDoc on all public classes, functions, properties | ✅ REQUIRED | All new types and updated enums must have class-level KDoc |
| Use typed IDs | ✅ N/A | No new IDs introduced |
| No `expect/actual` in shared code | ✅ N/A | No platform branching needed |
| Type-safe Gradle version catalog accessors | ✅ N/A | No build file changes needed |

**Gate result**: PASS — no violations.

---

## Project Structure

### Documentation (this feature)

```
kitty-specs/006-core-common-constants-and-permissions/
├── plan.md       # This file
└── spec.md       # Feature specification
```

No `research.md` — all decisions resolved from the DB schema and existing domain.
No `data-model.md` — no new entities.
No `contracts/` — no API changes.

### Source Code

**New files (WP02)**:
```
core/common/src/commonMain/kotlin/com/vibely/common/
├── Permission.kt          # Permission enum (11 values)
├── RolePermissions.kt     # Role → Set<Permission> mapping + hasPermission()
├── DatabaseConstants.kt   # Placeholder DB connection constants
├── ApiConstants.kt        # Placeholder API constants
└── SyncConstants.kt       # Placeholder sync constants
```

**Modified files (WP01)**:
```
core/domain/src/commonMain/kotlin/com/vibely/domain/
├── staff/Role.kt           # SERVER → WAITER, add VIEWER
├── ordering/OrderStatus.kt # Replace all values to match DB schema
├── ordering/TableStatus.kt # FREE → AVAILABLE, add CLEANING
└── payment/PaymentMethod.kt# VOUCHER → SPLIT

core/domain/src/jvmTest/kotlin/com/vibely/domain/
├── staff/StaffTest.kt      # Update Role references
├── ordering/OrderTest.kt   # Update OrderStatus references
└── payment/PaymentTest.kt  # Update PaymentMethod references
```

No changes to any `build.gradle.kts` file, no new source sets.

---

## Enum Corrections Detail (WP01)

### `Role` — `com.vibely.domain.staff`
| Before | After | Change |
|--------|-------|--------|
| OWNER | OWNER | — |
| MANAGER | MANAGER | — |
| CASHIER | CASHIER | — |
| SERVER | WAITER | renamed |
| KITCHEN | KITCHEN | — |
| *(missing)* | VIEWER | added |

### `OrderStatus` — `com.vibely.domain.ordering`
| Before | After | Change |
|--------|-------|--------|
| OPEN | DRAFT | replaced |
| IN_PROGRESS | PENDING | replaced |
| *(missing)* | PREPARING | added |
| DELIVERED | READY | replaced |
| CLOSED | COMPLETED | replaced |
| VOID | CANCELLED | replaced |

### `TableStatus` — `com.vibely.domain.ordering`
| Before | After | Change |
|--------|-------|--------|
| FREE | AVAILABLE | renamed |
| OCCUPIED | OCCUPIED | — |
| RESERVED | RESERVED | — |
| *(missing)* | CLEANING | added |

### `PaymentMethod` — `com.vibely.domain.payment`
| Before | After | Change |
|--------|-------|--------|
| CASH | CASH | — |
| CARD | CARD | — |
| DIGITAL_WALLET | DIGITAL_WALLET | — |
| VOUCHER | BANK_TRANSFER | replaced |

Note: `SPLIT` is not a payment instrument — split bills are modelled as two `Payment` records (one per instrument). This enables per-instrument reporting.

---

## New Types Detail (WP02)

### `Permission` — `com.vibely.common`
```kotlin
enum class Permission {
    MANAGE_STORE, MANAGE_USERS, MANAGE_MENU, MANAGE_INVENTORY,
    CREATE_ORDER, UPDATE_ORDER_STATUS, PROCESS_PAYMENT,
    VIEW_ORDERS, VIEW_MENU, VIEW_TABLES, VIEW_REPORTS
}
```

### `RolePermissions` — `com.vibely.common`
Object that holds a `Map<Role, Set<Permission>>` and exposes:
- `fun permissionsFor(role: Role): Set<Permission>`
- Extension: `fun Role.hasPermission(permission: Permission): Boolean`

Mapping per FR-003:
```
OWNER   → all 11 permissions
MANAGER → all except MANAGE_USERS (10 permissions)
CASHIER → CREATE_ORDER, UPDATE_ORDER_STATUS, PROCESS_PAYMENT, VIEW_ORDERS, VIEW_MENU, VIEW_TABLES
WAITER  → CREATE_ORDER, VIEW_ORDERS, VIEW_MENU, VIEW_TABLES
KITCHEN → VIEW_ORDERS, UPDATE_ORDER_STATUS
VIEWER  → VIEW_ORDERS, VIEW_MENU, VIEW_TABLES, VIEW_REPORTS
```

### Placeholder Constants Strategy
Use Kotlin's `TODO("description")` for values not yet known. This compiles cleanly but throws `NotImplementedError` if accidentally used at runtime, providing a strong safety signal. Each constant carries a description of what it represents and where its value will come from.

**`DatabaseConstants`**: MAX_POOL_SIZE, MIN_IDLE, CONNECTION_TIMEOUT_MS, IDLE_TIMEOUT_MS, MAX_LIFETIME_MS, PREPARED_STATEMENT_CACHE_SIZE

**`ApiConstants`**: BASE_URL, API_VERSION (`"v1"` — this one is known), TIMEOUT_MS, MAX_RETRIES, RETRY_BACKOFF_MS

**`SyncConstants`**: SYNC_INTERVAL_MS, MAX_PENDING_EVENTS, EVENT_BATCH_SIZE

---

## Implementation Phases

All work is independent; WP01 and WP02 can run in parallel.

### WP01 — Enum Corrections in `core:domain`
1. Update `Role.kt`, `OrderStatus.kt`, `TableStatus.kt`, `PaymentMethod.kt`
2. Fix all test files referencing old enum values
3. `./gradlew :core:domain:build` must pass with zero errors/warnings
4. `./gradlew :core:domain:detekt` must pass (KDoc on all updated enums)

### WP02 — Permission Model and Constants in `core:common`
1. Create `Permission.kt`, `RolePermissions.kt`
2. Create `DatabaseConstants.kt`, `ApiConstants.kt`, `SyncConstants.kt`
3. `./gradlew :core:common:build` must pass with zero errors/warnings
4. `./gradlew :core:common:detekt` must pass

---

## Testing Strategy

No new tests required. WP01 must update existing tests to use the new enum values and ensure they continue to pass. WP02 types have no runtime behaviour — build + Detekt is the full gate.

---

## Definition of Done

- [ ] All four enums exactly match the DB schema types
- [ ] `Permission` enum with 11 values exists in `core:common`
- [ ] `RolePermissions` provides `permissionsFor(role)` and `hasPermission` extension
- [ ] All three constant objects exist with `TODO()` placeholders and descriptions
- [ ] `./gradlew :core:domain:build` passes — zero errors, zero warnings
- [ ] `./gradlew :core:common:build` passes — zero errors, zero warnings
- [ ] `./gradlew :core:domain:detekt` passes
- [ ] `./gradlew :core:common:detekt` passes
- [ ] All existing `core:domain` tests pass after enum corrections