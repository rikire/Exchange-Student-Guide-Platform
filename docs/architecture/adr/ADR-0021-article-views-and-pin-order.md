# ADR-0021 — Article views and the pinned order, two columns of `article`

**Status:** accepted (by the human, 1 Oct)
**Date:** 2026-10-01

## Context

FR-033 orders the list of all articles by most viewed, which needs FR-034's view count. FR-025, taken
into phase 3 on 1 Oct, lets the moderator order the pinned articles by hand. Neither is stored
anywhere today: `article.pinned_at` says when an article was pinned and orders the pinned section
newest first. The schema froze on 26 Sep (phase 2); after that it changes by agreement recorded as an
ADR, as V7 did for tag visits (ADR-0017).

## Options

### A. Two columns of `article`, one migration (V8)

`view_count BIGINT DEFAULT 0 NOT NULL` and `pin_position INT NULL`.
- A view is one atomic `UPDATE … SET view_count = view_count + 1`, so two readers at once both count.
  This is ADR-0017's pattern.
- `pin_position` numbers the pinned articles 1, 2, 3. It is null for an unpinned one. The articles
  pinned before V8 are numbered in their current order.
- Moving an article up swaps its number with its neighbour's in one transaction.

### B. A table of view events, and the order kept in `pinned_at`

- A row per view could tell readers apart later. But it grows with every read, and FR-034 asks only
  for a count.
- Ordering by swapping `pinned_at` values between neighbours needs no column. But then `pinned_at`
  no longer says when an article was pinned, and that date is exported (NFR-004).

## Decision

**A.** The one thing that decided it: each column means one thing, and both needs are served with no
table that grows with reads.

What counts as a view, and why:
- **Every `200` of the article page counts, except the signed-in moderator's.** There are no
  accounts (CON-001), so one reader cannot be told from another. Bots and reloads count too; for
  ordering a guide's own list that is good enough, and nothing public shows the number.
- **Both travel in the export and the import**, as `views` and `pin` in front matter (the human,
  1 Oct). ADR-0007 keeps the export to the knowledge base rather than the database, and tag visits
  stayed out on that ground. But an export is also how OGE would move the guide to another host, and
  a move that resets the "most viewed" order and the pinned order loses what the office set up. A
  file without the keys has no views and is not pinned. The seed uses `views` to give a fresh stand an
  order to show.

## Consequences

**Good:**
- FR-033's "most viewed" is a sort over at most a few hundred rows, which needs no index.
- The pinned order is explicit and survives an unpin of another article.
- `pinned_at` keeps its meaning.

**Bad:**
- Every article read becomes a write. It is one row update, and it is only the article page.
- A crawler can lift an article up the "most viewed" order.
- The view count changes with every read, so two exports a day apart differ in the files of every
  article read in between, even with no text changed.
- Tag visits (ADR-0017) still do not travel: a tag has no file of its own.

**Reversal:** drop the two columns in a migration. The list then offers only title and updated
order, and the pinned section goes back to `pinned_at` order. Reconsider the counting if the order
is visibly gamed, by counting once per address a day or by leaving known crawlers out.
