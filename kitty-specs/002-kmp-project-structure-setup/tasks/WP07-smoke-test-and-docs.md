---
work_package_id: WP07
title: Smoke Test & Documentation
lane: "doing"
dependencies: [WP04, WP05, WP06]
base_branch: 002-kmp-project-structure-setup-WP07-merge-base
base_commit: 4270994e7b73eb38af82f3f1b98ab8bcb75efdcb
created_at: '2026-03-23T22:59:24.039980+00:00'
subtasks:
- T028
- T029
- T030
- T031
phase: Phase 1 - Validation
assignee: ''
agent: "claude"
shell_pid: "45064"
review_status: ''
reviewed_by: ''
history:
- timestamp: '2026-03-23T21:23:31Z'
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
- FR-006
- FR-007
- FR-008
- FR-009
---

# Work Package Prompt: WP07 – Smoke Test & Documentation

## IMPORTANT: Review Feedback Status

- **Has review feedback?**: Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section immediately.
- **You must address all feedback** before your work is complete.
- **Mark as acknowledged**: When you understand the feedback, update `review_status: acknowledged`.

---

## Review Feedback

*[Empty initially. Reviewers will populate if work is returned.]*

---

## Markdown Formatting

Use language identifiers in code blocks: ```bash, ```kotlin, ```properties

---

## Objectives & Success Criteria

Run the full build smoke test, install and validate the Git pre-commit hook, update `.gitignore` for Gradle artifacts, and document the developer setup workflow in `README.md`.

**Done when:**
- [ ] `.gitignore` includes `build/`, `.gradle/`, `local.properties` without removing existing entries
- [ ] `README.md` has a "Getting Started" section with the two required setup commands
- [ ] `./gradlew build` completes with zero errors on a clean checkout
- [ ] `./gradlew installGitHooks` succeeds and `.git/hooks/pre-commit` exists and is executable
- [ ] Staging a file with a KtLint violation and running `git commit` aborts the commit
- [ ] All acceptance criteria from `spec.md` are met (SC-001 through SC-005)

---

## Context & Constraints

- **Spec**: `kitty-specs/002-kmp-project-structure-setup/spec.md` — Success Criteria SC-001 through SC-005
- **Plan**: `kitty-specs/002-kmp-project-structure-setup/plan.md` — Phase 1 Step 6
- **All prior WPs must be complete** before running T030 and T031.

**Key constraints**:
- This WP is the acceptance gate for the entire feature. If `./gradlew build` fails, diagnose and fix in this WP — do NOT proceed to done until it passes.
- `README.md` may already exist with other content — append to it, do NOT overwrite.
- `.gitignore` already exists — append entries, do NOT remove existing lines.

**Implement command** (depends on WP04, WP05, WP06): `spec-kitty implement WP07 --base WP06`

---

## Subtasks & Detailed Guidance

### Subtask T028 – Update `.gitignore`

**Purpose**: Prevent Gradle build artifacts and local configuration files from being committed to the repository. These are generated per-developer and per-machine.

**Steps**:

1. Open `.gitignore` (exists already). Do NOT remove any existing lines.

2. Append the following section if not already present:

```gitignore
# Gradle
.gradle/
build/
**/build/
!gradle/wrapper/gradle-wrapper.jar

# Local configuration
local.properties
*.local.properties

# IDE
.idea/
*.iml
*.ipr
*.iws

# OS
.DS_Store
Thumbs.db

# Android
*.apk
*.aab
*.ap_
*.dex
```

3. Verify the existing entries (from feature 001's `.gitignore`) are preserved — do a diff before saving.

**Files**: `.gitignore` (append only)
**Parallel?**: Yes — independent of T029.
**Notes**: `!gradle/wrapper/gradle-wrapper.jar` exempts the wrapper JAR from the Gradle ignore pattern — the wrapper JAR must be committed.

---

### Subtask T029 – Update `README.md`

**Purpose**: New contributors need explicit instructions to install the pre-commit hook and run the build. Without documentation, the `installGitHooks` task is invisible.

**Steps**:

1. Open `README.md` (may or may not exist). If it doesn't exist, create it.

2. Add or append the following section:

```markdown
## Getting Started

