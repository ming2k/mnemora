# ADR-0005: Library Recency Ordering

- **Status**: Accepted
- **Date**: 2026-04-26
- **Deciders**: Project maintainers

## Context

The Library is the app's primary entry point after importing or resuming study.
Users expect the book they just imported, practiced, reviewed, previewed, or tested to be immediately accessible at the top of the list.

Previously, Library ordering used only `study_sessions.lastActiveTime`. This worked for books with study history, but newly imported books had no session yet and could appear below older content.

## Decision

Library ordering is recency-first:

1. A book with study history is ordered by the newest `study_sessions.lastActiveTime`.
2. A book without study history falls back to `books.updatedAt`, then `books.createdAt`.
3. `sortOrder` and `id` are stable tie-breakers only.

New imports set `createdAt` and `updatedAt` when the `BookEntity` is created, so a freshly imported package appears at the top immediately. Starting or resuming a study flow updates session recency, so recently used books remain at the top.

## Consequences

- Positive: Importing a package makes it immediately visible and ready to use.
- Positive: Recent study activity and recent import activity use the same ordering rule.
- Positive: Search results follow the same recency ordering as the normal Library list.
- Trade-off: The `books` table now stores lightweight timestamps in addition to explicit `sortOrder`.
