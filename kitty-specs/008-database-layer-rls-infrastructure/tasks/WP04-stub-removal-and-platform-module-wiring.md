---
work_package_id: WP04
title: Stub Removal and PlatformModule Wiring
lane: planned
dependencies:
- WP02
subtasks:
- T014
- T015
- T016
phase: Phase C - Integration
assignee: ''
agent: ''
shell_pid: ''
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-25T01:40:02Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-019
- FR-020
---

# Work Package Prompt: WP04 – Stub Removal and PlatformModule Wiring

## ⚠️ IMPORTANT: Review Feedback Status

**Read this first if you are implementing this task!**

- **Has review feedback?**: Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section immediately (right below this notice).
- **You must address all feedback** before your work is complete. Feedback items are your implementation TODO list.
- **Mark as acknowledged**: When you understand the feedback and begin addressing it, update `review_status: acknowledged` in the frontmatter.
- **Report progress**: As you address each feedback item, update the Activity Log explaining what you changed.

---

## Review Feedback

*[This section is empty initially. Reviewers will populate it if the work is returned from review. If you see feedback here, treat each item as a must-do before completion.]*

---

## Markdown Formatting
Wrap HTML/XML tags in backticks: `` `<div>` ``, `` `<script>` ``
Use language identifiers in code blocks: ````kotlin`, ````bash`

---

## Objectives & Success Criteria

Delete `DatabaseStubs.kt`, add `projects.core.database` to `shared/build.gradle.kts`, and update `PlatformModule.kt` to bind the real `DatabaseConfig` and `DatabaseFactory`.

**Done when**:
- Full project Gradle build completes cleanly: `./gradlew build`
- `server` module starts without `ClassNotFoundException` for `DatabaseFactory`
- `DatabaseStubs.kt` is gone
- `PlatformModule.kt` uses `DatabaseConfig.fromEnvironment()` and `DatabaseFactory(config = get())` Koin bindings

**Implementation command** (depends on WP02 and WP03):
```bash
spec-kitty implement WP04 --base WP03
```

---

## Context & Constraints

**Key references**:
- Constitution: `.kittify/memory/constitution.md` — no circular dependencies between modules
- Plan: `kitty-specs/008-database-layer-rls-infrastructure/plan.md` — section 1.9
- Tasks: `kitty-specs/008-database-layer-rls-infrastructure/tasks.md` — WP04 implementation notes

**Critical constraint — implement atomically**:
After T014 deletes `DatabaseStubs.kt`, the project will NOT compile until T015 and T016 are complete. Implement all three subtasks in a single commit to avoid leaving the project in a broken state.

**Circular dependency check**:
`core:database` must NOT depend on `shared`. The dependency is one-way: `shared → core:database → core:domain + core:common`. Verify with `./gradlew :shared:dependencies` — `core:database` must appear, but `shared` must NOT appear in `core:database`'s transitive deps.

**Existing files to read before editing**:
- `shared/src/jvmMain/kotlin/com/vibely/shared/di/DatabaseStubs.kt` — understand what bindings it provides
- `shared/src/jvmMain/kotlin/com/vibely/shared/di/PlatformModule.kt` — understand current Koin module structure
- `shared/build.gradle.kts` — understand current dependency declarations

---

## Subtasks & Detailed Guidance

### Subtask T014 – Delete DatabaseStubs.kt

- **Purpose**: The stub file held temporary `DatabaseConfig` and `DatabaseFactory` inline definitions used before the real `core:database` module existed. Once the real implementations are ready, the stub must be removed to avoid duplicate class definitions.
- **Parallel?**: Yes — can be done simultaneously with T015 (different files).
- **Files**: Delete `shared/src/jvmMain/kotlin/com/vibely/shared/di/DatabaseStubs.kt`
- **Steps**:
  1. Read the file first to understand what it currently defines (inline `DatabaseConfig`, inline `DatabaseFactory`, any Koin bindings).
  2. Delete the file. In the Bash tool: `rm shared/src/jvmMain/kotlin/com/vibely/shared/di/DatabaseStubs.kt`
  3. Note: The project will not compile after this step until T015 + T016 are complete. This is expected.
- **Notes**:
  - The file has a `@file:Suppress("ForbiddenComment")` annotation — this is a signal it was always temporary.
  - Do not create a replacement file — the real `DatabaseFactory` lives in `core:database`.

---

### Subtask T015 – Add core:database dependency to shared/build.gradle.kts

