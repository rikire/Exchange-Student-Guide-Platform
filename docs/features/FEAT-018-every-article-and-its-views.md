---
id: FEAT-018
title: Every article, ordered and narrowed, and its views
status: done
covers: [FR-033, FR-034]
slice: articleview
routes: ["GET /articles", "GET /articles/{title}"]
tables: [article, article_tag, tag]
code:
  - app/src/main/java/in/ac/iitm/guide/articleview/web/ArticleListController.java
  - app/src/main/java/in/ac/iitm/guide/articleview/persistence/ArticleListRepository.java
  - app/src/main/java/in/ac/iitm/guide/articleview/persistence/ArticleViewRepository.java
  - app/src/main/java/in/ac/iitm/guide/articleview/web/ArticleController.java
  - app/src/main/java/in/ac/iitm/guide/shared/persistence/Article.java
  - app/src/main/java/in/ac/iitm/guide/backup/internal/FrontMatter.java
  - app/src/main/java/in/ac/iitm/guide/backup/ArticleArchive.java
  - app/src/main/resources/db/migration/V8__add_article_view_count_and_pin_position.sql
  - app/src/main/resources/templates/articleview/ArticleList.html
  - app/src/main/resources/templates/home/Landing.html
tests:
  - app/src/test/java/in/ac/iitm/guide/articleview/ArticleListTest.java
  - app/src/test/java/in/ac/iitm/guide/articleview/ArticleViewCountTest.java
  - app/src/test/java/in/ac/iitm/guide/backup/ArticleArchiveTest.java
  - app/src/test/java/in/ac/iitm/guide/backup/SeedRunnerTest.java
  - app/src/test/java/in/ac/iitm/guide/PageQueryCountTest.java
---

# FEAT-018 — Every article, ordered and narrowed, and its views

## Why

The walkthrough of 1 Oct (F-7) found the landing page showing the twelve newest articles and the
other 24 reachable only by search or tag, *Registering with FRRO* among them. A reader who does not
know the right word could not see what the guide holds. The human asked for a list of everything,
ordered and narrowed, and for "most viewed" as one of the orders.

## Scenario

1. A reader follows "All articles →" under "Recently added" on the landing page.
2. `/articles` lists every published article by title, 50 to a page, with its summary, tags and
   "Updated", the date in the reader's own zone (ADR-0020).
3. The reader switches to "Recently updated" or "Most viewed", or picks a tag. The address carries the
   choice (`/articles?sort=views&tag=visa`), so it can be sent to someone.
4. Each opening of an article's page by anyone but the signed-in moderator adds one view.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /articles` | The list, `sort`, `tag`, `page` | `articleview/ArticleList.html` |
| `GET /articles/{title}` | Now also counts a view (FR-034) | `articleview/Article.html` |

## Schema impact

V8 ([ADR-0021](../architecture/adr/ADR-0021-article-views-and-pin-order.md)):
- `article.view_count` is added, and with it `pin_position`, which FR-025 uses;
- both travel in the export as `views` and `pin` (the human, 1 Oct), so a move to another host keeps
  them;
- the seed carries made-up `views` for a demonstration (the seed's README says so).

## Decisions this feature fixed

- **50 to a page** (the human, 1 Oct), as on a tag's page (ADR-0010).
- **Orders:** `title` (default, case-insensitive), `updated`, `views`, each with title as tie-break.
- **`400` for an order the list does not offer or a page below 1, and `404` past the last page.**
- **The tag filter takes a tag's address**, as `/tags/{tag}` does, and is a row of links, not a form.
- **The moderator's views are not counted**, and the count is not shown on any page.
- **Views and the pinned place are exported and imported** (the human, 1 Oct, over leaving them out
  as ADR-0017 did for tag visits).
- **A view is counted by `ArticleViewRepository`**, apart from `ArticleReadRepository`, which stays
  read only.

## Acceptance criteria

- [x] 50 articles by title on the first page, the 51st on the second; no submission or removed article.
- [x] Ordered by most viewed, the list follows the counts; by recently updated, the newest first.
- [x] Narrowed to a tag, only its articles; to a tag nobody carries, a note and a link to all.
- [x] A reader's view adds one; the moderator's does not; a `404` counts nothing.
- [x] `views` and `pin` import, export and round-trip; a file without them has none.
- [x] The list's query count does not grow with the number of articles (ADR-0010).

**Fixed 2 Oct:** narrowed to a tag in the default A–Z order, the list answered `500` on the PostgreSQL stand: `select distinct` ordered by `lower(title)` is refused there and accepted by H2. The tag is now matched in an `exists` subquery. `ArticleListTest.narrowed_to_a_tag_only_the_articles_carrying_it_are_listed` was red under `-P postgres` before the change; the whole suite passes on PostgreSQL after it.

## Deliberately out of scope

- Showing the count to readers.
- More than one tag at once.
- Telling crawlers from readers (ADR-0021, "Reversal").
- The card design, which is fix 3.3's.

## Open questions

None.
