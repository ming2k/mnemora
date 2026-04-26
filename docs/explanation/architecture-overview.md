# Architecture Overview

## Overview

Mnemora follows a layered Android architecture using Jetpack Compose for UI, Hilt for dependency injection, and Room for local persistence. The codebase is organized into three primary layers: **UI**, **Domain**, and **Data**.

## How it works

### UI Layer

- **Screens**: One `Screen` + `ViewModel` per feature (Home, Book Detail, Practice, Review, Test, Settings, Collection Detail).
- **Navigation**: `MnemaNavHost` routes between screens using Compose Navigation.
- **Components**: Reusable composables (`QuestionCard`, `AiChatPanel`, `DopamineProgressBar`, etc.) live in `ui/components/`.

### Domain Layer

- **Services**: Business logic is encapsulated in domain services:
  - `SrsService` — Spaced repetition scheduling
  - `AiService` — AI chat integration
  - `FeedbackService` — Answer evaluation and feedback
  - `SmartCollectionEngine` — Auto-generated study sets
  - `PackageService` — Content packaging / import

### Data Layer

- **Local Database**: Room database (`AppDatabase`) with entities for:
  - `Book`, `Node`, `Question`
  - `UserAnswer`, `SrsReview`
  - `ChatSession`, `ChatHistory`
  - `Collection`, `CollectionItem`
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

## Related decisions

- [ADR-0001: Record architecture decisions](../adr/0001-record-architecture-decisions.md)
- [ADR-0005: Library recency ordering](../adr/0005-library-recency-ordering.md)

## Further reading

- [Jetpack Guide to App Architecture](https://developer.android.com/topic/architecture)
