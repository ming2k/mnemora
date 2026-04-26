# ADR-0004: Multiple Debug Sample Packages

- **Status**: Accepted
- **Date**: 2026-04-25
- **Deciders**: Project maintainers

## Context

The project previously seeded a single `sample-package.zip` for debug builds. This had two problems:

1. **Incomplete type coverage**: Only `multiple_choice` and `passage` types were represented, leaving `true_false`, `fill_blank`, `cloze`, and `flashcard` untested in the built-in dataset.
2. **Leaked into release builds**: The package lived in `src/main/assets/`, so release APKs unnecessarily carried test data.
3. **Conflicting import paths**: `HomeViewModel` also tried to import the same file on first launch, causing race conditions and hard-to-debug failures when the asset was renamed or removed.

## Decision

1. **Move all sample packages from `src/main/assets/` to `src/debug/assets/`**. Gradle only merges `src/debug/assets/` for debug builds, guaranteeing zero release APK impact.
2. **Create one comprehensive reference package plus one focused demo package per question type**:
   - `demo-comprehensive.zip`
   - `demo-multiple-choice.zip`
   - `demo-true-false.zip`
   - `demo-fill-blank.zip`
   - `demo-cloze.zip`
   - `demo-flashcard.zip`
   - `demo-passage.zip`
3. **Let `DebugHooks` (debug-only) own the entire seeding flow**:
   - `MnemaApplication.onCreate()` → `DebugHooks.seedIfNeeded()` imports all demo packages if the database is empty.
   - `HomeViewModel` no longer imports built-in packages; it simply loads whatever books exist.
4. **Fix `sub_questions` import**: `DatabaseRepository.importData()` now inserts the parent `QuestionEntity` first, reads its generated `id`, and inserts child questions with `parentId` set. Previously `sub_questions` were silently dropped.

## Consequences

- Positive: Every supported `QuestionType` has a ready-made dataset for manual QA.
- Positive: Release builds contain no test data and no dead seeding code.
- Positive: A single owner (`DebugHooks`) controls debug seeding, eliminating the `HomeViewModel` race.
- Trade-off: Seven small zip files instead of one; total debug-assets size is still small and debug-only.
