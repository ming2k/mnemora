# ADR-0003: Debug Data Seeding

- **Status**: Superseded by ADR-0004
- **Date**: 2026-04-25
- **Deciders**: Project maintainers

## Context

Developers need realistic data (books, questions, study sessions) to manually test features like Resume and Records. This data must never leak into production builds or affect release APK size.

## Decision

Use **source-set isolation** with identically-named classes in `src/debug/` and `src/release/`.

```
app/src/debug/java/com/hihusky/mnema/initialization/DebugHooks.kt   // seeds data
app/src/release/java/com/hihusky/mnema/initialization/DebugHooks.kt // no-op
```

`MnemaApplication.onCreate()` calls `DebugHooks.seedIfNeeded(this)` unconditionally. Gradle compiles only the source set matching the current build type, so release builds link the no-op stub while debug builds link the real seeder.

### What the debug seeder does

1. Imports `sample-package.zip` (already in `assets/`) if no books exist.
2. Creates 2–3 synthetic `StudySessionEntity` rows so Resume and Records are immediately visible.

All work runs on `Dispatchers.IO` inside a `GlobalScope` launched from `Application.onCreate()`.

## Alternatives considered

- **Reflection (`Class.forName`) from `src/main/`**: Rejected — not type-safe, fragile, silently fails on refactor.
- **Product flavors (`dev` / `prod`)**: Rejected — overkill. The project already distinguishes debug vs release via `buildTypes`.
- **Directly referencing a `src/debug/` class from `src/main/`**: Rejected — `src/main/` cannot see `src/debug/` classes when compiling the release variant; the build would break.
- **BuildConfig flag + conditional logic in `src/main/`**: Rejected — seeding logic would live in `src/main/`, increasing release APK size and surface area for bugs.

## Consequences

- Positive: Zero runtime cost and zero APK size impact in release.
- Positive: Type-safe — `MnemaApplication` imports `DebugHooks` directly; no reflection.
- Positive: Standard Android practice used by Square, Stripe, and Google teams.
- Trade-off: Two files must be kept in sync if the public API of `DebugHooks` changes.
