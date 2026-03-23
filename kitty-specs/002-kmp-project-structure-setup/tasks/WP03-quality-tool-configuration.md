---
work_package_id: WP03
title: Quality Tool Configuration
lane: "doing"
dependencies: [WP01]
base_branch: 002-kmp-project-structure-setup-WP01
base_commit: 06994075dbf5ca82d9e31cccc788524b8c1a8f4d
created_at: '2026-03-23T22:39:27.131347+00:00'
subtasks:
- T008
- T009
- T010
- T011
phase: Phase 0 - Foundation
assignee: ''
agent: "claude"
shell_pid: "4839"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-23T21:23:31Z'
  lane: planned
  agent: system
  shell_pid: ''
  action: Prompt generated via /spec-kitty.tasks
requirement_refs:
- FR-004
- FR-005
- FR-006
- FR-009
---

# Work Package Prompt: WP03 – Quality Tool Configuration

## IMPORTANT: Review Feedback Status

- **Has review feedback?**: Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section immediately.
- **You must address all feedback** before your work is complete.
- **Mark as acknowledged**: When you understand the feedback, update `review_status: acknowledged`.

---

## Review Feedback

*[Empty initially. Reviewers will populate if work is returned.]*

---

## Markdown Formatting

Wrap HTML/XML tags in backticks: `<manifest>`
Use language identifiers in code blocks: ```yaml, ```ini, ```bash, ```kotlin

---

## Objectives & Success Criteria

Configure Detekt and KtLint with rulesets aligned with the project constitution, create the Git pre-commit hook script, and verify the Gradle wrapper scripts are present.

**Done when:**
- [ ] `detekt.yml` exists at the project root with all constitution-required rules enabled
- [ ] `.editorconfig` exists at the project root with KtLint configuration (indent=4, max_line=120)
- [ ] `scripts/git-hooks/pre-commit` exists and contains a valid bash script
- [ ] `gradlew` and `gradlew.bat` exist and are executable
- [ ] `./gradlew detekt ktlintCheck` passes with zero violations on the empty project skeleton

---

## Context & Constraints

- **Spec**: `kitty-specs/002-kmp-project-structure-setup/spec.md`
- **Plan**: `kitty-specs/002-kmp-project-structure-setup/plan.md`
- **Research**: `kitty-specs/002-kmp-project-structure-setup/research.md` — Decisions 5, 6, 7
- **Constitution**: `.kittify/memory/constitution.md` — Code Quality section mandates specific Detekt rules
- **Target files**: `detekt.yml` (new), `.editorconfig` (new), `scripts/git-hooks/pre-commit` (new)

