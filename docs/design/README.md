# How design work happens in this project

Written 7 Sep 2026, phase 1, when design work started. This is the durable record of decisions made
about the design process itself — where things live, how screens are chosen, how a design session
feeds back into requirements, and how work moves into Figma. None of this is automatic; it is a set
of conventions the two of us (and the agent) follow deliberately.

## Where things live

Claude Design's canvas (via `/design`) publishes to a **hosted Artifact** — a live, directly
editable page, separate from this git repository. "Save" publishes a new version to that hosted
artifact, not to a file here. That's a property of the tool, not a choice we made.

Because this is graded coursework, an external, editable link is not sufficient evidence on its
own — it can change or lapse, access to it is per-account (see the sharing note below), and the
rubric needs evidence *in* the repository. **Revised 10 Sep**, after two iterations on what that
evidence should look like:
- 7 Sep: only the `.dc.html` source was committed; the rendered result lived only behind the
  Artifact link.
- 9 Sep: committed the seeded page itself instead (source plus the ~2.5 MB canvas-editor bundle)
  after the access-scope confusion below — self-contained, but heavy and not something a reviewer
  can quickly read as "the screen."
- 10 Sep, current: committed a **plain, per-screen HTML file** for each screen instead — no editor
  code, a few KB each, opens instantly and reads as the mockup itself. The seeded editor bundle is
  back to regenerate-on-demand, not committed, since it now duplicates what the plain screens show.

- **Lives outside git, as the editable surface:** the live Artifact — what either of us edits
  directly, and what `watch` (below) can subscribe to.
- **Always committed to git:**
  - [reference.md](reference.md) — the design tokens as plain text/tables (colors, type, spacing,
    logo usage), sourced from the real IITM/OGE sites, not a design-tool export.
  - The `.dc.html` source for every artboard, under [canvas-src/](canvas-src/) — small,
    human-readable, and how a canvas gets edited again from here (see below).
  - A **plain HTML file per screen**, under [screens/](screens/) — the `.dc.html` source's markup
    and styles unwrapped from the canvas editor's `<x-dc>`/`<helmet>` tags into an ordinary page, so
    it opens in any browser with no editor, no account, and no network dependency but the Google
    Fonts stylesheet (falls back to a system sans-serif without it). Regenerate after editing a
    `.dc.html` source; the two must not drift apart.
  - The artifact URL for each canvas, recorded in this file's [Canvases](#canvases) section, for
    the live/editable copy.
  - A static export (PNG/PDF) of the design system board and every finished screen, under
    [exports/](exports/), when one is needed for a document that embeds an image rather than a
    file — `docs/course/design-doc.md`'s PDF, since that PDF can't embed a live canvas or an HTML
    file.

## Canvases

_(Filled in as each is published — feature, artifact URL, export file.)_

