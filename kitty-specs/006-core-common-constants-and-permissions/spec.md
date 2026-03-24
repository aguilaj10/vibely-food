# Feature Specification: Core Common — Constants and Permissions

**Feature**: 006-core-common-constants-and-permissions
**Status**: Draft
**Date**: 2026-03-24
**Mission**: software-dev

---

## Overview

Establish a shared `core:common` module that provides the cross-cutting constants, permission definitions, and role enumeration used across all features of the Vibely POS. This feature also corrects misalignments between the domain enumerations introduced in feature 003 and the authoritative database schema.

---

## Problem Statement

The domain layer defines several enumerations that have drifted from the database schema, which is the authoritative source of truth for the data model. Before any infrastructure or persistence layer is built, these enumerations must be aligned so that future implementations map cleanly to the database without requiring disruptive corrections. Additionally, the system has no centralised location for shared constants (connection settings, API timeouts, sync intervals) or for the permission model that governs what each user role can do.

---

## Goals

- Align all domain enumerations to the database schema.
- Define a clear, testable permission model mapping each role to the actions it is allowed to perform.
- Provide a single `core:common` module as the home for cross-cutting definitions that do not belong to any specific bounded context.
- Use placeholder values for infrastructure constants (database, API, sync) so the structure is established without hardcoding values that are not yet known.

---

## Scope

### In Scope

- **Role alignment**: Update the `Role` enumeration to match the database schema exactly (`OWNER`, `MANAGER`, `CASHIER`, `WAITER`, `KITCHEN`, `VIEWER`). This replaces `SERVER` (renamed to `WAITER`) and adds `VIEWER`.
- **Permission model**: Define a `Permission` type listing every discrete action in the system, and a mapping from each `Role` to the set of `Permission` values it holds.
- **Enum corrections**: Correct the following enumerations introduced in feature 003 to match the database schema:
  - `OrderStatus`: align to `DRAFT`, `PENDING`, `PREPARING`, `READY`, `COMPLETED`, `CANCELLED`
  - `TableStatus`: align to `AVAILABLE`, `OCCUPIED`, `RESERVED`, `CLEANING`
  - `PaymentMethod`: align to `CASH`, `CARD`, `DIGITAL_WALLET`, `BANK_TRANSFER`
- **Placeholder constants**: Create named constant groups for database configuration, API configuration, and sync configuration using placeholder values. Each placeholder must be documented with a description of what the value represents and where it will come from.

### Out of Scope

- Filling in real values for the placeholder constants (deferred until the relevant infrastructure layer is built).
- Authentication or session management.
- Any persistence implementation.
- Creating a new `core:common` Gradle module — if the module does not yet exist, files are added to the existing project structure; module creation is deferred to the infrastructure feature.

---

## Functional Requirements

### FR-001 — Role Enumeration Aligned to Schema
The `Role` enumeration must contain exactly these values, matching the database `user_role` type: `OWNER`, `MANAGER`, `CASHIER`, `WAITER`, `KITCHEN`, `VIEWER`. No other values are permitted. The previous `SERVER` value is replaced by `WAITER`.

### FR-002 — Permission Model
A `Permission` type defines every discrete action a user can perform:
- `MANAGE_STORE` — configure store settings
- `MANAGE_USERS` — create, update, and deactivate users
- `MANAGE_MENU` — create, update, and remove menu items and categories
- `MANAGE_INVENTORY` — adjust stock levels and thresholds
- `CREATE_ORDER` — open new orders and add items
- `UPDATE_ORDER_STATUS` — advance or cancel an order
- `PROCESS_PAYMENT` — record and finalise payments
- `VIEW_ORDERS` — read order records
- `VIEW_MENU` — read menu and category records
- `VIEW_TABLES` — read table and section layout
- `VIEW_REPORTS` — access sales and shift reports

### FR-003 — Role-to-Permission Mapping
Each role maps to a fixed, non-negotiable set of permissions:
- `OWNER` — all permissions
- `MANAGER` — all permissions except `MANAGE_USERS`
- `CASHIER` — `CREATE_ORDER`, `UPDATE_ORDER_STATUS`, `PROCESS_PAYMENT`, `VIEW_ORDERS`, `VIEW_MENU`, `VIEW_TABLES`
- `WAITER` — `CREATE_ORDER`, `VIEW_ORDERS`, `VIEW_MENU`, `VIEW_TABLES`
- `KITCHEN` — `VIEW_ORDERS`, `UPDATE_ORDER_STATUS`
- `VIEWER` — `VIEW_ORDERS`, `VIEW_MENU`, `VIEW_TABLES`, `VIEW_REPORTS`

### FR-004 — Permission Check API
Given a role and a permission, the system can answer whether that role holds that permission without requiring the caller to inspect the mapping table directly.

### FR-005 — OrderStatus Aligned to Schema
`OrderStatus` must contain exactly: `DRAFT`, `PENDING`, `PREPARING`, `READY`, `COMPLETED`, `CANCELLED`. The previous values (`OPEN`, `IN_PROGRESS`, `DELIVERED`, `CLOSED`, `VOID`) are replaced.

### FR-006 — TableStatus Aligned to Schema
`TableStatus` must contain exactly: `AVAILABLE`, `OCCUPIED`, `RESERVED`, `CLEANING`. The previous values (`FREE`, `OCCUPIED`, `RESERVED`) are replaced or renamed (`FREE` → `AVAILABLE`, add `CLEANING`).