### Prerequisites

- JDK 17+ (required by the Kotlin JVM and server modules)
- Android SDK with API level 35 (required by the Android target)
- Gradle 9.4.1 — managed automatically by the Gradle wrapper

### First-Time Setup

After cloning the repository, run these two commands:

```bash
# 1. Install the Git pre-commit hook (run once per clone)
./gradlew installGitHooks

# 2. Verify the project builds cleanly
./gradlew build
```

### Development Workflow

```bash
# Run Detekt (static analysis)
./gradlew detekt

# Run KtLint check
./gradlew ktlintCheck

# Auto-fix KtLint formatting issues
./gradlew ktlintFormat

# Build a specific module
./gradlew :core:domain:build

# List all modules
./gradlew projects
```

### Project Structure

| Module | Type | Purpose |
|--------|------|---------|
| `composeApp` | Android + Desktop app | UI entry point |
| `server` | JVM (Ktor) | Backend API server |
| `shared` | KMP library | Cross-platform shared logic |
| `core/domain` | KMP library | Pure business entities (zero framework deps) |
| `core/network` | KMP library | HTTP client abstraction |
| `core/database` | KMP library | Database driver abstraction |
| `core/ui` | KMP library | Shared design system components |
| `core/common` | KMP library | Utilities and platform abstractions |
| `feature/*` | KMP library | Feature modules (auth, menu, orders, etc.) |
```

**Files**: `README.md` (update or create)
**Parallel?**: Yes — independent of T028.

---

### Subtask T030 – Run `./gradlew build` smoke test

**Purpose**: Validate that all 15 modules compile successfully across Android, JVM, and Kotlin/JS targets. This is SC-001 — the acceptance gate for the entire feature.

**Steps**:

1. Run a clean build:
```bash
./gradlew clean build --info 2>&1 | tail -50
```

2. **Expected outcome**: `BUILD SUCCESSFUL` with all tasks executed or UP-TO-DATE.

3. **If the build fails**, diagnose using the error output:

   - **"Cannot access class 'android.content.Context'"** → Android SDK not installed or `compileSdk` mismatch. Verify `compileSdk = 35` in `kmp-library.gradle.kts` and `android-app.gradle.kts`.
   - **"Unresolved reference: libs"** in buildSrc → `buildSrc/settings.gradle.kts` version catalog declaration is missing or path is wrong.
   - **"Task ':xxx:compileKotlinJs' failed"** → Kotlin/JS IR configuration error. Verify `js(IR) { browser() }` in `kmp-library.gradle.kts`.
   - **"Configuration 'androidDebugImplementation' not found"** → Module missing `android { namespace = "..." }` block.
   - **"Empty Kotlin/JS source set warning"** → Harmless warning. Suppress by adding to `gradle.properties`:
     ```properties
     kotlin.js.generate.executable.default=false
     ```

4. Verify SC-002: Pick any feature module build file and count its non-comment lines. Must be ≤15.
```bash
grep -v "^//" feature/auth/build.gradle.kts | grep -v "^$" | wc -l
# Expected: ≤ 15
```

5. Verify SC-003: Run quality checks as a single command:
```bash
./gradlew detekt ktlintCheck
# Expected: BUILD SUCCESSFUL (zero violations on empty skeleton)
```

**Files**: `gradle.properties` (may need to create or update), no other file changes.
**Parallel?**: No — must run after all module creation WPs complete.
**Notes**: If the build succeeds on the first attempt, record the success in the activity log. If fixes are needed, document them clearly in the review guidance section.

---

### Subtask T031 – Run `installGitHooks` and validate the pre-commit hook

**Purpose**: Verify that the Git pre-commit hook is correctly installed and rejects commits with lint violations. This validates SC-004 (hook rejects within 30 seconds).

**Steps**:

1. Install the hook:
```bash
./gradlew installGitHooks
```
Expected output: `Git hooks installed. Pre-commit hook is now active.`

2. Verify the hook file exists and is executable:
```bash
ls -la .git/hooks/pre-commit
# Expected: -rwxr-xr-x (has execute bit)
cat .git/hooks/pre-commit
# Expected: content of scripts/git-hooks/pre-commit
```

3. Test that the hook rejects a violation. Create a temporary test file with a KtLint violation:
```bash
# Create a file with a wildcard import (KtLint violation)
cat > /tmp/TestHook.kt << 'EOF'
import kotlin.math.*

