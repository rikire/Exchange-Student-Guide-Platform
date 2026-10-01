---
id: FEAT-006
title: Moderating a submission
status: done
covers: [FR-014, FR-015, FR-017, FR-018, FR-019, FR-020, FR-029]
slice: moderate
routes: ["GET /moderate/login", "POST /moderate/login", "GET /moderate/queue", "GET /moderate/submissions/{number}", "POST /moderate/submissions/{number}/approve", "POST /moderate/submissions/{number}/reject"]
tables: [submission, submission_tag, article, article_tag, tag, revision]
code:
  - app/src/main/java/in/ac/iitm/guide/moderate/package-info.java
  - app/src/main/java/in/ac/iitm/guide/moderate/web/ModerationController.java
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/ModerationService.java
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/SubmissionNotFoundException.java
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/AlreadyDecidedException.java
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/ApprovalRefusedException.java
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/ApprovalConflictException.java
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/RejectionRefusedException.java
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/TextDiff.java
  - app/src/main/java/in/ac/iitm/guide/shared/web/DisplayTime.java
  - app/src/main/resources/templates/shared/web/LocalTime.html
  - app/src/main/java/in/ac/iitm/guide/moderate/persistence/ModerateSubmissionRepository.java
  - app/src/main/java/in/ac/iitm/guide/moderate/persistence/ModerateArticleRepository.java
  - app/src/main/java/in/ac/iitm/guide/moderate/persistence/RevisionRepository.java
  - app/src/main/java/in/ac/iitm/guide/shared/security/WebSecurity.java
  - app/src/main/java/in/ac/iitm/guide/shared/security/ModeratorLoginController.java
  - app/src/main/resources/templates/moderate/ModerationQueue.html
  - app/src/main/resources/templates/moderate/SubmissionReview.html
  - app/src/main/resources/templates/shared/security/AdminLogin.html
tests:
  - app/src/test/java/in/ac/iitm/guide/moderate/ModerationFlowTest.java
  - app/src/test/java/in/ac/iitm/guide/moderate/internal/TextDiffTest.java
  - app/src/test/java/in/ac/iitm/guide/shared/web/DisplayTimeTest.java
  - app/src/test/java/in/ac/iitm/guide/BrowserLocalTimeTest.java
  - app/src/test/java/in/ac/iitm/guide/shared/security/ModeratorLoginTest.java
  - app/src/test/java/in/ac/iitm/guide/moderate/persistence/ModerateArticleRepositoryTest.java
  - app/src/test/java/in/ac/iitm/guide/shared/security/ModeratorLoginWithoutHashTest.java
  - app/src/test/java/in/ac/iitm/guide/shared/security/SessionCookieTest.java
---

# FEAT-006 — Moderating a submission

## Why

Every article and every edit waits in the queue until someone at OGE looks at it (ADR-0003). Without
this feature nothing a contributor writes ever reaches a reader, and the second half of the mid-demo
scenario — the moderator approves the edit and it goes live — does not exist.

## Scenario

