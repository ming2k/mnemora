# ADR-0006: Test session resume limitation

- **Status**: Accepted
- **Date**: 2026-04-26
- **Deciders**: Ming Li

## Context

`TestViewModel` accepts a `sessionId` navigation parameter and reads `currentIndex` from the
stored session to resume a test. However, `loadTest()` always shuffles all answerable questions
from scratch using `.shuffled()`. The index is then applied to a newly-randomised list, not the
original one — so "resuming" places the user at a random question in a different question order
than the original session.

`RecordsScreen` offers a "Resume" button for Test-mode sessions, which exercises this broken path.

## Decision

The `resumeSessionId` parameter and session restoration code have been removed from `TestViewModel`.
Every Test navigation now creates a fresh session. The `Records` screen no longer passes a `sessionId`
when navigating to Test mode. Sessions still appear in history and can be marked complete.

## Alternatives considered

- **Persist question order**: Store the shuffled question IDs in a `session_questions` join table at
  session creation time and reload them on resume. Correct, but requires a new DB table and a
  Room migration.
- **Disable Test resume in RecordsScreen**: Show Test sessions in history but disable the Resume
  button. Honest, but removes a navigation shortcut users may expect.
- **Re-shuffle from a seeded RNG**: Deterministic shuffle using the session ID as the seed. Simpler
  than a join table, but still requires storing the seed and knowing the original question set size.

## Consequences

- Positive: No DB schema change needed in the short term.
- Trade-off: Users who tap "Resume" on a Test session start a fresh random test at the stored index,
  not the original question sequence.
- Negative (accepted): The session resume feature for Test mode is misleading until properly fixed.
