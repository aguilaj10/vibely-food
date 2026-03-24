# Research: Core Domain Models

**Feature**: 003-core-domain-models
**Date**: 2026-03-24
**Status**: Complete — no open questions remain

---

## Decision 1: Typed ID Value Classes

**Decision**: Every entity has its own `@JvmInline value class` ID wrapper (e.g., `OrderId(val value: String)`).

**Rationale**:
- Prevents accidental ID cross-assignment at compile time (e.g., passing a `CustomerId` where an `OrderId` is expected).
- Zero runtime overhead — the JVM erases the wrapper to its underlying `String`.
- The `@JvmInline` annotation makes this idiomatic Kotlin with KMP support across Android, JVM, and JS targets.
- Consistent from day one: retrofitting typed IDs into a codebase with raw `String` IDs is a large, error-prone refactor.

**Alternatives considered**:
- Raw `String` IDs — simpler but allow cross-entity ID confusion; rejected per user direction.
- A generic `Id<T>` wrapper — adds type-parameter noise at every call site; the per-entity value class is cleaner.
- `UUID` as the underlying type — not available in the Kotlin common stdlib without external dependencies; `String` representation keeps `core:domain` pure Kotlin.

**ID underlying type**: `String` (UUID formatted, e.g., `"550e8400-e29b-41d4-a716-446655440000"`). Generation of UUID values is a responsibility of the layer that creates entities (use cases / repositories), not the domain model.

---

## Decision 2: Money as a Typed Value Class

**Decision**: `@JvmInline value class Money(val cents: Long)` with a companion `ZERO` constant and basic arithmetic operators.

**Rationale**:
- Floating-point `Double` produces rounding errors when summing prices (e.g., 3 × €0.10 ≠ €0.30 in IEEE 754). Using integer cents eliminates this entirely.
- `Long` supports amounts up to ~92 quadrillion cents — sufficient for any restaurant POS.
- Basic operators (`+`, `−`, `×`) are intrinsic to the `Money` type, not business logic, following the Value Object pattern from DDD. Everything involving external rules (tax rates, discounts, tips) belongs to service layers.
- The `@JvmInline` annotation means zero allocation overhead — identical to a raw `Long` at runtime.

**Alternatives considered**:
- `BigDecimal` — available on JVM but not in the Kotlin common stdlib without `kotlin-math` or third-party libraries; would violate zero-framework-dep rule for `core:domain`.
- `Double` — rejected due to rounding errors in financial calculations.
- `Int` cents — overflows above ~21 million euros; `Long` is safer with no practical cost.

---

## Decision 3: Timestamps as Typed Long Wrappers

**Decision**: `@JvmInline value class Timestamp(val epochMillis: Long)` for all date-time fields. No external datetime library in `core:domain`.

**Rationale**:
- `kotlinx.datetime` is a JetBrains library external to the Kotlin stdlib. Importing it into `core:domain` would add a framework dependency — a direct constitution violation.
- `Long` epoch milliseconds are universally portable across all KMP targets (Android, JVM, JS).
- The typed wrapper (`Timestamp`) prevents accidental use of a raw `Long` where a `Timestamp` is expected, while remaining pure Kotlin.
- Conversion to human-readable dates and timezone handling belongs to the UI/presentation layer, not the domain.

**Alternatives considered**:
- `kotlinx.datetime.Instant` — clean API but adds an external dependency; rejected.
- Raw `Long` — avoids dependency but loses type safety; the `Timestamp` wrapper gives both.

---

## Decision 4: Duration as Typed Long Wrapper

**Decision**: `@JvmInline value class Duration(val millis: Long)` for `Shift.breakDuration`.

**Rationale**: Same reasoning as `Timestamp` — keeps `core:domain` pure Kotlin, avoids `kotlinx.datetime.DateTimePeriod`, and is zero-overhead. Arithmetic (`breakEnd - breakStart`) happens in the service layer.

---

## Decision 5: Inventory Quantities as Double

**Decision**: `IngredientStock.quantity` and `IngredientStock.alertThreshold` use `Double`.

**Rationale**:
- Kitchen quantities are inherently fractional (1.5 kg of tomatoes, 0.25 L of cream).
- Exact-cent precision is not required for stock tracking — floating-point errors at the gram level are operationally irrelevant.
- Using `Long` with sub-unit encoding (e.g., millilitres) forces UI layers to handle all display conversions, adding complexity for no material benefit.

**Alternatives considered**:
- `Long` in the smallest unit (grams/ml) — avoids float math but complicates ingredient entry UX; rejected.
- Custom `Quantity` value type — over-engineered for this stage; can be introduced later if needed.

---

## Decision 6: Package Structure Within `core:domain`

**Decision**: Sub-packages mirror bounded contexts.

```
com.vibely.domain.common        # Money, Timestamp, Duration value classes
com.vibely.domain.ordering      # MenuItem, Category, Modifier*, Order, OrderItem, Table, Section, enums
com.vibely.domain.payment       # Payment, PaymentMethod, PaymentStatus, Receipt, ReceiptLineItem
com.vibely.domain.customer      # Customer
com.vibely.domain.inventory     # IngredientStock, StockAlert
com.vibely.domain.staff         # Employee, Role, Shift
```

**Rationale**: Each bounded context is independently legible. Developers and agents working on the auth feature can go directly to `staff` without scanning an unstructured flat package. This structure also maps cleanly to the `feature/*` modules that will consume these types.

---

## Decision 7: No Contracts or API Artifacts for This Feature

**Decision**: No `contracts/` directory and no `quickstart.md` for feature 003.

**Rationale**: This feature produces only Kotlin source files (data classes, enums, value classes). There are no HTTP endpoints, no CLI commands, and no runnable artifact. The "quickstart" for consuming code is: add `core:domain` as a dependency and import the types. That does not require a separate document.

---

## Decision 8: SelectedModifier as a Snapshot Value Type

**Decision**: `OrderItem` contains `List<SelectedModifier>` where `SelectedModifier` is a snapshot data class capturing the modifier's name and price at order time — not a reference to `ModifierId`.

**Rationale**: Menu prices change over time. An order placed last month at €0.50 for extra cheese must remain correct even after the modifier price is updated to €0.75. Copying the name and price into the order item creates an immutable record of what was ordered and charged. This is the standard "price snapshot" pattern in e-commerce domain modelling.

---

## No Open Questions

All design decisions are resolved. No `[NEEDS CLARIFICATION]` markers remain. Ready for Phase 1 (data-model.md).
