# Quickstart: Staff Authentication & Login

**Feature**: 009-staff-authentication-and-login

---

## Build

```bash
# From repo root

# Default (production mode)
./gradlew :composeApp:assembleDebug

# Debug auth mode (skips login screen)
AUTH_MODE=debug ./gradlew :composeApp:assembleDebug

# Fake auth mode (for automated tests)
AUTH_MODE=fake ./gradlew :feature:auth:jvmTest
```

## Run Tests

```bash
# Unit tests — all auth use cases via FakeAuthMode (no network, no emulator)
./gradlew :feature:auth:jvmTest

# Full build with quality gates
./gradlew ktlintFormat && ./gradlew ktlintCheck && ./gradlew detekt && ./gradlew build
```

## Environment Variables

| Variable | Values | Default | Description |
|---|---|---|---|
| `AUTH_MODE` | `production`, `debug`, `fake` | `production` | Selects `AuthMode` implementation at build time |

## AUTH_MODE Modes

| Mode | Behaviour |
|---|---|
| `production` | Full JWT auth via backend. Login screen shown on cold start. |
| `debug` | Auto-authenticates as `Role.OWNER`. Login screen never shown. |
| `fake` | `FakeAuthMode` with all results defaulting to `Result.failure`. Configure in tests. |

## Module Dependencies

```
composeApp
  └── feature:auth
        └── core:network
        └── core:domain

core:network
  └── (ktor-client, kotlinx-serialization)

feature:auth
  └── core:domain  (UserId, StoreId, Role)
  └── core:network (AuthApiClient)
```
