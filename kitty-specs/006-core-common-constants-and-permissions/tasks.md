# Work Packages: Core Common — Constants and Permissions

**Inputs**: `kitty-specs/006-core-common-constants-and-permissions/`
**Prerequisites**: plan.md ✅, spec.md ✅

Both work packages are fully independent and can be executed in parallel. WP01 touches `core:domain` exclusively; WP02 touches `core:common` exclusively.

---

## Work Package WP01: Enum Corrections in core:domain (Priority: P0) 🎯 MVP

**Goal**: Align all four enumerations in `core:domain` to the database schema and update every test reference to renamed or replaced values.
**Independent Test**: `./gradlew :core:domain:build :core:domain:detekt` passes with zero errors, zero warnings, and all existing tests pass.
**Prompt**: `tasks/WP01-enum-corrections-core-domain.md`

### Requirement Refs
- FR-001, FR-005, FR-006, FR-007, SC-001, SC-005, SC-006

### Included Subtasks
- [x] T001 Update `Role.kt` — rename SERVER → WAITER, add VIEWER
- [x] T002 Update `OrderStatus.kt` — replace all values to match DB schema
- [x] T003 Update `TableStatus.kt` — rename FREE → AVAILABLE, add CLEANING
- [x] T004 Update `PaymentMethod.kt` — rename VOUCHER → BANK_TRANSFER
- [x] T005 [P] Fix `StaffTest.kt` — update Role references
- [x] T006 [P] Fix `OrderTest.kt` — update OrderStatus references
- [x] T007 [P] Fix `PaymentTest.kt` — update PaymentMethod references

### Implementation Notes
- Update the four enum files in `core/domain/src/commonMain/kotlin/com/vibely/domain/`
- After each enum change, immediately update the corresponding test file so the build never enters a broken state
- KDoc must be present on the enum class itself (Detekt enforces `UndocumentedPublicClass`)
- No build file changes needed

### Parallel Opportunities
- T005, T006, T007 can proceed in parallel once T001–T004 are done (each touches a different test file)

### Dependencies
- None — starting package, independent of WP02

### Risks & Mitigations
- Old enum values referenced elsewhere in domain code (not just tests) → grep for all usages before editing
- Missing KDoc on enum class → Detekt failure; add class-level KDoc during the enum edit

---

## Work Package WP02: Permission Model and Constants in core:common (Priority: P0) 🎯 MVP

**Goal**: Populate `core:common` with the Permission enum, the RolePermissions mapping object, and three placeholder constant objects.
**Independent Test**: `./gradlew :core:common:build :core:common:detekt` passes with zero errors and zero warnings.
**Prompt**: `tasks/WP02-permission-model-and-constants-core-common.md`

### Requirement Refs
- FR-002, FR-003, FR-004, FR-008, FR-009, FR-010, SC-002, SC-003, SC-004

### Included Subtasks
- [x] T008 [P] Create `Permission.kt` — 11-value enum in `com.vibely.common`
- [x] T009 Create `RolePermissions.kt` — mapping object with `permissionsFor()` and `hasPermission` extension
- [x] T010 [P] Create `DatabaseConstants.kt` — placeholder constants with `TODO()` bodies
- [x] T011 [P] Create `ApiConstants.kt` — placeholder constants with `TODO()` bodies
- [x] T012 [P] Create `SyncConstants.kt` — placeholder constants with `TODO()` bodies

### Implementation Notes
- All five files go in `core/common/src/commonMain/kotlin/com/vibely/common/`
- `RolePermissions` depends on `Permission` (T009 depends on T008) and on `Role` from `core:domain`; verify that `core:common` already depends on `core:domain` in its `build.gradle.kts`, or add the dependency
- `TODO("description")` placeholders compile cleanly and throw `NotImplementedError` at runtime — this is intentional
- KDoc required on every public class, object, function, and property
- No new source sets or build file changes beyond a potential `core:domain` dependency in `core:common`

### Parallel Opportunities
- T008, T010, T011, T012 can all be written in parallel; T009 must come after T008

### Dependencies
- None — independent of WP01 at runtime; `Role` from `core:domain` must already exist, which it does

### Risks & Mitigations
- Missing `core:domain` dependency in `core:common/build.gradle.kts` → `Role` import fails; check and add if absent
- `TODO()` placeholders for `val` constants → Kotlin requires them to be `val` with a type annotation (`val MAX_POOL_SIZE: Int`); the body is `TODO("…")` returning `Nothing`, which satisfies any type
- Missing KDoc on any public symbol → Detekt violation; add class-level and property-level KDoc during file creation

---

## Dependency & Execution Summary

- **Sequence**: WP01 and WP02 are fully independent and can run in parallel.
- **Parallelization**: Assign two agents — one to WP01, one to WP02 — for fastest completion.
- **MVP Scope**: Both WPs are MVP; neither can be deferred without leaving the domain misaligned.

---

## Subtask Index (Reference)

| Subtask ID | Summary | Work Package | Priority | Parallel? |
|------------|---------|--------------|----------|-----------|
| T001 | Update Role.kt | WP01 | P0 | No |
| T002 | Update OrderStatus.kt | WP01 | P0 | No |
| T003 | Update TableStatus.kt | WP01 | P0 | No |
| T004 | Update PaymentMethod.kt | WP01 | P0 | No |
| T005 | Fix StaffTest.kt | WP01 | P0 | Yes |
| T006 | Fix OrderTest.kt | WP01 | P0 | Yes |
| T007 | Fix PaymentTest.kt | WP01 | P0 | Yes |
| T008 | Create Permission.kt | WP02 | P0 | Yes |
| T009 | Create RolePermissions.kt | WP02 | P0 | No (needs T008) |
| T010 | Create DatabaseConstants.kt | WP02 | P0 | Yes |
| T011 | Create ApiConstants.kt | WP02 | P0 | Yes |
| T012 | Create SyncConstants.kt | WP02 | P0 | Yes |
