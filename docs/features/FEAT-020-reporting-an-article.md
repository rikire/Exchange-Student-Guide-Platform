---
id: FEAT-020
title: Reporting an article, and closing the report
status: in-progress
covers: [FR-021, FR-022]
slice: report
routes: ["GET /articles/{title}/report", "POST /articles/{title}/reports", "GET /moderate/reports", "POST /moderate/reports/{id}/close"]
tables: [report, article]
code:
  - app/src/main/java/in/ac/iitm/guide/report/package-info.java
  - app/src/main/java/in/ac/iitm/guide/report/web/ReportController.java
  - app/src/main/java/in/ac/iitm/guide/report/internal/ReportService.java
  - app/src/main/java/in/ac/iitm/guide/report/internal/ReportSettings.java
  - app/src/main/java/in/ac/iitm/guide/report/persistence/ReportRepository.java
  - app/src/main/java/in/ac/iitm/guide/report/persistence/ReportedArticleRepository.java
  - app/src/main/java/in/ac/iitm/guide/shared/web/AddressLimit.java
  - app/src/main/resources/templates/report/ReportForm.html
  - app/src/main/resources/templates/report/ReportInbox.html
  - app/src/main/resources/templates/articleview/Article.html
  - app/src/main/resources/templates/shared/web/Layout.html
tests:
  - app/src/test/java/in/ac/iitm/guide/report/ReportFlowTest.java
---

# FEAT-020 — Reporting an article, and closing the report

## Why

A guide written by students goes out of date: an office moves, a fee changes. The reader who
notices is not always willing to write an edit; a line to OGE is enough for the office to fix it or
ask someone to (FR-021). The office needs the reports in one place and a way to clear them (FR-022).
Built 2 Oct in phase 4, at the human's choice, on the `report` table V1 already had.

## Scenario

On an article, "Report this article" in the sidebar opens a form with one field, "What is wrong?".
Sent, the reader is back on the article, which says "Thanks — OGE will look at it." The moderator's
header has "Reports": the open reports on published articles, newest first, each with the article,
when it was reported and the message, "Open article" and "Close report". A closed report leaves the
list and says "Report closed."

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /articles/{title}/report` | The form | `report/ReportForm.html` |
| `POST /articles/{title}/reports` | Store the report | redirect, or the form on `422`/`429` |
| `GET /moderate/reports` | The inbox | `report/ReportInbox.html` |
| `POST /moderate/reports/{id}/close` | Close one | redirect |

Codes as in [ui-routes.md](../architecture/ui-routes.md).

## Schema impact

None: the `report` table and its `closed_at` index are V1's and V2's.

## Decisions this feature fixed

Confirmed by the human on 2 Oct with the agent's suggested answers:

- **A page of its own** for the form, not a box in the sidebar: simpler on a phone.
- **The message is required, at most 2,000 characters.** Blank or longer answers `422` with the text
  kept, and nothing is stored.
- **Ten reports an hour per client address** (`guide.report.limit`), counted as ADR-0019 counts
  submissions, so a flood cannot bury the inbox. `AddressLimit` and `RetryAfter` moved from
  `contribute` to `shared/web` for it, unchanged.
- **"Open article"** where the screen has "Edit article", until FR-024 lets the moderator edit
  directly.
- **Reports on an article removed since are not shown**; they stay in the table.

## Acceptance criteria

- [x] A reader flags a published article with a message; the article and the message are in the
      inbox — `a_reader_flags_an_article_with_a_message_and_the_moderator_sees_both_in_the_inbox`
- [x] Without a message the report is refused — `a_report_without_a_message_is_refused_and_nothing_is_stored`
- [x] A closed report leaves the inbox — `a_closed_report_leaves_the_inbox`
- [x] Over 2,000 characters is refused; past the limit `429` with `Retry-After`; the inbox and
      closing are the moderator's only; an unpublished article cannot be reported; a removed
      article's report is not listed; the article page links to the form and thanks the reader

Red first, 2 Oct: eight of the ten `ReportFlowTest` tests (no route, no link); the two that passed at
once are the negatives the existing 404 and the moderator's gate already gave.

## Deliberately out of scope

- Notifying OGE by email: the inbox is where the moderator looks, as for the queue.
- A count of open reports in the header.
- The inbox's query count in `PageQueryCountTest`: it runs two queries whatever its size, by
  construction (the reports, then their articles in one `IN`), and has no case of its own yet.

## Open questions

None open.
