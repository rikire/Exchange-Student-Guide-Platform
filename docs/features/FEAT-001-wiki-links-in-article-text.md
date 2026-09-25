---
id: FEAT-001
title: Wiki links in article text
status: done
covers: [FR-002, FR-004]
slice: wikilink
routes: []
tables: []
code:
  - app/src/main/java/in/ac/iitm/guide/wikilink/WikiLinkRenderer.java
  - app/src/main/java/in/ac/iitm/guide/wikilink/TitleResolver.java
  - app/src/main/java/in/ac/iitm/guide/wikilink/ArticleAddress.java
tests:
  - app/src/test/java/in/ac/iitm/guide/wikilink/WikiLinkRendererTest.java
  - app/src/test/java/in/ac/iitm/guide/ArchitectureRulesTest.java
---

# FEAT-001 — Wiki links in article text

## Why

The 20 seed articles are cross-linked with `[[Title]]`, and a reader following a chain of steps
(register on the portal, then upload documents, then apply for the visa) has to be able to click from
one article to the next. A title with no article behind it has to look different, so a reader can
tell a gap in the guide from a working link (FR-004).

## Scenario

An author writes `See [[Hostel Life]] first` or `[[Applying for Your Student Visa|your student
visa]]`. When the article is displayed, the first shows "Hostel Life" and the second shows "your
student visa"; each is a link if a published article has that title, in any letter case, and a red
word if none does.

## Routes

None of its own. The renderer is a library the article page calls; the page is
`GET /articles/{title}` in [ui-routes.md](../architecture/ui-routes.md), which belongs to a later
feature.

## What it does, and where it stops

`WikiLinkRenderer.render(markdown, resolver)` turns a Markdown body into HTML. It asks the
`TitleResolver` for **all** the titles of the page in one call, and not at all when the body has none,
so the caller can answer with one query (the N+1 rule in [security.md](../ai/security.md)). Deciding
which article a title names, and what its address is, belongs to the caller: the renderer only reads
the answer.

Decisions this feature fixed, each with a test:

- **`[[Title|words]]` is supported** (agreed 25 Sep; added to FR-002 and the glossary). The article is
  matched by `Title`, the reader sees `words`. An empty `words` part shows the title.
- **A link is recognised only inside one run of plain text.** Markdown inside the words
  (`[[Title|some *words*]]`) or a link wrapped over two lines stays as typed. Found by parsing
  examples with commonmark-java 0.30.0 rather than assumed: it cuts the text at any inline markup.
- **Not searched:** code spans and code blocks (so an author can show the syntax), and the text of an
  ordinary Markdown link or an image (a link inside a link is not valid HTML).
- **`[[]]`, `[[   ]]` and `[[|words]]` are not links** and stay as typed.
- **A red link is a `<span class="wikilink wikilink-missing">`, not an anchor.** FR-005 (clicking a
  red link opens the create-an-article page) is `could` and has no route yet; an anchor with nowhere to
  point would be a dead link.
- **The output is safe by configuration, not by filtering.** commonmark-java's `escapeHtml` and
  `sanitizeUrls` are on, so raw HTML in a body is shown as text and a `javascript:` link is emptied.
  Switching either off makes a test go red (checked by switching each off).
- **External links get `rel="nofollow noopener noreferrer"`.** [security.md](../architecture/security.md)
  already required `noopener noreferrer` for an external URL in article text, and the library's own
  output for a Markdown link carried only `nofollow`, so nothing delivered the rule until this
  renderer did. Internal links (`/articles/other`) are left as the library renders them.

## Schema impact

None.

## Acceptance criteria

- [x] A published title renders as a link to that article (FR-002)
- [x] Matching ignores letter case: the renderer passes the title as typed and shows it as typed, and
      the match is the resolver's. Proved through the article page, where `[[hostel LIFE]]` links to
      "Hostel Life" (`ArticleControllerTest`, [FEAT-002](FEAT-002-article-page.md)) (FR-002)
- [x] A title with no article renders red (FR-004)
- [x] `[[Title|words]]` shows the words, links by the title, in both the found and the red case
- [x] One resolver call per page, none when there are no links
- [x] Raw HTML is escaped and a `javascript:` URL is not live (ADR-0001, [security.md](../architecture/security.md))
- [x] An external link carries `noopener noreferrer`, an internal one does not ([security.md](../architecture/security.md))
- [x] `wikilink` imports neither Spring nor JPA (`ArchitectureRulesTest`, demonstrated by adding a
      Spring import and watching it fail)

## Deliberately out of scope

- Extracting the links of an article into `article_link` on publish (ADR-0012, FR-006): phase 3, with
  `contribute` and `moderate`. **When it is built, it stores the `Title` part only**, never the
  words; [ADR-0012](../architecture/adr/ADR-0012-article-link-storage.md) does not yet say so and
  needs a clause when that happens.
- A red link that leads somewhere (FR-005, `could`).
- Backlinks (FR-006, `could`).

## Open questions

The article address this feature depended on was decided 25 Sep and is built in
[FEAT-002](FEAT-002-article-page.md). FR-002's wording for a match by article address was settled
the same day, so nothing is open here.
