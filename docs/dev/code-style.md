# Code Style

## Working Language

The working language for this project is **English**.

All documentation, comments, commit messages, and communications must be written in English. This ensures consistency and accessibility for all contributors.

## Kotlin Style

- Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Use 4-space indentation.
- Prefer explicit types for public APIs; allow type inference for local variables.
- Use trailing commas in multi-line parameter and argument lists.

## Naming

| Element | Convention | Example |
|---|---|---|
| Packages | lowercase, no underscores | `com.hihusky.mnema.data.local` |
| Classes / Interfaces | PascalCase | `BookRepository` |
| Functions / Variables | camelCase | `fetchNodes()` |
| Constants / Enums | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Compose functions | PascalCase | `QuestionCard()` |
| ViewModels | suffix `ViewModel` | `BookDetailViewModel` |

## Architecture Rules

- ViewModels must not reference Android framework classes directly (except `Application`).
- Repository methods should expose `Flow` or `suspend` functions.
- Use `Result<T>` or sealed classes for error propagation, not exceptions for expected failures.

## Imports

- No wildcard imports (`import com.hihusky.mnema.data.*` is prohibited).
- Group imports: Kotlin stdlib, Android/Jetpack, third-party, project-internal.

## Formatting

The project does not currently enforce a formatter in CI. Run Android Studio's **Reformat Code** (`Ctrl+Alt+L`) before submitting.
