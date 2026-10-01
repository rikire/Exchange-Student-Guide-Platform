---
id: FEAT-007
title: Searching the guide
status: done
covers: [FR-007, NFR-002]
slice: search
routes: ["GET /search"]
tables: [article, article_tag, tag]
code:
  - app/src/main/java/in/ac/iitm/guide/search/package-info.java
  - app/src/main/java/in/ac/iitm/guide/search/web/SearchController.java
  - app/src/main/java/in/ac/iitm/guide/search/internal/ArticleSearchService.java
  - app/src/main/java/in/ac/iitm/guide/search/internal/ArticleSearchMapping.java
  - app/src/main/java/in/ac/iitm/guide/search/internal/EnglishAnalysis.java
  - app/src/main/java/in/ac/iitm/guide/search/internal/SearchIndexBuilder.java
  - app/src/main/java/in/ac/iitm/guide/search/internal/SearchResults.java
  - app/src/main/java/in/ac/iitm/guide/search/internal/Passage.java
  - app/src/main/java/in/ac/iitm/guide/search/internal/BodyAsText.java
  - app/src/main/java/in/ac/iitm/guide/wikilink/WikiLinkRenderer.java
  - app/src/main/resources/static/css/site.css
  - app/src/main/resources/templates/search/SearchResults.html
tests:
  - app/src/test/java/in/ac/iitm/guide/search/SearchFlowTest.java
  - app/src/test/java/in/ac/iitm/guide/StartupOrderTest.java
  - app/src/test/java/in/ac/iitm/guide/wikilink/WikiLinkRendererTest.java
---

# FEAT-007 — Searching the guide

## Why

A newly arrived student does not know what the guide calls things; they know the word they were told
at the airport — "FRRO", "hostel", "bank account". Searching is the first step of the mid-demo
scenario, and the landing page's search box already sends its query to `/search`, which nothing
answers yet.

## Scenario