- **Purpose**: `shared:jvmMain` must import `DatabaseFactory` and `DatabaseConfig` from `core:database`. Without this dependency declaration, the classes will be invisible to the compiler.
- **Parallel?**: Yes — can be done simultaneously with T014 (different files).
- **Files**: Edit `shared/build.gradle.kts`
- **Steps**:
  1. Read the existing `build.gradle.kts` to find the `jvmMain.dependencies` block.
  2. Add `implementation(projects.core.database)` inside the `jvmMain.dependencies` block:
     ```kotlin
     jvmMain.dependencies {
         // ... existing deps ...
         implementation(projects.core.database)  // ADD THIS
     }
     ```
  3. Do NOT add this to `commonMain.dependencies` — `core:database` is JVM-only.
  4. Verify `projects.core.database` resolves correctly using the type-safe project accessors.
- **Notes**:
  - Use `projects.core.database` (type-safe accessor), not `project(":core:database")` (string-based). Project convention requires type-safe accessors.

---

### Subtask T016 – Update PlatformModule.kt to bind real DatabaseConfig and DatabaseFactory

- **Purpose**: Replace the inline stub bindings with proper Koin bindings that delegate to the real `core:database` implementations. This wires the production database factory into the server's DI graph.
- **Parallel?**: No — depends on T014 (stub gone) and T015 (import available).
- **Files**: Edit `shared/src/jvmMain/kotlin/com/vibely/shared/di/PlatformModule.kt`
- **Steps**:
  1. Read the current `PlatformModule.kt` to understand the existing Koin module structure and what imports reference `DatabaseStubs.kt`.
  2. Remove any import referencing the stub file or the inline stub classes.
  3. Add imports for `DatabaseConfig` and `DatabaseFactory` from `com.vibely.database`:
     ```kotlin
     import com.vibely.database.DatabaseConfig
     import com.vibely.database.DatabaseFactory
     ```
  4. Replace the old inline `DatabaseConfig(url=..., username=..., password=...)` block and any inline `DatabaseFactory` binding with:
     ```kotlin
     single<DatabaseConfig> { DatabaseConfig.fromEnvironment() }
     single<DatabaseFactory> { DatabaseFactory(config = get()) }
     ```
  5. Preserve all other Koin bindings in the module — only replace the database-related ones.
- **Notes**:
  - `single<DatabaseFactory> { DatabaseFactory(config = get()) }` — Koin's `get()` resolves `DatabaseConfig` from the module; the `config = ` named parameter ensures clarity.
  - `DatabaseFactory` is a singleton (`single`) because it owns the connection pool — creating multiple instances would create multiple pools.
  - If the current `PlatformModule.kt` uses a different Koin DSL style (e.g., `module { ... }` vs `val platformModule = module { ... }`), preserve that style.

---

## Test Strategy

Full project build is the verification gate:

```bash
# Full build — must pass cleanly
./gradlew build

# Circular dependency check
./gradlew :shared:dependencies --configuration jvmMainRuntimeClasspath | grep "core:database"
# Should show: +--- project :core:database
# Should NOT show: +--- project :shared (in core:database's output)
./gradlew :core:database:dependencies --configuration jvmMainRuntimeClasspath | grep "shared"
# Should produce NO output
```

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Breaking build between T014 and T016 | Implement all three subtasks atomically in a single commit. The project won't compile after T014 until T015 + T016 are in place. |
| Circular dependency: `core:database → shared` | Verify with `./gradlew :core:database:dependencies`. If `shared` appears, there is a circular dep — do NOT add `shared` as a dep of `core:database`. |
| Koin binding type mismatch after removing stubs | The `single<DatabaseConfig>` binding must match what the server DI graph requests. Search for `DatabaseConfig` usages in the server module to confirm the injected type matches. |
| `projects.core.database` accessor doesn't exist | Run `./gradlew projects` to confirm the project path. If the accessor is `projects.core.database`, this confirms the module is registered in the Gradle composite build. |

---

## Review Guidance

- [ ] `DatabaseStubs.kt` is deleted (file does not exist)
- [ ] `shared/build.gradle.kts` has `implementation(projects.core.database)` in `jvmMain.dependencies`
- [ ] `projects.core.database` uses type-safe accessor (not string-based `project(":core:database")`)
- [ ] `PlatformModule.kt` has `import com.vibely.database.DatabaseConfig` and `import com.vibely.database.DatabaseFactory`
- [ ] Koin bindings: `single<DatabaseConfig> { DatabaseConfig.fromEnvironment() }` and `single<DatabaseFactory> { DatabaseFactory(config = get()) }`
- [ ] No inline `DatabaseConfig(url=..., ...)` block remaining in `PlatformModule.kt`
- [ ] All other existing Koin bindings in `PlatformModule.kt` are preserved
- [ ] `./gradlew build` passes
- [ ] `./gradlew :core:database:dependencies` does NOT show `shared` module

---

## Activity Log

> **CRITICAL**: Activity log entries MUST be in chronological order (oldest first, newest last).

- 2026-03-25T01:40:02Z – system – lane=planned – Prompt created.
