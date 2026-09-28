---
id: FEAT-012
title: Looking up a submission's status
status: done
covers: [FR-012]
slice: contribute
routes: ["GET /submissions/status"]
tables: [submission, article]
code:
  - app/src/main/java/in/ac/iitm/guide/contribute/web/SubmissionController.java
  - app/src/main/java/in/ac/iitm/guide/contribute/internal/SubmissionService.java
  - app/src/main/java/in/ac/iitm/guide/contribute/persistence/SubmissionRepository.java
  - app/src/main/java/in/ac/iitm/guide/contribute/persistence/ContributeArticleRepository.java
  - app/src/main/resources/templates/contribute/SubmissionStatus.html
  - app/src/main/resources/templates/contribute/SubmissionConfirmation.html
tests:
  - app/src/test/java/in/ac/iitm/guide/contribute/SubmissionStatusTest.java
---

# FEAT-012 — Looking up a submission's status

## Why

A contributor has no account and no email address on file (CON-001), so the submission number is the
only thing that ties them to what they sent. Without this page they never learn whether it was
published or rejected, and FR-019's rejection reason reaches nobody but OGE.

## Scenario

The contributor submits an article or an edit and is shown its number, with a "Check its status" link
(UC-013). Later they open `/submissions/status`, type the number — any case, hyphens optional — and see
one of: pending; approved, with a link to the article; rejected, with the moderator's reason if one
was given. A number that was never issued answers "No such submission was found".

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /submissions/status` | The lookup form, and the status of the number typed into it | `contribute/SubmissionStatus.html` |

Codes as in [ui-routes.md](../architecture/ui-routes.md).

## Schema impact

None. Reads `submission` by `submission_number` (its `UNIQUE` index) and, for an approved submission,
the `slug` of the live `article` it produced.

## Decisions this feature fixed

Confirmed by the human on 29 Sep with the contract for this step (prompt journal, 29 Sep).

- **The page shows the status and the reason, never the submission's text.** The number is the only
  authorisation (ADR-0011); a status is what its holder needs, and the text stays unreadable to anyone
  but the moderator until it is published.
- **The route is `GET /submissions/status?number=…`**, so a link can carry the number and the browser's
  history does not turn a lookup into a resubmission.
- **An approved edit links to its article by id**, following a later rename. **An approved new article
  is found by the address of its title**, because `article` keeps no link back to its submission
  (ADR-0003); after a rename or a removal the page shows "Approved" with no link rather than a wrong one.
- **A blank number shows the form alone** (`200`); a number never issued, or text that cannot be one,
  answers `404` with the form and the message — the two look the same from outside, as on the
  confirmation page.

## Acceptance criteria

- [x] A valid number shows its status: pending, approved or rejected (FR-012)
- [x] A rejected submission with a stored reason shows the reason (FR-012)
- [x] A number that does not exist shows that no such submission was found (FR-012)

Beyond the criteria, each a test in `SubmissionStatusTest`: the form without a number, a blank number,
text that cannot be a number, a number typed in lower case without hyphens and with spaces around, the
article link for an approved new article and for an approved edit renamed since, no reason block for a
rejection without one, the submission's text absent from the page, and the confirmation page's link.
Ten were red before the implementation. "The text is absent" passes against any page that does not
render it, and guards the decision above rather than an implementation step. "A blank number" was
written after the code and shown red with the blank check removed.

**Accepted by the human on 29 Sep** after using it in a browser (H2, `seed` profile): a pending
submission, one approved with its article link, one rejected with its reason, and a wrong number. On
that acceptance FR-012 is `done`.

## Deliberately out of scope

- Telling the contributor without their asking (email) — CON-001; queue item 7 of the roadmap.
- The text of the submission, its media, or who decided it.
- A link to the status page in the site header — the confirmation page links to it.
