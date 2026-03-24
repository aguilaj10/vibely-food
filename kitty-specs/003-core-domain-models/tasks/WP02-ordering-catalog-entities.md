---
work_package_id: WP02
title: Ordering — Catalog Entities
lane: "doing"
dependencies:
- WP01
base_branch: 003-core-domain-models-WP01
base_commit: 7c5a57dca0f4840ab52667f4afbc0d6562373dab
created_at: '2026-03-24T02:12:56.661079+00:00'
subtasks:
- T005
- T006
- T007
- T008
- T009
- T010
phase: Phase 2 - Ordering Bounded Context (Part 1)
assignee: ''
agent: "claude"
shell_pid: "89213"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-24T00:40:45Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-001
- FR-002
- FR-003
- FR-004
- FR-008
- FR-009
---

# Work Package Prompt: WP02 – Ordering — Catalog Entities

## ⚠️ IMPORTANT: Review Feedback Status

Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section.

---

## Review Feedback

*[Empty initially. Populated by reviewers if work is returned.]*

---

## Implement Command

```bash
spec-kitty implement WP02 --base WP01
```

---

## Objectives & Success Criteria

1. Six files exist in `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/`: `Category.kt`, `Modifier.kt`, `ModifierGroup.kt`, `MenuItem.kt`, `Section.kt`, `Table.kt`
2. Each file co-locates its `@JvmInline value class` ID at the top
3. `TableStatus` enum is declared inside `Table.kt`
4. `./gradlew :core:domain:build` passes with zero errors and zero warnings
5. Zero non-Kotlin imports

---

## Context & Constraints

- **Feature**: 003-core-domain-models | **Plan**: `kitty-specs/003-core-domain-models/plan.md` | **Data model**: `kitty-specs/003-core-domain-models/data-model.md`
- **Constitution**: Zero framework imports. Only `com.vibely.domain.common.*` imports are permitted.
- **Convention**: Every entity file declares its `@JvmInline value class` ID at the top of the same file (e.g., `CategoryId` in `Category.kt`).
- **Embedding vs. ID references**:
  - `ModifierGroup.options: List<Modifier>` — embed full objects (all info co-located for display)
  - `MenuItem.modifierGroups: List<ModifierGroup>` — embed full objects
  - `Section.tables: List<TableId>` — ID reference only (display-order list; `Table` objects live elsewhere)
- **No tests in this WP** — tests are part of WP04 (which tests the complete ordering context including `Order`).

---

## Subtasks & Detailed Guidance

### Subtask T005 – Create `Category.kt`

**Purpose**: Top-level grouping for menu items (e.g., "Starters", "Mains").

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/Category.kt`

**Target**:
```kotlin
package com.vibely.domain.ordering

@JvmInline
value class CategoryId(val value: String)

data class Category(
    val id: CategoryId,
    val name: String,
    val displayOrder: Int,
    val available: Boolean,
)
```

**Validation**: No imports needed (no cross-package references).

---

### Subtask T006 – Create `Modifier.kt`

**Purpose**: A single selectable option within a `ModifierGroup` (e.g., "Extra cheese — +€1.50").

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/Modifier.kt`

**Target**:
```kotlin
package com.vibely.domain.ordering

import com.vibely.domain.common.Money

@JvmInline
value class ModifierId(val value: String)

data class Modifier(
    val id: ModifierId,
    val name: String,
    val priceAdjustment: Money, // positive = surcharge, negative = discount, 0 = free
    val available: Boolean,
)
```

**Notes**: `priceAdjustment` can be negative (discount) or zero (free add-on).

---

### Subtask T007 – Create `ModifierGroup.kt`

**Purpose**: A set of modifier options attached to a `MenuItem` (e.g., "Cooking preference", "Extras").

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/ModifierGroup.kt`

**Target**:
```kotlin
package com.vibely.domain.ordering

@JvmInline
value class ModifierGroupId(val value: String)

data class ModifierGroup(
    val id: ModifierGroupId,
    val name: String,
    val required: Boolean,       // must the customer select at least one option?
    val minSelections: Int,      // inclusive lower bound (0 if not required)
    val maxSelections: Int,      // inclusive upper bound
    val options: List<Modifier>,
)
```

**Notes**:
- `options: List<Modifier>` embeds full `Modifier` objects — no `List<ModifierId>`.
- Enforcement that `minSelections <= maxSelections` belongs to the service layer.

---

### Subtask T008 – Create `MenuItem.kt`

**Purpose**: A dish or drink available for order; carries base price, category membership, and modifier groups.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/MenuItem.kt`

**Target**:
```kotlin
package com.vibely.domain.ordering

import com.vibely.domain.common.Money

@JvmInline
value class MenuItemId(val value: String)

data class MenuItem(
    val id: MenuItemId,
    val name: String,
    val description: String,
    val basePrice: Money,
    val available: Boolean,
    val categoryId: CategoryId,
    val modifierGroups: List<ModifierGroup>,
)
```

**Notes**: `modifierGroups` may be empty (item has no customisation options — valid per spec edge cases).

---

### Subtask T009 – Create `Section.kt`

**Purpose**: A named area of the restaurant (e.g., "Terrace", "Bar"). Holds an ordered list of table IDs for display.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/Section.kt`

**Target**:
```kotlin
package com.vibely.domain.ordering

@JvmInline
value class SectionId(val value: String)

data class Section(
    val id: SectionId,
    val name: String,
    val tables: List<TableId>, // ordered for display; ID reference only
)
```

**Notes**: `Section.tables` holds `TableId`, NOT `Table`. Do not accidentally embed `Table` objects.

---

### Subtask T010 – Create `Table.kt`

**Purpose**: A physical seating location. Includes `TableStatus` enum co-located in the same file.

**File**: `core/domain/src/commonMain/kotlin/com/vibely/domain/ordering/Table.kt`

**Target**:
```kotlin
package com.vibely.domain.ordering

@JvmInline
value class TableId(val value: String)

enum class TableStatus {
    FREE, OCCUPIED, RESERVED
}

data class Table(
    val id: TableId,
    val sectionId: SectionId,
    val capacity: Int,
    val status: TableStatus,
)
```

**Notes**: `TableStatus` is co-located with `Table` for discoverability (not in a separate file).

---

## Risks & Mitigations

- **`Section.tables` type**: Must be `List<TableId>`, not `List<Table>`. Double-check after writing.
- **Modifier embedding**: `ModifierGroup.options: List<Modifier>` — full objects, not `List<ModifierId>`. This is intentional for display completeness.
- **No business logic**: None of the files should contain validation, computed properties, or init blocks. Pure data.

---

## Review Guidance

- Check that each file has exactly one `@JvmInline value class` ID at the top
- Verify `Section.tables: List<TableId>` (not `List<Table>`)
- Verify `ModifierGroup.options: List<Modifier>` (not `List<ModifierId>`)
- Verify `TableStatus` enum is inside `Table.kt`
- Confirm zero non-Kotlin imports (only `com.vibely.domain.common.Money` is allowed)
- Run `./gradlew :core:domain:build` — zero errors

---

## Activity Log

- 2026-03-24T00:40:45Z – system – lane=planned – Prompt created.
- 2026-03-24T02:12:58Z – claude-wp02 – shell_pid=82665 – lane=doing – Assigned agent via workflow command
- 2026-03-24T02:35:16Z – claude-wp02 – shell_pid=82665 – lane=for_review – Ready for review: 6 ordering catalog entities, build clean, no tests (deferred to WP04)
- 2026-03-24T02:38:19Z – claude – shell_pid=89213 – lane=doing – Started review via workflow command
