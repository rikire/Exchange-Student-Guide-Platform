---
id: FEAT-013
title: Removing an article
status: done
covers: [FR-026]
slice: moderate
routes: ["GET /moderate/articles/{title}/remove", "POST /moderate/articles/{title}/remove"]
tables: [article]
code:
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/ArticleRemoval.java
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/ArticleNotLiveException.java
  - app/src/main/java/in/ac/iitm/guide/moderate/web/ArticleRemovalController.java
  - app/src/main/java/in/ac/iitm/guide/moderate/persistence/ModerateArticleRepository.java
  - app/src/main/resources/templates/moderate/RemoveArticle.html
  - app/src/main/java/in/ac/iitm/guide/articleview/web/ArticleController.java
  - app/src/main/resources/templates/articleview/Article.html
  - app/src/main/java/in/ac/iitm/guide/contribute/internal/ArticleRemovedWhileEditingException.java
  - app/src/main/java/in/ac/iitm/guide/contribute/internal/SubmissionService.java
  - app/src/main/java/in/ac/iitm/guide/contribute/web/SubmissionController.java
tests:
  - app/src/test/java/in/ac/iitm/guide/moderate/ArticleRemovalTest.java
  - app/src/test/java/in/ac/iitm/guide/contribute/SubmissionFlowTest.java
---

# FEAT-013 — Removing an article

## Why

An article that is wrong, outdated or should never have been approved stays on the site until OGE can
take it down. Every public read already left out an article with `removed_at` set; nothing could set it.

## Scenario

The signed-in moderator opens the article and follows "Remove this article". The confirmation names
it and says what removal does; Remove takes it down and returns to the queue, Cancel goes back to the
article. From then on its address answers `404`, it is gone from search, its tags and the landing
page, and wiki links to it render red. Readers never see the link.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /moderate/articles/{title}/remove` | The confirmation | `moderate/RemoveArticle.html` |
| `POST /moderate/articles/{title}/remove` | Set `removed_at`, back to the queue | — |

Codes as in [ui-routes.md](../architecture/ui-routes.md).

## Schema impact

None. Sets `article.removed_at`, which V1 created and every read already filters on; Hibernate Search
re-indexes the article on commit, and the search query already excludes a document with `removedAt`.

## Decisions this feature fixed

Confirmed by the human on 29 Sep with the contract for this step (prompt journal, 29 Sep).

- **Removal is soft:** the row, its revisions and its media stay; there is no restore in the
  application, which is why removal asks first. Restoring is a database edit.
- **The link is on the article page for the signed-in moderator only**, decided by the same
  `MODERATOR` role check `media` uses; no new dependency for it.
- **An edit to an article removed while it was being written answers `409`** with the text kept, so
  the contributor can copy it; the form carries the article's id for this alone. Closes
  [DEBT-009](../tech-debt.md).
- **Approving an edit whose article was removed** was already refused with `409` (FEAT-006).

## Acceptance criteria

- [x] After removal the route no longer resolves, and the article is gone from search and its tags (FR-026)
- [x] A wiki link to a removed article renders as a red link (FR-026)
- [x] A removed pinned article leaves the pinned section (FR-026)
- [x] A removed article's route shows no content (FR-026)

Beyond the criteria, each a test: the confirmation names the article and Cancel returns to it;
removing an already removed article answers `404`; removal takes the moderator back to the queue; the
link is shown to the moderator and not to a reader; a POST without a moderator session is sent to the
login and the article stays; and DEBT-009's `409` in `SubmissionFlowTest`. All but one were red before
the implementation: "without a session" passed from the start, because `/moderate/**` was already
behind the login, and guards that.

**Accepted by the human on 29 Sep** in a browser: the link absent for a reader and present for the
moderator, Cancel, Remove, and the article then answering "not found" and absent from search. On that
acceptance FR-026 is `done`.

## Deliberately out of scope

- Restoring a removed article, or listing removed ones — no requirement asks for it.
- A reason for the removal, or telling the article's contributors — no requirement asks for it.
- The old address of a renamed article (DEBT-010) — a separate debt.
