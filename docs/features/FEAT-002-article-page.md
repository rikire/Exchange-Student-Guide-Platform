---
id: FEAT-002
title: Reading an article
status: in-progress
covers: [FR-001, FR-002, FR-004]
slice: articleview
routes: ["GET /articles/{title}"]
tables: [article, tag, article_tag]
code:
  - app/src/main/java/in/ac/iitm/guide/articleview/web/ArticleController.java
  - app/src/main/java/in/ac/iitm/guide/articleview/web/ArticleNotFoundException.java
  - app/src/main/java/in/ac/iitm/guide/articleview/persistence/ArticleReadRepository.java
  - app/src/main/java/in/ac/iitm/guide/wikilink/ArticleAddress.java
  - app/src/main/resources/templates/articleview/Article.html
  - app/src/main/resources/templates/error/404.html
  - app/src/main/resources/db/migration/V5__add_article_slug.sql
tests:
  - app/src/test/java/in/ac/iitm/guide/articleview/ArticleControllerTest.java
  - app/src/test/java/in/ac/iitm/guide/wikilink/ArticleAddressTest.java
  - app/src/test/java/in/ac/iitm/guide/shared/persistence/SchemaMigrationTest.java
  - app/src/test/java/in/ac/iitm/guide/TemplateTokensTest.java
  - app/src/test/java/in/ac/iitm/guide/PageQueryCountTest.java
---

# FEAT-002 — Reading an article

## Why

The first page a reader can actually use: open an article, read it, follow its links to the next one.
It is also where FR-002 and FR-004 become visible, and where the article address decision
([02-skeleton.md](../roadmap/02-skeleton.md), open question 9) landed in code.

## Scenario

A reader follows a card on the landing page, or a wiki link in another article, to
`/articles/registering-with-frro`. They see the tags, the title and the body; wiki links inside the
body lead on or show red. An address that names nothing gets the 404 page.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /articles/{title}` | One published article | `articleview/Article.html` |

`{title}` is the slug ([ui-routes.md](../architecture/ui-routes.md)). `200` for a live article; `404`
for no match, a removed article, and a submission that was pending or rejected (those live in
`submission`, which this route never reads).

## Schema impact

`article.slug`, V5: unique, indexed by its constraint, `NOT NULL`. See
[data-model.md](../architecture/data-model.md). Nothing else changed. **Run on H2 only:** the
migration has not been run against PostgreSQL (no `postgres` profile exists yet in either pom, which
[workflow.md](../ai/workflow.md) expects before a stage submission), and its `NOT NULL` without a
default is safe only while `article` is empty, as it is everywhere today.

## Decisions this feature fixed

- **The address in the request is put through the slug rule before the lookup**, so a capitalised or
  differently-punctuated address reaches the same article instead of a 404.
- **Wiki links are resolved with one query per page** (`findLiveSlugs`), whatever the number of links.
- **`spring.jpa.open-in-view` is `false`.** A page can no longer load an association lazily while the
  view renders; the article's tags come with the query (`@EntityGraph`). The tests commit their rows
  and are not transactional, so a forgotten fetch fails a test, not a visitor.
- **`-parameters` is on for the compiler.** Spring reads `@PathVariable` and query parameter names from
  the bytecode, and this project imports Boot's BOM instead of inheriting its parent POM, which is
  what normally switches it on.
- **The article page shows tags, title and body only.** The design sketch also shows "Propose an
  edit", "What links here" and "Report this article"; each belongs to a requirement without a route
  yet (FR-011, FR-006, FR-021) and is left out rather than drawn as a dead control.
- **A 404 is a page in the shared frame for a browser** (`error/404.html`), and Spring's JSON error
  body for a client that does not ask for HTML.
- **Checked in the running application**, not only through MockMvc: `/` and the stylesheets and logo
  serve, an unknown address gives the 404 page to a request that accepts HTML, and an address
  containing `%2F` or a broken percent sequence is refused by Tomcat with `400` before it reaches
  the controller.

## Acceptance criteria

- [x] A published article: `200`, its title, body and tags shown (FR-001)
- [x] A browser asking for an unknown address gets the 404 page in the shared frame, through a real
      server (`NotFoundPageTest`; MockMvc never dispatches to the error page)
- [x] An address that matches no article: `404` (FR-001)
- [x] A removed article: `404` (FR-001)
- [x] A submission not yet approved, and a rejected one: `404` (FR-001)
- [x] The address matches whatever the letter case (FR-002)
- [x] A wiki link matches its title in any letter case, whatever the spacing and punctuation between
      the words, and `[[Title|words]]` shows the words (FR-002)
- [x] A wiki link to no article, or to a removed one, is shown red (FR-004). The tests assert the
      `wikilink-missing` class; that it is red is `site.css`, checked by looking at it, not by a test
- [x] Raw HTML in a body and in a title reaches the page as text (ADR-0001)
- [x] A title in Devanagari is served at its percent-encoded address
- [x] No template holds a literal colour, length or inline style (`TemplateTokensTest`)
- [ ] The article's **media assets** are shown (FR-001, "…and zero or more attached media assets"):
      needs the `media` slice, phase 3

## Deliberately out of scope

- The summary is not shown on the article page (the design does not show it; it appears on cards).
- Media assets (phase 3), backlinks (FR-006), propose-an-edit (FR-011), report (FR-021).
- Public Sans is named in the font stack but not shipped: loading it from a third party on every page
  view is a decision about a request to another site, not made here. The page falls back to the
  system font.

## Open questions

**Resolved 25 Sep, by the human: matching stays by article address**, and FR-002 now says so. A wiki
link matches a title with the same slug, so `[[fees   payments]]` and `[[Fees-Payments]]` reach "Fees &
Payments" (`ArticleControllerTest`). Wikipedia was read for comparison
([Help:Link](https://en.wikipedia.org/wiki/Help:Link)): it treats a link target as case-sensitive
except for the first letter, collapses runs of spaces, keeps punctuation significant, and handles
variants with redirect pages. It was set aside because the guide has a few dozen articles written by
students, where a stray space or full stop should not turn a link red; all 76 links in the seed
articles resolve under every rule, so today the choice changes nothing.

**Two risks of an address made from the title**, both for `moderate`/`contribute` in phase 3, neither
handled yet:

- A **title change** (FR-011 lets an edit change the title) changes the address, so the old address
  answers `404` and `[[Old Title]]` in other articles turns red. Wikipedia leaves a redirect behind; we
  have none. When an edit is approved the slice must recompute `slug`, and whether to keep the old
  address answering is a decision to make then.
- **Titles that differ only in symbols share an address** ("C#", "C++" and "C" are all `c`), so the
  unique constraint rejects the second one; `contribute` must tell the contributor why.

For later: `contribute`/`moderate` must reject a title whose slug is taken
when they publish (data-model.md, `slug`), and FR-010 names only the case-insensitive title check.