fun main() { println("test") }
EOF
cp /tmp/TestHook.kt /tmp/TestHookViolation.kt
git add /tmp/TestHookViolation.kt 2>/dev/null || true
```

Actually, create the test file inside the project:
```bash
mkdir -p core/common/src/commonMain/kotlin
echo 'import kotlin.math.*\nfun test() {}' > core/common/src/commonMain/kotlin/TestViolation.kt
git add core/common/src/commonMain/kotlin/TestViolation.kt
git commit -m "test: should be rejected" --dry-run
```

Expected: The pre-commit hook runs Detekt and KtLint, detects the wildcard import violation, prints the error, and exits with code 1 (commit aborted).

4. Clean up the test file:
```bash
rm core/common/src/commonMain/kotlin/TestViolation.kt
git checkout -- .
```

5. Verify SC-004: The rejection should complete within 30 seconds. Time the hook run:
```bash
time ./gradlew ktlintCheck --daemon --quiet 2>&1 | tail -5
# Expected: real time < 30 seconds on warm daemon
```

**Files**: No permanent file changes (test file is temporary).
**Parallel?**: No — must run after T030 confirms the build passes.
**Notes**: On the first run, the Gradle daemon may take longer than 30s as it starts up. Subsequent runs should be well within 30s. The hook uses `--daemon` to reuse the warm daemon.

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| `./gradlew build` fails due to missing Gradle wrapper | T011 (WP03) ensures gradlew exists; if still missing, `gradle wrapper --gradle-version=9.4.1` |
| Empty Kotlin/JS source set causes build failure (not just warning) | Add `kotlin.js.generate.executable.default=false` to `gradle.properties` (Step 3 of T030) |
| Pre-commit hook too slow (>30s on cold daemon) | Run `./gradlew --daemon` once to warm up the daemon before timing in T031 |
| Hook doesn't install because `scripts/git-hooks/` doesn't exist | WP03 T010 creates this directory; if skipped, create it now with the pre-commit script content |
| `README.md` already exists with important content | T029 appends new section — does NOT overwrite; diff before saving |

---

## Review Guidance

Reviewers should verify all success criteria from `spec.md`:
1. **SC-001**: `./gradlew build` output shows `BUILD SUCCESSFUL` — no errors.
2. **SC-002**: A feature module build file is ≤15 non-comment lines.
3. **SC-003**: `./gradlew detekt ktlintCheck` produces a single `BUILD SUCCESSFUL` with zero violations.
4. **SC-004**: A commit with a KtLint violation is rejected. The rejection completes within 30 seconds (warm daemon).
5. **SC-005**: Adding a new module requires only 2 file edits (confirmed by the convention plugin architecture).
6. `.gitignore` has `build/` and `.gradle/` entries without duplicating existing lines.
7. `README.md` has the "Getting Started" section with `installGitHooks` and `build` commands.

---

## Activity Log

- 2026-03-23T21:23:31Z – system – lane=planned – Prompt created.
- 2026-03-23T22:59:24Z – claude – shell_pid=12202 – lane=doing – Assigned agent via workflow command
- 2026-03-24T00:08:22Z – claude – shell_pid=12202 – lane=for_review – Ready for review: BUILD SUCCESSFUL 477 tasks; installGitHooks worktree-aware; all convention plugin AGP 9.x fixes applied
- 2026-03-24T00:13:10Z – claude – shell_pid=45064 – lane=doing – Started review via workflow command
