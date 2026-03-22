---
work_package_id: WP01
title: Update Version Catalog and Add Gradle Wrapper
lane: planned
dependencies: []
subtasks:
- T001
- T002
- T003
- T004
phase: Phase 1 - Configuration
assignee: ''
agent: ''
shell_pid: ''
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-22T21:46:54Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-001
- FR-002
- FR-003
- FR-004
- FR-005
---

# Work Package Prompt: WP01 - Update Version Catalog and Add Gradle Wrapper

## IMPORTANT: Review Feedback Status

- **Has review feedback?**: Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section immediately.
- **You must address all feedback** before your work is complete.
- **Mark as acknowledged**: When you understand the feedback, update `review_status: acknowledged`.

---

## Review Feedback

*[Empty initially. Reviewers will populate if work is returned.]*

---

## Markdown Formatting

Wrap HTML/XML tags in backticks: `<div>`, `<script>`
Use language identifiers in code blocks: ```toml, ```bash

---

## Objectives & Success Criteria

Update `gradle/libs.versions.toml` with the latest stable library versions defined in the
project constitution, and create the missing `gradle/wrapper/gradle-wrapper.properties` file.

**Done when:**
- [ ] Every version string in `[versions]` matches the resolved latest from `research.md`
- [ ] `compose-compiler` standalone version entry is removed from `[versions]`
- [ ] The `compose-compiler` plugin references `version.ref = "kotlin"`
- [ ] `gradle/wrapper/gradle-wrapper.properties` exists and declares Gradle 9.4.1
- [ ] All library Maven coordinates in `[libraries]` and `[plugins]` are verified valid

---

## Context & Constraints

- **Spec**: `kitty-specs/001-update-version-catalog-to-latest/spec.md`
- **Plan**: `kitty-specs/001-update-version-catalog-to-latest/plan.md`
- **Research**: `kitty-specs/001-update-version-catalog-to-latest/research.md` -- all
  resolved versions are here. Use this as the authoritative source.
- **Constitution**: `.kittify/memory/constitution.md` -- mandates always-latest-stable policy
- **Target file**: `gradle/libs.versions.toml` (exists, needs version updates)
- **New file**: `gradle/wrapper/gradle-wrapper.properties` (does not exist, must be created)

**Key constraint**: This is a greenfield project with no source code yet. No breaking changes
from major version bumps (Exposed 1.x, HikariCP 7.x, Flyway 12.x, Kotest 6.x,
Testcontainers 2.x, ktlint 14.x) will affect existing code.

**Implement command** (no dependencies): `spec-kitty implement WP01`

---

## Subtasks & Detailed Guidance

### Subtask T001 - Update `[versions]` block and remove `compose-compiler` entry

**Purpose**: Replace all outdated version strings with their latest stable counterparts
from `research.md`. Also remove the now-redundant `compose-compiler` standalone entry
(the Compose compiler is bundled with Kotlin 2.x).

**Steps**:

1. Open `gradle/libs.versions.toml`.

2. Replace the entire `[versions]` block with the following:

```toml
[versions]
kotlin = "2.3.20"
compose = "1.10.3"
koin = "4.2.0"
ktor = "3.4.1"
exposed = "1.1.1"
sqldelight = "2.3.2"
hikari = "7.0.2"
flyway = "12.1.1"
postgresql = "42.7.10"
kotlinx-coroutines = "1.10.2"
kotlinx-serialization = "1.10.0"
kotlinx-datetime = "0.7.1"
turbine = "1.2.1"
kotest = "6.1.7"
testcontainers = "2.0.4"
detekt = "1.23.8"
ktlint = "14.2.0"
```

Note: `compose-compiler = "1.5.15"` is intentionally removed. The Compose compiler
plugin is bundled with Kotlin since 2.0.0 and must reference `version.ref = "kotlin"`.

**Files**: `gradle/libs.versions.toml`
**Parallel?**: No -- T002 depends on this completing first (same file).
**Notes**: Do not modify `[libraries]`, `[plugins]`, or `[bundles]` blocks in this step.

---

### Subtask T002 - Fix `compose-compiler` plugin `version.ref`

**Purpose**: The `compose-compiler` plugin entry in `[plugins]` still references
`version.ref = "compose-compiler"` which no longer exists after T001. It must reference
`version.ref = "kotlin"` to stay compatible with Kotlin 2.x bundled compiler.

**Steps**:

1. In `gradle/libs.versions.toml`, locate the `[plugins]` block.

2. Find this line:
```toml
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "compose-compiler" }
```

