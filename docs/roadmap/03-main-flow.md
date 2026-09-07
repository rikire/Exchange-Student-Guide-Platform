# Phase 3 — The main flow

**Status: not started.** Runs 21 September – 8 October 2026. Ends at the **mid-demo, 9 October**.

## Goal

The demo scenario working on real content: a student searches, reads, follows a wiki link, proposes
an edit with a photo; the moderator approves it and it goes live.

## Steps

Every check below is the same shape: **the acceptance criteria already written under the covering
requirement each become a named test.** Those criteria are in Given/When/Then form in
[functional.md](../requirements/functional.md), so the work of deciding what "done" means was done
in phase 1 and does not need redoing here. Where a step's check says more than that, it is because
the criteria do not reach it.

- [ ] `contribute` — submitting a new article and proposing an edit to an existing one
      — check: a submission made through the form is in the queue and reachable from no public page;
      each covering FR's criteria is a test
- [ ] `moderate` — the queue, approval, rejection; the state machine with its invariants
      — check: every transition has a test, and one test asserts that **no path publishes a
      submission that was not approved** — that is the invariant the whole slice exists for
- [ ] `wikilink` — the link parser, red links, the "what links here" block
      — check: a link to a missing article renders as a red link and one to an existing article
      resolves to its route; the parser's corner cases are derived the way
      [testing.md](../ai/testing.md) describes rather than guessed
- [ ] `taxonomy` — tags, navigation by tag
      — check: browsing a tag returns exactly the published articles carrying it — an unapproved
      submission with that tag must not appear
- [ ] `search` — indexing and querying, both analyzers
      — check: FR-007's four criteria are four tests, including both negative ones (an unapproved
      submission and a rejected one must not appear in results)
- [ ] `media` — upload, type detection from content, safe delivery
      — check: a file whose extension lies about its content is rejected, and media attached to an
      unapproved submission is unreachable by anyone who has not been given its id
- [ ] Admin panel behind the single password
      — check: a test enumerates the admin routes **from the route contract** rather than by hand,
      and asserts each one redirects when unauthenticated; a route added later without a test fails it
- [ ] Thymeleaf generated from Figma; the landing page brought up to the IITM reference
      — check: no literal colour or spacing value in the templates, only tokens; the landing page
      compared against the IITM reference side by side and the differences listed
- [ ] Content: 30 or more articles in the database
      — check: the count comes from the database after import, and searching "FRRO registration"
      returns a relevant article — that is the first step of the demo scenario
- [ ] `ai-tools`: the gap list and rubric generators; the first honest gap list
      — check: `ai-tools gaps` (phase 3) produces a non-empty list containing at least one item
      neither of us would have volunteered
- [ ] Ownership balance check — if it has drifted, the next tasks come from the lighter side
      — check: both members have authored commits in **every** week of this phase, with the hook's
      journal commits excluded — `sh scripts/contribution.sh` reports the two apart

## Readiness criterion

The mid-demo scenario runs end to end on real data; both of us appear in the git history in every
week of the phase; the gap list is written honestly rather than trimmed before the demo.

## Open questions

Raised on 7 September while giving every step a checkable result.

1. **The moderation state machine's states and transitions are not written down.** The check above
   assumes there is a set of them to test. The `ADR`, `ERD entity` and `Route` columns of the feature
   coverage tracker are still empty for every row, so nothing yet says what the states are. This
   blocks `moderate`, and `moderate` is the middle of the demo scenario.
2. **Is this phase's scope survivable?** Six of these eleven steps are whole slices, and the phase
   runs eighteen days from an application that is currently 51 lines of Java. The audit of
   7 September puts the capacity arithmetic and a proposed MoSCoW cut in front of the human; cutting
   scope is not the agent's call, and neither is deciding that no cut is needed.
