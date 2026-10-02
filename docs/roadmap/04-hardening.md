# Phase 4 — Hardening, security, deployment

**Status: not started.** Runs 10–30 October 2026.

## Goal

Make it survive real use and real inputs, and make it something a third person can stand up.

## Steps

- [x] Moved from phase 3, 10 Sep — see the revised milestone plan in
      [01-requirements-design.md](01-requirements-design.md): admin panel behind the single
      password, plus FR-023 (publishing a new article directly) and FR-024 (editing an article
      directly)
      — check: FR-023's four criteria and FR-024's four criteria are eight tests; the admin-route
      enumeration test from the original phase-3 step (every admin route redirects when
      unauthenticated, enumerated from the route contract) is included
      **Done 2 Oct**, accepted by the human on the stand:
      [FEAT-021](../features/FEAT-021-publishing-directly.md), eight criterion tests among
      `DirectPublishingTest`'s twelve; `ModeratorLoginTest` enumerates the four new routes from
      `routes.yml`
- [x] ~~FR-025 — editing the homepage's pinned articles~~
      **Moved to phase 3, 1 Oct** by the human, with the "All articles" page: the walkthrough found
      *Registering with FRRO* missing from the landing page — see the step "Every article reachable,
      FRRO first (F-7)" in [03-main-flow.md](03-main-flow.md)
      — check: FR-025's two criteria are two tests, built under that phase-3 step
      **Built 1 Oct** in phase 3: [FEAT-019](../features/FEAT-019-pinning-articles-in-order.md).
- [x] Moved from phase 3, 10 Sep: FR-019 — providing a rejection reason
      — check: rejecting with a reason stores it, rejecting without one stores none — two tests
      **Built early, 29 Sep:** [FEAT-006](../features/FEAT-006-moderating-a-submission.md), both
      criteria and five more cases (the field on the form, trimming, blank, the 2000-character limit
      and one over it) as tests in `ModerationFlowTest`. Accepted by the human in a browser the same day.
- [x] FR-012 — looking up a submission's status by its number, taken early at the human's choice on
      29 Sep so that FR-019's reason reaches the contributor
      — check: FR-012's three criteria are three tests
      **Built 29 Sep:** [FEAT-012](../features/FEAT-012-looking-up-a-submission.md), `GET
      /submissions/status`, the three criteria and nine more cases in `SubmissionStatusTest`. Accepted by
      the human in a browser the same day.
- [x] FR-030 — saving an article as a PDF, asked for by the human on 29 Sep; the browser's print with
      print styles, chosen over a server-side PDF (Hindi and Tamil shaping) and a Markdown download
      — check: FR-030's two criteria are tests, one of them in a real browser in print media
      **Built 29 Sep:** [FEAT-002](../features/FEAT-002-article-page.md), `ArticleControllerTest` and
      `BrowserPrintTest`. Accepted by the human in a browser the same day.
- [x] Moved from phase 3, 10 Sep: FR-006 — backlinks on an article, the "what links here" block
      — check: a published article linking to another appears in its backlink list; a link from an
      unapproved or rejected submission does not — three tests
      **Built early, 29 Sep:** [FEAT-014](../features/FEAT-014-backlinks.md), a `backlink` slice fed by
      `ArticleTextChanged` ([ADR-0016](../architecture/adr/ADR-0016-backlinks-through-an-event-fed-slice.md),
      chosen by the human over three alternatives); the three checks and six more cases are tests.
      Closes DEBT-006. Accepted by the human in a browser the same day.
- [x] Moved from phase 3, 10 Sep: FR-026 — removing a published article
      — check: after removal the article's route no longer resolves, it drops from search and its
      tags, a wiki link to it becomes red, and it drops from the pinned section if pinned — FR-026's
      four criteria as four tests
      **Built early, 29 Sep:** [FEAT-013](../features/FEAT-013-removing-an-article.md) — "Remove this
      article" for the signed-in moderator, a confirmation, `removed_at` set; the four criteria are
      tests in `ArticleRemovalTest`, and DEBT-009's `409` one in `SubmissionFlowTest`. Accepted by the
      human in a browser the same day.
- [x] Moved from phase 3, 10 Sep: FR-016 — downloading a media attachment
      — check: a media asset on a published article downloads; one attached to an unapproved or a
      rejected submission does not — three tests
      **Built early, 29 Sep:** [FEAT-009](../features/FEAT-009-attaching-media-to-a-submission.md), the
      Download link on the article page; the three checks are tests in `MediaDeliveryTest`, the
      delivery and access ones from 28 Sep. Accepted by the human in a browser the same day.
