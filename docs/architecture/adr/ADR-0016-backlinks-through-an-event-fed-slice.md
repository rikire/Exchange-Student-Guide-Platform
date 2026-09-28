# ADR-0016 — Backlinks through an event-fed slice

**Status:** accepted
**Date:** 2026-09-29

## Context

FR-006 lists, under an article, the published articles that link to it. ADR-0012 decided to store the
links in `article_link` at write time and warned of the one way that goes wrong: a path that changes a
body without re-extracting its links, and the table drifting from the text. Two paths write a body
today — `moderate` on approval, `backup` on import — and phase 4 adds a third (FR-023, FR-024).
DEBT-006 is that drift already: the importer has written 30 articles and no links.

`architecture-rules.md` gave backlinks to `wikilink`, but `wikilink` must stay plain Java with no
Spring or JPA (`ArchitectureRulesTest`), so it can parse links and cannot store them.

## Options

Put to the human on 29 Sep:

### A. Each writer stores the links

`moderate` and `backup` extract and write rows through repositories of their own. The pattern the
project already uses for `article`. Every new write path has to remember to do it, which is the drift
ADR-0012 names.

### B. A `backlink` slice fed by an application event

Writers publish `ArticleTextChanged(articleId)`; `backlink` owns `article_link`, re-extracts on the
event and answers "what links here". A Spring application event is one of the two channels between
slices that `architecture-rules.md` allows. A new writer publishes the event and extracts nothing.

### C. Ask the search index

No table: find the articles whose indexed body contains the title. The English analyzer drops the
brackets, so a title mentioned in prose would count as a link; it also reverses ADR-0012.

### D. Scan the bodies on every page view

The unbounded read ADR-0010 rules out.

## Decision

**B, chosen by the human**, because one slice owning the table is what keeps it in step with the text
as write paths are added.

- **The listener runs synchronously, in the writer's transaction** (`@EventListener`): the links
  commit with the article or not at all. The event registry stays off, as `architecture-rules.md`
  says; nothing here needs delivery after a restart.
- **Links are found by `WikiLinkRenderer`'s own parser** (`linkedTitles`), so a `[[link]]` in a code
  span is not a backlink, exactly as it is not a link on the page.
- **`target_title` holds the linked title's address** (`ArticleAddress.slugOf`), not the text as
  typed, so `[[hostel life]]` and `[[Hostel Life]]` are one target. No schema change: the column
  keeps its name and length (the schema is frozen), and the address is what the page resolves by.
- **`target_article_id` is resolved when a link is written, and re-pointed when its target changes:**
  publishing or renaming article X points every row whose `target_title` is X's address at X, and
  clears X from rows that named its old address. "What links here" then reads by
  `target_article_id` on its existing index, joined to live sources, at most 50 (ADR-0010).
- **A removed source keeps its rows** and is left out when read; this closes ADR-0012's open question
  for the only reader there is.
- **An empty table with articles present is filled once at start-up**, as the search index is — the
  30 seed articles of DEBT-006, and any database imported before this slice.

## Consequences

**Good:** one place writes `article_link`; a new write path costs one event. DEBT-006 closes, and a
future red-link or "what links here" query reads the table instead of scanning.

**Bad:** a writer that forgets the event still drifts — the risk is smaller, not gone, and a test per
writer covers it. Re-pointing on publish is an update by `target_title`, which has no index of its
own; it runs on the write path, once per approval, not on a page. Links to an address nobody has
published stay unresolved rows until someone does.

**Reversal:** `backlink` is the only reader and writer of `article_link`; dropping the slice means
deleting it and the two `publishEvent` calls. Reconsider if moderators need backlinks to unpublished
submissions, which would change what a link's source is.
