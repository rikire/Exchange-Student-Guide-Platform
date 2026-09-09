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
own — it can change or lapse, access to it is per-account (see the "does not resolve" note below),
and the rubric needs evidence *in* the repository. **Revised 9 Sep** after that access-scope
confusion: the seeded, self-contained page is now committed too, so opening the design needs
neither claude.ai access nor the publishing account — a checkout and a double-click are enough.

- **Lives outside git, as the editable surface:** the live Artifact — what either of us edits
  directly, and what `watch` (below) can subscribe to.
- **Always committed to git:**
  - [reference.md](reference.md) — the design tokens as plain text/tables (colors, type, spacing,
    logo usage), sourced from the real IITM/OGE sites, not a design-tool export.
  - The `.dc.html` source for every artboard, under [canvas-src/](canvas-src/) — small,
    human-readable, and how a canvas gets edited again from here (see below).
  - The **seeded page itself** — the seeded `.html` (source plus the canvas editor's own code),
    next to its `.dc.html` sources. It is a single self-contained file: opening it in any browser,
    online or off, renders and lets you interact with the exact canvas that was published, without
    needing an account or a live link. This is what makes the repository the source of truth rather
    than the hosted link — regenerate it with `seed-canvas.mjs` after editing the `.dc.html` source,
    then re-publish *and* re-commit the regenerated file so the two never drift apart.
  - The artifact URL for each canvas, recorded in this file's [Canvases](#canvases) section, for
    the live/editable copy.
  - A static export (PNG/PDF) of the design system board and every finished screen, under
    [exports/](exports/), when one is needed for a document that embeds an image rather than a
    file — `docs/course/design-doc.md`'s PDF, since that PDF can't embed a live canvas or an
    interactive HTML file. Not needed just to keep a screen safe any more; the seeded HTML above
    already does that.

## Canvases

_(Filled in as each is published — feature, artifact URL, export file.)_

| Canvas | Artifact URL (live, editable) | Committed HTML (offline, read-only) | Export |
|---|---|---|---|
| Design system | [claude.ai/code/artifact/f471d758](https://claude.ai/code/artifact/f471d758-83b2-47d8-9b5e-727860589366) | [canvas-src/exchange-guide-design-system.html](canvas-src/exchange-guide-design-system.html) | [exports/Main.pdf](exports/Main.pdf) |
| Landing page | [claude.ai/code/artifact/91ba6eef](https://claude.ai/code/artifact/91ba6eef-67d4-415d-92cc-bdf59cc3f6c5) | [canvas-src/exchange-guide-landing-screen.html](canvas-src/exchange-guide-landing-screen.html) | _not needed — superseded by the flow overview below_ |
| Flow overview — all nine screens on one canvas (Landing, Article, Search results, Tag browse, Submission form, Submission status, Moderation queue, Submission review, Report inbox), with sticky notes describing the intended navigation between them | [claude.ai/code/artifact/d0e9bee3](https://claude.ai/code/artifact/d0e9bee3-c22e-46c3-9b2c-460e51444032) | [canvas-src/flow/exchange-guide-flow-overview.html](canvas-src/flow/exchange-guide-flow-overview.html) | _deferred to the `docs/course/design-doc.md` assembly step, if that document needs a pasted image_ |

**On the "does not resolve" report (9 Sep):** checked from the publishing account and each of these
three still resolves, still owned by that account, still readable with full content — not deleted.
Artifacts publish **private by default**: reading one from an account it wasn't shared with returns
exactly the "not found — it may have been deleted, or has not been shared with you" message that
prompted the original check, and the publisher's own artifact list is naturally empty to anyone
else's account too. So the earlier check was almost certainly run from a different account than the
one that published these — an access-scope read, not evidence of loss. Worth remembering for next
time: before recording a canvas as gone, re-check from the account it was actually published under.

**On "interactive"**: this canvas preview does not support a click on one artboard jumping to
another — artboards share no runtime state (see "Known limits" in the `design` skill). The flow
overview lays all nine out together with sticky notes naming the intended transition at each
boundary, so the relationships are visible even though nothing is actually clickable between them.
Real navigation is `ui-routes.md`'s job, once Stage 4 formalizes it from what these sketches found.

Source `.dc.html` for each canvas lives under [canvas-src/](canvas-src/) and is kept in git — small,
human-readable, and re-seeding from it is how a canvas gets edited again from here. A single-artboard
canvas's source sits directly in `canvas-src/`; a multi-artboard one (like the flow overview) gets
its own subdirectory (`canvas-src/flow/`) since every canvas needs its own `Main.dc.html`. The
*seeded* `.html` next to it — what the `Artifact` tool actually publishes, source plus ~2.5 MB of the
canvas editor's own code — **is committed** (see 9 Sep revision above): it is a diff-hostile blob,
so it is replaced wholesale rather than reviewed line by line when it changes, but that is the
accepted cost of being able to open the design without any external account.

### Making a canvas link shareable

The live Artifact link publishes **private by default** — only the publishing account can open it;
anyone else gets "not found" (the exact confusion the 9 Sep note above records). To let a teammate
open the *live, editable* version (not needed just to view it — the committed HTML above covers
that): open the canvas at its artifact URL, use its **Share** control, and switch it from private to
"Anyone with the link can view" (or invite the other member's account by name, if that option is
offered). This is a setting on the hosted page itself; no command in this repository's tooling can
change it — it has to be done once, by whoever's account published the canvas, from the browser.

## Screens come from requirements, not one slice each

A slice supplies *content* to a screen — tags on the article page come from `taxonomy`, backlinks
from `wikilink`, the report button from `report` — it was never going to be one slice, one screen.
The screen list is built by grouping the written `FR`s by *what page a person is looking at*:

| Screen | Fed by (slice) | Key FRs |
|---|---|---|
| Landing page | `home`, `taxonomy`, `search` | FR-009, FR-025 |
| Article page | `articleview`, `wikilink`, `taxonomy`, `media`, `report` | FR-001, 002, 004, 005, 006, 016, 021 |
| Search results | `search` | FR-007 |
| Tag browse | `taxonomy` | FR-008 |
| Submission form (new / edit — shared) | `contribute` | FR-010, 011, 023, 024 |
| Submission status lookup | `contribute` | FR-012, 019 |
| Moderation queue | `moderate` | FR-014 |
| Submission review/decision | `moderate` | FR-015, 017, 018, 019, 020 |
| Moderator report inbox | `report` | FR-021, 022 |

The Stage 3 claim table in
[01-requirements-design.md](../roadmap/01-requirements-design.md) still works for *ownership* —
whoever holds a slice sketches the screen(s) that slice mainly feeds — but the deliverable is this
screen list, not a mockup per slice.

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