### FR-007 — PaymentMethod Aligned to Schema
`PaymentMethod` must contain exactly: `CASH`, `CARD`, `DIGITAL_WALLET`, `BANK_TRANSFER`. The previous `VOUCHER` value is replaced by `BANK_TRANSFER`. Split payments are modelled as multiple `Payment` records per order (one per instrument used), not as a single `SPLIT` record; this allows accurate per-instrument reporting.

### FR-008 — Database Connection Constants (Placeholder)
A named constant group documents the database connection configuration: maximum pool size, minimum idle connections, connection timeout, idle timeout, maximum connection lifetime, prepared statement cache size. All values are placeholders documented with descriptions; actual values come from environment configuration at runtime.

### FR-009 — API Constants (Placeholder)
A named constant group documents API configuration: base URL, API version string, request timeout, maximum retry count, retry backoff interval. All values are placeholders.

### FR-010 — Sync Constants (Placeholder)
A named constant group documents offline sync configuration: sync polling interval, maximum pending event queue size, event batch size. All values are placeholders.

---

## User Scenarios

### Scenario 1 — Role permission check at order creation
A waiter attempts to create an order. The system checks whether the `WAITER` role holds the `CREATE_ORDER` permission and returns true, allowing the action to proceed.

### Scenario 2 — Viewer blocked from payment
A viewer attempts to process a payment. The system checks whether `VIEWER` holds `PROCESS_PAYMENT` and returns false, blocking the action.

### Scenario 3 — Kitchen display loads in-progress orders
A kitchen screen queries orders with status `PREPARING`. The corrected `OrderStatus` enum ensures the value maps cleanly to the database column without transformation.

### Scenario 4 — Floor plan shows available tables
The table layout screen filters tables by `TableStatus.AVAILABLE`. The renamed value (formerly `FREE`) maps to the database `AVAILABLE` value without a custom mapping layer.

### Scenario 5 — Split payment recorded
A cashier splits a bill between cash and card. Two `Payment` records are created — one with `PaymentMethod.CASH` and one with `PaymentMethod.CARD`. Each maps directly to its corresponding database value, enabling accurate per-instrument revenue reporting.

---

## Key Entities

| Entity | Location | Description |
|---|---|---|
| `Role` | `core:domain/staff` (updated) | User roles aligned to database schema |
| `Permission` | `core:common` (new) | Discrete actions in the system |
| `OrderStatus` | `core:domain/ordering` (updated) | Order lifecycle states aligned to schema |
| `TableStatus` | `core:domain/ordering` (updated) | Table occupancy states aligned to schema |
| `PaymentMethod` | `core:domain/payment` (updated) | Payment method types aligned to schema |
| `DatabaseConstants` | `core:common` (new) | Placeholder DB connection configuration |
| `ApiConstants` | `core:common` (new) | Placeholder API configuration |
| `SyncConstants` | `core:common` (new) | Placeholder sync configuration |

---

## Success Criteria

- SC-001: All five enumerations (`Role`, `OrderStatus`, `TableStatus`, `PaymentMethod`) exactly match the corresponding database schema types — no extra values, no missing values.
- SC-002: Given any role, a permission check can be performed in a single call and returns a boolean result.
- SC-003: The permission mapping is exhaustive — every `Role` value has an explicitly defined set of `Permission` values, with no role left undefined.
- SC-004: The three constant groups exist with placeholder values and every placeholder is documented with a description of what it represents.
- SC-005: The project compiles with zero errors and zero warnings after the enum corrections are applied.
- SC-006: All existing tests that reference the renamed enum values (`FREE`, `SERVER`, `OPEN`, `IN_PROGRESS`, `DELIVERED`, `CLOSED`, `VOID`, `VOUCHER`) are updated and continue to pass.
- SC-007: `PaymentMethod.SPLIT` does not exist; split payments are represented as multiple `Payment` records, each with a specific instrument value.

---

## Assumptions

- The database schema (`docs/database-schema.sql`) is the authoritative source of truth for enum values. If the schema changes, the domain enumerations must be updated to match.
- `core:common` files are placed in the existing source tree under a `common` package; a dedicated Gradle module is deferred.
- Placeholder constant values use sensible defaults that are clearly marked as placeholders in code comments — they are not intended to be used at runtime until replaced by environment-driven configuration.
- `SPLIT` in `PaymentMethod` represents any scenario where a single order is settled across multiple payment instruments; the exact split logic is handled at the use-case layer, not the enum level.

---

## Dependencies

- Feature 003 (core-domain-models) — merged to `main` ✅. This feature corrects enumerations introduced there.
- Feature 004 (domain-repository-interfaces) — merged to `main` ✅. Repository interfaces that reference corrected enum types will need no signature changes; only callers using the old values need updating.

---

## Out of Scope (Explicit)

- Actual runtime values for `DatabaseConstants`, `ApiConstants`, `SyncConstants` — filled in when the infrastructure layer is built.
- Authentication logic or session token handling.
- Any database migration or schema changes — the schema is already correct; this feature aligns the domain to it.
- `SubscriptionTier`, `Organization`, `Store` domain types — deferred to the multi-tenancy feature.
