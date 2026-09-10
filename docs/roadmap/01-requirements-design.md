# Phase 1 — Requirements and design

**Status: in progress.** Runs 5–11 September 2026. Ends at the **design document, due 11 September**.

**As of 10 September: 16 of 17 steps done, 1 open.** Closed on 10 Sep — the glossary audit, the CJM
scenario trace, C4 levels 1–3 with the ERD, the ADRs (eleven now, against the eight topics planned),
the four screen gaps the diagram review found, the test plan, the revised milestone plan, the route
contract and its same-day review, and the design document itself. Still open: the ten article
drafts, listed in full below.

**Correction, same edit:** this line previously read "11 of 17 steps done, 6 open" and named the test
plan, the revised milestone plan and the screen gaps as still open — they were already checked off
below when that count was written. Recounted from the checkboxes themselves rather than carried
forward.

## Goal

Decide what the system does and how it is shaped, before code is written under those decisions. The
design document is the deliverable; the repository documents are the source it is assembled from.

## Steps

- [x] `FR` / `NFR` / `CON` in `docs/requirements/` for the product as agreed
      — check: every `FR` has a status and every `CON` a rationale. Done 7 Sep: 25 `FR` (FR-001–025,
      every Feature coverage tracker row has an id), 5 `NFR` (NFR-001–005), 7 `CON`; every `FR`
      carries Status/Priority, every `CON` a Rationale.
      **Corrected 10 Sep.** This line read "4 `NFR` (NFR-001–004), 5 `CON` (CON-001–005)". It was
      wrong on the day it was written, not merely out of date: NFR-005 and CON-006 landed in that
      same 7 September commit and were counted by eye afterwards. `ai-tools count` is the number
      now — 26 `FR`, 5 `NFR`, 7 `CON`, 26 `UC` as of 10 Sep, the `FR` having grown by FR-026 on
      9 Sep. Counting by hand is what `Requirements.java` exists to stop.
- [x] Glossary — one vocabulary for article, submission, revision, tag, media asset
      — check: every term the requirements use is defined once, and no concept appears under two
      names across `functional.md` and the journeys. Done 10 Sep: audited against every `FR`, `NFR`
      and `CON`, the three journeys and all thirteen screens. Seven terms the requirements already
      relied on were undefined — submission number, submission type, submission status, rejection
      reason, pinned article, and the two timestamps — and two concepts were running under two names
      each. Both settled rather than left to whichever name a document used: **media asset** over
      *attachment* (which survives as the on-screen label only), and **submission** over *proposal*,
      which FR-011 used throughout while FR-017 and FR-020 did not. The reverse pass found no dead
      term: all thirteen original entries are used somewhere. `Moderator` was reworded against
      CON-001 — it read like a stored identity and is a role, reached through the shared password of
      [ADR-0009](../architecture/adr/ADR-0009-admin-authentication.md).
- [x] CJM for three roles: reader, contributor, OGE moderator
      — check: the mid-demo scenario traces through the reader and contributor journeys end to end.
      Done 10 Sep: [cjm/scenario-trace.md](../cjm/scenario-trace.md) traces all eight steps, each
      naming a journey, a use case, a requirement and a screen, and is linked from all three journey
      files. The 26 use cases already existed; what did not was any check that the scenario actually
      crossed them. **What this did not close:** the trace found that step 5 does not chain — nothing
      on the article screen reaches "propose an edit", though FR-011, UC-011 and the shared form all
      exist. That is gap 2 in the screens step below, not a hole in the journeys.
- [x] C4 levels 1–3 and the ERD in PlantUML — check: the diagram script renders them. Done 10 Sep:
      four sources in [docs/diagrams/src/](../diagrams/src/), rendered by `scripts/diagrams.sh` into
      `docs/diagrams/out/`, which is committed so the architecture documents can embed it.
      [overview.md](../architecture/overview.md) and [data-model.md](../architecture/data-model.md)
      replace their placeholders. PlantUML is fetched into a gitignored cache against a pinned
      checksum rather than added to a pom — its artifact is GPL and this repository is MIT. Reviewed
      against every `FR`, `NFR`, `CON` and all thirteen screens before committing; five errors found
      and fixed, including a level-3 diagram that showed no slice touching the database (inverting
      [ADR-0002](../architecture/adr/ADR-0002-vertical-slices-on-spring-modulith.md)) and a
      cardinality that allowed only one pending edit per article.
      **What this did not close:** three schema decisions are recorded as open in `data-model.md`
      rather than taken, since entity fields and cardinality are the human's call — FR-026 removal
      has no representation, `pinned` cannot order two pinned articles, and where the rate limit is
      counted. Two of the three block gaps 3 and 4 of the screens step below.