- [x] Edge cases: empty query, injection attempt, HTML in article text, duplicate titles, a title
      over 255 characters, circular wiki links, a corrupt import archive, a file whose extension
      lies about its content, a file over the limit
      — check: each of the nine is a named test, and each fails when its guard is removed — nine
      tests that pass against no implementation would be worse than none
      **Corrected 30 Sep:** the limit read "100 characters"; FR-010's form and the schema allow 255,
      and the human kept 255.
      **Done 30 Sep:** [edge-cases-and-security.md](../verification/edge-cases-and-security.md). Four
      tests are new (injection in search and in a submission, circular links, a file that is not
      UTF-8), and eight of the nine went red with their guard removed. Circular links have no guard:
      nothing follows a link beyond one step. The lying-extension test was green without Tika, because
      its only case was a photo, which re-encoding refuses anyway; a document case and a video case were
      added, and they are red without Tika.
- [x] The full list in [security.md](../architecture/security.md) (moved there from `docs/ai/security.md`
      on 10 Sep), plus a security review
      — check: every item on that list is a test, or points to its unbuilt requirement or its debt
      entry (changed on 30 Sep by the human from "a recorded constraint in
      [constraints.md](../requirements/constraints.md)", since those items are deferred, not
      declined); the review's findings are closed or entered in the debt register, never merely
      discussed
      **Done 30 Sep:** [edge-cases-and-security.md](../verification/edge-cases-and-security.md). Five
      tests were added for rules that had none: `th:utext`, the session cookie, no hash no login, and
      the logging of decisions and of removal. The review covered all of `app/src/main` and found no
      vulnerability; its three hardening notes are DEBT-018 to DEBT-020. The walk found DEBT-021 (no
      query-count case or bound test for search) and a missing comment on a `th:utext`, which was
      added.
- [x] Load check: 100 articles of 500 words, search under 2 seconds
      — check: run on the demo stand, not a developer machine, and the number recorded
      **Done 30 Sep:** `scripts/search-latency.sh` on the compose stand, in a project of its own
      beside the demo stand: 100 searches, maximum 0.023 s, median 0.009 s
      ([search-latency.md](../verification/search-latency.md)). The demo stand is compose on the
      development Mac; OGE's server is not measured, and the script runs there unchanged.
- [~] PostgreSQL profile with Testcontainers
      — check: the full suite passes against PostgreSQL as well as H2, in CI rather than locally
      **Built early, 27 Sep:** `-P postgres` passes all tests locally on PostgreSQL 17.11. Left for
      this step: running it in CI.
      **30 Sep:** the profile no longer passes. Test classes leave rows that break other classes'
      cleanup ([DEBT-022](../tech-debt.md)), and running it showed that an edit's review page answered
      `500` on PostgreSQL. That page is fixed ([FEAT-006](../features/FEAT-006-moderating-a-submission.md)),
      and DEBT-022 was closed the same day: all 369 tests pass on PostgreSQL 17.11 in one run. Left
      for this step: running it in CI.
- [x] `docker-compose.yml` with volumes for media and the search index; one-command start scripts
      — check: media and the index survive `docker compose down` and come back on the next start
      **Started early, 26 Sep:** `Dockerfile` plus `app` and `db` (PostgreSQL 17) services, with the
      `seed` profile. Checked by hand: `/` and the FRRO article return 200, 20 articles are in the
      database, and after `down` and `up` they are still 20. **28 Sep:** `guide-index` is mounted
      (FEAT-007), and the index was checked by hand to survive `down` and `up` without a rebuild.
      Left for this step: `guide-media`, mounted nowhere, since no code writes media yet.
      **Done 29 Sep:** `guide-media` mounted at `GUIDE_MEDIA_ROOT`, and in the demo run on the stand
      ([03-main-flow.md](03-main-flow.md), "Demo run on the compose stand") an uploaded photo
      survived both a restart and a recreated container. The one-command start is `docker compose up
      --build`, in the README; the human decided on 29 Sep that no separate script is needed.
- [x] Demo stand: compose plus the real content
      — check: the mid-demo scenario runs end to end on the stand, from a browser, in one sitting
      **Done 29 Sep:** from empty volumes, PostgreSQL 17.11 and the 30 seed articles, the scenario
      was walked over HTTP and then accepted by the human in a browser in one sitting — see "Demo run
      on the compose stand" in [03-main-flow.md](03-main-flow.md). The red link is shown only in the
      editor's preview, since no seed article carries one.
- [ ] **Meeting with OGE** — show Mr. Thukaram the working stand, record what he says in
      `docs/stakeholder/` and turn it into requirements or constraints
      — check: every point he raises becomes a requirement, a recorded constraint with its reason,
      or a line saying plainly that it is not being done — none is left as a note

## Readiness criterion

A third person stands the application up from the written instructions on a clean machine. Edge
cases are covered by tests rather than by having been tried once.

## Open questions

1. How much of the stakeholder's feedback can still be absorbed at this point without putting the
   final deadline at risk. Decide the cut-off before the meeting, not during it.
