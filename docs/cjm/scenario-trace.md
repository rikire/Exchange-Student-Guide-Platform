# The mid-demo scenario, traced

The one-sentence scenario from [docs/course/mid-demo.md](../course/mid-demo.md), and again in
[rubric.md](../course/rubric.md):

> A student opens the landing page, searches for "FRRO registration", reads the article, follows a
> wiki link, proposes an edit with a photo, and the OGE moderator approves it.

This file exists because the scenario is graded as "the core workflow runs end to end on at least
one real input", and a scenario that reads well in a sentence can still cross a step no journey
covers. Each step below has to name a journey, a use case, a requirement and a screen. A step that
cannot fill all four columns is a finding, not a blank cell — see
[Step 5](#step-5-has-no-entry-point) below.

Written 10 September, phase 1.

## The trace

| # | Step | Journey | UC | FR | Screen |
|---|---|---|---|---|---|
| 1 | Opens the landing page | [reader](reader.md) | UC-007 | FR-009 | [Landing](../design/screens/Landing.html) |
| 2 | Searches "FRRO registration" | [reader](reader.md) | UC-001 | FR-007 | [Search results](../design/screens/SearchResults.html) |
| 3 | Reads the article | [reader](reader.md) | UC-003 | FR-001 | [Article](../design/screens/Article.html) |
| 4 | Follows a wiki link | [reader](reader.md) | UC-005 | FR-002 | [Article](../design/screens/Article.html) |
| 5 | Proposes an edit, attaching a photo | [contributor](contributor.md) | UC-011 | FR-011 | [Submission form](../design/screens/SubmissionForm.html) |
| 6 | Moderator opens the queue | [moderator](moderator.md) | UC-014 | FR-014 | [Moderation queue](../design/screens/ModerationQueue.html) |
| 7 | Reads the submission and its photo | [moderator](moderator.md) | UC-015 | FR-015 | [Submission review](../design/screens/SubmissionReview.html) |
| 8 | Approves, and it publishes | [moderator](moderator.md) | UC-016 | FR-017 | [Submission review](../design/screens/SubmissionReview.html) |

Every requirement in that column is `must` except FR-011's own screen-level detail, so the scenario
does not depend on anything deferred to phase 4 or later.

**Not in the scenario, deliberately:** the contributor looking their submission up afterwards
(UC-013 / FR-012 / [Submission status](../design/screens/SubmissionStatus.html)). The scenario ends
at the moderator's approval, and the demo is stronger for showing the article live rather than a
status page. It is listed here so that its absence reads as a choice rather than an oversight.

## Step 5 has no entry point

Found while writing this trace, 10 September.

Steps 1–4 chain: the landing page offers search, search results link to the article, the article
renders wiki links. Step 5 does not chain to step 4. The article screen's only actions are
**Download** (on the media asset) and **Report this article**; its header offers **Browse tags**,
**Submit an article** and **Track a submission**. "Submit an article" starts a *new* article
(FR-010) — the submission form is shared between new and edit, but nothing on an article page puts
a reader into its edit mode.

So the scenario's central move — a reader who has just read something wrong proposing a fix — has a
requirement (FR-011), a use case (UC-011) and a screen, and no way to reach it.

Nothing is wrong with FR-011 itself; what is missing is the affordance. It resolves in one of two
places, and which one it is is not a call to make inside a journey document:

- **The article screen** grows a "Propose an edit" action, which makes it a screen change and a row
  in the route contract.
- **The route contract** ([ui-routes.md](../architecture/ui-routes.md), still to be written) carries
  it as a route reachable from the article, and the screen catches up.

Raised for the route-contract step rather than decided here.
