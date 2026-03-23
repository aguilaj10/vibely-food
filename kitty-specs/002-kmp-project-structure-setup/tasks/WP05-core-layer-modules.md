---
work_package_id: WP05
title: Core Layer Modules
lane: "done"
dependencies: [WP02]
base_branch: 002-kmp-project-structure-setup-WP02
base_commit: 3b2b21ca62ff5649d1378db14e8fa896d735811c
created_at: '2026-03-23T22:53:39.159982+00:00'
subtasks:
- T015
- T016
- T017
- T018
- T019
phase: Phase 1 - Module Scaffold
assignee: ''
agent: "claude"
shell_pid: "10687"
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
- FR-007
---

# Work Package Prompt: WP05 – Core Layer Modules

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

Create the five `core/*` KMP library modules. Each applies `kmp-library`. The `core/domain` module is special — its build file must contain a documented zero-framework-dependencies constraint to prevent future violations.

**Done when:**
- [ ] `core/domain/build.gradle.kts` applies `kmp-library`, sets namespace, and has a prominent comment block stating the zero-framework-deps rule
- [ ] `core/network/`, `core/database/`, `core/ui/`, `core/common/` each have a valid `build.gradle.kts` applying `kmp-library`
- [ ] All five modules have `src/commonMain/kotlin/.gitkeep` (and optionally platform-specific source dirs)
- [ ] `./gradlew :core:domain:build :core:network:build :core:database:build :core:ui:build :core:common:build` completes without errors

---

## Context & Constraints

- **Plan**: `kitty-specs/002-kmp-project-structure-setup/plan.md` — Phase 1 Step 3
- **Constitution**: `.kittify/memory/constitution.md` — "Clean Architecture layer separation is STRICT — the `core/domain` module must have ZERO framework dependencies (pure Kotlin only)"
- **Agent Directive**: "NEVER add framework imports (ktor, koin, exposed, etc.) to the `core/domain` module — it must be pure Kotlin"
- **Module paths**: Already registered in `settings.gradle.kts` (WP01 T002)

**Key constraints**:
- `core/domain` MUST have zero framework dependencies — this is a constitution hard rule. Enforce it in the build file with a comment AND (from WP03) a Detekt `ForbiddenImport` rule scoped to this module.
- Each module applying `kmp-library` must set `android { namespace = "..." }`.
- Namespace convention: `com.vibely.core.<name>` (e.g., `com.vibely.core.domain`).

**Implement command** (depends on WP02): `spec-kitty implement WP05 --base WP02`

---

## Subtasks & Detailed Guidance

### Subtask T015 – Create `core/domain/` module

**Purpose**: `core/domain` is the heart of the Clean Architecture layer. It contains pure business entities, value objects, and repository interfaces — all in plain Kotlin with zero framework dependencies. This constraint must be visible in the build file itself, not just in documentation.

**Steps**:

1. Create the directory structure:
```
core/domain/
├── build.gradle.kts
└── src/
    └── commonMain/
        └── kotlin/
            └── .gitkeep
```

2. Create `core/domain/build.gradle.kts`:
```kotlin
// =============================================================================
// ARCHITECTURE RULE: ZERO FRAMEWORK DEPENDENCIES
// =============================================================================
// This module MUST remain pure Kotlin. Do NOT add:
//   - Ktor (io.ktor.*)
//   - Koin (io.insert-koin.*)
//   - Exposed (org.jetbrains.exposed.*)
//   - SQLDelight (app.cash.sqldelight.*)
//   - Any Android SDK classes (android.*)
//   - Any Compose classes (androidx.compose.*)
//
// Only kotlin-stdlib and kotlinx-coroutines-core are permitted.
// Violations will be caught by Detekt (ForbiddenImport rule) and CI.
// See: .kittify/memory/constitution.md — Tribal Knowledge section
// =============================================================================

plugins {
    id("kmp-library")
}

android {
    namespace = "com.vibely.core.domain"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // PERMITTED: Kotlin stdlib (included automatically)
            // PERMITTED: kotlinx-coroutines-core (for Flow/suspend in interfaces)
            implementation(libs.kotlinx.coroutines.core)
            // PERMITTED: kotlinx-datetime (for date/time value objects)
            implementation(libs.kotlinx.datetime)
        }
    }
}
```

**Files**: `core/domain/build.gradle.kts`, `core/domain/src/commonMain/kotlin/.gitkeep`
**Parallel?**: No — complete this first as the reference; T016–T019 can run in parallel after.
**Notes**: `kotlinx-coroutines-core` and `kotlinx-datetime` are Kotlin-first libraries with no framework coupling — they are explicitly permitted by the constitution. Do NOT add any other dependencies.

---

### Subtask T016 – Create `core/network/` module

**Purpose**: `core/network` will hold the HTTP client abstraction layer (Ktor client). In this phase it is an empty shell. The namespace and build file establish the module as a valid KMP library.