**Key constraints**:
- Constitution requires: `UndocumentedPublicClass`, `UndocumentedPublicFunction`, `UndocumentedPublicProperty` must be enabled in Detekt.
- Constitution requires: `CyclomaticComplexity`, `NestedBlockDepth`, `LongMethod` must be enabled.
- The `installGitHooks` Gradle task (defined in WP01's `build.gradle.kts`) copies `scripts/git-hooks/` to `.git/hooks/`. The pre-commit script must be at this exact path.
- KtLint 14.x reads `.editorconfig` from the project root automatically.
- This WP can run in parallel with WP02 — these files are independent of the convention plugins.

**Implement command** (depends on WP01): `spec-kitty implement WP03 --base WP01`

---

## Subtasks & Detailed Guidance

### Subtask T008 – Create `detekt.yml`

**Purpose**: Detekt's default configuration enables many rules but the constitution requires specific ones to be explicitly enabled and tuned. A project-level `detekt.yml` serves as the single source of truth for all code quality rules. This file is referenced in the root `build.gradle.kts` via `config.setFrom(rootProject.files("detekt.yml"))`.

**Steps**:

1. Create `detekt.yml` at the project root with the following content:

```yaml
# Detekt configuration for Vibely POS
# Aligned with .kittify/memory/constitution.md — Code Quality section
# See: https://detekt.dev/docs/introduction/configurations

build:
  maxIssues: 0
  excludeCorrectable: false

config:
  validation: true
  warningsAsErrors: false

processors:
  active: true

console-reports:
  active: true

output-reports:
  active: true

comments:
  active: true
  UndocumentedPublicClass:
    active: true
    excludes: ["**/test/**", "**/androidTest/**"]
  UndocumentedPublicFunction:
    active: true
    excludes: ["**/test/**", "**/androidTest/**"]
  UndocumentedPublicProperty:
    active: true
    excludes: ["**/test/**", "**/androidTest/**"]

complexity:
  active: true
  CyclomaticComplexity:
    active: true
    threshold: 15
  NestedBlockDepth:
    active: true
    threshold: 4
  LongMethod:
    active: true
    threshold: 60
  TooManyFunctions:
    active: true
    thresholdInFiles: 20
    thresholdInClasses: 15
    thresholdInInterfaces: 10
  LongParameterList:
    active: true
    functionThreshold: 6
    constructorThreshold: 7

coroutines:
  active: true
  GlobalCoroutineUsage:
    active: true
  RedundantSuspendModifier:
    active: true
  SuspendFunWithFlowReturnType:
    active: true

empty-blocks:
  active: true

exceptions:
  active: true
  TooGenericExceptionCaught:
    active: true
  TooGenericExceptionThrown:
    active: true
  SwallowedException:
    active: true

naming:
  active: true

performance:
  active: true

potential-bugs:
  active: true
  UnsafeCallOnNullableType:
    active: true
  UnsafeCast:
    active: true

style:
  active: true
  ForbiddenComment:
    active: true
    values: ["FIXME:", "HACK:", "TODO:"]
    allowedPatterns: ""
  MagicNumber:
    active: true
    excludes: ["**/test/**", "**/androidTest/**", "**/*.gradle.kts"]
    ignoreNumbers: ["-1", "0", "1", "2"]

# Exclude generated and build directories
# These patterns apply globally to all rules
```

2. Add a `detekt {}` extension in `gradle.properties` if needed to exclude build directories:
```properties
detekt.source.set=src
```
Actually, exclusions are better handled in `build.gradle.kts` — skip the properties file for now.

**Files**: `detekt.yml` (new at project root)
**Parallel?**: No — T009, T010, T011 are independent and can run after this.
**Notes**: `maxIssues: 0` means any violation fails the build. This is intentional (constitution: "Detekt analysis — zero violations"). Relax specific rules in `detekt.yml` rather than raising the `maxIssues` threshold.

---

### Subtask T009 – Create `.editorconfig`

**Purpose**: KtLint 14.x reads `.editorconfig` for formatting rules. This file configures indentation, line length, and import style for all Kotlin files. It also benefits IDE auto-formatting.

**Steps**:

1. Create `.editorconfig` at the project root:

```ini
# EditorConfig — https://editorconfig.org
# KtLint reads this automatically in version 14.x

root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
insert_final_newline = true
trim_trailing_whitespace = true

[*.{kt,kts}]
indent_size = 4
max_line_length = 120
ktlint_standard_no-wildcard-imports = enabled
ktlint_standard_import-ordering = enabled
ktlint_standard_filename = enabled
ktlint_standard_final-newline = enabled
ktlint_standard_trailing-comma-on-call-site = disabled
ktlint_standard_trailing-comma-on-declaration-site = disabled

[*.{yml,yaml}]
indent_size = 2

[*.{json}]
indent_size = 2

[*.{md}]
trim_trailing_whitespace = false
```

**Files**: `.editorconfig` (new at project root)
**Parallel?**: Yes — independent of T008.
**Notes**: `max_line_length = 120` is a reasonable default for Kotlin; adjust if the team prefers 100. `trailing-comma-*` rules are disabled because Kotlin trailing commas are optional and teams have mixed preferences.

---

### Subtask T010 – Create `scripts/git-hooks/pre-commit`

**Purpose**: This script is copied to `.git/hooks/pre-commit` by the `installGitHooks` Gradle task (defined in WP01). It runs Detekt and KtLint before every commit and aborts if either check fails.

**Steps**:

1. Create the directory:
```bash
mkdir -p scripts/git-hooks
```

2. Create `scripts/git-hooks/pre-commit`:

```sh
#!/bin/sh
# pre-commit hook for Vibely POS
# Runs Detekt and KtLint on every commit.
# Install with: ./gradlew installGitHooks
# Skip (emergency): git commit --no-verify

set -e

echo "Running pre-commit quality checks..."

./gradlew detekt ktlintCheck --daemon --quiet
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
    echo ""
    echo "❌ Pre-commit checks FAILED. Fix the violations above before committing."
    echo "   To auto-fix KtLint issues: ./gradlew ktlintFormat"
    echo "   To skip (emergency only): git commit --no-verify"
    exit 1
fi

echo "✓ Pre-commit checks passed."
exit 0
```

3. The file does NOT need to be made executable here — the `installGitHooks` Gradle task (WP01) sets the executable bit when it copies the file to `.git/hooks/`.

**Files**: `scripts/git-hooks/pre-commit` (new)
**Parallel?**: Yes — independent of T008 and T009.
**Notes**: The `--daemon` flag reuses the Gradle daemon for faster runs. `--quiet` suppresses task progress output, showing only violations. `set -e` ensures the script exits on the first error.

---

### Subtask T011 – Verify `gradlew` and `gradlew.bat` exist

**Purpose**: The Gradle wrapper scripts must be present in the repository for `./gradlew build` to work without a locally installed Gradle distribution. The `gradle/wrapper/gradle-wrapper.properties` file was created in feature 001, but the wrapper scripts may still be missing.

**Steps**:

1. Check if `gradlew` exists at the project root:
```bash
ls -la gradlew gradlew.bat 2>/dev/null
```

2. **If both exist**: Verify `gradlew` is executable (`-rwxr-xr-x`). If not, run:
```bash
chmod +x gradlew
```

3. **If either is missing**: Generate the wrapper scripts using a local Gradle installation:
```bash
gradle wrapper --gradle-version=9.4.1 --distribution-type=bin
```
Or, if Gradle is not installed locally, copy the standard `gradlew` and `gradlew.bat` scripts from the Gradle wrapper GitHub repository (they are version-agnostic launcher scripts).

4. Commit `gradlew` and `gradlew.bat` to the repository. These files must be tracked by git.

**Files**: `gradlew` (verify/create), `gradlew.bat` (verify/create)
**Parallel?**: Yes — independent of T008, T009, T010.
**Notes**: The `gradle/wrapper/gradle-wrapper-launcher-sha256` file is optional but recommended for security. If generated by `gradle wrapper`, include it.

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Detekt fails on empty source sets | Empty modules have no Kotlin files; Detekt should produce zero issues. If it errors on "no source files", add `detekt { ignoreFailures = true }` for modules with empty sources — but verify this isn't masking real issues. |
| KtLint flags generated files in `build/` | Add `build/` to `.editorconfig` exclusions or configure KtLint to exclude generated directories in root `build.gradle.kts` |
| `gradlew` missing entirely | Document in README: run `gradle wrapper --gradle-version=9.4.1` to regenerate |
| Pre-commit hook runs full project scan (slow) | `--daemon` flag mitigates cold start; subsequent runs are fast. Acceptable for now; optimize to scan only staged files in a future iteration. |

---

## Review Guidance

Reviewers should verify:
1. `detekt.yml` has `UndocumentedPublicClass`, `UndocumentedPublicFunction`, `UndocumentedPublicProperty` all set to `active: true`.
2. `detekt.yml` has `CyclomaticComplexity`, `NestedBlockDepth`, `LongMethod` configured with the specified thresholds.
3. `.editorconfig` has `max_line_length = 120` and `ktlint_standard_no-wildcard-imports = enabled`.
4. `scripts/git-hooks/pre-commit` has `#!/bin/sh` header and runs `./gradlew detekt ktlintCheck`.
5. `gradlew` is executable (`ls -la gradlew` shows `x` permission).

---

## Activity Log

- 2026-03-23T21:23:31Z – system – lane=planned – Prompt created.
- 2026-03-23T22:39:27Z – claude – shell_pid=4839 – lane=doing – Assigned agent via workflow command
