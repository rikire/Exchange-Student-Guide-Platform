# Phase 1, Stage 1 — session notes, 5 September 2026

Temporary handoff note, not part of the permanent roadmap structure — written so the session can
resume tomorrow without re-deriving context. Delete once Stage 1 is fully closed out, or fold its
still-open item into [01-requirements-design.md](01-requirements-design.md) directly.

## What was done today

- Phase 0 closed — see [00-init.md](00-init.md).
- Ran Stage 1 of phase 1 end to end: actors → use cases → features → slices → MoSCoW priority.
  - **Actors confirmed, unchanged:** Reader, Contributor, OGE Moderator.
  - **22 use cases** written across the three actors into
    [docs/cjm/reader.md](../cjm/reader.md), [contributor.md](../cjm/contributor.md),
    [moderator.md](../cjm/moderator.md) (`UC-001`…`UC-022`).
  - **23 features** derived from those use cases (22 use-case-backed + rate limiting/honeypot,
    which protects submission but isn't itself a use case) — full coverage, no orphan use case.
  - **Slice list revisited** against the real features, not treated as fixed: added a tenth slice,
    `report` (flagging an article + resolving the flag), in
    [architecture-rules.md](../ai/architecture-rules.md). The original nine otherwise stand;
    submission-status lookup and the moderator's direct-publish/edit path were folded into
    `contribute`.
  - **MoSCoW pass, all 23 features:** 11 must, 6 should, 6 could, 0 won't — recorded in the
    Feature coverage tracker's `Priority` column in
    [01-requirements-design.md](01-requirements-design.md). All 11 `must` features already trace to
    a step already named in [02-skeleton.md](02-skeleton.md) /
    [03-main-flow.md](03-main-flow.md) — neither file needed an edit.
  - Resolved the roadmap's pre-existing open question about homepage-pin curation: moderator-curated,
    via direct homepage editing (`UC-022`).

## Still open — decide this before (or at the start of) the next session

**Version-history groundwork timing conflict** — [01-requirements-design.md](01-requirements-design.md),
open question 1. It is `should` priority (the system still functions without it) but carries a
`must-start-in-phase-2` urgency (the data cannot be reconstructed later if delayed). Nobody has
picked an answer: build the retention groundwork in phase 2 regardless of its `should` label, or
accept that any article approved before phase 4 has no history.

## Next step once that's answered

Move to writing full `FR` text, **one feature at a time** (explicitly requested — not batched), into
[docs/requirements/functional.md](../requirements/functional.md). Agreed starting point before this
session paused: **"Read a published article"** (`UC-003`, `articleview`, must).

## Where everything lives

| What | File |
|---|---|
| Actors and use cases | `docs/cjm/reader.md`, `contributor.md`, `moderator.md` |
| Slices (ten, `report` added 5 Sep) | `docs/ai/architecture-rules.md` |
| Feature list, priority, slice assignment | `docs/roadmap/01-requirements-design.md` — Feature coverage tracker |
| Phase 0 closure record | `docs/roadmap/00-init.md` |
