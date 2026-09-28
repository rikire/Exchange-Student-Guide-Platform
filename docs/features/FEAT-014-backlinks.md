---
id: FEAT-014
title: What links here
status: done
covers: [FR-006]
slice: backlink
routes: ["GET /articles/{title}"]
tables: [article_link, article]
code:
  - app/src/main/java/in/ac/iitm/guide/backlink/package-info.java
  - app/src/main/java/in/ac/iitm/guide/backlink/ArticleTextChanged.java
  - app/src/main/java/in/ac/iitm/guide/backlink/Backlinks.java
  - app/src/main/java/in/ac/iitm/guide/backlink/internal/LinkIndexer.java
  - app/src/main/java/in/ac/iitm/guide/backlink/internal/LinkBackfill.java
  - app/src/main/java/in/ac/iitm/guide/backlink/persistence/LinkRepository.java
  - app/src/main/java/in/ac/iitm/guide/backlink/persistence/LinkArticleRepository.java
  - app/src/main/java/in/ac/iitm/guide/wikilink/WikiLinkRenderer.java
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/ModerationService.java
  - app/src/main/java/in/ac/iitm/guide/backup/ArticleArchive.java
  - app/src/main/java/in/ac/iitm/guide/articleview/web/ArticleController.java
  - app/src/main/resources/templates/articleview/Article.html
tests:
  - app/src/test/java/in/ac/iitm/guide/backlink/BacklinkFlowTest.java
  - app/src/test/java/in/ac/iitm/guide/backlink/internal/LinkBackfillTest.java
  - app/src/test/java/in/ac/iitm/guide/wikilink/WikiLinkRendererTest.java
---

# FEAT-014 — What links here

## Why

A reader on one article cannot see which other articles lead to it; the guide's articles link to each
other heavily (every seed article carries `[[links]]`), and the reverse direction is how a reader finds
the neighbouring topics.

## Scenario

Under an article, below its text and files, "What links here" lists the published articles whose body
links to it, each a link, by title. An article nobody links to shows no such block. Submissions not yet
approved, rejected ones and removed articles never appear in it.

## Routes

No route of its own: the block is on `GET /articles/{title}`.

## Schema impact

None. `article_link` (V1) is written for the first time, by `backlink` alone; what its columns hold is
in [data-model.md](../architecture/data-model.md), "`article_link`". The read uses
`article_link_target_article_id_idx`.

## Decisions this feature fixed

Confirmed by the human on 29 Sep with the contract for this step (prompt journal, 29 Sep): the design
is [ADR-0016](../architecture/adr/ADR-0016-backlinks-through-an-event-fed-slice.md), chosen over each
writer storing links, the search index, and scanning bodies.

- **`moderate` (approval) and `backup` (import) publish `ArticleTextChanged`**; `backlink` re-reads
  the article's links in the same transaction.
- **An article is not its own backlink**: a link to its own address is not stored.
- **At most 50 articles are listed** (ADR-0010).
- **The block is left out of print** (FR-030): it is navigation, not the article.

## Acceptance criteria

- [x] A published article B linking to published article A appears in A's backlink list (FR-006)
- [x] A link from a submission not yet approved does not appear (FR-006)
- [x] A link from a rejected submission does not appear (FR-006)

Beyond the criteria, each a test: a removed article's link does not appear; an approved new article
with a link appears; an approved edit that removes the link takes its article off; a link written
before its target existed appears once the target is published; a renamed article keeps no backlinks
that named its old title; no block when nobody links; the start-up fill of an empty table, and a table
with rows left alone; and, in `WikiLinkRendererTest`, the linked titles of a body and a code span not
counting. Six of the thirteen were red before the implementation. The other seven assert an absence,
which an empty table satisfies: five were shown red under a mutation of the guard each covers (the
removed-source filter, the delete before re-reading, the unhooking on rename, the empty-block check, the
start-up fill's count), and the pending and rejected ones hold by construction — only published
articles are ever read for links, so there is no guard in the code to remove.

**Accepted by the human on 29 Sep** in a browser: "Registering with FRRO" listing the four seed
articles that link to it, filled at start-up from the existing table. On that acceptance FR-006 is
`done`.

## Deliberately out of scope

- Backlinks to a submission, or from one, for the moderator.
- Red links read from `article_link` instead of the body (FR-004 stays as built).