| Canvas | Artifact URL (live, editable) | Export |
|---|---|---|
| Design system | [claude.ai/code/artifact/f471d758](https://claude.ai/code/artifact/f471d758-83b2-47d8-9b5e-727860589366) | [exports/Main.pdf](exports/Main.pdf) |
| Landing page | [claude.ai/code/artifact/91ba6eef](https://claude.ai/code/artifact/91ba6eef-67d4-415d-92cc-bdf59cc3f6c5) | _superseded by the flow overview below_ |
| Flow overview — all thirteen screens on one canvas (Landing, Article, Search results + its empty state, Tag browse, Create-from-red-link invite, Submission form, Submission status, Moderation queue + its empty state, Submission review, Report inbox, Homepage & article administration), with sticky notes describing the intended navigation between them | [claude.ai/code/artifact/d0e9bee3](https://claude.ai/code/artifact/d0e9bee3-c22e-46c3-9b2c-460e51444032) | _deferred to the `docs/course/design-doc.md` assembly step, if that document needs a pasted image_ |

The thirteen screens themselves, as plain HTML, are under [screens/](screens/) — see the table below
for which FRs each one covers. Every `FR` that names a reader- or moderator-facing state now has a
screen, except FR-020 (revision retention), which by its own text names no such state — see the
Feature coverage tracker's note on that row.

**On the "does not resolve" report (9 Sep):** checked from the publishing account and each of these
canvases still resolves, still owned by that account, still readable with full content — not
deleted. Artifacts publish **private by default**: reading one from an account it wasn't shared with
returns exactly the "not found — it may have been deleted, or has not been shared with you" message
that prompted the original check, and the publisher's own artifact list is naturally empty to anyone
else's account too. So the earlier check was almost certainly run from a different account than the
one that published these — an access-scope read, not evidence of loss. Worth remembering for next
time: before recording a canvas as gone, re-check from the account it was actually published under.

**On "interactive"**: this canvas preview does not support a click on one artboard jumping to
another — artboards share no runtime state (see "Known limits" in the `design` skill). The flow
overview lays all thirteen out together with sticky notes naming the intended transition at each
boundary, so the relationships are visible even though nothing is actually clickable between them.
Real navigation is `ui-routes.md`'s job, once Stage 4 formalizes it from what these sketches found.

Source `.dc.html` for each canvas lives under [canvas-src/](canvas-src/) and is kept in git — small,
human-readable, and re-seeding from it is how a canvas gets edited again from here, or how
[screens/](screens/) gets regenerated. A single-artboard canvas's source sits directly in
`canvas-src/`; a multi-artboard one (like the flow overview) gets its own subdirectory
(`canvas-src/flow/`) since every canvas needs its own `Main.dc.html`. The *seeded* `.html` next to
it — what the `Artifact` tool actually publishes, source plus ~2.5 MB of the canvas editor's own
code — is **not** committed: regenerate it on demand with `seed-canvas.mjs` when re-publishing.

### Making a canvas link shareable

The Artifact tool's own publish confirmation reports each of these three canvases as
**"sharing public"** as of 10 Sep — which, if accurate, means the link already works for anyone
without needing the publishing account, and the 9 Sep "does not resolve" report might have had a
different cause than access scope (an old or mistyped URL, most likely). That status line is worth
trusting only after checking it directly, though: open a canvas's artifact URL in a private/logged-out
browser window (or have the other person try their own account) and confirm it actually loads. If it
doesn't, the setting to fix is on the hosted page itself — its own **Share** control, switched to
"Anyone with the link can view" — not something any command in this repository's tooling can change;
whoever's account published the canvas has to do it once, from the browser.

## Screens come from requirements, not one slice each

A slice supplies *content* to a screen — tags on the article page come from `taxonomy`, backlinks
from `wikilink`, the report button from `report` — it was never going to be one slice, one screen.
The screen list is built by grouping the written `FR`s by *what page a person is looking at*:

| Screen | Fed by (slice) | Key FRs |
|---|---|---|
| [Landing page](screens/Landing.html) | `home`, `taxonomy`, `search` | FR-009 |
| [Article page](screens/Article.html) | `articleview`, `wikilink`, `taxonomy`, `media`, `report` | FR-001, 002, 004, 006, 016, 021 |
| [Search results](screens/SearchResults.html) ([no matches](screens/SearchResultsEmpty.html)) | `search` | FR-007 |
| [Tag browse](screens/TagBrowse.html) | `taxonomy` | FR-008 |
| [Create-from-red-link invite](screens/RedlinkInvite.html) | `wikilink` | FR-005 |
| [Submission form](screens/SubmissionForm.html) (new / edit — shared) | `contribute` | FR-010, 011, 023, 024 |
| [Submission status lookup](screens/SubmissionStatus.html) | `contribute` | FR-012, 019 |
| [Moderation queue](screens/ModerationQueue.html) ([empty](screens/ModerationQueueEmpty.html)) | `moderate` | FR-014 |
| [Submission review/decision](screens/SubmissionReview.html) | `moderate` | FR-015, 017, 018, 019, 020 |
| [Moderator report inbox](screens/ReportInbox.html) | `report` | FR-021, 022 |
| [Homepage & article administration](screens/HomeAdmin.html) | `home`, `moderate` | FR-025, FR-026 |

The Stage 3 claim table in
[01-requirements-design.md](../roadmap/01-requirements-design.md) still works for *ownership* —
whoever holds a slice sketches the screen(s) that slice mainly feeds — but the deliverable is this
screen list, not a mockup per slice.

## Screens now out of step with the requirements

Found 10 September, reviewing the four diagrams and the data model against every `FR`, every `NFR`,
every `CON` and all thirteen screens. The loop below describes a gap found *while sketching*; these
are the reverse — the requirements moved, or an action was drawn that nothing behind it supports.
Recorded rather than quietly redrawn, because a screen is evidence of what was designed on a date.

**Who picks these up.** Every one of them sits in a slice already claimed `[~] abdirakhim` in the
Stage 3 table in [01-requirements-design.md](../roadmap/01-requirements-design.md) — that table is
the claim, not this list, and the `Slice` column below only says which row each belongs under. The
roadmap carries the whole set as one step with a check that can fail.

| # | Gap | Slice | Screens | Why |
|---|---|---|---|---|
| 1 | ~~**No summary field.**~~ Resolved 10 Sep: a Summary input added to [Submission form](screens/SubmissionForm.html) and an editable Summary field added to [Submission review](screens/SubmissionReview.html), both `.dc.html` and the plain screen | `contribute` | [Submission form](screens/SubmissionForm.html), [Submission review](screens/SubmissionReview.html) | The field was added to the requirements after these were sketched |
| 2 | ~~**No way to reach "propose an edit."**~~ Resolved 10 Sep: a "Propose an edit" action added to the [Article](screens/Article.html) sidebar, ahead of "Report this article" | `articleview` | [Article](screens/Article.html) | Step 5 of the mid-demo scenario — see [scenario-trace.md](../cjm/scenario-trace.md) |
| 3 | ~~**Pinned articles have no order.**~~ Resolved 10 Sep: `pinned` replaced by `pinned_at` (nullable timestamp) — see open decision 2 in [data-model.md](../architecture/data-model.md). No screen change needed: [Homepage & article administration](screens/HomeAdmin.html) only pins/unpins, it never drew a manual reorder control | `home` | [Homepage & article administration](screens/HomeAdmin.html) | Found 10 Sep against the data model; the schema half was open decision 2 in [data-model.md](../architecture/data-model.md) |
| 4 | ~~**Remove is drawn, and nothing behind it is decided.**~~ Resolved 10 Sep: `removed_at` (nullable timestamp, soft delete) added to `article` — see open decision 1 in [data-model.md](../architecture/data-model.md). No screen change needed: the Remove button already reads as "removes it from view," which is what a soft delete does | `moderate` | [Homepage & article administration](screens/HomeAdmin.html) | Open decision 1 in [data-model.md](../architecture/data-model.md) |

Rows 1 and 2 were affordances only — `submission.summary` exists and the submission form is already
shared between new articles and edits — resolved directly on the screen; each still needs its own
row in [ui-routes.md](../architecture/ui-routes.md), which is not written yet.

Rows 3 and 4 were schema questions, not screen fixes — entity fields and cardinality are the human's
decision (`.claude/rules/schema.md`). Both were decided 10 Sep in
[data-model.md](../architecture/data-model.md); neither screen needed a redraw once the schema
caught up, since neither drew anything the decision contradicts.

Re-seeded 10 Sep: the live "Flow overview" Artifact (see [Canvases](#canvases) above) now matches
these sources. While re-seeding, the canvas's own `oge-logo.svg` — present on the hosted page but
missing from git — was pulled back into
[canvas-src/flow/](canvas-src/flow/) so the committed source is self-contained again.

## Discovering a requirement or architecture gap while sketching

Sketching a screen routinely surfaces a missing field, an undecided route, or a state no `FR`
covers. When it does:

1. Stop and name the gap before continuing the sketch.
2. Fix it at the right layer immediately — a new/amended `FR`/`NFR`
   (`docs/requirements/functional.md` / `non-functional.md`), a glossary term, a `CJM` step, or (if
   genuinely architectural) `docs/architecture/data-model.md` / `ui-routes.md`.
3. Update the Feature coverage tracker's `ERD entity` / `Route` / `Screen` columns for the affected
   row(s) once they have real values.
4. Resume sketching only once the doc is updated.

This is a manual discipline — `/design` has no git hook and cannot trigger a commit or a doc
update by itself. The one piece of real automation available is the `Artifact` tool's `watch`
action: once a canvas is published, a live Claude session can subscribe to it and get notified when
either of us republishes it, prompting a re-check at that moment. That is a session-side
subscription, not a repository-side guarantee — useful, but not a substitute for the manual loop
above.

## Moving a design into Figma

Not through Figma's MCP server — its rate limits are tight enough to make an iteration loop
impractical (a Starter-plan seat gets 6 tool calls a month; a paid Dev/Full seat is 200/day,
10/minute).

Use **Code to Canvas** instead — Anthropic and Figma's own integration for sending a generated
interface into Figma as fully editable layers ("send this to Figma"). It's a one-way push, not a
read/write loop through the rate-limited API, so the limits above don't apply to it.

If a case ever needs more than Code to Canvas covers: a community Figma MCP server exists that
reads/writes through a local plugin bridge into Figma Desktop instead of the REST API — no token,
no rate limit, because it never calls the throttled endpoint. Building a custom plugin from scratch
is not recommended; both options above already solve this without that engineering cost.
