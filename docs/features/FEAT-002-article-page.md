---
id: FEAT-002
title: Reading an article
status: in-progress
covers: [FR-001, FR-002, FR-004, FR-030]
slice: articleview
routes: ["GET /articles/{title}"]
tables: [article, tag, article_tag]
code:
  - app/src/main/java/in/ac/iitm/guide/articleview/web/ArticleController.java
  - app/src/main/java/in/ac/iitm/guide/articleview/web/ArticleNotFoundException.java
  - app/src/main/java/in/ac/iitm/guide/articleview/persistence/ArticleReadRepository.java
  - app/src/main/java/in/ac/iitm/guide/wikilink/ArticleAddress.java
  - app/src/main/resources/templates/articleview/Article.html
  - app/src/main/resources/static/js/print.js
  - app/src/main/resources/templates/error/404.html
  - app/src/main/resources/db/migration/V5__add_article_slug.sql
tests:
  - app/src/test/java/in/ac/iitm/guide/articleview/ArticleControllerTest.java
  - app/src/test/java/in/ac/iitm/guide/BrowserPrintTest.java
  - app/src/test/java/in/ac/iitm/guide/wikilink/ArticleAddressTest.java
  - app/src/test/java/in/ac/iitm/guide/shared/persistence/SchemaMigrationTest.java
  - app/src/test/java/in/ac/iitm/guide/TemplateTokensTest.java
  - app/src/test/java/in/ac/iitm/guide/PageQueryCountTest.java
  - app/src/test/java/in/ac/iitm/guide/BrowserLayoutTest.java
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
  body for a client that does not ask for HTML. Since 2 Oct (walkthrough fix 1.7) its text
  names what was asked for, from the path in the error model: an article, a tag, or any other page
  (`NotFoundPageTest`).
- **Checked in the running application**, not only through MockMvc: `/` and the stylesheets and logo
  serve, an unknown address gives the 404 page to a request that accepts HTML, and an address
  containing `%2F` or a broken percent sequence is refused by Tomcat with `400` before it reaches
  the controller.

- **Every other error is the site's page too** (`templates/error.html`, 5 Oct): the frame, one line
  on what happened and the way back, nothing from the exception (security.md). Until then a `500`
  was Tomcat's bare page, seen in the demo rehearsal (`ErrorPageTest`).

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
- [x] Fits every width from 320 to 1920 px, with 16-px text, 44-px targets on a phone and no WCAG 2.2
      AA violation found by axe (NFR-007, NFR-008; `BrowserLayoutTest`, added 27 Sep)

**29 Sep, FR-030 (asked for by the human, option A of three):** "Save as PDF" beside "Propose an
edit" opens the browser's print, and print styles leave out the header, navigation, footer, controls,
Download links and videos, so saving as a PDF gives the article alone. The browser draws the PDF,
which keeps Hindi and Tamil shaping that a server-side PDF library would risk; no dependency was added.
The control is `hidden` until `/js/print.js` runs, so a browser without scripts shows nothing dead.
Tests: the control and its script on the page (`ArticleControllerTest`), and in Chromium
(`BrowserPrintTest`) the click reaching `window.print` and the printed page keeping the title and text
without the site around them; all red before the implementation. Accepted by the human on 29 Sep
after saving an article as a PDF from a browser; on that acceptance FR-030 is `done`.

**Fix 3.2, 2 Oct:** the page's actions wear the site's buttons (FEAT-011, [Buttons.html](../design/screens/Buttons.html)): "Propose an edit" primary, "Save as PDF" secondary, "Remove this article" danger.

**Fix 3.5's layout, 2 Oct** (as [Article.html](../design/screens/Article.html) draws it, confirmed by the human): the text at its 46-rem measure and a 260-px sidebar beside it, the pair centred in the frame; the sidebar holds a card of actions (Propose an edit, Save as PDF, Remove for the moderator) and the What links here card. Below 1024 px one column, the sidebar after the text. No Report card: reporting is not built. `BrowserLayoutTest.on_a_wide_screen_the_article_has_its_sidebar_beside_the_text_as_its_design_screen_draws` and `on_a_phone_the_article_sidebar_follows_the_text` were red first. The rest of 3.5 (summary, date, contents, external links) is its own step.

**Fix 3.5, the rest, 2 Oct** (confirmed by the human): under the title the summary (the glossary's
"Summary" changed to say so) and "Updated …" as Mikhail's fix 1.3 shows times (`<relative-time>`,
ADR-0020); a Contents card in the sidebar for three or more headings (`ArticleController.CONTENTS_FROM`),
its links to ids `commonmark-ext-heading-anchor` puts on the headings, prefixed `section-`
(`WikiLinkRenderer.renderBody`); an external link opens in a new tab with "↗", read as "(opens in a
new tab)". Below 1024 px the actions and the contents come under the title and the backlinks after
the text — F-10's complaint was the actions at the very end. Tags stay above the title, as the
design screen draws them. Red first: `WikiLinkRendererTest` 3, `ArticleControllerTest` 2 and
`BrowserLayoutTest.on_a_phone_the_articles_actions_come_under_its_title_before_the_text`.

**Fix 3.8, 2 Oct** (confirmed by the human, no design screen: the page is made of parts that have one): an article address that answers nothing is rendered by `ArticleController` itself as `error/404.html`, with "Did you mean" and up to five titles from `search`'s `SimilarTitles`; every not-found page offers the search box and "Back to the guide". `NotFoundPageTest`, two tests red first.

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
