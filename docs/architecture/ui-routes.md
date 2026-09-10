# Route contract

Written 10 September, phase 1 stage 4, replacing the placeholder below. Formalizes what the
[screens](../design/README.md#screens-come-from-requirements-not-one-slice-each) sketched in stage 3
found — which page needs which data, and which action posts where. Stage 3 sketched eleven; stage 4
added two more, both because writing this file found a route with nothing behind it.

**Reviewed 10 September, the day it was written** — see [the review](#what-the-review-of-10-september-found)
below for the ten findings and what changed.

## Scope

Only the **12 `must`-priority features** get a row in this pass — the same bar the roadmap step's own
check sets, and the same cut the [revised milestone plan](../roadmap/01-requirements-design.md) made
for phase 3 (`should`/`could` moved to phase 4). The 14 `should`/`could` features are listed at the
bottom, unrouted, so the gap is visible rather than silently absent.

## Conventions

Nothing below is written in code yet — no slice package exists past the skeleton (phase 2 has not
started) — so every convention here is this document's own decision, not something read off existing
code.

- **Path shape.** REST-ish nouns, prefixed by the slice that owns the controller (matching the package
  names in [architecture-rules.md](../ai/architecture-rules.md)): `/articles/**`, `/search`,
  `/submissions/**`, `/moderate/**`, `/media/**`.

  **One exception, deliberate:** `/moderate/login` is owned by `shared/security`, not by `moderate`.
  It sits under the `moderate` prefix anyway so that the admin area is exactly one path prefix —
  which is what lets the gating rule below be a single pattern. A login form at `/security/login`
  would mean two prefixes to protect, one of which protects nothing.
- **Article identity in a URL.** No `slug` column exists in
  [data-model.md](data-model.md) and adding one is a schema decision, not this document's to make. A
  title is already the closest thing to a natural key — FR-010/FR-011 reject a case-insensitive title
  collision — so `{title}` in a path is that same title, lowercased, non-letter/digit runs collapsed
  to a single `-`, percent-encoded for the URL, and matched case-insensitively against the same
  comparison FR-002's wiki-link resolution already does. Nothing new to store; the comparison is
  reused, not duplicated. Holds for non-Latin titles too (NFR-003) — the transform lower-cases only
  where the script has case, and percent-encoding carries the rest.
- **Submission identity in a URL.** `{number}` is the submission number, which
  [ADR-0011](adr/ADR-0011-submission-number-format.md) makes a generated 60-bit token rather than a
  sequence. Matched case-insensitively and ignoring the hyphens, because a contributor retyping it
  from a screenshot will get one of those wrong. This is what lets the confirmation route below be
  public — see [Why the confirmation route needs no gate](#why-the-confirmation-route-needs-no-gate).
- **Templates.** `templates/<slice>/<Screen>.html`, one directory per slice, named after the file
  already committed under [screens/](../design/screens/) — e.g. `screens/Article.html` becomes
  `templates/articleview/Article.html`. A screen with an empty/decided-state variant (Search results,
  Moderation queue) is one template with a condition, not two files — `screens/SearchResultsEmpty.html`
  is a state of `SearchResults.html`, not a second route.
- **Every state-changing form carries a CSRF token.** Required by
  [security.md](security.md) on every POST here. The token is a hidden field on each form below, and
  a POST without a valid one is rejected before any field is read. It appears in each row's
  `Form fields` anyway, because a column that omits a required field is a column an implementer will
  trust.
- **Response codes.**
  - `200` — a GET that resolves, including an empty/zero-result state (FR-001's own acceptance
    criterion names this: "the response is 200").
  - `404` — a GET whose target must not be revealed to exist: an unapproved, rejected, or removed
    article, a submission number that was never issued, or a media asset the caller is not entitled
    to. This is what "the route does not resolve" (FR-001, FR-007, FR-008) means concretely.
  - `302` — redirect-after-POST on success (POST/redirect/GET), and the un-authenticated hit of a
    *gated* `/moderate/**` route, to the login form.
  - `400` — a GET whose required query parameter is missing or blank. Only `/search` has one.
  - `401` — the admin login form re-rendered after a wrong password. Deliberately not `422`: nothing
    about the submitted form is malformed, and classing a failed login with a title collision would
    put an authentication failure and a validation failure in the same bucket.
  - `422` — a POST rejected by a validation rule the contributor can fix by re-submitting the same
    form (title collision, oversized or wrong-type attachment): the form is re-rendered with the error
    and, where FR-010/FR-011 name one, the offered link.
  - `409` — a POST rejected because the target changed under the caller between GET and POST (a
    submission already decided, an edit's target article no longer published): re-rendering a stale
    form would lie about what is still true, so this is a plain conflict page, not the form again.
- **Moderator gating.** Every `/moderate/**` route is gated by the shared-password session filter —
  **except `GET|POST /moderate/login` itself**, which is the one path in that prefix the filter must
  let through. Without the carve-out, the rule sends the login form to the login form.
  [ADR-0009](adr/ADR-0009-admin-authentication.md) is explicit that this file is public and was never
  the control: every row below is reachable by anyone who reads this repository, and the session
  filter is what stops them.

## Routes

| Route | Slice | Template | Form fields | Response codes | Serves |
|---|---|---|---|---|---|
| `GET /articles/{title}` | `articleview` | `articleview/Article.html` | — (read only) | `200` published; `404` unapproved / rejected / removed / no match | FR-001 (UC-003). Also where FR-002 and FR-004 are visible — the body is rendered with wiki links resolved and red links styled inline; neither has a route of its own |
| `GET /` | `home` | `home/Landing.html` | — | `200` always | FR-009 (UC-007) |
| `GET /search?q={query}` | `search` | `search/SearchResults.html` | `q` (text, required) | `200` always, including zero matches; `400` if `q` is missing or blank — an empty query is the landing page's job, and redirecting there silently would hide a broken link from whoever built it | FR-007 (UC-001) |
| `GET /media/{id}` | `media` | — (bytes, not a template) | — | `200` for an asset on a published article; `200` for an asset on a pending or rejected submission **only** with a moderator session; `404` otherwise. `nosniff` always, `Range` supported for video — see [security.md](security.md) | FR-001 (an Article is "…and zero or more attached media assets"), FR-015 ("full text **and any media assets**"). FR-016's download response is phase 4 — see the deferred table |
| `GET /submit` | `contribute` | `contribute/SubmissionForm.html` | — (empty form; fields below belong to the POST) | `200` always | FR-010 (UC-010) — entry point |
| `POST /submissions` | `contribute` | re-renders `contribute/SubmissionForm.html` on `422` | CSRF token (hidden, required); `title` (text, required, unique among published titles case-insensitively); `summary` (text, required); `body` (markdown textarea, required — may contain `[[Title]]`, stored unchanged per FR-003); `attachment` (file, optional — one photo/document/video, NFR-001's size limits and CON-006's type list); `tags` (repeatable text, optional) | `302` to the confirmation route on success; `422` on a title collision (with the offered link/rename choice) or a rejected attachment | FR-010 (UC-010), FR-003 (UC-012) |
| `GET /articles/{title}/edit` | `contribute` | `contribute/SubmissionForm.html`, pre-filled from the article | — (pre-filled form; fields below belong to the POST) | `200` published; `404` not published (same rule as the read route) | FR-011 (UC-011) — entry point |
| `POST /articles/{title}/edits` | `contribute` | re-renders `contribute/SubmissionForm.html` on `422` | Same fields as `POST /submissions`, CSRF token included; FR-011 allows any subset to change. The target article comes from `{title}`, not a form field | `302` to the confirmation route on success; `404` if `{title}` matches no published article — FR-011's "no longer published" is unconditional, so a POST aimed straight at an unknown or never-published title is refused the same way the GET is; `422` on a title collision with a *different* article, or a rejected attachment; `409` if the target was published at the GET and is not at this POST | FR-011 (UC-011), FR-003 (UC-012) |
| `GET /submissions/{number}/confirmation` | `contribute` | `contribute/SubmissionConfirmation.html` | — | `200` if `{number}` exists; `404` otherwise | FR-010 / FR-011's "shown a submission number" |
| `GET /moderate/queue` | `moderate` | `moderate/ModerationQueue.html` | — | `200` always, including the empty-queue state; `302` to `/moderate/login` unauthenticated | FR-014 (UC-014) |
| `GET /moderate/submissions/{number}` | `moderate` | `moderate/SubmissionReview.html` (also hosts the two POST forms below) | — (the editable fields below are part of this page but submit to the two POST routes) | `200` pending, or `200` showing "no longer pending" if already decided (FR-015 shows the state, it does not hide the submission); `404` if `{number}` was never issued; `302` unauthenticated | FR-015 (UC-015) |
| `POST /moderate/submissions/{number}/approve` | `moderate` | `moderate/SubmissionReview.html` on error | CSRF token (hidden, required); `summary` (text, editable, pre-filled from the submission); `tags` (repeatable text, editable, pre-filled from the submission's suggested tags) | `302` to `/moderate/queue` on success; `409` if already decided; `302` unauthenticated | FR-017 (UC-016) |
| `POST /moderate/submissions/{number}/reject` | `moderate` | `moderate/SubmissionReview.html` on error | CSRF token (hidden, required); `reason` (text, optional — FR-019, `could`; the field is already on the shared review screen, so it is recorded here rather than left off, even though rejecting itself does not require it) | `302` to `/moderate/queue` on success; `409` if already decided; `302` unauthenticated | FR-018 (UC-017) |
| `GET /moderate/login`, `POST /moderate/login` | `shared/security` | `shared/security/AdminLogin.html` | CSRF token (hidden, required); `password` (text, required) | `200` form; `302` to `/moderate/queue` on success; `401` re-rendering the form on a wrong password. **Not gated** — the one carve-out in the rule above | Not itself an `FR` — the session gate [ADR-0009](adr/ADR-0009-admin-authentication.md) decided, without which every `/moderate/**` row above is unreachable |

### Why the confirmation route needs no gate

It answers `200` or `404` to anyone who asks, which for most of this table would be the enumeration
hole the `404` convention exists to close. It is safe here for one reason only: `{number}` is a
60-bit token ([ADR-0011](adr/ADR-0011-submission-number-format.md)), so there is nothing to walk.

Stated plainly, because the property is not local to this row. Weaken the generator to a sequence and
this route — along with FR-012's later lookup — becomes a list of every submission the platform has
received, including the unapproved and the rejected ones, while nothing in this file would look
wrong. [NFR-006](../requirements/non-functional.md) is what fails instead.

## Gap found while formalizing this file — closed the same day

`GET /submissions/{number}/confirmation` had no sketched screen. `screens/SubmissionStatus.html`
looked like the natural template but was not one — it is FR-012's *lookup* form (`could`, out of this
pass's scope: a contributor types in a number they already have), not the *confirmation* FR-010/FR-011
already require ("show them a submission number") the moment a `must`-priority submission succeeds.
The flow canvas's own sticky note had actually been pointing "Submitting shows a number ->" at
`SubmissionStatus` since stage 3 — the same conflation, drawn before either FR existed in its current
form.

Closed 10 Sep, same day: [Submission confirmation](../design/screens/SubmissionConfirmation.html)
sketched and added to the flow canvas (`canvas-src/flow/SubmissionConfirmation.dc.html`), the arrow
corrected, and a note added explaining that `SubmissionStatus` is reached independently and later —
see [design/README.md](../design/README.md#canvases). The route row above now points at the real
template.

## What the review of 10 September found

The table above was reviewed the day it was written, against `functional.md`, `glossary.md`,
[data-model.md](data-model.md), `erd.puml`, [security.md](security.md), ADR-0001/0006/0009,
[architecture-rules.md](../ai/architecture-rules.md), the sketched screens, and the roadmap's
feature-coverage tracker. Recorded here rather than quietly corrected: a contract that reads as
though it was right first time teaches nobody where these go wrong.

**What held:** all 12 `must` FRs routed; slice names matching the package map; the `409`/`422` split;
the refusal to invent a `slug` column.

**Two decisions, both taken and recorded the same day:**

1. **The submission number was undecided, and the screens had decided it.** The glossary already
   called it "the only handle they have on it afterwards" — a bearer capability — while five sketched
   screens rendered `SUB-04815`…`SUB-04822`, a counter. Against a counter, the confirmation route is
   an enumeration of every submission ever received.
   → [ADR-0011](adr/ADR-0011-submission-number-format.md), measured by
   [NFR-006](../requirements/non-functional.md).
2. **Media had no route at all**, though FR-001 and FR-015 are both `must` and both need one, and
   [security.md](security.md) had already fixed the controller's behaviour.
   → the `GET /media/{id}` row, and a dated amendment to
   [ADR-0006](adr/ADR-0006-media-storage-and-upload-security.md).

   Routing it exposed a contradiction neither document could show alone: FR-016 says an asset on an
   unapproved submission is "**not returned**", while security.md said it was "reachable only by an
   unguessable id". Those describe different systems. Resolved in favour of the requirement — the
   gate is the moderator session — and security.md corrected with the date on it.

**Eight corrections to this file:**

| # | What was wrong | Where it is fixed |
|---|---|---|
| 3 | `/moderate/login` was caught by its own gating rule — "any `/moderate/**` route" redirects to the login form, including the login form | Moderator gating, and the login row |
| 4 | No row carried a CSRF token, though [security.md](security.md) requires one on every state-changing form | A convention of its own, plus all five POST-bearing rows |
| 5 | `401` was used by the login row but never declared among the response codes | Response codes, with why it is not `422` |
| 6 | `POST /articles/{title}/edits` handled only the race — "stopped being published between the GET and this POST" — while FR-011's condition is unconditional | A `404` on that row |
| 7 | `q` was called required, with no code given for a missing or blank one | `400`, in the codes and on the row |
| 8 | The login row pointed at `shared/AdminLogin.html` — neither what this file's own template rule yields for slice `shared/security`, nor a screen that existed | The row, and a sketched [Admin login](../design/screens/AdminLogin.html) screen |
| 9 | Path shape said "prefixed by the slice that owns the controller", which `/moderate/login` breaks | Stated as a deliberate exception, with the reason |
| 10 | The roadmap tracker's `Route` column was missing this file's own confirmation route | [01-requirements-design.md](../roadmap/01-requirements-design.md) |

Finding 8 is the one worth keeping in view: the same class of gap this file had already found and
closed for the confirmation screen was sitting two rows below it, in the row added last. A
found-and-closed note is not a substitute for re-reading the whole table afterwards.

## Deferred — `should`/`could` features, not routed in this pass

| Feature | Priority | FR |
|---|---|---|
| Browse/filter articles by tag | should | FR-008 |
| Download a media attachment | should | FR-016 — the bytes *are* routed above, because FR-001 and FR-015 need them. What is deferred is the reader-facing download: the `Content-Disposition: attachment` response and the button on the article screen |
| Backlinks on an article | could | FR-006 |
| Creating an article from a red link | could | FR-005 |
| Report an article | could | FR-021 |
| Look up a submission's status by its number | could | FR-012 — needs no gate when it arrives, per [ADR-0011](adr/ADR-0011-submission-number-format.md) |
| Abuse handling (rate limiting + CAPTCHA) | should | FR-013 |
| Providing a rejection reason | could | FR-019 — the field is already routed above, since it lives on a `must`-scope screen; the requirement behind it is still deferred |
| Version-history groundwork | should | FR-020 — no UI route; storage only |
| Closing a report | could | FR-022 |
| Write/publish directly, bypassing the queue (×2) | could | FR-023, FR-024 |
| Edit the homepage, including pinned | could | FR-025 |
| Remove a published article | should | FR-026 |

Added to `ui-routes.md` when phase 4 picks each one up, per the roadmap's revised milestone plan.
