# Research: KMP Project Structure Setup

**Feature**: `002-kmp-project-structure-setup`
**Date**: 2026-03-23
**Status**: Complete — all decisions resolved

---

## Decision 1: Convention Plugin Approach (`buildSrc` vs `includeBuild`)

**Decision**: Use `buildSrc` with precompiled Kotlin script plugins (`.gradle.kts` files).

**Rationale**:
- `buildSrc` is the established convention for single-repo convention plugins and has first-class IDE support in IntelliJ/Android Studio.
- Precompiled script plugins (`.gradle.kts` in `buildSrc/src/main/kotlin/`) are the recommended pattern for KMP projects. They allow version catalog accessors inside the plugin via the `versionCatalogs` extension.
- `includeBuild` (composite builds) is preferred when plugins need to be shared across multiple independent repositories. Not applicable here.
- Gradle 9.4.1 fully supports this pattern with no deprecations.

**Alternatives considered**:
- `includeBuild` with a separate `build-logic` project — more overhead, better for monorepo with separate deployable components. Rejected: single repo, no need.
- Inline configuration in root `build.gradle.kts` with `allprojects {}` — poor maintainability, cannot be tested or reused. Rejected.

---

## Decision 2: Version Catalog Access in `buildSrc`

**Decision**: Declare the version catalog inside `buildSrc/build.gradle.kts` using the `versionCatalogs` settings extension, pointing to `../gradle/libs.versions.toml`.

```kotlin
// buildSrc/build.gradle.kts
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
```

**Rationale**: This is the official Gradle pattern for accessing the root project's version catalog from `buildSrc`. Gradle 9.x supports this pattern stably. Convention plugins can then use `libs.plugins.*` and `libs.versions.*` accessors.

**Alternatives considered**:
- Hardcoding versions in `buildSrc` — violates FR-008 (version catalog only). Rejected.
- Duplicating the catalog in `buildSrc/settings.gradle.kts` — creates version drift risk. Rejected.

---

## Decision 3: Kotlin/JS Target Mode

**Decision**: Use `js(IR) { browser() }` — Kotlin/JS IR compiler targeting browser.

**Rationale**:
- The Kotlin/JS IR (Intermediate Representation) compiler is the stable, production-ready choice as of Kotlin 2.x. The legacy compiler is removed in Kotlin 2.0+.
- `browser()` mode targets Web browsers via JavaScript, which aligns with Vibely POS's web interface goal.
- Kotlin/WASM (`wasmJs`) is deferred: it requires a separate browser WASM flag and has more limited ecosystem support in Compose Multiplatform 1.10.x for production use.

**Alternatives considered**:
- `js(IR) { nodejs() }` — for Node.js server-side JS. Not applicable for a web UI.
- `wasmJs { browser() }` — experimental in Compose 1.10.x for production; deferred to a future phase.

---

## Decision 4: Android Gradle Plugin (AGP) Version Compatible with KMP 2.3.20

**Decision**: Use AGP **8.9.0** (latest stable as of early 2026, compatible with Kotlin 2.3.x).

**Rationale**:
- KMP 2.3.20 requires AGP ≥ 8.1.0 for the Android target.
- AGP 8.9.0 is the latest stable release and aligns with `compileSdk = 35`.
- Requires `distributionUrl` pointing to Gradle 9.4.1 (already set).
- Must be added to `libs.versions.toml` as a new entry: `agp = "8.9.0"` with plugin `android-application = { id = "com.android.application", version.ref = "agp" }`.

**Alternatives considered**:
- AGP 8.5.x — stable but missing performance improvements in 8.9. Rejected in favor of latest stable per constitution policy.

---

## Decision 5: Detekt Project-Wide Configuration

**Decision**: Apply Detekt via the root `build.gradle.kts` using `subprojects { apply(plugin = "io.gitlab.arturbosch.detekt") }` with a shared `detekt.yml` baseline at the project root.

**Rationale**:
- Applying via `subprojects {}` ensures every module is analyzed without per-module configuration.
- A single `detekt.yml` file at root provides one place to tune rules, consistent with constitution requirements.
- The `detekt` plugin version 1.23.8 (from `libs.versions.toml`) supports this configuration pattern fully.