The OGE moderator opens `/moderate/queue`, is sent to the login page, types the office's shared
password and lands on the queue: every pending submission, oldest first. They open one and read its
full text as it would be published — or, for an edit, first what it changes, the published text on
the left and the proposed on the right (FR-029). They adjust the summary and the tags if needed and approve it: a
new article goes live at its address, an edit replaces the article's text and the text it replaced is
kept as a revision. Or they reject it, with a reason if they give one, and it leaves the queue. Either way they are back at the queue.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /moderate/login`, `POST /moderate/login` | The shared-password login (ADR-0009) | `shared/security/AdminLogin.html` |
| `GET /moderate/queue` | Pending submissions, oldest first | `moderate/ModerationQueue.html` |
| `GET /moderate/submissions/{number}` | One submission in full, or "no longer pending" | `moderate/SubmissionReview.html` |
| `POST /moderate/submissions/{number}/approve` | Publish, or apply the edit | re-renders the review on `409` |
| `POST /moderate/submissions/{number}/reject` | Mark rejected, storing the reason if given | re-renders the review on `409` and `422` |

Codes as in [ui-routes.md](../architecture/ui-routes.md).

## Schema impact

None. Reads and updates `submission`; inserts or updates `article` and `article_tag`; inserts
`revision`. The queue reads `WHERE status = 'PENDING' ORDER BY submitted_at` on the existing
`submission_status_submitted_at_idx`.

## Decisions this feature fixed

Confirmed by the human on 28 Sep with the contract for this step (prompt journal, 28 Sep).

- **An edit that changes the title changes the address**, and the old address answers `404`; a
  redirect from it is deferred ([DEBT-010](../tech-debt.md)). This closes the open decision in
  [data-model.md](../architecture/data-model.md), `slug`.
- **Failed logins are logged at `WARN`, not yet rate limited.** ADR-0009's rate limit waits for
  NFR-005 ([DEBT-011](../tech-debt.md)).
- **A title that became taken while the submission waited refuses the approval** with `409` and a
  message on the review page; the submission stays pending, and the moderator can reject it.
- **An approved edit retains the article's previous title, summary and body as a revision**
  (FR-020), in the same transaction.
- **The password is the office's, set by whoever runs the server** as a BCrypt hash in
  `GUIDE_ADMIN_PASSWORD_HASH`. Never in git; with no hash set, no password logs in.
- **The queue shows when a submission was sent in the office's time zone**, with its label
  (`1 Oct 2026, 17:28 IST`), from the `guide.time-zone` setting through `shared/web/DisplayTime`
  (walkthrough F-23, 1 Oct; it showed the stored UTC until then).
  In a browser with scripts the reader sees it in their own zone, relative while recent, the full
  local time on hover; the IST text is the fallback
  ([ADR-0020](../architecture/adr/ADR-0020-times-in-the-readers-own-zone.md), 1 Oct).
- **The approval keeps the contributor's limits**: a summary over 300 characters
  (`Article.LONGEST_SUMMARY`) or more than 10 tags is refused with `422` on the review page (1 Oct,
  walkthrough F-32 and F-33).

## Acceptance criteria

- [x] Of two pending submissions, the older appears first in the queue (FR-014)
- [x] With nothing pending, the queue says it is empty (FR-014)
- [x] A pending submission opens with its full text (FR-015)
- [x] A decided submission opens showing that it is no longer pending (FR-015)
- [x] Approving a new-article submission publishes it as a new article (FR-017)
- [x] Approving an edit submission updates the existing article with the proposed changes (FR-017)
- [x] Approving an already decided submission is refused (FR-017)
- [x] Rejecting a pending submission marks it rejected and removes it from the queue (FR-018)
- [x] Rejecting an already decided submission is refused (FR-018)
- [x] A reason given with the rejection is stored with it (FR-019)
- [x] A rejection without a reason is stored without one (FR-019)
- [x] Approving an edit retains the article's previous text as a revision (FR-020)
- [x] No path but approval changes the published table: rejecting, deciding twice and approving
      without a login leave `article` as it was (the step's invariant, roadmap phase 3)
- [x] Approval is refused when the title's address became taken while the submission waited
- [x] Every `/moderate/**` route but the login redirects to the login without a session; a wrong
      password answers `401`; the right one reaches the queue

Beyond the criteria: an approval with a blank summary, or a tag too long to store, re-renders the
review with `422` and what the moderator typed; a number never issued answers `404`. The review page
renders the body as the article page will, red links included, so the moderator sees what readers
will. The login is a controller rather than `formLogin`, which needs a username and answers a wrong
password with a redirect where the contract says `401`; the hash check, the session id change and
the stored security context are Spring Security's own classes.

Checked 28 Sep: all 16 tests red before the implementation, green after; removing the "already
decided" guard turns three of them red, the invariant test among them.

**After the review of 28 Sep** (the `dod-reviewer` pass, confirmed by the human): the article an edit
is approved against is locked for the approval, so two approved edits of one article at once each
keep the other's text as their revision instead of both keeping the same old one
(`ModerateArticleRepositoryTest`). Three lesser risks are debt: simultaneous approvals under one
address answer `500` ([DEBT-012](../tech-debt.md), closed on 28 Sep as the constraint
[CON-009](../requirements/constraints.md), not fixed), the CSRF token is not replaced on login
([DEBT-013](../tech-debt.md)), and the stand's session cookie is not `Secure` until TLS
([DEBT-014](../tech-debt.md)).

**Accepted by the human on 28 Sep** after using it in a browser (H2, `seed` profile): submitting two
articles and an edit, a wrong and a right password, approving one article, rejecting the other and
approving the edit. On that acceptance FR-014, FR-017, FR-018 and FR-020 are `done`; FR-015 stays
`in-progress` until the review page shows media (`media` step).

**28 Sep, with FEAT-008:** the queue's date is formatted in English whatever the moderator's browser
asks for (`the_date_in_the_queue_is_in_english_whatever_language_the_browser_asks_for`, red first
with a Russian `Accept-Language`), as the human asked that everything be in English.

**29 Sep, FR-019 (phase 4, taken early at the human's choice):** the reject form has an optional
`reason`, stored in the existing `submission.rejection_reason` column, so no migration. Decided with
the contract: the surrounding whitespace is trimmed and a blank reason is stored as none; 2000
characters is the limit (the column is unbounded), enforced by the service with `422` and by
`maxlength` in the form. The reason is stored only: showing it to the contributor is FR-012's, which is
not built. Seven tests; five red before the implementation, and the two that were already green (no
reason, a blank one) turn red with the trimming removed — except "no reason", which guards the
criterion rather than an implementation choice. Accepted by the human on 29 Sep after rejecting a
submission with a reason in a browser (H2, `seed` profile); on that acceptance FR-019 is `done`.

**29 Sep, FR-029 (queue item 8, decided by the human):** the review of an edit shows what it changes
against the live article, as [ADR-0015](../architecture/adr/ADR-0015-showing-an-edit-as-a-diff.md)
decides: two columns (stacked on a phone), removed text in red and struck through, added text in
green and underlined, unchanged paragraphs folded, and title, summary and tags as old and new. The
full proposed text sits under "Show the full proposed text". `TextDiff` arranges what java-diff-utils
finds into plain segments, which the template escapes. Tests: eleven in `TextDiffTest`, eight in
`ModerationFlowTest`; sixteen were red before the implementation, and the three that were not (a new
article, HTML in an edit, a text that did change) each went red under a mutation of the guard it
covers. `BrowserLayoutTest`'s sample submission is now an edit, so the comparison is measured at four
widths: it first found the phone labels under 16 px, now fixed. Accepted by the human on 29 Sep
after proposing an edit and reviewing it in a browser, wide and narrow; on that acceptance FR-029 is
`done`.

**Security list, 30 Sep** ([edge-cases-and-security.md](../verification/edge-cases-and-security.md)):
four rules of security.md that this feature holds had no test, and now have one each. With no hash
set, no password logs in (`ModeratorLoginWithoutHashTest`). The session cookie a login sets is
`HttpOnly` and `SameSite=Lax` (`SessionCookieTest`, on a real server). Approving and rejecting are
logged with the submission number (`ModerationFlowTest`). The review page's two `th:utext` now carry
the comment the article page's has, and `TemplateUnescapedOutputTest` holds both pages to it. No
behaviour changed. The moderator still has no logout: [DEBT-019](../tech-debt.md).

**Fixed 30 Sep: an edit's review page answered `500` on PostgreSQL.** The comparison (FR-029) read
the article through `findWithTagsByIdAndRemovedAtIsNull`, which is locked for approval, inside the
review's read-only transaction. PostgreSQL refuses `SELECT ... FOR UPDATE` there, and H2 does not, so
every test passed on H2. Under `-P postgres`, twelve `ModerationFlowTest` tests were red with that
error. The review now reads through `readWithTagsByIdAndRemovedAtIsNull`, which takes no lock, and
all 37 are green; approval still takes the lock, and `ModerateArticleRepositoryTest` is green.
Checked by the human the same day on the compose stand: an edit's review page opens with its
comparison.

**Fix 1.8, 2 Oct:** a long title in the queue is shortened with an ellipsis and shown whole on hover
and on the review page, and the Review column stays pinned to the table's right edge on a phone,
where the table scrolls inside itself (`BrowserLayoutTest.the_queue_keeps_every_review_link_in_view_beside_a_long_title`).

**Fix 2.3, 2 Oct (F-24, F-15):** the moderator's own header on every page while signed in (Queue,
Homepage & articles, The guide, Log out), a message on the queue after each decision with the title
and, after an approval, a link to the article; the review of a decided submission links to its live
article; the queue marks each submission's files by kind in one query; the login page leads back to
the guide.

## Deliberately out of scope

- Media on the review page (the rest of FR-015's first criterion) — the `media` step (DEBT-008).
- Showing the rejection reason to anyone — FR-012 (looking up a submission's status), not built.
- A diff of the rendered page rather than of its Markdown — ADR-0015's option C.
- Logout — no route in the contract; the session expires on its own.
- The admin panel, direct publishing (FR-023, FR-024), removal (FR-026) — phase 4.

## Open questions

None.
