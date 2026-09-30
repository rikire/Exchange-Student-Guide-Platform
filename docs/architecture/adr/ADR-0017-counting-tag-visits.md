# ADR-0017 — Counting tag visits in a column of `tag`

**Status:** accepted
**Date:** 2026-09-30

## Context

FR-031 orders the landing page's tags by how often their pages are opened. Nothing records that today,
so the schema has to change. The schema froze on 26 Sep (phase 2), and after the freeze it changes by
agreement recorded as an ADR.

The number of articles beside each tag, the other half of FR-031, needs no storage. It is counted from
`article_tag` when the page is read, through `article_tag_tag_id_idx` (V4).

Three constraints bind:

- There are no accounts (CON-001), so a visit cannot be tied to a person. The count is of page openings.
- The landing page is bounded and issues a fixed number of queries (ADR-0010), so the order has to
  come from the same query that lists the tags.
- Export holds published content only (ADR-0007). A counter is not content.

## Options

### A. A `visit_count` column on `tag`

A `BIGINT NOT NULL DEFAULT 0`. Opening a tag's page runs one
`UPDATE tag SET visit_count = visit_count + 1` for the names behind that address. The database
applies each increment, so two readers at the same moment both count, and nothing is read first.
The landing query orders by the column directly.

It cannot tell when a visit happened, so "most visited this month" is out of reach. It is one small
table and one statement per visit.

### B. A `tag_visit` table, one row per visit

This keeps the time of every visit, so a window ("this month") becomes possible later. But the table
grows with every page view for as long as the site runs, and the landing page would count its rows at
every read, or need a summary table that is option A again. Nothing asks for a window.

### C. A counter in memory

No schema change. It is lost at every restart of the container, and the compose stand restarts on
every deploy, so the order would reset to alphabetical. It fails the requirement.

## Decision

**A.** The human agreed to a counter column in `tag` on 30 Sep, as part of FR-031's contract. What
decided it: the order is read on every landing page, and a column is the only option that serves it
from the row already being read, without a table that grows per view.

Every opening of a tag page that answers `200` counts, including a refresh and the moderator's own,
by the human's decision. A `404` counts nothing. The column is not exported (ADR-0007).

Migration: `V7__add_tag_visit_count.sql`.

## Consequences

**Good:**

- One column, one atomic statement per visit.
- The landing page keeps its fixed query count: the tags, their article counts and their order come
  from one grouped query.

**Bad:**

- A `GET` now writes. `GET /tags/{tag}` is no longer read-only, which `routes.yml` says.
- A script or a crawler that opens a tag page repeatedly moves that tag up. There is no rate limit
  until NFR-005.
- The count has no time dimension. An early favourite stays on top until others overtake it.

**Reversal:** dropping the column and ordering by name again is one migration and one query. The
trigger would be the order being gamed, or a need for recency, which would mean option B.