3. Replace it with:
```toml
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

**Files**: `gradle/libs.versions.toml`
**Parallel?**: No -- depends on T001 removing the `compose-compiler` version entry.
**Notes**: The plugin alias name (`compose-compiler`) stays the same. Only the
`version.ref` value changes. All `build.gradle.kts` files that reference
`libs.plugins.compose.compiler` will continue to work without changes.

---

### Subtask T003 - Create `gradle/wrapper/gradle-wrapper.properties`

**Purpose**: The Gradle wrapper pins the exact Gradle version for all contributors
and CI pipelines, ensuring reproducible builds. This file does not exist yet.

**Steps**:

1. Create the directory `gradle/wrapper/` if it does not exist.

2. Create `gradle/wrapper/gradle-wrapper.properties` with this exact content:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

**Files**: `gradle/wrapper/gradle-wrapper.properties` (new file)
**Parallel?**: Yes -- independent of T001/T002 (different file/directory).
**Notes**:
- Use `gradle-9.4.1-bin.zip` (not `-all.zip`). The `-bin` distribution is smaller
  and sufficient for builds. Use `-all` only if sources and docs are needed in the IDE.
- The backslash before `://` in the URL is intentional -- it is a `.properties` escape
  for the colon character.
- The `gradlew` and `gradlew.bat` wrapper scripts also need to exist for the wrapper to
  work. If they are missing from the project, they must be added (standard Gradle
  wrapper scripts -- copy from any Gradle project or generate via
  `gradle wrapper --gradle-version=9.4.1` if Gradle is installed locally).

---

### Subtask T004 - Verify Maven coordinates in `[libraries]` and `[plugins]`

**Purpose**: Library Maven coordinates (group IDs, artifact IDs) occasionally change
between major versions. This step confirms every coordinate in `[libraries]` and
`[plugins]` is still valid for the updated versions.

**Steps**:

1. Review each library entry in `[libraries]` against the updated versions. Key ones to
   double-check given major version bumps:

   | Entry | Coordinate | Check |
   |-------|-----------|-------|
   | `exposed-*` | `org.jetbrains.exposed:exposed-*` | Unchanged in 1.x |
   | `hikari` | `com.zaxxer:HikariCP` | Unchanged in 7.x |
   | `flyway-*` | `org.flywaydb:flyway-*` | Unchanged in 12.x |
   | `kotest-*` | `io.kotest:kotest-assertions-core` | Unchanged in 6.x |
   | `testcontainers-postgresql` | `org.testcontainers:postgresql` | Unchanged in 2.x |
   | `ktlint` (plugin) | `org.jlleitschuh.gradle.ktlint` | May change in 14.x -- verify |

2. Confirm the `[bundles]` entries still reference valid library aliases.
   No bundle changes are expected since aliases did not change.

3. If any coordinate has changed, update the `module` value in `[libraries]` accordingly.

**Files**: `gradle/libs.versions.toml` (read-only unless a coordinate changed)
**Parallel?**: No -- depends on T001/T002 completing (reads the updated file).
**Notes**: For this greenfield project no coordinates are expected to have changed.
This step is a safety check to avoid silent build failures later.

---

## Risks & Mitigations

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| ktlint Gradle plugin DSL changed in 14.x | Medium | T004 catches this; no `build.gradle.kts` files exist yet so no immediate impact |
| Testcontainers 2.x container lifecycle API change | Low | No test code exists yet; noted for when tests are written |
| Gradle wrapper scripts (`gradlew`, `gradlew.bat`) missing | Medium | T003 notes this; create or generate them if absent |
| Typo in distributionUrl causes wrapper download failure | Low | Copy exact URL from template in T003; double-check backslash escape |

---

## Review Guidance

Reviewers should verify:

1. **`[versions]` block**: Every entry matches the resolved version in
   `kitty-specs/001-update-version-catalog-to-latest/research.md`.
2. **No `compose-compiler` version**: The standalone entry must be gone from `[versions]`.
3. **`compose-compiler` plugin**: Must reference `version.ref = "kotlin"`, not
   `version.ref = "compose-compiler"`.
4. **Wrapper file**: `gradle/wrapper/gradle-wrapper.properties` exists and declares
   `gradle-9.4.1-bin.zip`.
5. **No other changes**: `[libraries]` and `[bundles]` should be unchanged unless a
   coordinate was found to have moved in T004.

---

## Activity Log

- 2026-03-22T21:46:54Z - system - lane=planned - Prompt created.
