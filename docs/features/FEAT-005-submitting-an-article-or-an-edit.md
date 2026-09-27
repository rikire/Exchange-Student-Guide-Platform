---
id: FEAT-005
title: Submitting a new article or an edit
status: in-progress
covers: [FR-003, FR-010, FR-011, NFR-006]
slice: contribute
routes: ["GET /submit", "POST /submissions", "GET /articles/{title}/edit", "POST /articles/{title}/edits", "GET /submissions/{number}/confirmation"]
tables: [submission, submission_tag, tag, article]
code:
  - app/src/main/java/in/ac/iitm/guide/taxonomy/Tags.java
  - app/src/main/java/in/ac/iitm/guide/taxonomy/TagRejectedException.java
  - app/src/main/java/in/ac/iitm/guide/taxonomy/persistence/TagRepository.java
tests:
  - app/src/test/java/in/ac/iitm/guide/taxonomy/TagsTest.java
---

# FEAT-005 — Submitting a new article or an edit

## Why

The guide is written by the students who use it. Without a way to propose an article or a correction
there is nothing for OGE to moderate, and the middle of the mid-demo scenario — a student proposes an
edit, the moderator approves it — has no first half.

## Scenario

A contributor opens "Submit an article", fills in a title, a summary, a body that may link to other
articles with `[[Title]]`, and suggests tags. They submit and are shown a submission number, which is
the only handle they will ever have on it. Nothing is published: the submission waits for a
moderator (FEAT to come, `moderate`).

A reader on an article's page follows "Propose an edit", gets the same form filled in from the
article, changes any part of it and submits. The same confirmation follows.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /submit` | The empty form | `contribute/SubmissionForm.html` |
| `POST /submissions` | Submit a new article | re-renders the form on `422` |
| `GET /articles/{title}/edit` | The form, filled in from a published article | `contribute/SubmissionForm.html` |
| `POST /articles/{title}/edits` | Submit an edit | re-renders the form on `422` |
| `GET /submissions/{number}/confirmation` | The number, after a successful POST | `contribute/SubmissionConfirmation.html` |

Codes as in [ui-routes.md](../architecture/ui-routes.md), except the `409` noted below.

## Schema impact

None. Writes `submission` and `submission_tag`; a suggested tag that does not exist yet is created in
`tag` through `taxonomy`. Reads `article` to check a title and to fill in the edit form.

## Decisions this feature fixed

Each follows from something already decided; each has a test.

- **A title collides when its address does.** The address (`article.slug`) is the title lower-cased
  with punctuation collapsed, so a case-insensitive title match is always an address match, and an
  address match that is not a title match ("Fees & Payments" against "Fees Payments") would fail the
  unique `slug` at publishing anyway. One check covers FR-010's rule and the roadmap's slug rule of
  25 Sep.
- **The collision is checked against every article row, removed ones too**, because both unique
  constraints (`title`, `slug`) hold across them. The link to propose an edit is offered only when the
  colliding article is live; for a removed one only the rename is offered, since the link would 404.
- **A title with no letters or digits is refused with `422`**: it has no address and "cannot be
  published" (ui-routes.md, "Article identity in a URL").
- **Title, summary and body are required**; blank after trimming is empty. The title is at most 255
  characters (the column). Blank tag fields are ignored; a tag longer than 64 characters (the column)
  is refused with `422` by `taxonomy`.
- **Tags go through `taxonomy`**, which trims and lower-cases them and creates the missing ones
  (ADR-0005). A tag suggested on a submission that is never approved stays in `tag`; the landing page
  lists only tags on live articles, so it is not shown anywhere.
- **An edit may keep its own title** or change its capitalisation; only a *different* article's
  address collides.
- **The submission number** is `SUB-` and 12 Crockford base32 characters in groups of four, 60 bits
  from `SecureRandom` (ADR-0011, NFR-006). Stored upper-case with hyphens; looked up with case and
  hyphens ignored.
- **The confirmation page shows the number for any submission that exists**, whatever its state:
  holding the number is the authorisation (ADR-0011).
- **CSRF** is Spring Security's default token on every POST (security.md), added with this feature
  because it is the first to have a form. Every route stays public; the moderator login comes with
  `moderate`.

## Acceptance criteria

- [ ] A new article submitted through the form is pending in the queue and the contributor is shown
      its number (FR-010)
- [ ] A title matching an existing article's case-insensitively is refused with a link to propose an
      edit to it and the option to change the title (FR-010)
- [ ] A title whose address is taken by another title is refused the same way (roadmap, 25 Sep)
- [ ] A proposed edit is pending in the queue, tied to its article, and the contributor is shown its
      number (FR-011)
- [ ] An edit whose new title matches a different article's is refused with a link to that article
      and the option to change the title (FR-011)
- [ ] An edit to an article that is not published is refused: `404` on the form and on the POST
      (FR-011)
- [ ] `[[Title]]` markup in a submitted body is stored unchanged (FR-003)
- [ ] A submission is reachable from no public page: its title is on neither `/` nor an article
      address
- [ ] Submission numbers are not ordered by issue and are drawn from `SecureRandom` (NFR-006)
- [ ] A number is found ignoring case and hyphens; a number never issued is `404`
- [ ] A POST without a CSRF token is refused and stores nothing
- [ ] Tags are stored trimmed and lower-cased, one row per tag however it was spelled (ADR-0005)

## Deliberately out of scope

- **The attachment** — FR-010 and FR-011's two media criteria (size, type). They arrive with the
  `media` step of phase 3: [DEBT-008](../tech-debt.md).
- **`409` when an edit's target stops being published between the form and the POST.** Nothing can
  unpublish an article until FR-026 (phase 4); until then the POST answers `404`:
  [DEBT-009](../tech-debt.md).
- Publishing, approval, rejection — `moderate`.
- Looking up a submission's status later (FR-012, `could`), rate limiting and CAPTCHA (FR-013,
  NFR-005, phase 4).

## Open questions

None open for this feature.
