---
work_package_id: WP06
title: Feature Module Placeholders
lane: "done"
dependencies: [WP02]
base_branch: 002-kmp-project-structure-setup-WP02
base_commit: 3b2b21ca62ff5649d1378db14e8fa896d735811c
created_at: '2026-03-23T22:55:37.988401+00:00'
subtasks:
- T020
- T021
- T022
- T023
- T024
- T025
- T026
- T027
phase: Phase 1 - Module Scaffold
assignee: ''
agent: "claude"
shell_pid: "11536"
review_status: "approved"
reviewed_by: "Jonathan Sánchez Muñoz"
history:
- timestamp: '2026-03-23T21:23:31Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-001
- FR-002
---

# Work Package Prompt: WP06 – Feature Module Placeholders

## IMPORTANT: Review Feedback Status

- **Has review feedback?**: Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section immediately.
- **You must address all feedback** before your work is complete.
- **Mark as acknowledged**: When you understand the feedback, update `review_status: acknowledged`.

---

## Review Feedback

*[Empty initially. Reviewers will populate if work is returned.]*

---

## Markdown Formatting

Use language identifiers in code blocks: ```kotlin, ```bash

---

## Objectives & Success Criteria

Create all eight `feature/*` module placeholder directories. Each is an empty KMP library shell. T020 defines the canonical pattern; T021–T027 replicate it with the correct module name.

**Done when:**
- [ ] All 8 feature modules exist with a valid `build.gradle.kts` and empty `src/commonMain/kotlin/`
- [ ] Each `build.gradle.kts` is ≤10 lines (convention plugin handles everything)
- [ ] Each module sets `android { namespace = "com.vibely.feature.<name>" }`
- [ ] `./gradlew :feature:auth:build :feature:menu:build :feature:orders:build :feature:payments:build :feature:inventory:build :feature:customers:build :feature:reports:build :feature:settings:build` completes without errors

---

## Context & Constraints

- **Plan**: `kitty-specs/002-kmp-project-structure-setup/plan.md` — Phase 1 Step 3
- **Constitution**: `.kittify/memory/constitution.md` — "feature/* modules MAY depend on core/domain" (that dependency is NOT declared in this phase)
- **Module paths**: Already registered in `settings.gradle.kts` (WP01 T002)

**Key constraints**:
- Feature modules do NOT depend on `core/domain` or any other module in this phase — that wiring is added when feature implementation begins.
- All 8 modules use the identical structure: `kmp-library` plugin + Android namespace + empty `commonMain`.
- Namespace convention: `com.vibely.feature.<name>` (e.g., `com.vibely.feature.auth`).

**Implement command** (depends on WP02): `spec-kitty implement WP06 --base WP02`

---

## Module Pattern (applies to ALL 8 subtasks)

Each feature module follows this exact pattern:

**Directory structure**:
```
feature/<name>/
├── build.gradle.kts
└── src/
    └── commonMain/
        └── kotlin/
            └── .gitkeep
```

**`build.gradle.kts` template** (substitute `<name>` with the module name):
```kotlin
plugins {
    id("kmp-library")
}

android {
    namespace = "com.vibely.feature.<name>"
}
```

That's it — ≤10 lines. The convention plugin handles targets, Android SDK configuration, and serialization.

---

## Subtasks & Detailed Guidance

### Subtask T020 – Create `feature/auth/` module (reference pattern)

**Purpose**: Define the canonical feature module structure. All subsequent feature modules (T021–T027) replicate this exact pattern.

**Steps**:

1. Create `feature/auth/` using the Module Pattern above.
2. `build.gradle.kts` content:
```kotlin
plugins {
    id("kmp-library")
}

android {
    namespace = "com.vibely.feature.auth"
}
```
3. Create `feature/auth/src/commonMain/kotlin/.gitkeep`.

**Files**: `feature/auth/build.gradle.kts`, `feature/auth/src/commonMain/kotlin/.gitkeep`
**Parallel?**: Yes — all feature modules are independent.

---

### Subtask T021 – Create `feature/menu/` module

Apply the Module Pattern with `name = "menu"` and `namespace = "com.vibely.feature.menu"`.

