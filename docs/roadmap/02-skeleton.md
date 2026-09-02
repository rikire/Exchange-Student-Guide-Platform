# Phase 2 — Walking skeleton and the rest of the tooling

**Status: not started.** Runs 12–20 September 2026.

## Goal

One thin vertical slice working end to end, the schema settled, and the gates that keep documentation
honest actually switched on.

## Steps

- [ ] First vertical slice by TDD: `home` and `articleview` — the landing page leads to an article
      — check: a red MockMvc test existed before the controller
- [ ] `shared/persistence`: entities and Flyway migrations carrying trace anchors
      — check: the migration runs against an empty database and one with data; the ERD matches
- [ ] **The schema freezes at the end of this phase.** After that it changes by agreement only
- [ ] `backup`: export and import of the archive format; seeding runs through the importer
      — check: export, wipe, import produces an identical database
- [ ] `ai-tools`: `trace` in full, `links`, the blocking `stop` gate, the edit reminder, `weekly`,
      `ownership`
      — check: the traceability matrix generates and is non-empty
- [ ] Gate wired into the `Stop` hook and into CI
      — check: break a migration without touching the data model document; the turn must not close
- [ ] Spring Modulith documenter in the build; ArchUnit for the two rules Modulith does not cover
- [ ] Slash commands for ownership and the gap list, once their generators exist
- [ ] Design moved into Figma, tokens extracted
- [ ] Content: 20 or more articles

## Readiness criterion

The gate genuinely blocks — demonstrated by breaking something on purpose, not assumed. Export and
import round-trip cleanly. The weekly log and the ownership summary are produced without anyone
writing them by hand.

## Open questions

None recorded yet; this phase has not been planned in detail.
