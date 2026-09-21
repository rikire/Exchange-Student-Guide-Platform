# ADR-0012 — Storage for article-to-article links

**Status:** accepted
**Date:** 2026-09-21

## Context

[data-model.md](../data-model.md) names `article_link` — FR-006's backlinks and FR-004's red links —
as the one table in the diagram no ADR had decided. FR-004 (red links) is `must`; FR-006 (backlinks)
is `could`. The constraint that actually binds: [ADR-0010](ADR-0010-bounded-reads.md) already forbids
scanning the whole `article` table on a public page, which rules out computing "what links here" by
reading article bodies at request time.

## Options

### A. Store the links, extracted on publish

Parse the body on every publish (new article) and every approved edit, and write one row per
`[[link]]` found into `article_link`. Backlinks and red-link status both become an indexed lookup.
Cost: the table must be kept in step with the body on every write path that changes it — a missed
extraction leaves a stale or wrong link colour, and this is exactly the failure mode MediaWiki's own
`pagelinks` table has to guard against.

### B. Derive at read time, no table

Scan article bodies for `[[links]]` when a page is rendered. Nothing to keep in sync, no possibility
of drift. Cost: this is precisely the unbounded read ADR-0010 already closed off for public pages —
"what links here" would mean scanning every article on every view, and it gets slower as the guide
grows, not stays flat.

## Decision

**Option A — store the links.** Option B is not a live alternative here; it contradicts ADR-0010
outright, so this is closer to the only option than a genuine two-way choice. The one detail fixed
now because it is cheap to fix and expensive to fix later: **target the link by the article's title,
not by a required foreign key to `article.id`.** A `must`-priority red link (FR-004) points at a
title with no row yet — MediaWiki's `pagelinks` table does the same for the same reason: a link
target is a name, and whether that name currently resolves to a published article is a separate
question, answered by a lookup, not by the shape of the foreign key. The exact columns of
`article_link` are still [`.claude/rules/schema.md`](../../../.claude/rules/schema.md)'s call when
the migration for it is written; this ADR settles the strategy and the title-keying, not the full
column list.

## Consequences

**Good:** backlinks and red-link colouring are both a single indexed lookup, not a scan; consistent
with ADR-0010; a red link is representable without inventing a placeholder `article` row.

**Bad:** every write path that changes a body (publish, approved edit) must re-run extraction, and a
skipped or buggy extraction is a silent drift between the body and the link table — nothing catches
it automatically today. `removed_at` (soft delete) raises a question this ADR does not close: whether
a removed article's *outbound* links are cleared or left stale, deferred to the migration itself.

**Reversal:** moving to derive-at-read-time later means dropping the table and rewriting every
backlink/red-link query as a live scan — expensive, and directly reopens the ADR-0010 conflict this
decision exists to avoid. Reconsider only if ADR-0010's own bound is revisited first.