**Files**: `feature/menu/build.gradle.kts`, `feature/menu/src/commonMain/kotlin/.gitkeep`
**Parallel?**: Yes.

---

### Subtask T022 – Create `feature/orders/` module

Apply the Module Pattern with `name = "orders"` and `namespace = "com.vibely.feature.orders"`.

**Files**: `feature/orders/build.gradle.kts`, `feature/orders/src/commonMain/kotlin/.gitkeep`
**Parallel?**: Yes.

---

### Subtask T023 – Create `feature/payments/` module

Apply the Module Pattern with `name = "payments"` and `namespace = "com.vibely.feature.payments"`.

**Files**: `feature/payments/build.gradle.kts`, `feature/payments/src/commonMain/kotlin/.gitkeep`
**Parallel?**: Yes.

---

### Subtask T024 – Create `feature/inventory/` module

Apply the Module Pattern with `name = "inventory"` and `namespace = "com.vibely.feature.inventory"`.

**Files**: `feature/inventory/build.gradle.kts`, `feature/inventory/src/commonMain/kotlin/.gitkeep`
**Parallel?**: Yes.

---

### Subtask T025 – Create `feature/customers/` module

Apply the Module Pattern with `name = "customers"` and `namespace = "com.vibely.feature.customers"`.

**Files**: `feature/customers/build.gradle.kts`, `feature/customers/src/commonMain/kotlin/.gitkeep`
**Parallel?**: Yes.

---

### Subtask T026 – Create `feature/reports/` module

Apply the Module Pattern with `name = "reports"` and `namespace = "com.vibely.feature.reports"`.

**Files**: `feature/reports/build.gradle.kts`, `feature/reports/src/commonMain/kotlin/.gitkeep`
**Parallel?**: Yes.

---

### Subtask T027 – Create `feature/settings/` module

Apply the Module Pattern with `name = "settings"` and `namespace = "com.vibely.feature.settings"`.

**Files**: `feature/settings/build.gradle.kts`, `feature/settings/src/commonMain/kotlin/.gitkeep`
**Parallel?**: Yes.

---

## Verification Checklist

After creating all 8 modules, verify:

```bash
# Count feature module directories
ls feature/ | wc -l
# Expected: 8

# List all build files
ls feature/*/build.gradle.kts
# Expected: 8 files

# Check namespace declarations
grep -r "namespace" feature/*/build.gradle.kts
# Expected: 8 lines, each with "com.vibely.feature.<name>"
```

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Namespace typo (e.g., `com.vibely.features.auth` with an 's') | Use exact convention `com.vibely.feature.<name>` — grep check above catches typos |
| Feature module accidentally adds a dependency on `core/network` or similar | Build files are empty shells — no dependency blocks. Any addition would be obvious in code review. |
| 8 empty modules increase Gradle configuration time | Gradle 9.x lazy configuration mitigates this; acceptable for a skeleton project. |

---

## Review Guidance

Reviewers should verify:
1. Exactly 8 `feature/*` directories exist.
2. Each `build.gradle.kts` applies exactly `"kmp-library"` and sets `android { namespace = "com.vibely.feature.<name>" }`.
3. No `build.gradle.kts` has a `dependencies {}` block — no inter-module deps in this phase.
4. Each module's `src/commonMain/kotlin/` exists with a `.gitkeep` file.
5. All 8 modules appear in `./gradlew projects` output.

---

## Activity Log

- 2026-03-23T21:23:31Z – system – lane=planned – Prompt created.
- 2026-03-23T22:55:38Z – claude – shell_pid=9537 – lane=doing – Assigned agent via workflow command
- 2026-03-23T22:56:30Z – claude – shell_pid=9537 – lane=for_review – Ready for review: 8 feature module shells applying kmp-library plugin with commonMain source dirs
- 2026-03-23T22:58:51Z – claude – shell_pid=11536 – lane=doing – Started review via workflow command
- 2026-03-23T22:59:07Z – claude – shell_pid=11536 – lane=done – Review passed: 8 feature modules with kmp-library plugin, correct namespaces, commonMain source dirs, no inter-module deps
