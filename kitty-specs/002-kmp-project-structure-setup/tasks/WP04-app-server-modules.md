---
work_package_id: WP04
title: App & Server Entry-Point Modules
lane: "done"
dependencies: [WP02]
base_branch: 002-kmp-project-structure-setup-WP02
base_commit: 3b2b21ca62ff5649d1378db14e8fa896d735811c
created_at: '2026-03-23T22:48:38.197057+00:00'
subtasks:
- T012
- T013
- T014
phase: Phase 1 - Module Scaffold
assignee: ''
agent: "claude"
shell_pid: "10459"
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
---

# Work Package Prompt: WP04 – App & Server Entry-Point Modules

## IMPORTANT: Review Feedback Status

- **Has review feedback?**: Check the `review_status` field above. If it says `has_feedback`, scroll to the **Review Feedback** section immediately.
- **You must address all feedback** before your work is complete.
- **Mark as acknowledged**: When you understand the feedback, update `review_status: acknowledged`.

---

## Review Feedback

*[Empty initially. Reviewers will populate if work is returned.]*

---

## Markdown Formatting

Wrap HTML/XML tags in backticks: `<manifest>`, `<application>`
Use language identifiers in code blocks: ```kotlin, ```xml, ```bash

---

## Objectives & Success Criteria

Create the three application entry-point modules: `composeApp` (Android + Desktop), `server` (JVM Ktor backend), and `shared` (full KMP library for cross-platform logic). All are empty shells — valid `build.gradle.kts` files and empty source directories only.

**Done when:**
- [ ] `composeApp/build.gradle.kts` applies the `android-app` convention plugin and declares its Android namespace
- [ ] `server/build.gradle.kts` applies the `jvm-server` convention plugin
- [ ] `shared/build.gradle.kts` applies the `kmp-library` convention plugin and declares its Android namespace
- [ ] All three modules have empty source directories with `.gitkeep` placeholders
- [ ] `./gradlew :composeApp:build :server:build :shared:build` completes without errors

---

## Context & Constraints

- **Plan**: `kitty-specs/002-kmp-project-structure-setup/plan.md` — Phase 1 Step 3
- **Research**: `kitty-specs/002-kmp-project-structure-setup/research.md` — Decision 4 (AGP), Decision 3 (Kotlin/JS)
- **Convention plugins**: Created in WP02 (`android-app`, `jvm-server`, `kmp-library`)
- **Module paths**: Already registered in `settings.gradle.kts` (WP01 T002)

**Key constraints**:
- `composeApp` requires a minimal `AndroidManifest.xml` — the Android Gradle Plugin will fail without it.
- Each module applying `kmp-library` or `android-app` must declare `android { namespace = "..." }` — the convention plugin does NOT set namespace.
- Namespace convention: `com.vibely.<module_name>` (e.g., `com.vibely.app`, `com.vibely.shared`).
- No source code, no dependencies declared beyond what the convention plugin provides.

**Implement command** (depends on WP02): `spec-kitty implement WP04 --base WP02`

---

## Subtasks & Detailed Guidance

### Subtask T012 – Create `composeApp/` module

**Purpose**: The `composeApp` module is the Android + Desktop application entry point. It applies the `android-app` convention plugin which configures Android and Desktop (JVM) targets with Compose.

**Steps**:

1. Create the directory structure:
```
composeApp/
├── build.gradle.kts
├── src/
│   ├── androidMain/
│   │   ├── AndroidManifest.xml
│   │   └── kotlin/
│   │       └── .gitkeep
│   ├── desktopMain/
│   │   └── kotlin/
│   │       └── .gitkeep
│   └── commonMain/
│       └── kotlin/
│           └── .gitkeep
```

2. Create `composeApp/build.gradle.kts`:
```kotlin
plugins {
    id("android-app")
}

android {
    namespace = "com.vibely.app"
}
```

3. Create `composeApp/src/androidMain/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="Vibely POS"
        android:theme="@android:style/Theme.Material.Light">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

**Files**: `composeApp/build.gradle.kts`, `composeApp/src/androidMain/AndroidManifest.xml`, `.gitkeep` files
**Parallel?**: Yes — independent of T013 and T014.
**Notes**: `MainActivity` does not need to exist yet — the manifest references a class that will be created in a future feature. The build will succeed as long as the manifest is valid XML.

