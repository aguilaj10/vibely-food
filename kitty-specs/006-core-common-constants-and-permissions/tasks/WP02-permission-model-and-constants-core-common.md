---
work_package_id: WP02
title: Permission Model and Constants in core:common
lane: "done"
dependencies: []
base_branch: main
base_commit: 2b58f7202055decbbcd818a70b09af4bd6752aad
created_at: '2026-03-24T15:16:07.663314+00:00'
subtasks:
- T008
- T009
- T010
- T011
- T012
phase: Phase 1 - Permission Model and Constants
assignee: ''
agent: "claude-reviewer"
shell_pid: "35288"
review_status: "approved"
reviewed_by: "Jonathan Sánchez Muñoz"
history:
- timestamp: '2026-03-24T15:10:01Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-002
- FR-003
- FR-004
- FR-008
- FR-009
- FR-010
---

# Work Package Prompt: WP02 – Permission Model and Constants in core:common

## ⚠️ IMPORTANT: Review Feedback Status

Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section.

---

## Review Feedback

*[Empty initially. Populated by reviewers if work is returned.]*

---

## Implement Command

```bash
spec-kitty implement WP02
```

---

## Objectives & Success Criteria

1. `Permission` enum with exactly 11 values exists in `core:common`.
2. `RolePermissions` object provides `permissionsFor(role)` and a `Role.hasPermission(permission)` extension — all six roles have explicitly defined permission sets.
3. Three constant objects (`DatabaseConstants`, `ApiConstants`, `SyncConstants`) exist with `TODO()` placeholder bodies and KDoc on every property.
4. `./gradlew :core:common:build` passes — zero errors, zero warnings.
5. `./gradlew :core:common:detekt` passes — KDoc on all public symbols.

---

## Context & Constraints

- **Feature**: 006-core-common-constants-and-permissions
- **Plan**: `kitty-specs/006-core-common-constants-and-permissions/plan.md`
- **Constitution**: `.kittify/memory/constitution.md` — `core:common` must have zero framework dependencies; KDoc on all public symbols.
- **Target package**: `com.vibely.common` in `core/common/src/commonMain/kotlin/com/vibely/common/`
- **`core:domain` dependency**: `RolePermissions` imports `Role` from `com.vibely.domain.staff`. Check `core/common/build.gradle.kts` — if `core:domain` is not already a dependency, add it with `implementation(projects.core.domain)` inside the `commonMain` dependencies block.
- **`TODO()` placeholder semantics**: Use `TODO("description of what this value represents and where it comes from")` as the body for every placeholder constant. Kotlin's `TODO()` returns `Nothing`, satisfying any return type. The constants must be declared as `val NAME: Type` with explicit type annotations.
- **KDoc is critical**: Detekt enforces `UndocumentedPublicClass`, `UndocumentedPublicFunction`, `UndocumentedPublicProperty`. Every public class, object, function, and property needs KDoc.
- **No new source sets or build files**: All five files go into the existing `commonMain` source set. The only allowed build file change is adding the `core:domain` dependency if absent.

---

## Subtasks & Detailed Guidance

### Subtask T008 — Create `Permission.kt` [P]

**Purpose**: Define the 11 discrete actions that form the permission model.

**File**: `core/common/src/commonMain/kotlin/com/vibely/common/Permission.kt`

**Target implementation**:
```kotlin
package com.vibely.common

/**
 * Represents a discrete action a user may perform within the system.
 *
 * Permissions are assigned per [com.vibely.domain.staff.Role] via [RolePermissions].
 */
enum class Permission {

    /** Configure store name, currency, timezone, and operational settings. */
    MANAGE_STORE,

    /** Create, update, and deactivate user accounts. */
    MANAGE_USERS,

    /** Create, update, and remove menu items and categories. */
    MANAGE_MENU,

    /** Adjust stock levels and low-stock alert thresholds. */
    MANAGE_INVENTORY,

    /** Open new orders and add line items. */
    CREATE_ORDER,

    /** Advance an order through its lifecycle or cancel it. */
    UPDATE_ORDER_STATUS,

    /** Record and finalise payments for an order. */
    PROCESS_PAYMENT,

    /** Read order records. */
    VIEW_ORDERS,

    /** Read menu and category records. */
    VIEW_MENU,

    /** Read restaurant table and section layout. */
    VIEW_TABLES,

    /** Access sales and shift reports. */
    VIEW_REPORTS,
}
```

