# Phase 4 — Hardening, security, deployment

**Status: not started.** Runs 10–30 October 2026.

## Goal

Make it survive real use and real inputs, and make it something a third person can stand up.

## Steps

- [ ] Moved from phase 3, 10 Sep — see the revised milestone plan in
      [01-requirements-design.md](01-requirements-design.md): admin panel behind the single
      password, plus FR-023 (publishing a new article directly) and FR-024 (editing an article
      directly)
      — check: FR-023's four criteria and FR-024's four criteria are eight tests; the admin-route
      enumeration test from the original phase-3 step (every admin route redirects when
      unauthenticated, enumerated from the route contract) is included
- [ ] Moved from phase 3, 10 Sep: FR-019 — providing a rejection reason
      — check: rejecting with a reason stores it, rejecting without one stores none — two tests
- [ ] Moved from phase 3, 10 Sep: FR-006 — backlinks on an article, the "what links here" block
      — check: a published article linking to another appears in its backlink list; a link from an
      unapproved or rejected submission does not — three tests
- [ ] Moved from phase 3, 10 Sep: FR-026 — removing a published article
      — check: after removal the article's route no longer resolves, it drops from search and its
      tags, a wiki link to it becomes red, and it drops from the pinned section if pinned — FR-026's
      four criteria as four tests
- [ ] Moved from phase 3, 10 Sep: FR-016 — downloading a media attachment
      — check: a media asset on a published article downloads; one attached to an unapproved or a
      rejected submission does not — three tests
- [ ] Edge cases: empty query, injection attempt, HTML in article text, duplicate titles, a title
      over 100 characters, circular wiki links, a corrupt import archive, a file whose extension
      lies about its content, a file over the limit
      — check: each of the nine is a named test, and each fails when its guard is removed — nine
      tests that pass against no implementation would be worse than none
- [ ] The full list in `docs/ai/security.md`, plus a security review over the diff
      — check: every item on that list is either a test or a recorded constraint in
      [constraints.md](../requirements/constraints.md) saying why not; the review's findings are
      closed or entered in the debt register, never merely discussed
- [ ] Load check: 100 articles of 500 words, search under 2 seconds
      — check: run on the demo stand, not a developer machine, and the number recorded
- [ ] PostgreSQL profile with Testcontainers
      — check: the full suite passes against PostgreSQL as well as H2, in CI rather than locally
- [ ] `docker-compose.yml` with volumes for media and the search index; one-command start scripts
      — check: media and the index survive `docker compose down` and come back on the next start
- [ ] Demo stand: compose plus the real content
      — check: the mid-demo scenario runs end to end on the stand, from a browser, in one sitting
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
