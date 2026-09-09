# Phase 1 — Requirements and design

**Status: in progress.** Runs 5–11 September 2026. Ends at the **design document, due 11 September**.

## Goal

Decide what the system does and how it is shaped, before code is written under those decisions. The
design document is the deliverable; the repository documents are the source it is assembled from.

## Steps

- [x] `FR` / `NFR` / `CON` in `docs/requirements/` for the product as agreed
      — check: every `FR` has a status and every `CON` a rationale. Done 7 Sep: 25 `FR` (FR-001–025,
      every Feature coverage tracker row has an id), 4 `NFR` (NFR-001–004), 5 `CON` (CON-001–005);
      every `FR` carries Status/Priority, every `CON` a Rationale.
- [ ] Glossary — one vocabulary for article, submission, revision, tag, media asset
      — check: every term the requirements use is defined once, and no concept appears under two
      names across `functional.md` and the journeys
- [ ] CJM for three roles: reader, contributor, OGE moderator
      — check: the mid-demo scenario traces through the reader and contributor journeys end to end
- [ ] C4 levels 1–3 and the ERD in PlantUML — check: the diagram script renders them
- [ ] ADRs: slices and Modulith; moderation and the version-history groundwork; search and
      multilingual content; taxonomy; media storage and upload security; export format;
      abuse handling without accounts; article content format and injection safety (added 5 Sep,
      while writing FR-001 — [ADR-0001](../architecture/adr/ADR-0001-article-body-format.md), decided)
      — check: each has at least two genuinely considered options
- [ ] `docs/architecture/ui-routes.md` — the route contract
      — check: every row names the requirement it serves, and the `Route` column of the feature
      coverage tracker is filled from it for every `must` feature
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
- [ ] Test plan: at least one test per slice — check: the plan names the test, not just the module
- [ ] Revised milestone plan, risks and plan B tied to seams that exist in the code
      — check: the revision says what moved or was cut and why — a plan reissued unchanged is not a
      revision; each risk names the signal that would tell us it is happening, and a plan B that
      could be carried out in the days actually remaining
- [ ] Assemble `docs/course/design-doc.md` and produce the PDF (2–4 pages)
      — check: every rubric row for this stage points at a section that exists and says what the
      criterion asks for; a section that exists but is a heading counts as missing
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
| Full-text search across articles | must | `search` | FR-007 | | UC-001 | | | Search results (canvas) |
| Browse/filter articles by tag | should | `taxonomy` | FR-008 | | UC-002 | | | Tag browse (canvas) |
| Read a published article (body, tags, media, wiki links) | must | `articleview` | FR-001 | | UC-003 | | | Article (canvas) |
| Download a media attachment | should | `media` | FR-016 | | UC-004 | | | Article (canvas) |
| Parse and render `[[wiki links]]` | must | `wikilink` | FR-002 | | UC-005 | | | Article (canvas) |
| Backlinks on an article | could | `wikilink` | FR-006 | | UC-006 | | | Article (canvas) |
| Landing page: pinned items + search | must | `home` | FR-009 | | UC-007 | | | Landing (canvas) |
| Red-link rendering | must | `wikilink` | FR-004 | | UC-008 | | | Article (canvas) |
| Creating an article from a red link | could | `wikilink` | FR-005 | | UC-023 | | | — not sketched yet |
| Report an article | could | `report` | FR-021 | | UC-009 | | | Article (canvas) |
| Submit a new article (with optional media attachment and suggested tags) | must | `contribute` | FR-010 | | UC-010 | | | Submission form (canvas) |
| Propose an edit to an existing article (with optional media attachment and suggested tags) | must | `contribute` | FR-011 | | UC-011 | | | Submission form (canvas) |
| Write `[[wiki links]]` inline while composing a submission | must | `wikilink` | FR-003 | | UC-012 | | | Submission form (canvas) |
| Look up a submission's status by its number | could | `contribute` | FR-012 | | UC-013 | | | Submission status (canvas) |
| Abuse handling without accounts (rate limiting + CAPTCHA) | should | `shared/security` | FR-013 | | _(none — not a use case)_ | | | Submission form (canvas) |
| Moderation queue: list pending submissions | must | `moderate` | FR-014 | | UC-014 | | | Moderation queue (canvas) |
| Review a submission's full text and attachments | must | `moderate` | FR-015 | | UC-015 | | | Submission review (canvas) |
| Approve a submission (adjust/finalize tags, publish) | must | `moderate` | FR-017 | | UC-016 | | | Submission review (canvas) |
| Reject a submission | must | `moderate` | FR-018 | | UC-017 | | | Submission review (canvas) |
| Providing a rejection reason | could | `moderate` | FR-019 | | UC-024 | | | Submission review (canvas) |
| Version-history groundwork: retain each approved revision | should | `moderate` | FR-020 | | UC-018 | | | — not sketched (no UI of its own) |
| Closing a report (was "Handle a reported article" — narrowed 7 Sep: correcting the article reuses the existing direct-edit capability, closing is the only new action) | could | `report` | FR-022 | | UC-019 (via UC-021 + UC-025), UC-025 | | | Report inbox (canvas) |
| Write and publish a new article directly, bypassing the queue | could | `contribute` | FR-023 | | UC-020 | | | Submission form (canvas) — layout only; its copy ("goes to OGE for review") is the contributor path's, not yet a moderator variant |
| Edit and publish an article directly, bypassing the queue | could | `contribute` | FR-024 | | UC-021 | | | Submission form (canvas) — same caveat as FR-023 |
| Edit the homepage, including what's pinned | could | `home` | FR-025 | | UC-022 | | | — not sketched yet |
| Remove a published article (added 9 Sep from the course's scoping feedback — see [scoping-feedback.md](../course/scoping-feedback.md)) | should | `moderate` | FR-026 | | UC-026 | | | — not sketched yet |

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
| `moderate` | moderation and version-history groundwork |
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
