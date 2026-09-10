# Route contract

Written 10 September, phase 1 stage 4, replacing the placeholder below. Formalizes what the eleven
[screens](../design/README.md#screens-come-from-requirements-not-one-slice-each) sketched in stage 3
found — which page needs which data, and which action posts where.

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
  `/submissions/**`, `/moderate/**`.
- **Article identity in a URL.** No `slug` column exists in
  [data-model.md](data-model.md) and adding one is a schema decision, not this document's to make. A
  title is already the closest thing to a natural key — FR-010/FR-011 reject a case-insensitive title
  collision — so `{title}` in a path is that same title, lowercased, non-letter/digit runs collapsed
  to a single `-`, percent-encoded for the URL, and matched case-insensitively against the same
  comparison FR-002's wiki-link resolution already does. Nothing new to store; the comparison is
  reused, not duplicated. Holds for non-Latin titles too (NFR-003) — the transform lower-cases only
  where the script has case, and percent-encoding carries the rest.
- **Templates.** `templates/<slice>/<Screen>.html`, one directory per slice, named after the file
  already committed under [screens/](../design/screens/) — e.g. `screens/Article.html` becomes
  `templates/articleview/Article.html`. A screen with an empty/decided-state variant (Search results,
  Moderation queue) is one template with a condition, not two files — `screens/SearchResultsEmpty.html`
  is a state of `SearchResults.html`, not a second route.
- **Response codes.**
  - `200` — a GET that resolves, including an empty/zero-result state (FR-001's own acceptance
    criterion names this: "the response is 200").
  - `404` — a GET whose target must not be revealed to exist: an unapproved, rejected, or removed
    article, or a submission number that was never issued. This is what "the route does not resolve"
    (FR-001, FR-007, FR-008) means concretely.
  - `302` — redirect-after-POST on success (POST/redirect/GET), and the un-authenticated hit of any
    `/moderate/**` route, to the login form.
  - `422` — a POST rejected by a validation rule the contributor can fix by re-submitting the same
    form (title collision, oversized or wrong-type attachment): the form is re-rendered with the error
    and, where FR-010/FR-011 name one, the offered link.
  - `409` — a POST rejected because the target changed under the caller between GET and POST (a
    submission already decided, an edit's target article no longer published): re-rendering a stale
    form would lie about what is still true, so this is a plain conflict page, not the form again.
- **Moderator gating.** ADR-0009 is explicit that this file is public and was never the control —
  every `/moderate/**` route is gated by the shared-password session filter, not by the path being
  hard to guess.

## Routes

| Route | Slice | Template | Form fields | Response codes | Serves |
|---|---|---|---|---|---|
| `GET /articles/{title}` | `articleview` | `articleview/Article.html` | — (read only) | `200` published; `404` unapproved / rejected / removed / no match | FR-001 (UC-003). Also where FR-002 and FR-004 are visible — the body is rendered with wiki links resolved and red links styled inline; neither has a route of its own |
| `GET /` | `home` | `home/Landing.html` | — | `200` always | FR-009 (UC-007) |
| `GET /search?q={query}` | `search` | `search/SearchResults.html` | `q` (text, required — blank query is the landing page's job, not this route's) | `200` always, including zero matches | FR-007 (UC-001) |
| `GET /submit` | `contribute` | `contribute/SubmissionForm.html` | — (empty form; fields below belong to the POST) | `200` always | FR-010 (UC-010) — entry point |
| `POST /submissions` | `contribute` | re-renders `contribute/SubmissionForm.html` on `422` | `title` (text, required, unique among published titles case-insensitively); `summary` (text, required); `body` (markdown textarea, required — may contain `[[Title]]`, stored unchanged per FR-003); `attachment` (file, optional — one photo/document/video, NFR-001's size limits and CON-006's type list); `tags` (repeatable text, optional) | `302` to the confirmation route on success; `422` on a title collision (with the offered link/rename choice) or a rejected attachment | FR-010 (UC-010), FR-003 (UC-012) |
| `GET /articles/{title}/edit` | `contribute` | `contribute/SubmissionForm.html`, pre-filled from the article | — (pre-filled form; fields below belong to the POST) | `200` published; `404` not published (same rule as the read route) | FR-011 (UC-011) — entry point |
| `POST /articles/{title}/edits` | `contribute` | re-renders `contribute/SubmissionForm.html` on `422` | Same fields as `POST /submissions`; FR-011 allows any subset to change. The target article comes from `{title}`, not a form field | `302` to the confirmation route on success; `422` on a title collision with a *different* article, or a rejected attachment; `409` if the target article stopped being published between the GET and this POST | FR-011 (UC-011), FR-003 (UC-012) |
| `GET /submissions/{number}/confirmation` | `contribute` | `contribute/SubmissionConfirmation.html` | — | `200` if `{number}` exists; `404` otherwise | FR-010 / FR-011's "shown a submission number" |
| `GET /moderate/queue` | `moderate` | `moderate/ModerationQueue.html` | — | `200` always, including the empty-queue state; `302` to `/moderate/login` unauthenticated | FR-014 (UC-014) |
| `GET /moderate/submissions/{number}` | `moderate` | `moderate/SubmissionReview.html` (also hosts the two POST forms below) | — (the editable fields below are part of this page but submit to the two POST routes) | `200` pending, or `200` showing "no longer pending" if already decided (FR-015 shows the state, it does not hide the submission); `404` if `{number}` was never issued; `302` unauthenticated | FR-015 (UC-015) |
| `POST /moderate/submissions/{number}/approve` | `moderate` | `moderate/SubmissionReview.html` on error | `summary` (text, editable, pre-filled from the submission); `tags` (repeatable text, editable, pre-filled from the submission's suggested tags) | `302` to `/moderate/queue` on success; `409` if already decided; `302` unauthenticated | FR-017 (UC-016) |
| `POST /moderate/submissions/{number}/reject` | `moderate` | `moderate/SubmissionReview.html` on error | `reason` (text, optional — FR-019, `could`; the field is already on the shared review screen, so it is recorded here rather than left off, even though rejecting itself does not require it) | `302` to `/moderate/queue` on success; `409` if already decided; `302` unauthenticated | FR-018 (UC-017) |
| `GET /moderate/login`, `POST /moderate/login` | `shared/security` | `shared/AdminLogin.html` | `password` (text, required) | `200` form; `302` to `/moderate/queue` on success; `401` re-rendering the form on a wrong password | Not itself an `FR` — the session gate ADR-0009 decided, without which every `/moderate/**` row above is unreachable |

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

## Deferred — `should`/`could` features, not routed in this pass

| Feature | Priority | FR |
|---|---|---|
| Browse/filter articles by tag | should | FR-008 |
| Download a media attachment | should | FR-016 |
| Backlinks on an article | could | FR-006 |
| Creating an article from a red link | could | FR-005 |
| Report an article | could | FR-021 |
| Look up a submission's status by its number | could | FR-012 |
| Abuse handling (rate limiting + CAPTCHA) | should | FR-013 |
| Providing a rejection reason | could | FR-019 — the field is already routed above, since it lives on a `must`-scope screen; the requirement behind it is still deferred |
| Version-history groundwork | should | FR-020 — no UI route; storage only |
| Closing a report | could | FR-022 |
| Write/publish directly, bypassing the queue (×2) | could | FR-023, FR-024 |
| Edit the homepage, including pinned | could | FR-025 |
| Remove a published article | should | FR-026 |

Added to `ui-routes.md` when phase 4 picks each one up, per the roadmap's revised milestone plan.
