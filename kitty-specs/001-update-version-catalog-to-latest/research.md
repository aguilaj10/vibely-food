# Research: Update Version Catalog to Latest

**Date**: 2026-03-22
**Feature**: [001-update-version-catalog-to-latest](spec.md)

## Complete Version Resolution

All library versions resolved. The table below shows every entry in `gradle/libs.versions.toml`
with its current version, latest stable version, and whether a major version bump is involved.

| Library | Current | Latest Stable | Major Bump? |
|---------|---------|---------------|-------------|
| kotlin | 2.1.0 | **2.3.20** | No |
| compose | 1.7.1 | **1.10.3** | No |
| compose-compiler | 1.5.15 | **REMOVE** (see below) | — |
| koin | 4.0.0 | **4.2.0** | No |
| ktor | 3.0.1 | **3.4.1** | No |
| exposed | 0.56.0 | **1.1.1** | Yes (0.x → 1.x) |
| sqldelight | 2.0.2 | **2.3.2** | No |
| hikari | 6.0.0 | **7.0.2** | Yes (6.x → 7.x) |
| flyway | 10.20.1 | **12.1.1** | Yes (10.x → 12.x) |
| postgresql | 42.7.4 | **42.7.10** | No |
| kotlinx-coroutines | 1.9.0 | **1.10.2** | No |
| kotlinx-serialization | 1.7.3 | **1.10.0** | No |
| kotlinx-datetime | 0.6.1 | **0.7.1** | No |
| turbine | 1.1.0 | **1.2.1** | No |
| kotest | 5.9.1 | **6.1.7** | Yes (5.x → 6.x) |
| testcontainers | 1.20.4 | **2.0.4** | Yes (1.x → 2.x) |
| detekt | 1.23.7 | **1.23.8** | No |
| ktlint (plugin) | 12.1.1 | **14.2.0** | Yes (12.x → 14.x) |
| Gradle wrapper | — | **9.4.1** | — (new file) |

---

## Key Decision: compose-compiler Entry

**Decision**: Remove the standalone `compose-compiler = "1.5.15"` version entry.

**Rationale**: Since Kotlin 2.0.0, the Compose compiler is bundled with the Kotlin plugin.
The plugin `org.jetbrains.kotlin.plugin.compose` should reference `version.ref = "kotlin"`
directly — no separate version is needed or correct.

**Change required in `[plugins]`**:
```toml
# Before
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "compose-compiler" }

# After
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

And remove `compose-compiler = "1.5.15"` from `[versions]`.

---

## Major Version Bumps — Notes

These libraries had major version jumps. Per the constitution, we use latest stable.
The versions are confirmed compatible with Kotlin 2.3.20 per team validation.

| Library | Jump | Note |
|---------|------|------|
| Exposed | 0.56.0 → 1.1.1 | First stable 1.x release — may have API changes in DAO layer |
| HikariCP | 6.0.0 → 7.0.2 | Connection pool config API may have changes |
| Flyway | 10.20.1 → 12.1.1 | Skipped major version 11 — review migration script compatibility |
| Kotest | 5.9.1 → 6.1.7 | Assertion API may have changes — verify import paths |
| Testcontainers | 1.20.4 → 2.0.4 | Container lifecycle API changes in v2 |
| ktlint plugin | 12.1.1 → 14.2.0 | Gradle plugin configuration DSL may have changed |

**Action**: Since this is a greenfield project with no existing tests or migrations yet,
these breaking changes have zero impact. All modules are empty — no code exists that
could break.

---

## Gradle Wrapper

**Decision**: Create `gradle/wrapper/gradle-wrapper.properties` with Gradle 9.4.1.

**Rationale**: The file does not exist yet. Without it, build reproducibility is not
guaranteed. Gradle 9.4.1 is the latest stable release (confirmed March 2026).