---

### Subtask T013 – Create `server/` module

**Purpose**: The `server` module is the JVM-only Ktor backend. It applies the `jvm-server` convention plugin. This module is NOT a KMP module — it has a standard JVM source layout.

**Steps**:

1. Create the directory structure:
```
server/
├── build.gradle.kts
└── src/
    └── main/
        └── kotlin/
            └── .gitkeep
```

2. Create `server/build.gradle.kts`:
```kotlin
plugins {
    id("jvm-server")
}
```

3. No Android namespace needed (JVM-only module).

**Files**: `server/build.gradle.kts`, `server/src/main/kotlin/.gitkeep`
**Parallel?**: Yes — independent of T012 and T014.
**Notes**: The server module has no `android {}` block and no KMP `kotlin {}` multiplatform block. The `jvm-server` convention plugin handles everything.

---

### Subtask T014 – Create `shared/` module

**Purpose**: The `shared` module holds cross-platform business logic and utilities shared between `composeApp` and `server`. It applies the `kmp-library` convention plugin targeting all three platforms (Android, JVM, Kotlin/JS).

**Steps**:

1. Create the directory structure:
```
shared/
├── build.gradle.kts
└── src/
    ├── commonMain/
    │   └── kotlin/
    │       └── .gitkeep
    ├── androidMain/
    │   └── kotlin/
    │       └── .gitkeep
    ├── jvmMain/
    │   └── kotlin/
    │       └── .gitkeep
    └── jsMain/
        └── kotlin/
            └── .gitkeep
```

2. Create `shared/build.gradle.kts`:
```kotlin
plugins {
    id("kmp-library")
}

android {
    namespace = "com.vibely.shared"
}
```

**Files**: `shared/build.gradle.kts`, source directories with `.gitkeep` files
**Parallel?**: Yes — independent of T012 and T013.
**Notes**: The `jsMain` source set is empty in this phase but must be declared (by the convention plugin via `js(IR) { browser() }`). Kotlin/JS will not error on empty source sets.

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| AGP fails due to missing `AndroidManifest.xml` in `composeApp` | T012 creates a minimal manifest; this is the minimum required by AGP |
| `android.namespace` not set — AGP 8.x fails build | Every module applying an Android plugin must set namespace; documented in T012 and T014 |
| `desktopMain` source set not recognized | Named `jvm("desktop")` in `android-app.gradle.kts`; Gradle creates `desktopMain` source set automatically |
| `server` module lacks `src/main/kotlin` for test source sets | Create `src/test/kotlin/.gitkeep` alongside `src/main/kotlin/.gitkeep` if JUnit tests are anticipated |

---

## Review Guidance

Reviewers should verify:
1. `composeApp/build.gradle.kts` applies exactly `"android-app"` — no other plugins or dependencies.
2. `server/build.gradle.kts` applies exactly `"jvm-server"` — no other content.
3. `shared/build.gradle.kts` applies `"kmp-library"` and sets `android { namespace = "com.vibely.shared" }`.
4. `composeApp/src/androidMain/AndroidManifest.xml` is valid XML with an `<application>` element.
5. All empty source directories have a `.gitkeep` so git tracks them.

---

## Activity Log

- 2026-03-23T21:23:31Z – system – lane=planned – Prompt created.
- 2026-03-23T22:48:38Z – claude – shell_pid=7993 – lane=doing – Assigned agent via workflow command
- 2026-03-23T22:52:54Z – claude – shell_pid=7993 – lane=for_review – Ready for review: composeApp (android-app plugin), server (jvm-server plugin), shared (kmp-library plugin) with empty source dirs and .gitkeep files
- 2026-03-23T22:57:55Z – claude – shell_pid=10459 – lane=doing – Started review via workflow command
- 2026-03-23T22:58:17Z – claude – shell_pid=10459 – lane=done – Review passed: composeApp (android-app, namespace set), server (jvm-server), shared (kmp-library, namespace set), AndroidManifest.xml present, source dirs with .gitkeep
