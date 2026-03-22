# Feature Specification: Update Version Catalog to Latest

**Feature Branch**: `001-update-version-catalog-to-latest`
**Created**: 2026-03-22
**Status**: Draft

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Version Catalog Reflects Latest Stable Versions (Priority: P1)

A developer setting up or building the project expects all declared library versions to be
current. Outdated versions may contain known bugs, security vulnerabilities, or lack
improvements the project wants to leverage. The version catalog is the single source of
truth for all dependency versions across every module.

**Why this priority**: The version catalog is foundational — every module and feature
built on top of it inherits these versions. Getting this right at the start avoids
cascading update chores later.

**Independent Test**: Can be fully tested by inspecting the version catalog entries against
the constitution-mandated versions and verifying the project builds cleanly.

**Acceptance Scenarios**:

1. **Given** the version catalog exists, **When** a developer reads it, **Then** every
   library version matches the latest stable version listed in the project constitution.
2. **Given** updated versions in the catalog, **When** a clean build is triggered,
   **Then** all dependency declarations resolve without errors.
3. **Given** updated versions in the catalog, **When** all modules are compiled,
   **Then** every module compiles successfully with no version conflict warnings.

---

### User Story 2 - Gradle Wrapper Pinned to Latest Stable (Priority: P2)

Every contributor and CI pipeline must use the same Gradle version to guarantee
reproducible builds. The wrapper file pins this version and must reflect the latest
stable release.

**Why this priority**: Reproducible builds are a prerequisite for reliable CI and
consistent developer experience. This is slightly lower priority than the library
versions but must be resolved in the same pass.

**Independent Test**: Can be fully tested by running `./gradlew --version` and confirming
the reported Gradle version matches the constitution-mandated version.

**Acceptance Scenarios**:

1. **Given** the Gradle wrapper is configured, **When** a developer runs `./gradlew --version`,
   **Then** the reported version matches the constitution-mandated Gradle version.
2. **Given** the updated wrapper, **When** a fresh build is executed on any supported
   platform, **Then** the build completes without wrapper version mismatch warnings.

---

### Edge Cases

- What happens if two libraries declare conflicting transitive dependency versions?
  The build must surface a clear resolution strategy (prefer the higher version).
- What happens if a library's latest stable version introduces a breaking API change?
  This must be flagged during the build; the spec assumes versions are confirmed
  compatible per the constitution.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The version catalog MUST declare each library at its latest stable version
  as mandated by the project constitution.
- **FR-002**: The version catalog MUST include all libraries listed in the constitution:
  Kotlin, Compose Multiplatform, Ktor, Koin, Exposed, SQLDelight, PostgreSQL driver,
  PgBouncer, Flyway, Kotest, Turbine, Testcontainers, Detekt, KtLint, HikariCP,
  kotlinx-coroutines, kotlinx-serialization, kotlinx-datetime.
- **FR-003**: The Gradle wrapper MUST be configured to use the constitution-mandated
  Gradle version.
- **FR-004**: All modules MUST compile successfully against the updated versions with
  no unresolved dependency errors.
- **FR-005**: The version catalog MUST use type-safe accessors (bundles and aliases)
  so modules can reference dependencies without hardcoding version strings.

### Key Entities

- **Version Catalog** (`gradle/libs.versions.toml`): Central file declaring all
  dependency versions, library coordinates, plugin IDs, and dependency bundles.
- **Gradle Wrapper** (`gradle/wrapper/gradle-wrapper.properties`): Pins the Gradle
  distribution version used by all contributors and CI.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every library version in the catalog matches the latest stable version
  defined in the project constitution — zero discrepancies.
- **SC-002**: A clean build against the updated catalog completes with zero dependency
  resolution errors or version conflict warnings.
- **SC-003**: All project modules compile successfully after the version update —
  zero compilation failures attributable to version changes.
- **SC-004**: The Gradle wrapper reports the constitution-mandated Gradle version on
  all supported platforms (macOS, Linux, Windows).

---

## Assumptions

- Library versions listed in the constitution (ratified 2026-03-22) are mutually
  compatible and have been verified by the project team.
- No breaking API changes in the updated library versions require code changes beyond
  the version catalog itself.
- The Gradle wrapper update does not require changes to any `build.gradle.kts` files.
