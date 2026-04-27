# Architecture Overview

## Overview

Mnemora follows a layered Android architecture using Jetpack Compose for UI, Hilt for dependency injection, and Room for local persistence. The codebase is organized into three primary layers: **UI**, **Domain**, and **Data**.

## How it works

### UI Layer

- **Screens**: One `Screen` + `ViewModel` per feature (Home, Book Detail, Practice, Review, Test, Settings, Collection Detail).
- **Root ViewModel**: `MainViewModel` (injected into `MainActivity`) observes theme mode as a `StateFlow` so the app theme responds to preference changes without restarting.
- **Navigation**: `MnemoraNavHost` routes between screens using Compose Navigation. The bottom bar shows only for the two root destinations (Library and Settings); `MnemoraBottomNavigation` hides itself on all sub-screens.
- **Components**: Reusable composables live in `ui/components/`:
  - `MnemoraAlertDialog` — Cupertino-style confirm/alert dialog used for destructive actions.
  - `ConfettiOverlay` — canvas-based particle animation for positive reinforcement in Practice mode.
  - `AiChatPanel`, `DopamineProgressBar`, `QuestionContent`, `MnemoraCard`, and others.

### Domain Layer

- **Services**: Business logic is encapsulated in domain services:
  - `SrsService` — Spaced repetition scheduling (SM-2 variant).
  - `AiService` — AI chat integration. Config is a `MutableStateFlow<AiConfig>` updated synchronously by `SettingsViewModel` via `syncAiConfig()` whenever any AI setting changes; no restart required.
  - `FeedbackService` — Answer feedback (sound + haptics), streak tracking.
  - `CollectionManager` — Collection CRUD facade.
  - `PackageService` — ZIP extraction and import orchestration.
  - `BookImporter` — Transactional DB insertion during import (owns the `importData` pipeline).

### Data Layer

- **Local Database**: Room database (`AppDatabase`) with entities for:
  - `Book`, `Node`, `Question`
  - `UserAnswer`, `SrsReview`
  - `ChatSession`, `ChatHistory`
  - `Collection`, `CollectionItem`
  - `StudySession`
- **Repositories**: `DatabaseRepository` and `SettingsRepository` abstract data access for ViewModels.
- **Preferences**: DataStore holds user settings.

### Library Ordering

The Library is ordered by recency so the next likely action is near the top. Books with study history use the newest `study_sessions.lastActiveTime`; books without study history fall back to their `books.updatedAt` / `books.createdAt` timestamps, which makes freshly imported packages appear first. `sortOrder` remains only a tie-breaker, not the primary Library ordering signal.

### Dependency Injection

Hilt modules (`AppModule`, `DatabaseModule`) provide singleton and scoped dependencies. Every screen ViewModel is injected with its required repositories and services.

## Why we designed it this way

- **Compose + ViewModel**: Unidirectional data flow with clear state ownership. ViewModels survive configuration changes and expose UI state as `StateFlow`.
- **Room for local-first**: The app is intended to work offline. Room provides compile-time SQL verification and coroutine-friendly APIs.
- **Domain services over UseCases**: For a medium-sized app, full UseCase classes add boilerplate without clear benefit. Services group related operations and keep ViewModels thin.
- **Single Activity**: Compose Navigation with a single `MainActivity` reduces manifest complexity and enables deep-linking later.

## Known architectural limitations

- **`DatabaseRepository` is large**: At ~600 lines it still mixes DAO delegation for 10 tables, entity-to-domain conversion, and collection/session logic. The import pipeline has been extracted to `BookImporter`, but further domain splits (e.g., separate session and SRS repositories) remain as future targets.

- **Test mode resume is broken by design**: `TestViewModel` accepts a `sessionId` nav parameter to resume a test, but `loadTest()` always re-shuffles the question list. Restoring `currentIndex` against a different list is meaningless. The session resume entry point for Test mode in `RecordsScreen` is misleading — see [ADR-0006](../adr/0006-test-session-resume-limitation.md).

## Related decisions

- [ADR-0001: Record architecture decisions](../adr/0001-record-architecture-decisions.md)
- [ADR-0005: Library recency ordering](../adr/0005-library-recency-ordering.md)
- [ADR-0006: Test session resume limitation](../adr/0006-test-session-resume-limitation.md)

## Further reading

- [Database Design](database-design.md)
- [Jetpack Guide to App Architecture](https://developer.android.com/topic/architecture)