**Key `detekt.yml` settings** (aligned with constitution):
```yaml
complexity:
  active: true
  CyclomaticComplexity:
    threshold: 15
  NestedBlockDepth:
    threshold: 4
  LongMethod:
    threshold: 60
comments:
  active: true
  UndocumentedPublicClass:
    active: true
  UndocumentedPublicFunction:
    active: true
  UndocumentedPublicProperty:
    active: true
```

---

## Decision 6: KtLint Project-Wide Configuration

**Decision**: Apply KtLint via the JLL KtLint Gradle plugin (version 14.2.0, already in catalog) to all subprojects from the root `build.gradle.kts`.

**Rationale**:
- Plugin ID `org.jlleitschuh.gradle.ktlint` version 14.x supports KtLint 1.x rules and Kotlin 2.x.
- Apply via `subprojects { apply(plugin = "org.jlleitschuh.gradle.ktlint") }` for project-wide coverage.
- `.editorconfig` at project root controls style rules (indentation, max line length, etc.).

**`.editorconfig` key settings**:
```ini
[*.{kt,kts}]
indent_size = 4
max_line_length = 120
ktlint_standard_no-wildcard-imports = enabled
```

---

## Decision 7: Git Pre-Commit Hook Installation

**Decision**: Use a Gradle `installGitHooks` task (type `Copy`) that copies `scripts/git-hooks/pre-commit` into `.git/hooks/` and sets the executable bit.

**Rationale**:
- Developers run `./gradlew installGitHooks` once after cloning — simple and cross-platform (macOS, Linux, Windows with Git Bash).
- The hook script calls `./gradlew detekt ktlintCheck --daemon` so it reuses the Gradle daemon (fast on warm runs).
- Committing the hook script source to `scripts/git-hooks/` ensures version control of the hook content.
- Alternative approaches (Husky, pre-commit framework) introduce Node.js or Python dependencies. Rejected to keep the toolchain Gradle-only.

---

## Decision 8: Module Dependency Rules Enforcement

**Decision**: Rely on Gradle's built-in dependency resolution failure for circular dependencies. Document layer rules in code comments and ADR. Use Detekt's `ForbiddenImport` rule to enforce that `core/domain` never imports framework packages.

**Rationale**:
- Gradle natively fails builds with circular dependencies — covers FR-007 partially.
- A Detekt `ForbiddenImport` rule in `detekt.yml` scoped to `core/domain` source files can flag accidental framework imports (e.g., `io.ktor.*`, `io.insert-koin.*`).
- Full module dependency graph enforcement (e.g., via `forbidden-dependency-checker` Gradle plugin) is deferred — not needed for an empty skeleton. Added as a note for future consideration.

---

## AGP Version Catalog Addition

The following entries must be added to `gradle/libs.versions.toml` as part of this feature:

```toml
[versions]
# ... existing entries ...
agp = "8.9.0"

[plugins]
# ... existing entries ...
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
```

This is the only change to `libs.versions.toml` in this feature. All other entries already exist.

---

## Summary Table

| # | Decision | Chosen | Key Reason |
|---|----------|--------|-----------|
| 1 | Convention plugin location | `buildSrc` precompiled scripts | IDE support, single-repo standard |
| 2 | Version catalog in buildSrc | `versionCatalogs { from(files(...)) }` | Official Gradle pattern, avoids duplication |
| 3 | Kotlin/JS target mode | `js(IR) { browser() }` | IR is only compiler in Kotlin 2.x |
| 4 | AGP version | 8.9.0 | Latest stable, compatible with KMP 2.3.20 |
| 5 | Detekt wiring | `subprojects {}` in root + `detekt.yml` | Single config, all modules covered |
| 6 | KtLint wiring | JLL plugin via `subprojects {}` + `.editorconfig` | Version 14.2 already in catalog |
| 7 | Git hooks | Gradle `installGitHooks` task | Gradle-only toolchain, cross-platform |
| 8 | Layer rule enforcement | Gradle circular dep detection + Detekt `ForbiddenImport` | Sufficient for skeleton; full plugin deferred |
