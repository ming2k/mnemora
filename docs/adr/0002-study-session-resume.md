# ADR-0002: Study Session and Resume

- **Status**: Accepted
- **Date**: 2026-04-25
- **Deciders**: Project maintainers

## Context

The app supports four study modes (Practice, Review, Test, Preview). Previously, each mode was stateless: leaving the screen and coming back always restarted from the beginning. Users asked for:

1. A **Resume** button that continues from where they left off.
2. A **Records** list showing past attempts.
3. The ability to start a **new** session without losing the old one (for modes that make sense).

This required a unified way to persist session state across modes.

## Decision

Introduce a `StudySessionEntity` table and a `StudySessionDao`.

### Schema

| Column | Type | Notes |
|---|---|---|
| `id` | Long (PK, auto) | Unique session identifier |
| `bookId` | Int (FK) | Which book this session belongs to |
| `mode` | String | Practice / Review / Preview / Test |
| `startTime` | Long | When the session began |
| `lastActiveTime` | Long | When progress was last saved |
| `currentIndex` | Int | Question position |
| `totalQuestions` | Int | Total questions in the session |
| `isCompleted` | Boolean | True when the user finishes |
| `isActive` | Boolean | True while the session is in progress |
| `answersJson` | String? | Optional serialized answer state |
| `collectionId` | Int? | Optional collection filter |
| `sectionId` | String? | Optional section filter |

### Instance Rules

| Mode | Instance Rule | Resume Behaviour |
|---|---|---|
| **Practice** | One active session per book | Resume the unique active session |
| **Review** | One active session per book | Resume the unique active session |
| **Preview** | One active session per book | Resume the unique active session |
| **Test** | Multiple sessions allowed | Resume by `sessionId` passed in navigation |

### Navigation Changes

- `test/{bookId}?sessionId={sessionId}` — optional session ID for resuming a specific test.
- `preview/{bookId}?mode=Preview` — mode parameter so Preview sessions are isolated from Practice sessions despite sharing the same screen.

### UI Changes

- **BookCard** bottom action:
  - Has active session → `[Resume <Mode>]` + `[New]`
  - No active session → `[Start]` (opens BottomSheet)
- **MoreVert menu** → added **Records** entry, opens BottomSheet listing all historical sessions.
- **Mode BottomSheet** order: Practice → Review → Test → Preview.

## Alternatives considered

- **Save progress inside each mode's existing tables (e.g., `user_answers`)**: Rejected — `user_answers` tracks correctness history, not session position. Mixing the two would make it impossible to have multiple concurrent Test attempts.
- **Save progress in `SharedPreferences` per book+mode**: Rejected — not queryable, cannot support a Records list, not type-safe, hard to migrate.
- **Separate table per mode**: Rejected — four nearly identical schemas with duplicated DAO boilerplate. A single `StudySessionEntity` with a `mode` column is simpler and queryable.

## Consequences

- Positive: Users can resume interrupted study and browse history.
- Positive: Test mode supports multiple parallel attempts.
- Trade-off: Each ViewModel must now load and update a session row on lifecycle events.
- Trade-off: Navigation routes grew optional query parameters.
- Negative (accepted): Database version incremented from 7 → 8; existing installs migrate automatically via Room's destructive fallback (acceptable during pre-release phase).
