# Feature Specification: Domain Repository Interfaces

**Feature**: 004-domain-repository-interfaces
**Status**: Draft
**Date**: 2026-03-24
**Mission**: software-dev

---

## Overview

Define a repository interface for each aggregate root across all six bounded contexts of the Vibely POS domain. Each interface declares the complete data access contract — what can be queried and what can be persisted — without specifying how or where data is stored. Implementations are deferred to a future feature.

---

## Problem Statement

The domain layer has fully defined all entities and value types. Before any screen or use case can be built, the application needs a stable contract describing how to load and save domain objects. Without these interfaces, every consumer would need to know about storage specifics, which would couple business logic to infrastructure.

---

## Goals

- Provide a single, stable interface per aggregate root that all use cases and UI can depend on.
- Keep the contract technology-agnostic so that implementations (local database, remote API, in-memory fake) can be swapped without touching business logic.
- Enable use cases and UI to be built immediately against fakes that implement these interfaces, without waiting for a real database.

---

## Scope

### In Scope

- One interface per aggregate root for all six bounded contexts:
  - **Ordering catalog**: `CategoryRepository`, `MenuItemRepository`, `TableRepository`, `SectionRepository`
  - **Orders**: `OrderRepository`
  - **Customers**: `CustomerRepository`
  - **Inventory**: `IngredientStockRepository`
  - **Staff**: `EmployeeRepository`, `ShiftRepository`
  - **Payments**: `PaymentRepository`
- Each interface lives in `core:domain` alongside its aggregate root, in the same package.
- Each interface defines read operations (find by ID, list all, filtered queries relevant to the domain) and write operations (save, delete where applicable).

### Out of Scope

- Any concrete implementation (Room, SQLDelight, Ktor, in-memory).
- Use cases or application logic.
- Pagination or cursor-based listing (deferred until a UI feature requires it).
- `ReceiptRepository` — `Receipt` is a derived value object generated at close time, not a persisted aggregate.
- `StockAlert` repository — alerts are derived from `IngredientStock` state, not independently persisted aggregates.

---

## Functional Requirements

### FR-001 — Ordering Catalog Repositories
Each catalog entity (Category, MenuItem, Table, Section) has a repository interface that supports:
- Retrieve a single record by its typed ID.
- Retrieve all records of that type.
- Save (insert or update) a record.
- Delete a record by its typed ID.

### FR-002 — Availability Filtering
`CategoryRepository` and `MenuItemRepository` expose a query to retrieve only available items (those marked `available = true`), since menus are frequently filtered by availability during service.

### FR-003 — Table Filtering by Status
`TableRepository` exposes a query to retrieve tables by `TableStatus` (FREE, OCCUPIED, RESERVED), since table assignment requires knowing which tables are free.

### FR-004 — Section-to-Table Association
`TableRepository` exposes a query to retrieve all tables belonging to a given `SectionId`.

### FR-005 — Order Repository
`OrderRepository` supports:
- Find an order by `OrderId`.
- Find all orders for a given `TableId` (active orders at a table).
- Find all orders for a given `CustomerId`.
- Save an order (insert or update).
- Delete an order by `OrderId`.

### FR-006 — Order Status Filtering
`OrderRepository` exposes a query to retrieve orders filtered by `OrderStatus`, so the kitchen display and cashier views can show only open or in-progress orders.

### FR-007 — Customer Repository
`CustomerRepository` supports:
- Find a customer by `CustomerId`.
- Find a customer by phone number (used during order placement to look up returning customers).
- List all customers.
- Save a customer.
- Delete a customer by `CustomerId`.

### FR-008 — Inventory Repository
`IngredientStockRepository` supports:
- Find an ingredient by `IngredientId`.
- List all ingredients.
- Find all ingredients where current quantity is below the alert threshold (low-stock query for the kitchen manager).
- Save an ingredient stock record.
- Delete an ingredient stock record by `IngredientId`.

### FR-009 — Employee Repository
`EmployeeRepository` supports:
- Find an employee by `EmployeeId`.
- Find an employee by PIN hash (used during clock-in authentication).
- List all employees.
- List employees by `Role`.
- Save an employee.
- Delete an employee by `EmployeeId`.