**Steps**:

1. Create the directory structure:
```
core/network/
├── build.gradle.kts
└── src/
    └── commonMain/
        └── kotlin/
            └── .gitkeep
```

2. Create `core/network/build.gradle.kts`:
```kotlin
plugins {
    id("kmp-library")
}

android {
    namespace = "com.vibely.core.network"
}
```

**Files**: `core/network/build.gradle.kts`, `core/network/src/commonMain/kotlin/.gitkeep`
**Parallel?**: Yes — can be created simultaneously with T017, T018, T019.

---

### Subtask T017 – Create `core/database/` module

**Purpose**: `core/database` will hold the SQLDelight schema definitions and database driver abstractions. Empty shell in this phase.

**Steps**:

1. Create the directory structure:
```
core/database/
├── build.gradle.kts
└── src/
    └── commonMain/
        └── kotlin/
            └── .gitkeep
```

2. Create `core/database/build.gradle.kts`:
```kotlin
plugins {
    id("kmp-library")
}

android {
    namespace = "com.vibely.core.database"
}
```

**Files**: `core/database/build.gradle.kts`, `core/database/src/commonMain/kotlin/.gitkeep`
**Parallel?**: Yes — alongside T016, T018, T019.

---

### Subtask T018 – Create `core/ui/` module

**Purpose**: `core/ui` will hold the shared design system components (colors, typography, spacing, common composables). Empty shell in this phase.

**Steps**:

1. Create the directory structure:
```
core/ui/
├── build.gradle.kts
└── src/
    └── commonMain/
        └── kotlin/
            └── .gitkeep
```

2. Create `core/ui/build.gradle.kts`:
```kotlin
plugins {
    id("kmp-library")
}

android {
    namespace = "com.vibely.core.ui"
}
```

**Files**: `core/ui/build.gradle.kts`, `core/ui/src/commonMain/kotlin/.gitkeep`
**Parallel?**: Yes — alongside T016, T017, T019.

---

### Subtask T019 – Create `core/common/` module

**Purpose**: `core/common` holds cross-cutting utilities (extensions, constants, logging abstractions, platform capabilities via `expect/actual`). Empty shell in this phase.

**Steps**:

1. Create the directory structure:
```
core/common/
├── build.gradle.kts
└── src/
    ├── commonMain/
    │   └── kotlin/
    │       └── .gitkeep
    ├── androidMain/
    │   └── kotlin/
    │       └── .gitkeep
    └── jvmMain/
        └── kotlin/
            └── .gitkeep
```

Note: `core/common` gets platform-specific source dirs because it is the designated location for `expect/actual` platform capability implementations. Creating them now prevents restructuring later.

2. Create `core/common/build.gradle.kts`:
```kotlin
plugins {
    id("kmp-library")
}

android {
    namespace = "com.vibely.core.common"
}
```

**Files**: `core/common/build.gradle.kts`, source directories with `.gitkeep` files
**Parallel?**: Yes — alongside T016, T017, T018.

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| `core/domain` gains a framework dependency in a future feature | Detekt `ForbiddenImport` rule (configured in WP03's `detekt.yml`) will catch it at commit time |
| `android.namespace` missing — AGP 8.x hard fails | Each build.gradle.kts explicitly sets namespace; review checklist item added |
| `core/common` platform source sets unused until expect/actual is added | Empty with `.gitkeep` is fine; git tracks the directory for future use |

---

## Review Guidance

Reviewers should verify:
1. `core/domain/build.gradle.kts` has the multi-line architecture rule comment block at the top.
2. `core/domain/build.gradle.kts` declares ONLY `kotlinx.coroutines.core` and `kotlinx.datetime` as dependencies — nothing else.
3. All five modules set `android { namespace = "com.vibely.core.<name>" }`.
4. `core/common` has `androidMain/` and `jvmMain/` platform source directories (in addition to `commonMain`).
5. No module has dependencies on other project modules yet (no `:core:domain` in `:core:network`, etc.) — that wiring is deferred.

---

## Activity Log

- 2026-03-23T21:23:31Z – system – lane=planned – Prompt created.
- 2026-03-23T22:53:39Z – claude – shell_pid=8827 – lane=doing – Assigned agent via workflow command
- 2026-03-23T22:55:17Z – claude – shell_pid=8827 – lane=for_review – Ready for review: 5 core modules applying kmp-library plugin, core/domain has zero-framework-deps comment block, all source dirs with .gitkeep
- 2026-03-23T22:58:22Z – claude – shell_pid=10687 – lane=doing – Started review via workflow command
- 2026-03-23T22:58:47Z – claude – shell_pid=10687 – lane=done – Review passed: 5 core modules with kmp-library plugin, domain has zero-framework-deps comment, all namespaces set, source dirs present
