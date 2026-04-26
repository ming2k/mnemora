# ADR-0001: Record Architecture Decisions

- **Status**: Accepted
- **Date**: 2026-04-25
- **Deciders**: Project maintainers

## Context

As the Mnemora project grows, we need a lightweight, discoverable way to preserve the reasoning behind significant architectural choices. Without this, future contributors must reverse-engineer intent from code or risk re-litigating settled debates.

## Decision

We will use Architecture Decision Records (ADRs) in `docs/adr/`. Each ADR is a Markdown file numbered sequentially (`0001`, `0002`, ...). Once accepted, an ADR is immutable; to change a decision, a new ADR supersedes the old one and updates its status.

## Alternatives considered

- **Wiki or Google Docs**: Rejected — not version-controlled alongside code; easy to drift out of sync.
- **Comments in source code**: Rejected — too scattered for high-level architectural reasoning.
- **Long-form design docs in `docs/explanation/`**: Rejected — explanation docs can be rewritten; ADRs must be append-only to preserve history.

## Consequences

- Positive: Design rationale is discoverable, version-controlled, and preserved permanently.
- Trade-off: Slight overhead when making architectural changes (must write or update an ADR).
- Negative (accepted): ADRs can become stale if not linked from relevant code and explanation docs.