**Validation**:
- [ ] Exactly 11 values, in the order above
- [ ] Class-level KDoc present
- [ ] Each enum value has a single-line KDoc comment

---

### Subtask T009 — Create `RolePermissions.kt`

**Purpose**: Provide the authoritative role-to-permission mapping and a convenient `hasPermission` check.

**File**: `core/common/src/commonMain/kotlin/com/vibely/common/RolePermissions.kt`

**Dependency**: T008 (`Permission`) must exist; `Role` from `core:domain` must be importable.

**Mapping** (from FR-003):
| Role | Permissions |
|------|-------------|
| OWNER | All 11 |
| MANAGER | All except MANAGE_USERS (10) |
| CASHIER | CREATE_ORDER, UPDATE_ORDER_STATUS, PROCESS_PAYMENT, VIEW_ORDERS, VIEW_MENU, VIEW_TABLES |
| WAITER | CREATE_ORDER, VIEW_ORDERS, VIEW_MENU, VIEW_TABLES |
| KITCHEN | VIEW_ORDERS, UPDATE_ORDER_STATUS |
| VIEWER | VIEW_ORDERS, VIEW_MENU, VIEW_TABLES, VIEW_REPORTS |

**Target implementation**:
```kotlin
package com.vibely.common

import com.vibely.domain.staff.Role

/**
 * Maps each [Role] to the set of [Permission] values it holds.
 *
 * This mapping is authoritative and non-negotiable. Permission checks must use
 * [permissionsFor] or the [Role.hasPermission] extension rather than inspecting
 * this object directly.
 */
object RolePermissions {

    private val ALL = Permission.entries.toSet()

    private val mapping: Map<Role, Set<Permission>> = mapOf(
        Role.OWNER to ALL,
        Role.MANAGER to ALL - Permission.MANAGE_USERS,
        Role.CASHIER to setOf(
            Permission.CREATE_ORDER,
            Permission.UPDATE_ORDER_STATUS,
            Permission.PROCESS_PAYMENT,
            Permission.VIEW_ORDERS,
            Permission.VIEW_MENU,
            Permission.VIEW_TABLES,
        ),
        Role.WAITER to setOf(
            Permission.CREATE_ORDER,
            Permission.VIEW_ORDERS,
            Permission.VIEW_MENU,
            Permission.VIEW_TABLES,
        ),
        Role.KITCHEN to setOf(
            Permission.VIEW_ORDERS,
            Permission.UPDATE_ORDER_STATUS,
        ),
        Role.VIEWER to setOf(
            Permission.VIEW_ORDERS,
            Permission.VIEW_MENU,
            Permission.VIEW_TABLES,
            Permission.VIEW_REPORTS,
        ),
    )

    /**
     * Returns the set of [Permission] values held by the given [role].
     *
     * @param role The [Role] to query.
     * @return The non-null, non-empty set of permissions for [role].
     */
    fun permissionsFor(role: Role): Set<Permission> =
        mapping.getValue(role)
}

/**
 * Returns `true` if this role holds the given [permission].
 *
 * @param permission The [Permission] to check.
 */
fun Role.hasPermission(permission: Permission): Boolean =
    RolePermissions.permissionsFor(this).contains(permission)
```

**Notes**:
- `Permission.entries` requires Kotlin 1.9+; the project uses 2.3.20 so this is fine.
- `mapping.getValue(role)` throws `NoSuchElementException` if a role is missing — this is intentional and catches mapping gaps at development time. Every `Role` value must appear in the map.
- The `- Permission.MANAGE_USERS` set subtraction syntax is idiomatic Kotlin.