A student types "frro registration" in the landing page's search box (UC-001). The results page lists
the published articles whose title, body or tags contain either word, in any case or word form
("registering" finds "registration"), the fuller matches first. Each result shows the title, linked to
the article, its summary and its tags. The page keeps the query in its own search box so it can be
changed. With nothing matched it says so and points to browsing by tag, as `SearchResultsEmpty.html`
draws it.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /search?q={query}` | Matching published articles, best first | `search/SearchResults.html` |

Codes as in [ui-routes.md](../architecture/ui-routes.md): `200` including zero matches, `400` when
`q` is missing or blank, or holds more than 50 words as the index's analyzer counts them. The `400`
renders the same page saying why.

## Schema impact

None. Reads `article`, `article_tag` and `tag`; the index is Hibernate Search's, not a table.

## Decisions this feature fixed

The contract for this step was confirmed by the human on 28 Sep (prompt journal, 28 Sep), with the
agent's suggested answers.

- **Only the `article` table is indexed.** Submissions are never in the index, so a pending or
  rejected one cannot appear by construction; the two negative criteria are the tests that hold it.
- **Word forms by an English stemming analyzer** built in the slice from Lucene's `analysis-common`
  (already on the classpath through `hibernate-search-backend-lucene`): standard tokenizer,
  lower case, ASCII folding, Porter stemming via Snowball. Hindi and Tamil are NFR-003's own step.
- **A blank query answers `400`**, as `routes.yml` has said since phase 2. The contract's suggested
  answer was an empty page with a hint; the route contract takes precedence, and the `400` page
  carries the hint instead.
- **The engine stays inside the slice** (`search/package-info.java`): the mapping is programmatic,
  written in `search`, so `shared/persistence/Article` carries no Hibernate Search annotation.
- **The index is a Lucene directory on disk on the stand**, in the `guide-index` volume, as ADR-0004
  and the deployment plan decided (human, 28 Sep, after an in-memory index was built and weighed
  against it). **An empty index is filled from the `article` table at start-up**, which covers the
  first start and a lost volume; one that has documents is left alone. Rebuilding after a database
  restore or a change to the mapping is removing the volume and starting again.
- **In development and in tests the index is in memory** (`GUIDE_INDEX_TYPE` unset). Tests: Lucene
  locks a directory to one of the several application contexts a test run starts (human, 28 Sep).
  Development: the H2 database is in memory and emptied at every start, and an index on disk would
  outlive it — the drift the on-disk choice has to avoid (found while building, confirmed by the
  human, 28 Sep).
- **A query of more than 50 words answers `400`** (human, 28 Sep). Review found that a thousand
  distinct words, a valid 4 KB URL, passed Lucene's limit of 1024 clauses and answered `500`. The
  words are counted by the index's own analyzer, so words joined by hyphens are not one word.
- **The search box on the results page is in its main content**, not in the site header: the
  header search of the design screens is on two of them, and moving it into the shared layout is a
  layout change for every page, left out of this step.

## Acceptance criteria

The criteria of FR-007, each a test in `SearchFlowTest`. The second and third replaced "matching
more of the query's words appears higher" on 2 Oct, when the human changed FR-007 to every word
(walkthrough fix 1.5):

- [x] A published article whose title, body or tags contain a word from the query appears in the
      results.
- [x] An article holding only some of the query's words does not appear —
      `only_articles_holding_every_word_of_the_query_appear`; the words may be spread over the title,
      the text and the tags — `the_words_may_be_spread_over_the_title_the_text_and_the_tags`
- [x] A result shows a passage of the text with the words marked, without Markdown —
      `a_result_shows_a_passage_of_the_text_with_the_words_marked`,
      `the_passage_is_plain_text_without_markdown_or_wiki_link_brackets`; the words are marked in
      the title too; HTML in the text is escaped in the passage
- [x] When only a submission not yet approved matches the query, no result appears for it.
- [x] When only a rejected submission matches the query, no result appears for it.

And from the route contract and the decisions above:

- [x] A missing or blank `q` answers `400`.
- [x] The match ignores case and word form ("Registering" finds "registration").
- [x] A removed article (`removed_at` set) does not appear.
- [x] A query of more than 50 words answers `400`; one of 50 is searched.
- [x] An empty index is filled from the database at start-up; one that is not empty is left as it is.
- [x] The seed is imported and indexed before the web server starts, so the first search after a
      fresh start finds every article (walkthrough fix 1.6, 2 Oct) —
      `StartupOrderTest.the_seed_is_imported_and_indexed_before_the_web_server_starts`
- [x] An article the moderator approves is found.
- [x] Markup and query syntax typed into the box find nothing, and the page says that no article
      holds all the words, with a link to the tags —
      `markup_and_query_syntax_typed_into_the_box_find_nothing`,
      `when_no_article_holds_every_word_the_page_says_so_and_links_to_the_tags`

Evidence, 28 Sep: the first thirteen tests of `SearchFlowTest` were red first (`404`, no route),
then green; the tests added after review were red first where they asked for new behaviour. Four negative tests were shown to catch their fault: with `Submission` indexed and searched
too, the pending and the rejected test went red; without the `removedAt` filter, the removed-article
test did; without `tags.name` among the fields, the tag test did.

**Accepted by the human on 28 Sep** after using it in a browser (H2, `seed` profile): searching,
following the results to articles and on through their links. The empty-query prompt in the
browser's own language (`required` on the input) is kept as it is, by the human's decision. Tags on
a result were not links yet: browsing by tag is FR-008, the `taxonomy` step (links since 28 Sep,
FEAT-008). On that acceptance FR-007 is `done`.

**Phase 4 edge cases, 30 Sep** ([edge-cases-and-security.md](../verification/edge-cases-and-security.md)):
`query_syntax_typed_into_the_search_box_is_searched_as_words_not_obeyed` pins that `acc*` finds
nothing and `bank -account` still finds "Bank account", because the query is analysed rather than
parsed. It goes red with `match` swapped for `simpleQueryString`. The bound of 20 results and the
page's query count have no test: [DEBT-021](../tech-debt.md), marked at `ArticleSearchService.LIMIT`.

**Walkthrough fix 1.5, 2 Oct (F-9):** every word is required, by one `must` clause per word as the
analyzer reduces it, searched with `skipAnalysis`; there is no fallback to fewer words, by the
human's decision, since it would bring the junk queries back. The body is indexed as plain text
(`BodyAsText`, through `WikiLinkRenderer.plainText`), so the passage carries no Markdown and a link's
address is not searched. Hibernate Search's plain highlighter marks the words with two control
characters that `Passage` cuts on, and the template prints every piece escaped: no `th:utext`
(security.md). The result follows `docs/design/screens/SearchResults.html`: a list, the "Best match"
badge, the title and the passage marked; the summary when only the title or a tag matched. Seven of
the new tests were red first. The index's fields changed, so an index kept on disk is rebuilt once,
by removing the `guide-index` volume.

**NFR-002, 30 Sep** ([search-latency.md](../verification/search-latency.md)): `scripts/search-latency.sh`
measured 100 searches on 100 generated articles of 500 words on the compose stand. The slowest took
0.023 s and the median 0.009 s, against a limit of 2 s. On that measurement, agreed with the human as
the check, NFR-002 is `done`.

**Fix 1.8, 2 Oct:** the result list's classes are `search-hit*`, since `.result-card` is the forms'
boxed panel and styled the results too; the "Best match" badge is body size on a phone and a tablet,
like the other secondary text (NFR-008), which `BrowserLayoutTest` found once two articles matched.

## Deliberately out of scope

- NFR-002 (search latency) and NFR-003 (Hindi and Tamil queries): their own steps.
- **A query mixing word forms the stemmer keeps apart.** English Porter reduces "registration" to
  `registr` and "registering" to `regist`, so with every word required "FRRO registration" does not
  find an article that only says "registering". Found by `BrowserKeyboardTest` on 2 Oct, whose
  article was given the word, as the seed's FRRO article has it. A lighter stemmer or synonyms
  would be their own decision.
- The one card shared by the landing, search and tag pages: walkthrough fix 3.3.
- Paging: the first 20 results are shown with the total count.
- The search box in the site header on every page.

Checked by hand on the stand, 28 Sep (`docker compose` with PostgreSQL, a separate project): the
first start logged an empty index and "frro" found 4 articles; after `restart` and after `down` and
`up` the log said "holds 20 articles; not rebuilt" and "frro" still found 4.

## Open questions

None open.
