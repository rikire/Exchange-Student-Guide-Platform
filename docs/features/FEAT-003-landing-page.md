---
id: FEAT-003
title: Landing page
status: in-progress
covers: [FR-009]
slice: home
routes: ["GET /"]
tables: [article, tag, article_tag]
code:
  - app/src/main/java/in/ac/iitm/guide/home/web/LandingController.java
  - app/src/main/java/in/ac/iitm/guide/home/internal/LandingPageService.java
  - app/src/main/java/in/ac/iitm/guide/home/internal/LandingPage.java
  - app/src/main/java/in/ac/iitm/guide/home/persistence/LandingReadRepository.java
  - app/src/main/resources/templates/home/Landing.html
  - app/src/main/resources/templates/shared/web/Layout.html
  - app/src/main/resources/static/css/tokens.css
  - app/src/main/resources/static/css/site.css
tests:
  - app/src/test/java/in/ac/iitm/guide/home/LandingControllerTest.java
  - app/src/test/java/in/ac/iitm/guide/TemplateTokensTest.java
---

# FEAT-003 — Landing page

## Why

The front door. A reader who arrives with no link needs to see what the guide holds, and the OGE's
pinned articles (the first things every exchange student must read) must come before whatever was
added last.

## Scenario

A reader opens `/`. They see a search box, the pinned articles, the most recently added ones and the
tags in use. Each article is a card that leads to its page. A guide with no articles yet still opens
and says so.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /` | The landing page | `home/Landing.html` |

Always `200`.

## Schema impact

None. Reads `article`, `tag`, `article_tag` through its own repository; `article_pinned_at_idx`
(V1) serves the pinned list, and the recent list orders by `published_at`, which has no index yet
(recorded in [02-skeleton.md](../roadmap/02-skeleton.md) step 1 for a deliberate pass).

## Decisions this feature fixed

The requirement fixes neither the numbers nor the overlap; these are ours, each with a test.

- **A pinned article is not repeated among the recent ones.**
- **Each of the two lists shows at most 12 cards**, the tag list at most 50 (ADR-0010: every public
  list has a bound the client cannot raise). One constant each, in `LandingPageService`.
- **Pinned articles are ordered by when they were pinned, newest first**; recent ones by when they
  were published, newest first.
- **Tags come from live articles only**: a tag carried only by removed articles is not listed.
- **Tags on the cards load in batches of 50** (`hibernate.default_batch_fetch_size`), not one query per
  card. A collection join fetch was rejected because Hibernate would then apply the page limit in
  memory. The exact query count is asserted by phase 2 step 4, not here.
- **The search box submits to `/search`, which does not exist until phase 3**, so submitting it gives
  a 404 for now. The box is the requirement ("a search entry point"); the route is FR-007.
- **Tags are shown but do not link anywhere yet**: browsing by tag is FR-008, phase 3.
- **The frame every page sits in is `shared/web/Layout.html`**, with the design tokens in
  `static/css/tokens.css`. Bootstrap is not used: the design screens are written on plain CSS with the
  tokens of [reference.md](../design/reference.md), and adding it later is one stylesheet link.

## Acceptance criteria

- [x] With a pinned article: it is shown before the recent ones, a tag list and a search entry point
      are shown (FR-009)
- [x] Without one: the most recently added are shown, with the tag list and the search entry point
      (FR-009)
- [x] Newest first among the recent
- [x] A removed article is not shown
- [x] A guide with no articles opens with `200` and says so
- [x] Each list is bounded
- [x] A card for a Devanagari title links to the percent-encoded address
- [x] Titles and summaries are escaped
- [ ] The number of queries does not grow with the number of articles: **phase 2 step 4**

## Deliberately out of scope

Search (FR-007), tag browsing (FR-008), the moderator's pinning screen (FR-025), the navigation links
in the design's header ("Browse tags", "Submit an article", "Track a submission") — each leads to a
route that does not exist yet.

## Open questions

None open for this feature.
