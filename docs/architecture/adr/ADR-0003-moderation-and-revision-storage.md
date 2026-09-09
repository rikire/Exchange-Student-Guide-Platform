# ADR-0003 — A submission is its own row, and a revision is a full retained copy

**Status:** accepted
**Decided:** 10 September for the table split, which had not been decided anywhere before; CON-004 and the FR-020 retention gap were settled 7 September
**Recorded:** 10 September — see [README.md](README.md)

## Context

Every new article and every edit passes through moderation before it is visible (FR-010, FR-011,
FR-014 to FR-018). A submission and an article carry the same authored fields — title, summary,
body, tags, an optional media asset — which invites storing them in one place. The moderation state
is what differs, and it differs in ways the reader must never see: FR-001, FR-007 and FR-008 each
carry a negative criterion that an unapproved or rejected submission does not resolve, does not
appear in search, and does not appear under a tag.

FR-020 additionally retains an article's content from before each approved edit, and
[CON-004](../../requirements/constraints.md) rules out rendering a diff — so what is retained is
read whole or not at all.

FR-020 is `should`, and its own roadmap open question was resolved on 7 September by accepting that
articles approved before the retention groundwork lands simply have no history. The storage shape
still has to be decided now, because choosing it later would mean migrating live rows.

## Options

### A. One `article` table with a status column

`status ∈ {pending, approved, rejected}`, and every read filters on it. Fewest tables, and an
approval is one column update.

The cost lands on every query in the application. Each of the three negative criteria above becomes
"and remember the status filter", in a codebase where ten slices each declare their own repository.
One forgotten `WHERE status = 'approved'` publishes unmoderated content — the exact failure the
whole moderation feature exists to prevent, reachable by omission rather than by a wrong decision.

It also fits edits badly: an edit submission is *about* an existing article, so it cannot be a row
in the same table without either overwriting the live article or inventing a parent pointer that
only some rows use.

### B. `submission` separate from `article`

Two tables. `article` holds only published content, so a query against it cannot accidentally return
something unapproved — the safety property is structural rather than remembered. `submission` holds
the proposed content plus `type` (new-article or edit), `target_article_id` for edits, `status`,
`submission_number` and the optional `rejection_reason`.

Approving copies fields across; rejecting sets a status and a reason. The authored fields are
therefore declared twice, in two tables, and adding a field later means adding it to both — which is
exactly what happened to `summary` while this phase's glossary was being audited.

### C. Event-sourced — an append-only log of submissions, with article state projected from it

Full history for free, including FR-020's revisions, and moderation becomes just another event.

It also makes the simplest question in the application — "show me this article" — a projection
concern, needs a rebuild path, and commits a two-person team on a course deadline to a model neither
of us has run before. Spring Modulith's event registry is deliberately switched off in this project
([architecture-rules.md](../../ai/architecture-rules.md)) precisely because nothing here needs
replay.

## Decision

**B** — a separate `submission` table, with `revision` as a third table holding full retained copies.

The deciding factor is that option A's safety property is a filter someone has to remember in every
repository, and option B's is the absence of the rows. Ten slices with their own repositories is too
many places for "remember the status filter" to survive contact with a deadline.

`revision` follows the same reasoning as CON-004: it stores the article's title, summary and body as
they stood before an approved edit, whole. There is no diff to reconstruct and nothing to
recompute — a retained revision is readable on its own, which is also what makes it survivable in
the export (NFR-004).

## Consequences

**Good:** a query against `article` cannot return unmoderated content, so FR-001, FR-007 and
FR-008's negative criteria hold structurally rather than by discipline; the `moderate` slice owns
`submission` without reaching into anyone else's reads; rejected submissions can be kept for the
status lookup (FR-012) without ever being adjacent to published rows.

**Bad:** the authored fields exist in three tables — `article`, `submission`, `revision` — and a new
field has to be added to all three. Approval is a copy, not a state change, so it is a transaction
that has to move the media asset's ownership across as well. Full retained copies cost more storage
than diffs would, which CON-004 accepts as the price of not building a diff view.

**Reversal:** collapsing to option A later means merging three tables and adding the status filter to
every read — cheap in schema terms and expensive in review, because the failure mode of a missed
filter is silent. Going to option C means a rebuild path and is not a migration so much as a
rewrite. Worth reconsidering if a requirement ever appears for an audit trail of who changed what
and when, which no `FR` currently asks for and CON-001 makes largely meaningless anyway.
