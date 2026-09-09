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
own — it can change or lapse, and the rubric needs evidence *in* the repository.

- **Lives outside git:** the live canvas itself (design system, and each screen) — the
  collaborative surface, edited directly by whoever is iterating.
- **Always committed to git:**
  - [reference.md](reference.md) — the design tokens as plain text/tables (colors, type, spacing,
    logo usage), sourced from the real IITM/OGE sites, not a design-tool export.
  - The artifact URL for the design system and for each screen, recorded in this file's
    [Canvases](#canvases) section below as they're published.
  - A static export (PNG/PDF) of the design system board and every finished screen, under
    [exports/](exports/) — this is what goes into `docs/course/design-doc.md`'s PDF, since a PDF
    can't embed a live canvas.

## Canvases

_(Filled in as each is published — feature, artifact URL, export file.)_

| Canvas | Artifact URL | Export |
|---|---|---|
| Design system | [claude.ai/code/artifact/f471d758](https://claude.ai/code/artifact/f471d758-83b2-47d8-9b5e-727860589366) | [exports/Main.pdf](exports/Main.pdf) |
| Landing page | [claude.ai/code/artifact/91ba6eef](https://claude.ai/code/artifact/91ba6eef-67d4-415d-92cc-bdf59cc3f6c5) | _pending_ |
| Flow overview — all nine screens on one canvas (Landing, Article, Search results, Tag browse, Submission form, Submission status, Moderation queue, Submission review, Report inbox), with sticky notes describing the intended navigation between them | [claude.ai/code/artifact/d0e9bee3](https://claude.ai/code/artifact/d0e9bee3-c22e-46c3-9b2c-460e51444032) | _pending_ |

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
its own subdirectory (`canvas-src/flow/`) since every canvas needs its own `Main.dc.html`. The *seeded*
`.html` the `Artifact` tool actually publishes is **not** committed: it's the source plus ~2 MB of
the canvas editor's own code, regenerated on demand with `seed-canvas.mjs` rather than tracked as a
diff-hostile blob.

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