**Validation**:
- [ ] All 6 roles present in the mapping
- [ ] OWNER has all 11 permissions
- [ ] MANAGER has 10 permissions (all except MANAGE_USERS)
- [ ] CASHIER has exactly 6 permissions as listed
- [ ] WAITER has exactly 4 permissions as listed
- [ ] KITCHEN has exactly 2 permissions as listed
- [ ] VIEWER has exactly 4 permissions as listed
- [ ] `permissionsFor` has KDoc
- [ ] `hasPermission` extension has KDoc
- [ ] `mapping` private property does NOT need KDoc (it's private)

---

### Subtask T010 — Create `DatabaseConstants.kt` [P]

**Purpose**: Establish placeholder constants for database connection configuration.

**File**: `core/common/src/commonMain/kotlin/com/vibely/common/DatabaseConstants.kt`

**Target implementation**:
```kotlin
package com.vibely.common

/**
 * Placeholder constants for database connection pool configuration.
 *
 * All values are `TODO()` placeholders. Actual values will be provided by
 * environment-driven configuration when the infrastructure layer is built.
 */
object DatabaseConstants {

    /** Maximum number of connections in the pool. Source: infrastructure config. */
    val MAX_POOL_SIZE: Int
        get() = TODO("Determine from load testing — typically 10–20 for a single-store POS")

    /** Minimum number of idle connections maintained in the pool. Source: infrastructure config. */
    val MIN_IDLE: Int
        get() = TODO("Determine from baseline load — typically 2–5")

    /** Maximum time (ms) to wait for a connection from the pool before throwing. Source: infrastructure config. */
    val CONNECTION_TIMEOUT_MS: Long
        get() = TODO("Determine from SLA requirements — typically 30_000L")

    /** Maximum time (ms) a connection may sit idle before being evicted. Source: infrastructure config. */
    val IDLE_TIMEOUT_MS: Long
        get() = TODO("Determine from DB server keepalive settings — typically 600_000L")

    /** Maximum lifetime (ms) of a connection in the pool before it is retired. Source: infrastructure config. */
    val MAX_LIFETIME_MS: Long
        get() = TODO("Typically slightly less than DB server wait_timeout — e.g. 1_800_000L")

    /** Number of prepared statements cached per connection. Source: infrastructure config. */
    val PREPARED_STATEMENT_CACHE_SIZE: Int
        get() = TODO("Determine from query diversity analysis — typically 250")
}
```

**Notes**: Using `get() = TODO(…)` on a `val` property is the idiomatic way to declare a placeholder that compiles cleanly and throws at runtime. Do not use `= TODO(…)` directly on a `val` without a getter — some Kotlin versions may warn about it being unreachable.

**Validation**:
- [ ] Object exists with exactly 6 properties: MAX_POOL_SIZE, MIN_IDLE, CONNECTION_TIMEOUT_MS, IDLE_TIMEOUT_MS, MAX_LIFETIME_MS, PREPARED_STATEMENT_CACHE_SIZE
- [ ] Each property has KDoc describing what it represents and where its value comes from
- [ ] Class-level KDoc present
- [ ] No hardcoded numeric values

---

### Subtask T011 — Create `ApiConstants.kt` [P]

**Purpose**: Establish placeholder constants for API client configuration.

**File**: `core/common/src/commonMain/kotlin/com/vibely/common/ApiConstants.kt`

**Target implementation**:
```kotlin
package com.vibely.common

/**
 * Placeholder constants for API client configuration.
 *
 * [API_VERSION] is the only known value. All other values are `TODO()` placeholders
 * that will be replaced when the network layer is built.
 */
object ApiConstants {

    /** Base URL of the Vibely API. Source: environment configuration at runtime. */
    val BASE_URL: String
        get() = TODO("Provide via environment config — e.g. https://api.vibely.app")

    /** API version prefix appended to all request paths. Known value: \"v1\". */
    const val API_VERSION: String = "v1"

    /** Maximum time (ms) to wait for an API response before the request is cancelled. Source: UX/SLA requirements. */
    val TIMEOUT_MS: Long
        get() = TODO("Determine from UX requirements — typically 30_000L")

    /** Maximum number of retry attempts for transient failures. Source: reliability requirements. */
    val MAX_RETRIES: Int
        get() = TODO("Determine from reliability requirements — typically 3")

    /** Base backoff interval (ms) between retry attempts. Source: reliability requirements. */
    val RETRY_BACKOFF_MS: Long
        get() = TODO("Determine from reliability requirements — typically 1_000L with exponential multiplier")
}
```

**Notes**: `API_VERSION` is the only `const val` because its value is known. All others use `val` with `get() = TODO(…)`.

**Validation**:
- [ ] Object exists with exactly 5 properties: BASE_URL, API_VERSION, TIMEOUT_MS, MAX_RETRIES, RETRY_BACKOFF_MS
- [ ] `API_VERSION` is `const val` with value `"v1"`
- [ ] All other properties are `val` with `get() = TODO(…)` bodies
- [ ] Each property has KDoc
- [ ] Class-level KDoc present

---

### Subtask T012 — Create `SyncConstants.kt` [P]

**Purpose**: Establish placeholder constants for offline sync configuration.

**File**: `core/common/src/commonMain/kotlin/com/vibely/common/SyncConstants.kt`

**Target implementation**:
```kotlin
package com.vibely.common

/**
 * Placeholder constants for offline sync configuration.
 *
 * All values are `TODO()` placeholders. Actual values will be determined
 * when the sync layer is designed and load-tested.
 */
object SyncConstants {

    /** Interval (ms) between sync polling attempts when the device is online. Source: sync layer design. */
    val SYNC_INTERVAL_MS: Long
        get() = TODO("Determine from battery/data usage tradeoffs — typically 30_000L–60_000L")

    /** Maximum number of events that may queue locally before sync is forced. Source: sync layer design. */
    val MAX_PENDING_EVENTS: Int
        get() = TODO("Determine from offline usage patterns — typically 1_000")

    /** Number of events sent to the server in a single sync batch. Source: sync layer design. */
    val EVENT_BATCH_SIZE: Int
        get() = TODO("Determine from server throughput limits — typically 50–100")
}
```

**Validation**:
- [ ] Object exists with exactly 3 properties: SYNC_INTERVAL_MS, MAX_PENDING_EVENTS, EVENT_BATCH_SIZE
- [ ] Each property has KDoc describing what it represents
- [ ] Class-level KDoc present
- [ ] No hardcoded numeric values

---

## Risks & Mitigations

- **`core:domain` not in `core:common` dependencies**: `RolePermissions.kt` imports `com.vibely.domain.staff.Role`. Open `core/common/build.gradle.kts` and verify `implementation(projects.core.domain)` (or similar) is present in `commonMain`. If absent, add it — this is the one permitted build file change for this WP.
- **`Permission.entries` not available**: Available since Kotlin 1.9. The project is on 2.3.20, so this is safe. Do not use `enumValues<Permission>()` — `entries` is preferred.
- **`mapping.getValue(role)` for an unmapped role**: This will throw at development time, which is intentional. All 6 roles must be present in the map. Double-check after WP01 lands that `Role` now has 6 values (OWNER, MANAGER, CASHIER, WAITER, KITCHEN, VIEWER) — the dependency check is conceptual, not a hard blocker since WP01 and WP02 run in parallel and both work against the same source.
- **`TODO()` on `val` without getter**: Some IDE inspections flag `val X: T = TODO(…)` as immediately throwing. Use `get() = TODO(…)` instead to avoid the warning.

---

## Review Guidance

- Confirm all 5 files exist at the exact paths under `core/common/src/commonMain/kotlin/com/vibely/common/`.
- Run `./gradlew :core:common:build` — must pass with zero errors.
- Run `./gradlew :core:common:detekt` — must pass (KDoc check).
- Verify `Permission` has exactly 11 values.
- Verify `RolePermissions` mapping covers all 6 roles, with correct counts: OWNER=11, MANAGER=10, CASHIER=6, WAITER=4, KITCHEN=2, VIEWER=4.
- Verify `API_VERSION` is `const val "v1"` and all other constants use `get() = TODO(…)`.
- Verify no framework imports in any file.
- Verify `core/common/build.gradle.kts` uses `libs.xxx` type-safe accessors for any dependency additions (project-level accessors like `projects.core.domain` are fine).

---

## Activity Log

- 2026-03-24T15:10:01Z – system – lane=planned – Prompt generated via /spec-kitty.tasks
- 2026-03-24T15:16:08Z – claude-2 – shell_pid=30475 – lane=doing – Assigned agent via workflow command
- 2026-03-24T15:27:30Z – claude-2 – shell_pid=30475 – lane=for_review – Ready for review: Permission (11 values), RolePermissions (6 roles mapped), DatabaseConstants, ApiConstants, SyncConstants — build and detekt green
- 2026-03-24T15:27:46Z – claude-reviewer – shell_pid=35288 – lane=doing – Started review via workflow command
- 2026-03-24T15:28:30Z – claude-reviewer – shell_pid=35288 – lane=done – Review passed: Permission (11 values), RolePermissions (all 6 roles, correct permission counts), DatabaseConstants/ApiConstants/SyncConstants with TODO() placeholders and KDoc — build and detekt green, SC-002/SC-003/SC-004 satisfied
