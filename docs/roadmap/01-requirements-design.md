# Phase 1 — Requirements and design

**Status: not started.** Runs 5–11 September 2026. Ends at the **design document, due 11 September**.

## Goal

Decide what the system does and how it is shaped, before code is written under those decisions. The
design document is the deliverable; the repository documents are the source it is assembled from.

## Steps

- [ ] `FR` / `NFR` / `CON` in `docs/requirements/` for the product as agreed
      — check: every `FR` has a status and every `CON` a rationale
- [ ] Glossary — one vocabulary for article, submission, revision, tag, media asset
- [ ] CJM for three roles: reader, contributor, OGE moderator
      — check: the mid-demo scenario traces through the reader and contributor journeys end to end
- [ ] C4 levels 1–3 and the ERD in PlantUML — check: the diagram script renders them
- [ ] ADRs: slices and Modulith; moderation and the version-history groundwork; search and
      multilingual content; taxonomy; media storage and upload security; export format;
      abuse handling without accounts
      — check: each has at least two genuinely considered options
- [ ] `docs/architecture/ui-routes.md` — the route contract
- [ ] **Decided here, not earlier:** what the landing page contains beyond pinned items and search;
      media quotas per file, per submission and for the volume; the allowed file types
- [ ] Design reference from the IITM sites, into `docs/design/reference.md`
- [ ] Draft screens with `/design`: landing, article, search results, submission form, queue
- [ ] Test plan: at least one test per slice — check: the plan names the test, not just the module
- [ ] Revised milestone plan, risks and plan B tied to seams that exist in the code
- [ ] Assemble `docs/course/design-doc.md` and produce the PDF (2–4 pages)
- [ ] Content: 10 or more articles drafted — absorbs the article-format-and-drafts item moved from
      phase 0 (4 Sep, see [00-init.md](00-init.md)); the seed front-matter shape is decided here,
      together with the glossary and ERD, not ahead of them

## Readiness criterion

The design document is submitted, and `/course-check design` finds evidence for all five marks with
no row resting on a template.

## Open questions

1. How much of the version-history groundwork to commit to in the ADR. Storage is nearly free and
   must start in phase 2 or the history cannot be reconstructed later; the UI is a nice-to-have.
   **Still open, and sharper now (5 Sep):** the feature coverage tracker prioritizes it `should`, not
   `must` — correct by the "does the system still function without it" test — but that priority label
   says nothing about *when* it has to be built. If `should`-priority work defaults to phase 4–5
   timing, the data is unrecoverable by then. Needs an explicit answer: build the retention groundwork
   in phase 2 regardless of its `should` label, or accept that any article approved before phase 4 has
   no history.
2. ~~Whether the landing page's pinned items are curated by the moderator or derived from activity.~~
   Resolved 5 Sep: moderator-curated, via direct homepage editing (UC-022, `home`).

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
| Full-text search across articles | must | `search` | | | UC-001 | | | |
| Browse/filter articles by tag | should | `taxonomy` | | | UC-002 | | | |
| Read a published article (body, tags, media, wiki links) | must | `articleview` | | | UC-003 | | | |
| Download a media attachment | should | `media` | | | UC-004 | | | |
| Parse and render `[[wiki links]]` | must | `wikilink` | | | UC-005 | | | |
| Backlinks on an article | could | `wikilink` | | | UC-006 | | | |
| Landing page: pinned items + search | must | `home` | | | UC-007 | | | |
| Red-link rendering | could | `wikilink` | | | UC-008 | | | |
| Report an article | should | `report` | | | UC-009 | | | |
| Submit a new article (with optional media attachment and suggested tags) | must | `contribute` | | | UC-010 | | | |
| Propose an edit to an existing article (with optional media attachment and suggested tags) | must | `contribute` | | | UC-011 | | | |
| Write `[[wiki links]]` inline while composing a submission | must | `wikilink` | | | UC-012 | | | |
| Look up a submission's status by its number | could | `contribute` | | | UC-013 | | | |
| Abuse handling without accounts (rate limiting + honeypot) | should | `shared/security` | | | _(none — not a use case)_ | | | |
| Moderation queue: list pending submissions | must | `moderate` | | | UC-014 | | | |
| Review a submission's full text and attachments | must | `moderate` | | | UC-015 | | | |
| Approve a submission (adjust/finalize tags, publish) | must | `moderate` | | | UC-016 | | | |
| Reject a submission | must | `moderate` | | | UC-017 | | | |
| Version-history groundwork: retain each approved revision | should | `moderate` | | | UC-018 | | | |
| Handle a reported article | should | `report` | | | UC-019 | | | |
| Write and publish a new article directly, bypassing the queue | could | `contribute` | | | UC-020 | | | |
| Edit and publish an article directly, bypassing the queue | could | `contribute` | | | UC-021 | | | |
| Edit the homepage, including what's pinned | could | `home` | | | UC-022 | | | |

MoSCoW test used: **must** = the system does not function as this product without it; **should** = a
real, non-cosmetic loss if missing, but the system still works; **could** = low impact if missing,
easily deferred. Agreed 5 Sep: 11 must, 6 should, 6 could, 0 won't — nothing among these 23 was cut
outright, so no constraint entry was needed for this pass.

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
| `contribute` | [ ] |
| `moderate` | [ ] |
| `media` | [ ] |
| `backup` | [ ] |
| `home` | [ ] |
| `articleview` | [ ] |
| `search` | [ ] |
| `taxonomy` | [ ] |
| `wikilink` | [ ] — likely a light touch; claim alongside `contribute` or `articleview` |

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
