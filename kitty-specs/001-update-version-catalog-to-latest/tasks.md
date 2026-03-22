---
description: "Work package task list for Update Version Catalog to Latest"
---

# Work Packages: Update Version Catalog to Latest

**Inputs**: Design documents from `kitty-specs/001-update-version-catalog-to-latest/`
**Prerequisites**: plan.md, spec.md, research.md

---

## Work Package WP01: Update Version Catalog and Add Gradle Wrapper (Priority: P1) MVP

**Goal**: Update all outdated library versions in `gradle/libs.versions.toml` to their
latest stable releases per the project constitution, fix the `compose-compiler` plugin
reference, and create the missing Gradle wrapper properties file.

**Independent Test**: A clean build resolves all dependencies without errors or
version conflict warnings. Running `./gradlew --version` reports Gradle 9.4.1.

**Prompt**: `kitty-specs/001-update-version-catalog-to-latest/tasks/WP01-update-version-catalog.md`

**Estimated prompt size**: ~250 lines

### Included Subtasks

- [x] T001 Update all version strings in `[versions]` block of `gradle/libs.versions.toml`
        and remove the now-redundant `compose-compiler` standalone version entry
- [x] T002 Fix the `compose-compiler` plugin entry in `[plugins]` block to reference
        `version.ref = "kotlin"` instead of `version.ref = "compose-compiler"`
- [x] T003 [P] Create `gradle/wrapper/gradle-wrapper.properties` with Gradle 9.4.1
- [x] T004 Verify all library Maven coordinates in `[libraries]` and `[plugins]` blocks
        are still valid for the updated versions

**Requirements Refs**: FR-001, FR-002, FR-003, FR-004, FR-005

### Implementation Notes

1. All resolved versions are in `kitty-specs/001-update-version-catalog-to-latest/research.md`
2. T001 and T002 both modify `gradle/libs.versions.toml` -- do T001 first, then T002
3. T003 can be done in parallel after T001 (different file)
4. T004 is a final review pass before declaring done

### Parallel Opportunities

- T003 (create wrapper file) is independent of T001/T002 and can run in parallel.

### Dependencies

- None (first and only work package).

### Risks & Mitigations

- **Exposed 0.x -> 1.x major bump**: No code exists yet, so zero risk. Noted for
  awareness.
- **Testcontainers 1.x -> 2.x major bump**: Same -- no code yet, zero risk.
- **Gradle wrapper missing**: Must be created, not updated. Use exact URL format for
  Gradle distributions.

---

## Dependency & Execution Summary

- **Sequence**: WP01 is the only package -- no dependencies.
- **Parallelization**: T003 can run alongside T001/T002.
- **MVP Scope**: WP01 IS the full scope.

---

## Subtask Index (Reference)

| Subtask ID | Summary                                          | Work Package | Priority | Parallel? |
|------------|--------------------------------------------------|--------------|----------|-----------|
| T001       | Update [versions] block + remove compose-compiler| WP01         | P1       | No        |
| T002       | Fix compose-compiler plugin version.ref          | WP01         | P1       | No        |
| T003       | Create gradle-wrapper.properties                 | WP01         | P1       | Yes       |
| T004       | Verify library Maven coordinates                 | WP01         | P1       | No        |
