# Vibely Food

A Kotlin Multiplatform (KMP) food ordering and POS system targeting Android, Desktop (JVM), and Web.

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
