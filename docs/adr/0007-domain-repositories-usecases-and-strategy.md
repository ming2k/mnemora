# ADR-0007: Domain Repositories, UseCases, and AI Strategy

- **Status**: Accepted
- **Date**: 2026-05-12
- **Deciders**: Engineering Team

## Context

The application was suffering from "God Objects" which limited scalability and testability:
1. `DatabaseRepository` handled all database operations (Books, Questions, Chats, Sessions, etc.), growing to ~600 lines.
2. `PracticeViewModel` was overloaded with UI state management, answering logic, AI chat streaming, and Collection management.
3. `AiService` coupled multiple AI provider implementations into a single class, violating the Open/Closed Principle.

## Decision

We have implemented a comprehensive architectural refactoring to align with Clean Architecture and SOLID principles:

1. **Repository Splitting**: `DatabaseRepository` has been split into domain-specific repositories (`BookRepository`, `QuestionRepository`, `SessionRepository`, `ChatRepository`, `CollectionRepository`, `NodeRepository`, `UserAnswerRepository`, `SrsRepository`).
2. **UseCase Layer**: A new `domain/usecase/` layer has been introduced. Complex business logic previously residing in ViewModels (especially `PracticeViewModel`) is now encapsulated in specific UseCases (e.g., `LoadPracticeSessionUseCase`, `SubmitAnswerUseCase`, `AiChatUseCase`).
3. **Strategy Pattern for AI**: `AiService` has been refactored to use the Strategy pattern. Concrete implementations for AI providers (Gemini, VertexAI, Kimi, DeepSeek, Anthropic) now implement the `AiProvider` interface and reside in `domain/service/ai/`.

## Alternatives considered

- **Facade Pattern over a single Repository**: We considered keeping a unified `DatabaseRepository` interface that delegates to smaller internal DAOs. *Why rejected*: This would still leave a massive interface and wouldn't solve the dependency injection bloat in ViewModels that only need access to a specific domain (like Collections).
- **Keeping Logic in ViewModels**: We considered just breaking ViewModels into smaller ones without UseCases. *Why rejected*: Business logic like SRS scheduling and scoring is domain logic, not UI logic, and belongs in the domain layer for reusability and isolated testing.

## Consequences

- Positive: Code is highly decoupled, making unit testing significantly easier. The architecture now strictly enforces the Single Responsibility Principle.
- Positive: Adding new AI providers simply requires adding a new `AiProvider` implementation without modifying existing provider logic.
- Trade-off: There is an increase in the number of files and boilerplate (e.g., Dagger/Hilt constructor injections for multiple UseCases).
- Negative (accepted): Developers must now navigate multiple smaller files rather than a single repository, which requires understanding the domain boundaries.
