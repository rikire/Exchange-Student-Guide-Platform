---
id: FEAT-006
title: Moderating a submission
status: in-progress
covers: [FR-014, FR-015, FR-017, FR-018, FR-020]
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
  - app/src/test/java/in/ac/iitm/guide/shared/security/ModeratorLoginTest.java
---

# FEAT-006 — Moderating a submission

## Why

Every article and every edit waits in the queue until someone at OGE looks at it (ADR-0003). Without
this feature nothing a contributor writes ever reaches a reader, and the second half of the mid-demo
scenario — the moderator approves the edit and it goes live — does not exist.

## Scenario

The OGE moderator opens `/moderate/queue`, is sent to the login page, types the office's shared
password and lands on the queue: every pending submission, oldest first. They open one and read its
full text as it would be published. They adjust the summary and the tags if needed and approve it: a
new article goes live at its address, an edit replaces the article's text and the text it replaced is
kept as a revision. Or they reject it, and it leaves the queue. Either way they are back at the queue.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /moderate/login`, `POST /moderate/login` | The shared-password login (ADR-0009) | `shared/security/AdminLogin.html` |
| `GET /moderate/queue` | Pending submissions, oldest first | `moderate/ModerationQueue.html` |
| `GET /moderate/submissions/{number}` | One submission in full, or "no longer pending" | `moderate/SubmissionReview.html` |
| `POST /moderate/submissions/{number}/approve` | Publish, or apply the edit | re-renders the review on `409` |
| `POST /moderate/submissions/{number}/reject` | Mark rejected | re-renders the review on `409` |

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

## Deliberately out of scope

- Media on the review page (the rest of FR-015's first criterion) — the `media` step (DEBT-008).
- The rejection reason (FR-019) — phase 4; the field is not on the form yet.
- A diff of an edit — CON-004, and queue item 8 of the roadmap.
- Logout — no route in the contract; the session expires on its own.
- The admin panel, direct publishing (FR-023, FR-024), removal (FR-026) — phase 4.

## Open questions

None.
