---
id: FEAT-008
title: Browsing by tag
status: in-progress
covers: [FR-008]
slice: taxonomy
routes: ["GET /tags/{tag}"]
tables: [article, article_tag, tag]
code:
  - app/src/main/java/in/ac/iitm/guide/taxonomy/package-info.java
  - app/src/main/java/in/ac/iitm/guide/taxonomy/TagLink.java
  - app/src/main/java/in/ac/iitm/guide/taxonomy/web/TagController.java
  - app/src/main/java/in/ac/iitm/guide/taxonomy/web/TagNotFoundException.java
  - app/src/main/java/in/ac/iitm/guide/taxonomy/internal/TagBrowseService.java
  - app/src/main/java/in/ac/iitm/guide/taxonomy/internal/TagPage.java
  - app/src/main/java/in/ac/iitm/guide/taxonomy/persistence/TagBrowseRepository.java
  - app/src/main/resources/templates/taxonomy/TagBrowse.html
  - app/src/main/resources/templates/home/Landing.html
  - app/src/main/resources/templates/articleview/Article.html
  - app/src/main/resources/templates/search/SearchResults.html
tests:
  - app/src/test/java/in/ac/iitm/guide/taxonomy/TagBrowseTest.java
---

# FEAT-008 — Browsing by tag

## Why

A student who does not know what the guide calls something finds it by topic instead (UC-002): the
tags on an article, a search result or the landing page are the way in. Until now a tag was a label
that led nowhere.

## Scenario

A reader sees the tag `visa` on an article and follows it to `/tags/visa`. The page names the tag and
lists every published article carrying it, most recently updated first: title linked to the article,
summary and the date it was updated, as `docs/design/screens/TagBrowse.html` draws it.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /tags/{tag}` | The published articles carrying a tag with this address | `taxonomy/TagBrowse.html` |

Codes as in [ui-routes.md](../architecture/ui-routes.md).

## Schema impact

None. Reads `article`, `article_tag` and `tag`. A tag's address is computed, not stored.

## Decisions this feature fixed

Confirmed by the human on 28 Sep (prompt journal, 28 Sep).

- **A tag's address follows the article address rule**: letters, marks and digits in lower case,
  anything else between them one hyphen (`wikilink`'s `ArticleAddress`). Tags are free-form
  (ADR-0005), and a name with `/`, `%`, `;` or `\` cannot sit in a path segment that Tomcat and
  Spring Security's firewall accept; the article rule was already decided and tested. Chosen over an
  unlinked chip, forbidding the characters (against ADR-0005), a query parameter, a relaxed
  firewall and the tag's id.
- **Tags with one address share one page**: `visa/frro` and `visa frro` both open
  `/tags/visa-frro`, and `c++` and `c#` both open `/tags/c`. A tag of only symbols has no address and
  its chip is not a link.
- **`404` when no published article carries a tag with the address**, including a tag only a
  pending or rejected submission carries, so the page does not reveal what the queue holds.
- **The tag names come from the `tag` table**, the small table ADR-0005 accepts scanning, and the
  articles through its unique name and `article_tag_tag_id_idx` (V4), as ADR-0005 and V4 intended;
  the names are matched by address in Java because the address is computed, not stored. A tag in the
  table that no published article carries answers `404` (review, 28 Sep: the first version gathered
  the names through every published article instead).
- **Most recently updated first**, with the date; the first 50 and the total, no paging (ADR-0010's
  bounded page).
- **Every tag chip is a link**: on the landing page, on an article and on a search result.

## Acceptance criteria

The four of FR-008, and the decisions above, each a test in `TagBrowseTest`:

- [x] A published article carrying the tag appears.
- [x] A published article not carrying it does not.
- [x] A tag only a submission not yet approved carries shows no result for it.
- [x] A tag only a rejected submission carries shows no result for it.
- [x] A removed article does not appear.
- [x] The address ignores case and punctuation; tags with one address share the page.
- [x] An address no published article's tag has answers `404`.
- [x] The most recently updated article comes first.
- [x] The tags on the landing page, an article and a search result link to their pages; a tag with
      no address is not a link.

Review, 28 Sep, added two tests, each shown red against its fault: the first 50 with the total
(red with the total taken from the page), and an English date for a browser asking for Russian (red
without `Locale.ENGLISH`). A linked chip is marked by a class, `chip-link`, rather than CSS `:has()`,
which older browsers lack.

Evidence, 28 Sep: eleven tests of `TagBrowseTest` were red first; the other five passed before the
route existed, because an absent route also answers `404` and an unlinked chip was all there was.
Four of those were then shown to catch their fault: with the tag names read from every tag rather
than from published articles, the pending, rejected and removed-only tests went red; with every
chip linked, the symbols-only test did. `BrowserLayoutTest` checks the tag page and the linked chips
at four widths; without the phone rule a chip link measured 23 × 17 px and the check went red.

**Dates are formatted in English** whatever the reader's browser asks for; the guide is written in
English (the human asked on 28 Sep that everything be in English).

## Deliberately out of scope

- Paging past the first 50.
- A list of all tags on its own page: the landing page already lists the tags in use (FR-009).
- A display label for a tag (`FRRO` shown as `frro`): ADR-0005's accepted loss.

## Open questions

None.