- [x] ADRs: slices and Modulith; moderation and the version-history groundwork; search and
      multilingual content; taxonomy; media storage and upload security; export format;
      abuse handling without accounts; article content format and injection safety (added 5 Sep,
      while writing FR-001 — [ADR-0001](../architecture/adr/ADR-0001-article-body-format.md), decided)
      — check: each has at least two genuinely considered options. Done 10 Sep: all eight topics
      covered, ADR-0002 to ADR-0008 written plus ADR-0001, each with two or three options costed and
      a named deciding factor. Two more came out of auditing where decisions had been filed —
      ADR-0009 (the admin area's single shared password) and ADR-0010 (bounded reads) — both of
      which had been sitting in `docs/ai/security.md`, the assistant's instruction directory, since
      3 and 5 September. The security architecture moved to
      [docs/architecture/security.md](../architecture/security.md), and `constraints.md`'s routing
      rule, which had pointed architecture into `docs/ai/`, was corrected.
      **Recorded honestly rather than quietly:** ADR-0002 to ADR-0008 were written after the fact,
      in one sitting, from a list of decisions someone remembered to look for — which cannot reveal
      a decision nobody listed. Each header carries `Decided` and `Recorded` separately, and
      [adr/README.md](../architecture/adr/README.md) holds both the objection and the audit of what
      still lives outside that directory. Two entries in that audit remain open: Modulith's event
      registry, and the tenth slice. The wider pass this argues for is
      [IB-004](../ai/instruction-backlog.md).
- [x] `docs/architecture/ui-routes.md` — the route contract
      — check: every row names the requirement it serves, and the `Route` column of the feature
      coverage tracker is filled from it for every `must` feature. Done 10 Sep: twelve routes
      written, covering all 12 `must` features (some rows serve more than one FR — FR-002/FR-004
      render inline on `GET /articles/{title}` rather than owning a route, and FR-003 is the `body`
      field on the two submission POSTs); the tracker's `Route` column above is filled from it.
      **Gap found the same day, closed the same day:** `GET /submissions/{number}/confirmation` —
      needed for FR-010 and FR-011's "shown a submission number" — had no sketched screen;
      `ui-routes.md` had recorded this rather than pointing it at FR-012's lookup screen, which is a
      different (and `could`-priority) thing. [Submission confirmation](../design/screens/SubmissionConfirmation.html)
      was sketched and added to the flow canvas the same day — see `ui-routes.md` and
      `design/README.md`. The 14 `should`/`could` features are still listed unrouted in
      `ui-routes.md`, added when phase 4 picks each one up.
      **Reviewed the same day it was written, and it did not survive the reading.** Ten findings
      against `functional.md`, `glossary.md`, `data-model.md`, `erd.puml`, `architecture/security.md`,
      ADR-0001/0006/0009 and the screens. Coverage held — all 12 `must` FRs routed, slice names
      matching the package map. Two findings were decisions nobody had taken: the submission number
      had no format, so five sketched screens had settled it as a counter, which turns the
      confirmation route into an enumeration of every submission ever received
      ([ADR-0011](../architecture/adr/ADR-0011-submission-number-format.md),
      [NFR-006](../requirements/non-functional.md)); and media had no route at all, though FR-001 and
      FR-015 both need one (`GET /media/{id}`, and a dated amendment to
      [ADR-0006](../architecture/adr/ADR-0006-media-storage-and-upload-security.md)). Routing media
      exposed an eleventh thing neither document could show alone — FR-016 says an asset on an
      unapproved submission is "not returned" while `security.md` said it was "reachable only by an
      unguessable id", which are different systems; resolved in favour of the requirement.
      The other eight were corrections to the contract: `/moderate/login` caught by its own gating
      rule, no CSRF token on any row, `401` used but never declared, the edit POST covering only the
      race rather than FR-011's unconditional rule, no code for a blank `q`, a login template naming
      a screen that did not exist, an unstated path-shape exception, and this tracker's own `Route`
      column missing the confirmation route. All ten are recorded in `ui-routes.md` under "What the
      review of 10 September found" rather than silently fixed.
      **Worth keeping:** the login-template gap is the same class as the confirmation gap the file
      had already found and closed — two rows below it, in the row added last. Finding one gap is not
      the same as re-reading the table.
- [x] **Decided here, not earlier:** ~~what the landing page contains beyond pinned items and
      search~~ — resolved 6 Sep, FR-009: recently added articles + a tag list, pinned articles shown
      first when any exist; ~~media quotas per file, per submission and for the volume~~ — resolved
      7 Sep, NFR-001: configurable, defaults 10 MB/image, 20 MB/document, 200 MB/video, 20 GB volume;
      ~~the allowed file types~~ — resolved 7 Sep, CON-006: images, video, documents and audio only.
- [x] Design reference from the IITM sites, into `docs/design/reference.md`
      — check: it names the specific pages looked at and what is being taken from each, rather than
      describing "the IITM style" in general. Done 7 Sep, extended 9 Sep: both fetches dated and
      cited, and a separate section names what was chosen in the design system rather than observed
      on the sites (typeface, derived neutrals, status-pill colors).
- [x] Draft screens with `/design`: landing, article, search results, submission form, queue
      — check: each screen shows the states that actually occur — empty search results, a queue with
      nothing in it, a rejected submission — and not only the path where everything works. Done
      10 Sep: eleven screens exist (the five listed plus tag browse, submission status, submission
      review, report inbox, and two added 10 Sep — an empty search-results state and an empty-queue
      state — see [docs/design/screens/](../design/README.md#canvases)); the rejected-submission
      state was already sketched in `SubmissionStatus.dc.html`. Plain per-screen HTML lives under
      `docs/design/screens/`, generated from the `.dc.html` sources.
      **Updated 10 Sep, second pass — every `FR` with a UI now has a `Screen`:** the tracker had three
      gaps (FR-005, FR-025, FR-026, all "not sketched yet"). Added two screens: a create-from-red-link
      invite page (FR-005), and a moderator "Homepage & article administration" screen combining
      pin/unpin (FR-025) and remove (FR-026), since both are the same actor acting on the same
      published article. Thirteen screens total. The one `FR` still without a `Screen`, FR-020
      (revision retention), stays that way deliberately — it names no reader- or moderator-facing
      state, only what the system stores when an edit is approved; see the tracker note on that row.
- [x] **Added 10 Sep, from the diagram and data-model review.** Screens brought back in step with
      the requirements — four gaps, listed with their slice in
      [docs/design/README.md](../design/README.md#screens-now-out-of-step-with-the-requirements).
      All four fall in slices claimed `[~] abdirakhim` in the Stage 3 table below; that table stays
      the claim, the list is only the finding
      — check: a reader gets from the article screen to the edit form without typing a URL; every
      field an `FR` says a contributor supplies has an input on the form that captures it; and the
      two screens that draw an action the schema does not support (pin order, remove) either match a
      decided model or are still waiting on it, said out loud rather than redrawn over the gap.
      Both the `.dc.html` source and the regenerated `screens/*.html` change together, or they have
      drifted. Done 10 Sep: gaps 1–2 fixed directly on the `Article`, `SubmissionForm` and
      `SubmissionReview` screens (source and plain HTML together); gaps 3–4 resolved by deciding the
      two schema questions below rather than by redrawing — neither screen contradicted the decided
      model, so neither needed a redraw once it landed. `docs/design/README.md`'s gap table and
      `docs/architecture/data-model.md` record both. The live "Flow overview" Artifact is re-seeded
      from the updated `.dc.html` sources and republished — see `docs/design/README.md`.
      — was blocked in part: gaps 3 and 4 waited on open decisions 1 and 2 in
      [data-model.md](../architecture/data-model.md), which were the human's to take. Resolved 10 Sep:
      `removed_at` (soft delete, decision 1) and `pinned_at` (pin order, decision 2), both nullable
      timestamps on `article` — see `data-model.md` and the regenerated `erd.svg`.
- [x] Test plan: at least one test per slice — check: the plan names the test, not just the module.
      Done 10 Sep: drafted from the `GIVEN`/`WHEN`/`THEN` criteria already agreed in
      [functional.md](../requirements/functional.md) — a starting point for whoever holds each slice
      to adjust in Stage 5's joint reconciliation ("Whoever holds a slice names its test; combine
      here"), not a final answer.

      | Slice | Named test | From |
      |---|---|---|
      | `home` | `LandingPageTest.pinnedArticlesShownBeforeRecentWhenAnyExist` | FR-009 |
      | `articleview` | `ArticleControllerTest.unapprovedSubmissionRouteDoesNotResolve` | FR-001 |
      | `search` | `SearchServiceTest.unapprovedSubmissionExcludedFromResults` | FR-007 |
      | `taxonomy` | `TagBrowseTest.unapprovedSubmissionExcludedFromTagResults` | FR-008 |
      | `contribute` | `SubmissionControllerTest.newSubmissionEntersQueueAndIsNotPubliclyReachable` | FR-010 |
      | `moderate` | `ModerationServiceTest.noPathPublishesAnUnapprovedSubmission` | The invariant named in [03-main-flow.md](03-main-flow.md)'s `moderate` step, covering FR-017/FR-018 |
      | `media` | `MediaUploadTest.fileWhoseExtensionLiesAboutContentIsRejected` | Named in [03-main-flow.md](03-main-flow.md)'s `media` step |
      | `wikilink` | `WikiLinkRendererTest.linkToMissingArticleRendersRed` | FR-004 |
      | `backup` | `BackupServiceTest.exportWipeImportProducesIdenticalDatabase` | Named in [02-skeleton.md](02-skeleton.md)'s `backup` step |
      | `report` | `ReportControllerTest.reportWithoutMessageIsRejected` | FR-021 |

      Ten slices, one test each, from the acceptance criteria phase 1 already agreed — not the full
      suite each slice needs (phase 3's steps name the rest), the minimum this checklist item asks
      for: a named test, not just a module.
- [x] Revised milestone plan, risks and plan B tied to seams that exist in the code
      — check: the revision says what moved or was cut and why — a plan reissued unchanged is not a
      revision; each risk names the signal that would tell us it is happening, and a plan B that
      could be carried out in the days actually remaining. Done 10 Sep.

      **The seam that exists in the code today:** none beyond the application skeleton
      (`GuideApplication.java`, `ApplicationSmokeTest`, `ModularityTest`) — no slice package exists,
      no persistence. Phase 2 has not started. The risk this revision addresses is phase 3's
      capacity, not a seam already in place.

      **What was cut, and why.** Phase 3 packs six whole slices (`contribute`, `moderate`,
      `wikilink`, `taxonomy`, `search`, `media` — 10 of the 12 `must` features) into its fixed
      18 days (21 Sep – 8 Oct, [03-main-flow.md](03-main-flow.md)), starting from the skeleton
      above. That capacity question was raised on 7 September and left for the human to decide
      (open question 2 there). Resolved 10 Sep: six `should`/`could` items move from phase 3 to
      phase 4, leaving each slice's `must`-priority acceptance criteria as the phase-3 exit bar.

      | Moved | Slice | Priority | Why it can wait |
      |---|---|---|---|
      | FR-023, FR-024 (direct publish, bypassing the queue) + the admin-panel step | `contribute` | could | The panel exists only to serve these two `FR`s; without them it has no purpose in phase 3 |
      | FR-019 (a rejection reason) | `moderate` | could | Rejection works as an action without a reason field |
      | FR-006 (backlinks / "what links here") | `wikilink` | could | The parser and red links (`must`, FR-002/003/004) don't depend on it |
      | FR-026 (removing a published article) | `moderate` | should | A moderator maintenance action, outside the mid-demo scenario |
      | FR-016 (downloading a media attachment) | `media` | should | Attaching media to a submission (`must`, via FR-010/011) stays; a dedicated download endpoint does not |
      | FR-020 (version-history groundwork) | `moderate` | should | Already deferred — decided 7 Sep, open question 1 above; listed here for completeness, not a new cut |

      Recorded in the phases themselves: [03-main-flow.md](03-main-flow.md) narrows the
      `contribute`, `moderate`, `wikilink` and `media` steps to their remaining `must` scope, moves
      the admin-panel step out, and closes open question 2; [04-hardening.md](04-hardening.md)
      gains the six moved items as new steps, each keeping the GIVEN/WHEN/THEN acceptance criteria
      already written in [functional.md](../requirements/functional.md) as its check.

      **Risk and signal.** Six whole slices may still not fit 18 days even after the cut. Signal: at
      the midpoint of phase 3, fewer than three of the six slices above have their `must`
      acceptance-criteria tests green.

      **Plan B.** If the signal fires, `taxonomy` — the one `should`-priority slice still in
      phase 3 — loses its own screen: tag storage and filtering stay (`search` and other `must` FRs
      depend on tags existing), but the dedicated tag-browse screen (FR-008) moves to phase 4
      alongside the six already moved. No further slice is cut without returning to the human —
      removing a slice entirely changes the mid-demo scenario itself, which is a joint call, not a
      mechanical extension of this one.
- [x] Assemble `docs/course/design-doc.md` and produce the PDF (2–4 pages)
      — check: every rubric row for this stage points at a section that exists and says what the
      criterion asks for; a section that exists but is a heading counts as missing. Done 10 Sep:
      [design-doc.tex](../course/design-doc.tex) → `design-doc.pdf`, 4 pages, with `design-doc.md`
      as a pointer rather than a second prose copy — the convention
      [proposal.md](../course/proposal.md) already set. Five sections: the slice boundary and what
      enforces it, the test plan's ten named tests, the milestone plan, risks with a signal each,
      and one unscored section on how the decisions were arrived at.
      **Two variants were built and compared** rather than choosing on description: one spending all
      four pages on the four scored rubric rows, one trading compression in those sections for the
      process section. The second was chosen; what it gave up is listed in its commit.
      **The `must` figure needed a build change:** `pdflatex` cannot embed SVG and no converter is
      installed, so `scripts/diagrams.sh` now emits PNG beside SVG from the same pinned PlantUML.
      **Not claimed:** the architecture rubric row is `[~]`, not `[x]`. The modules are declared and
      `ModularityTest` verifies them, but the packages are empty, so no interface between two slices
      exists yet and the C4 figure is drawn from `diagrams/src/` rather than generated from code by
      Modulith. The reasoning is in [rubric.md](../course/rubric.md) rather than left to be inferred
      from a tick.
- [ ] Content: 10 or more articles drafted — absorbs the article-format-and-drafts item moved from
      phase 0 (4 Sep, see [00-init.md](00-init.md)); the seed front-matter shape is decided here,
      together with the glossary and ERD, not ahead of them
      — check: 10 or more drafts exist in the seed format, and one of them is a full article on FRRO
      registration — that is the first step of the mid-demo scenario, so it cannot be a placeholder
- [x] **Added 9 Sep.** The course's scoping feedback of 28 August recorded, and the proposal revised
      in answer to it — [scoping-feedback.md](../course/scoping-feedback.md),
      [proposal.tex](../course/proposal.tex) → `proposal.pdf`, 5 pages
      — check: the feedback is in the repository verbatim, every verb it names maps to a requirement
      id, and the proposal's changed sections are listed rather than described as "updated". Done
      9 Sep; FR-026 / UC-026 came out of it.
- [x] **Added 9 Sep, closed the same day.** The reply to that feedback is sent
      — check: `scoping-feedback.md` names a send date instead of "not yet sent". Sent 9 Sep with
      revision 2 of the proposal attached, eight days after the 1 September deadline. What is *not*
      closed by this: approval was made conditional on the reply, the reply has landed and no answer
      has come back, so the design document states the approval as outstanding rather than assuming
      it. Any answer is pasted into `scoping-feedback.md` when it arrives.
- [x] **Added 9 Sep, from checking the design record.** Two defects found in
      [docs/design/](../design/): the canvas URL in the Canvases table does not resolve from the
      publishing account, and the typeface the design system actually uses (`Public Sans`) together
      with its derived neutrals and status green appear in the canvas but in no document
      — check: the table's link either resolves or says plainly that it does not, and every colour
      and face used in `Main.dc.html` is named in [reference.md](../design/reference.md). Done 9 Sep
      (second pass): the "does not resolve" report turned out to be an access-scope read from a
      different account — checked again from the publishing account and all three canvases resolve;
      `README.md` now says so instead of "does not resolve". The colour audit itself had missed two:
      `#EDF2EA`/`#FBEAE8` (the status-pill backgrounds) were in `Main.dc.html` but not in
      `reference.md` — added.

## Readiness criterion

The design document is submitted, and `/course-check design` finds evidence for all five marks with
no row resting on a template.

## Open questions

1. ~~How much of the version-history groundwork to commit to in the ADR, and whether its `should`
   priority conflicts with needing to start in phase 2 or lose the data.~~ Resolved 7 Sep: accept the
   gap. There is no deployment after phase 1, so an article approved before FR-020's retention
   groundwork lands (phase 4–5, per its `should` priority) simply has no history — a real but
   low-stakes loss given the project's own timeline, not worth forcing into phase 2 ahead of its
   priority.
2. ~~Whether the landing page's pinned items are curated by the moderator or derived from activity.~~
   Resolved 5 Sep: moderator-curated, via direct homepage editing (UC-022, `home`).
3. ~~What happens when a new submission's title collides (case-insensitively) with an existing
   published article's title.~~ Resolved 6 Sep: the submission is rejected at the point of
   publishing, and the contributor is offered a link to propose an edit to the existing article
   instead, or the option to change their own title. See FR-010.

## How the two of us work through this phase

Three earlier drafts of this split by document type (one of us writes requirements, the other writes
journeys/design) kept surfacing the same problem: NFR separated from the design it constrains, screens
separated from the data model they need, a moderation ADR separated from the queue screen that
visualizes it. The fix is already written down in
[docs/ai/architecture-rules.md](../ai/architecture-rules.md): this project's unit of parallel work is
the **vertical slice** — `home`, `articleview`, `search`, `taxonomy`, `contribute`, `moderate`,
`media`, `wikilink`, `backup` — because a slice is "both the unit of work someone picks up and the
unit ownership is measured against." Splitting this phase the same way means each of us owns a
feature's requirement, its architecture decision, and its screen together, instead of trading files
across a document-type boundary.

Per [docs/ai/collaboration.md](../ai/collaboration.md) §6 — no assigned areas, ownership measured from
git history — nobody is pre-assigned to a slice below. It's a shared pool, claimed as work starts.

### Claiming convention

Reuses the marks already defined in [docs/ai/roadmap.md](../ai/roadmap.md) (`[ ]` / `[~]` / `[x]`) as
the claim mechanism:

- **Before starting** a slice or item, mark it `[~] <your id>` (id from
  [docs/team/members.yml](../team/members.yml) — `mikhail` or `abdirakhim`) in your own small commit,
  then start. First to claim gets it.
- **On finishing**, mark `[x] <your id>`.
- If you both reach for the same thing, say so and pick — cheaper before either has started.
- Aim for roughly even slice counts by the end of stage 3. If claiming lands lopsided, say so and
  rebalance before stage 4 — even workload, not identical process.

**Untested as of 7 September:** `[~]` has never been used, here or anywhere else in the repository,
and every cell in the slice table below is still unclaimed. That is not a failure — stages 1 and 2
are joint, so there has been nothing to claim. It is worth saying because stage 3 is the first time
two people work in parallel, and a coordination mechanism that has never run is a mechanism nobody
has found the problem with yet. Claim the first slice in its own commit, and check that the other
person sees it, before relying on it for the second.

### Stages

```
1. Feature ideation & prioritization (joint)  -> check: every feature has a slice and a phase
2. Design system (joint, short)               -> check: both people's screens read as one system
3. Rough prototype (claimed, per slice)        -> check: exploratory, used to find real data/routes
4. Formalize (claimed, per slice)              -> check: FR/ERD/ADR/CJM written from what stage 3 found
5. Joint reconciliation + polish (joint)       -> check: each cross-slice pair agrees with itself
6. Assembly (joint)                            -> check: /course-check design finds all 5 marks
```

Article drafts run alongside all six — no dependency on any of them, and no claiming needed (separate
files can't collide) beyond a shared topic list so nobody duplicates one.

### Standing rule: the feature list is never frozen

Stage 1 produces a first cut, not a locked list — this file's own steps are not a contract, and change
as things become visible. Sketching a screen (stage 3) or writing full FR text (stage 4) routinely
surfaces that a feature is unneeded, missing, too big for this phase, or belongs to a different slice
than assumed.

- When that happens, say so to the other person immediately — don't hold it for stage 5.
- Update the shared feature list / FR skeleton in the same sitting, then continue.
- A change that moves a feature's phase, cuts it, or changes MVP scope is still a joint call (the
  "dividing test" in collaboration.md §1) — flag and agree, don't silently redraw the boundary alone.

### 1. Feature ideation & prioritization — joint, first pass

Grounded in what's already committed: the stakeholder acknowledgement
([docs/stakeholder/acknowledgement.md](../stakeholder/acknowledgement.md)) commits the project to "a
centralised resource... covering administrative procedures, campus facilities, essential services and
practical life at IITM and in Chennai," as a community-editable wiki with moderation — but explicitly
*not* to a fixed feature list. That list is this phase's job, and it's a whole-product call, not
something to split.

**Process:** each of you lists candidate features independently first, against the three CJM roles —
what a reader needs, a contributor needs, a moderator needs. Merge the two lists, then as a pair:

- [x] Sort every feature into a slice — a feature that doesn't fit is a sign the slice list itself
  needs revisiting, which is stop-and-ask territory, not a solo call. Done 5 Sep: three features
  didn't fit the original nine cleanly (reporting an article, submission status lookup, the
  moderator's direct-publish path); resolved by adding a tenth slice, `report`
  ([architecture-rules.md](../ai/architecture-rules.md)), and folding the other two into
  `contribute`. See the Feature coverage tracker above for the full assignment.
- [x] Prioritize: MVP now (this phase's design, phase 2–3's build) vs. later (phase 4–5) vs.
  explicitly out — the "out" pile becomes CON entries with a rationale, not silently dropped. Done
  5 Sep with MoSCoW: 11 must, 6 should, 6 could, 0 won't — see the Priority column in the Feature
  coverage tracker above. Nothing was cut outright, so no constraint entry was needed.
- [x] Map MVP features onto the existing phase boundaries
  ([02-skeleton.md](02-skeleton.md), [03-main-flow.md](03-main-flow.md)) — this is what "revised
  milestone plan" (step above) actually revises. Done 5 Sep: all 11 `must` features already trace to
  a step named in phase 2 or phase 3 — no edit to either file was needed. The `should`/`could` items
  are not yet placed in phase 4 ([04-hardening.md](04-hardening.md)); Open question 1 above flags the
  one case (version-history groundwork) where that can't just wait.

What falls out of this, recorded rather than written as a separate task: the FR skeleton (titles +
one-line scope, tagged by slice), first-cut CON with rationale, first-cut
[docs/stakeholder/acceptance.md](../stakeholder/acceptance.md) (protected — humans decide the
content), glossary terms as they come up, and the two open questions above.

### Feature coverage tracker

A checklist like "§4: FR, full text, for the slice's features" hides how much of a slice is actually
covered — one FR entry checks the whole row. This table is the real unit of progress: **one row per
feature**, checked off per column as that specific feature gets each artifact, not per slice or per
document.

Seeded once stage 1 produces the feature list — populated by the humans doing stage 1, not written in
advance — and grows or gets re-tagged as the standing rule above kicks in.

| Feature | Priority | Slice | FR id | ADR | CJM step | ERD entity | Route | Screen |
|---|---|---|---|---|---|---|---|---|
| Full-text search across articles | must | `search` | FR-007 | | UC-001 | | `GET /search?q={query}` | Search results (canvas) |
| Browse/filter articles by tag | should | `taxonomy` | FR-008 | | UC-002 | | | Tag browse (canvas) |
| Read a published article (body, tags, media, wiki links) | must | `articleview` | FR-001 | | UC-003 | | `GET /articles/{title}`, `GET /media/{id}` | Article (canvas) |
| Download a media attachment | should | `media` | FR-016 | | UC-004 | | _the bytes are routed as `GET /media/{id}` for FR-001/FR-015; the reader-facing download (`Content-Disposition: attachment`) is not_ | Article (canvas) |
| Parse and render `[[wiki links]]` | must | `wikilink` | FR-002 | | UC-005 | | `GET /articles/{title}` (inline rendering, no route of its own) | Article (canvas) |
| Backlinks on an article | could | `wikilink` | FR-006 | | UC-006 | | | Article (canvas) |
| Landing page: pinned items + search | must | `home` | FR-009 | | UC-007 | | `GET /` | Landing (canvas) |
| Red-link rendering | must | `wikilink` | FR-004 | | UC-008 | | `GET /articles/{title}` (inline rendering, no route of its own) | Article (canvas) |
| Creating an article from a red link | could | `wikilink` | FR-005 | | UC-023 | | | Create-from-red-link invite (canvas) |
| Report an article | could | `report` | FR-021 | | UC-009 | | | Article (canvas) |
| Submit a new article (with optional media attachment and suggested tags) | must | `contribute` | FR-010 | | UC-010 | | `GET /submit`, `POST /submissions`, `GET /submissions/{number}/confirmation` | Submission form (canvas) |
| Propose an edit to an existing article (with optional media attachment and suggested tags) | must | `contribute` | FR-011 | | UC-011 | | `GET /articles/{title}/edit`, `POST /articles/{title}/edits`, `GET /submissions/{number}/confirmation` | Submission form (canvas) |
| Write `[[wiki links]]` inline while composing a submission | must | `wikilink` | FR-003 | | UC-012 | | `POST /submissions`, `POST /articles/{title}/edits` (the `body` field) | Submission form (canvas) |
| Look up a submission's status by its number | could | `contribute` | FR-012 | | UC-013 | | | Submission status (canvas) |
| Abuse handling without accounts (rate limiting + CAPTCHA) | should | `shared/security` | FR-013 | | _(none — not a use case)_ | | | Submission form (canvas) |
| Moderation queue: list pending submissions | must | `moderate` | FR-014 | | UC-014 | | `GET /moderate/queue` | Moderation queue (canvas) |
| Review a submission's full text and attachments | must | `moderate` | FR-015 | | UC-015 | | `GET /moderate/submissions/{number}`, `GET /media/{id}` | Submission review (canvas) |
| Approve a submission (adjust/finalize tags, publish) | must | `moderate` | FR-017 | | UC-016 | | `POST /moderate/submissions/{number}/approve` | Submission review (canvas) |
| Reject a submission | must | `moderate` | FR-018 | | UC-017 | | `POST /moderate/submissions/{number}/reject` | Submission review (canvas) |
| Providing a rejection reason | could | `moderate` | FR-019 | | UC-024 | | | Submission review (canvas) |
| Version-history groundwork: retain each approved revision | should | `moderate` | FR-020 | | UC-018 | | | — not sketched (no UI of its own) |
| Closing a report (was "Handle a reported article" — narrowed 7 Sep: correcting the article reuses the existing direct-edit capability, closing is the only new action) | could | `report` | FR-022 | | UC-019 (via UC-021 + UC-025), UC-025 | | | Report inbox (canvas) |
| Write and publish a new article directly, bypassing the queue | could | `contribute` | FR-023 | | UC-020 | | | Submission form (canvas) — layout only; its copy ("goes to OGE for review") is the contributor path's, not yet a moderator variant |
| Edit and publish an article directly, bypassing the queue | could | `contribute` | FR-024 | | UC-021 | | | Submission form (canvas) — same caveat as FR-023 |
| Edit the homepage, including what's pinned | could | `home` | FR-025 | | UC-022 | | | Homepage & article administration (canvas) |
| Remove a published article (added 9 Sep from the course's scoping feedback — see [scoping-feedback.md](../course/scoping-feedback.md)) | should | `moderate` | FR-026 | | UC-026 | | | Homepage & article administration (canvas) |

MoSCoW test used: **must** = the system does not function as this product without it; **should** = a
real, non-cosmetic loss if missing, but the system still works; **could** = low impact if missing,
easily deferred. Agreed 5 Sep: 11 must, 6 should, 6 could, 0 won't across the original 23 — nothing
was cut outright, so no constraint entry was needed for that pass.

**Updated 6 Sep, while re-deriving FR-004:** red-link rendering raised from `could` to `must` (same
priority as the wiki-link rendering it's part of), and a new feature was found — creating an article
directly from a red link — added as `could`. Now 12 must, 6 should, 6 could, 24 features total.

**Updated 7 Sep, while re-deriving FR-018:** a new feature was found — giving a reason when
rejecting a submission — added as `could`. Now 12 must, 6 should, 7 could, 25 features total.

**Updated 7 Sep, while re-deriving FR-021:** reporting an article lowered from `should` to `could` —
low impact if missing, since a moderator can still notice a problem article on their own. Now
12 must, 5 should, 8 could, 25 features total.

**Updated 7 Sep, while re-deriving FR-022:** "Handle a reported article" narrowed to "Closing a
report" — deciding to leave/correct/take down an article on its own turned out to just be the
moderator's existing direct-edit capability (UC-021), reused rather than duplicated; the only new
action is closing the report. Lowered from `should` to `could` to match. Now 12 must, 4 should,
9 could, 25 features total.

**Updated 9 Sep, from the course's scoping feedback of 28 August:** that email names three verbs —
information can be "added / edited / removed based on the consent of the community" — and we had
written two. Removing a published article was covered by no requirement: `remove` appeared only for
taking a rejected submission off the queue, closing a report and unpinning. Added as FR-026 /
UC-026, `should`, slice `moderate`. The 7 Sep narrowing above is the near-miss: taking an article
down was folded into the moderator's direct-edit capability, and editing is not removing. Now
12 must, **5 should**, 9 could, **26 features total**. See
[docs/course/scoping-feedback.md](../course/scoping-feedback.md).

`backup` (export/import) has no row above: it is not a use case of Reader, Contributor or Moderator —
it is a DevOps/deployment concern, justified by the already-agreed export-format ADR and the
exportability NFR rather than by a CJM step. Recorded here so its absence from this table reads as a
decision, not an oversight.

- A cell is checked only when that specific feature's piece of that artifact exists — not when the
  artifact file has *some* content.
- "ADR" is slice-level, not feature-level — a feature's row can point at its slice's ADR (§4 mapping)
  rather than repeating it per row.
- This table is what "is the slice done" actually means: every feature tagged to that slice has every
  applicable column checked, not "the FR file has text in it."

### 2. Design system — joint, short

Either of you drafts a first pass from the IITM reference (tokens: color, type scale, spacing, and the
handful of components every screen reuses), the other reviews and adjusts in the same sitting.

### 3. Rough prototype — claimed, one slice at a time

Claim a slice with `[~] <id>` before sketching it. Sketch its prioritized features as rough screens
with `/design`. This is a thinking tool, not the deliverable — it surfaces what data and routes are
actually needed before either gets written up formally.

| Slice | Claim |
|---|---|
| `contribute` | [~] abdirakhim |
| `moderate` | [~] abdirakhim |
| `media` | [~] abdirakhim — light touch, sketched inside the Article and Submission Form screens |
| `backup` | [ ] — no screen needs it; export/import has no UI in the current screen list |
| `home` | [~] abdirakhim |
| `articleview` | [~] abdirakhim |
| `search` | [~] abdirakhim |
| `taxonomy` | [~] abdirakhim |
| `wikilink` | [~] abdirakhim — light touch, sketched inside the Article and Submission Form screens |
| `report` | [~] abdirakhim — added 6 Sep after this table was written; missing until now |

### 4. Formalize — claimed, per slice, now genuinely parallel

Whoever prototyped a slice in stage 3 formalizes it by default — context is fresh — but that's a
default, not a rule. For each *feature* held (not each slice in bulk), write up what stage 3's sketch
found and check the matching cell in the **feature coverage tracker** above as you go — not the file
as a whole:

| Artifact | Where | Tracker column |
|---|---|---|
| FR, full text, for one feature | `docs/requirements/functional.md` | FR id |
| The feature's CJM step | `docs/cjm/*.md` | CJM step |
| The feature's entity/fields, as your own rows in the shared ERD | `docs/architecture/data-model.md` | ERD entity |
| The feature's route, as your own row in the shared route contract | `docs/architecture/ui-routes.md` | Route |
| The feature's screen, polished from stage 3's rough sketch | — | Screen |

Slice-level, done once per slice rather than per feature: the slice's ADR (below), carrying any NFR
it implies, and the slice's component in C4 level 3.

| Slice | ADR |
|---|---|
| `moderate` | [ADR-0003](../architecture/adr/ADR-0003-moderation-and-revision-storage.md) — moderation and version-history groundwork |
| `media` | media storage and upload security — carries NFR: upload size limits |
| `backup` | export format — carries NFR: exportability |
| `contribute` | abuse handling without accounts |
| `search` | search and multilingual content — carries NFR: search latency, multilingual content |
| `taxonomy` | taxonomy |

`data-model.md` and `ui-routes.md` are shared files — touch only the rows for slices you hold.

### 5. Joint reconciliation + polish — joint, by slice pair

Sit down together once most claims have landed:

| Pair | Question |
|---|---|
| `moderate` vs. `articleview` | Does articleview's "published" filter match the states the moderation ADR defines? |
| `media` vs. `articleview` | Does the article screen reference media the way the media ADR delivers it? |
| `contribute` vs. `articleview` (wikilink) | Same `[[link]]` syntax entered and rendered? |
| ADR: slices and Modulith | Nobody's slice alone. One drafts from what architecture-rules.md already documents, the other reviews before it's marked decided. |
| C4 levels 1–2 | Joint — whole-system view, not one slice |
| Test plan | Whoever holds a slice names its test; combine here |

Then polish the rough screens to match the design system and the now-formalized routes/ERD.

### 6. Assembly — joint

Revised milestone plan/risks/plan B, assemble the design doc + PDF, run `/course-check design`.

### File-conflict hygiene

`data-model.md`, `ui-routes.md`, and this file are shared — touch only the rows/items for slices you
hold, in your own small commits; pull/rebase before pushing.