### FR-010 — Shift Repository
`ShiftRepository` supports:
- Find a shift by `ShiftId`.
- Find all open shifts (where `clockOut` is null) — used to identify who is currently clocked in.
- Find all shifts for a given `EmployeeId`.
- Save a shift.
- Delete a shift by `ShiftId`.

### FR-011 — Payment Repository
`PaymentRepository` supports:
- Find a payment by `PaymentId`.
- Find all payments for a given `OrderId` (to reconstruct how an order was settled).
- Save a payment.

### FR-012 — Suspend Functions
All repository operations are declared as `suspend` functions to allow non-blocking execution. No threading or dispatcher decisions are made in the interface.

### FR-013 — Result Wrapping for Fallible Operations
All operations that may fail (record not found, persistence error) return `Result<T>`. List operations that return empty collections on no match return `List<T>` directly (empty list is a valid, non-error result).

---

## User Scenarios

### Scenario 1 — Use case builds an order
A `PlaceOrderUseCase` calls `MenuItemRepository.findById(menuItemId)` to validate the item exists and get its current price snapshot. It then calls `TableRepository.findByStatus(TableStatus.FREE)` to confirm the table is free. It constructs an `Order` and calls `OrderRepository.save(order)`. None of these steps depend on any storage technology.

### Scenario 2 — Kitchen display loads open orders
A screen calls `OrderRepository.findByStatus(OrderStatus.OPEN)` and `OrderRepository.findByStatus(OrderStatus.IN_PROGRESS)` to render the active order list. The screen knows nothing about where orders are stored.

### Scenario 3 — Low-stock alert check
A background task calls `IngredientStockRepository.findBelowThreshold()` and surfaces the results to the manager's inventory screen.

### Scenario 4 — Employee clocks in
A clock-in use case calls `EmployeeRepository.findByPinHash(enteredHash)` to authenticate the employee, then calls `ShiftRepository.save(newShift)`.

### Scenario 5 — Fake implementation in tests
A test creates an in-memory `FakeOrderRepository` that implements `OrderRepository`. A use case is tested against the fake without any database, file system, or network dependency.

---

## Key Entities

All entity types are defined in feature 003 (core:domain). This feature adds no new entities — only contracts for accessing them.

| Interface | Aggregate Root | Package |
|---|---|---|
| `CategoryRepository` | `Category` | `com.vibely.domain.ordering` |
| `MenuItemRepository` | `MenuItem` | `com.vibely.domain.ordering` |
| `TableRepository` | `Table` | `com.vibely.domain.ordering` |
| `SectionRepository` | `Section` | `com.vibely.domain.ordering` |
| `OrderRepository` | `Order` | `com.vibely.domain.ordering` |
| `CustomerRepository` | `Customer` | `com.vibely.domain.customer` |
| `IngredientStockRepository` | `IngredientStock` | `com.vibely.domain.inventory` |
| `EmployeeRepository` | `Employee` | `com.vibely.domain.staff` |
| `ShiftRepository` | `Shift` | `com.vibely.domain.staff` |
| `PaymentRepository` | `Payment` | `com.vibely.domain.payment` |

---

## Success Criteria

- SC-001: All 10 repository interfaces exist and the project compiles with zero errors.
- SC-002: Every interface method that can fail returns `Result<T>`; list methods return `List<T>`.
- SC-003: All methods are declared `suspend`.
- SC-004: A fake in-memory implementation can be written for any interface with no more than 30 lines of code per interface, confirming the contracts are simple and focused.
- SC-005: Zero framework or storage imports appear in any repository interface file.
- SC-006: A use case that depends on any repository interface can be compiled and tested using a fake implementation without touching any database or network.

---

## Assumptions

- All interfaces live in `core:domain` in the same package as their aggregate root. A separate `core:repository` module is not needed at this stage.
- `suspend` is the chosen async contract. Reactive streams (`Flow`) are deferred until a feature explicitly requires reactive updates (e.g., real-time kitchen display).
- Pagination is deferred. All list operations return `List<T>`.
- `Receipt` and `StockAlert` are excluded — they are derived value objects, not independently persisted aggregates.

---

## Dependencies

- Feature 003 (core-domain-models) — must be merged to `main` before implementation begins. ✅ Already done.

---

## Out of Scope (Explicit)

- Concrete implementations (Room, SQLDelight, in-memory, REST).
- Fake/test implementations (those belong in a test-utilities module or within use case tests).
- Migration scripts or database schemas.
- Caching policies or retry logic.
