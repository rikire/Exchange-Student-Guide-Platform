---
id: FEAT-021
title: The moderator publishes and edits directly
status: done
covers: [FR-023, FR-024]
slice: moderate
routes: ["GET /moderate/write", "POST /moderate/articles", "GET /moderate/articles/{title}/edit", "POST /moderate/articles/{title}/edits"]
tables: [submission, article, revision, media_asset]
code:
  - app/src/main/java/in/ac/iitm/guide/moderate/web/DirectPublishingController.java
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/DirectPublishing.java
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/ModerationService.java
  - app/src/main/java/in/ac/iitm/guide/contribute/Submissions.java
  - app/src/main/java/in/ac/iitm/guide/contribute/Draft.java
  - app/src/main/java/in/ac/iitm/guide/contribute/SubmissionRejectedException.java
  - app/src/main/resources/templates/moderate/PublishForm.html
  - app/src/main/resources/templates/articleview/Article.html
  - app/src/main/resources/templates/report/ReportInbox.html
  - app/src/main/resources/templates/shared/web/Layout.html
tests:
  - app/src/test/java/in/ac/iitm/guide/moderate/DirectPublishingTest.java
---

# FEAT-021 — The moderator publishes and edits directly

## Why

OGE writes some of the guide itself — a new rule, a changed fee — and fixes what readers report.
Sending its own text through its own queue is a step with nobody to review it (FR-023, FR-024).
Built 2 Oct in phase 4, at the human's choice, after reports (FEAT-020), whose inbox now leads to
the edit.

## Scenario

The moderator's header has "Write an article": the contributor's form, with "Publish now" in place
of "Submit for review". Sent, the moderator is on the new article, which says "Published." On any
article the signed-in moderator sees "Edit now" in place of "Propose an edit"; in the report inbox
each report has "Edit article". The edit is live at once, and the text it replaced is a revision.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /moderate/write` | The form for a new article | `moderate/PublishForm.html` |
| `POST /moderate/articles` | Publish it | redirect, or the form on `409`/`422` |
| `GET /moderate/articles/{title}/edit` | The form, filled with the article | `moderate/PublishForm.html` |
| `POST /moderate/articles/{title}/edits` | Publish the edit | redirect, or the form on `409`/`422` |

Codes as in [ui-routes.md](../architecture/ui-routes.md).

## Schema impact

None. A direct publication is a `submission` row, approved, whose `decided_at` equals its
`submitted_at`; the schema has no author column, and a new one would need a migration after the
freeze, so the two equal times are how the history tells it from a queued one (human, 2 Oct).

## Decisions this feature fixed

Confirmed by the human on 2 Oct:

- **Through a submission, approved at once.** `moderate` submits the draft through `contribute`'s
  published `Submissions`, then approves it in the same transaction. The title, summary, body, tag
  and attachment checks are the contributor's, not a copy; the revision, the media move, the
  backlinks and the search index follow the ordinary approval. A refusal rolls everything back,
  so nothing is left pending in the queue.
- **`moderate` depends on `contribute`**, a new arrow on the slice diagram, rather than `moderate`
  repeating the checks.
- **`409` for a title another article has**, case-insensitively; `422` for everything else the form
  refuses, the attachment's size and type included.

## Acceptance criteria

- [x] A new article is published at once, without entering the queue —
      `a_new_article_written_by_the_moderator_is_live_at_once_and_never_pending`
- [x] A title matching an existing one, case-insensitively, is refused —
      `a_title_matching_an_existing_articles_case_insensitively_is_refused`
- [x] An attachment over its limit is refused with a message (new, edit) —
      `a_new_article_with_an_attachment_over_its_limit_is_refused_with_a_message`,
      `an_edit_with_an_attachment_over_its_limit_is_refused_with_a_message`
- [x] An attachment not of an accepted type is refused with a message (new, edit) —
      `a_new_article_with_an_attachment_not_of_an_accepted_type_is_refused_with_a_message`,
      `an_edit_with_an_attachment_not_of_an_accepted_type_is_refused_with_a_message`
- [x] An edit is published at once and the replaced text is kept as a revision —
      `an_edit_by_the_moderator_is_live_at_once_and_the_text_it_replaces_is_kept_as_a_revision`
- [x] A new title matching a different article's is refused —
      `a_new_title_matching_a_different_articles_case_insensitively_is_refused`
- [x] The history keeps it as approved at the moment it was sent; "Edit now", "Write an article" and
      the inbox's "Edit article" lead to the forms; every new route sends a visitor without the
      moderator's session to the login (`ModeratorLoginTest`, from `routes.yml`)

Red first, 2 Oct: all twelve `DirectPublishingTest` tests (no route, no link).

## Deliberately out of scope

- A file over the container's multipart ceiling (larger than the video limit) is answered by
  `contribute`'s `UploadTooLargeAdvice` with the contributor's empty form, as on the contributor's
  routes; under the ceiling, the size check is the one FR-023 and FR-024 name, and it answers here.
- A preview button on the moderator's form; the editor's own preview is there.

## Open questions

None open.
