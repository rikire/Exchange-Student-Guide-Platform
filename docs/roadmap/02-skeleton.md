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
      — check: every migration added after the closing date recorded in this file carries a header
      line naming who agreed to it and when; a migration without one is the freeze being broken
      quietly rather than deliberately
- [ ] `backup`: export and import of the archive format; seeding runs through the importer
      — check: export, wipe, import produces an identical database
- [ ] `ai-tools`: `trace` in full, `links`, the blocking `stop` gate, the edit reminder, `weekly`,
      `ownership`. `weekly` and `ownership` **exclude the journal commits the `Stop` hook writes**
      and report them in a separate column — see [docs/team/README.md](../team/README.md); counting
      them measures prompting rather than authoring, and would have read this team backwards
      — check: the traceability matrix generates and is non-empty; `ownership` agrees with
      `sh scripts/contribution.sh` on the authored/hook split
- [ ] Gate wired into the `Stop` hook and into CI
      — check: break a migration without touching the data model document; the turn must not close
- [ ] Query-count gate for the N+1 rule in [security.md](../ai/security.md), which has no mechanism
      until the persistence layer exists — a counter around the slice tests
      — check: seed one row, then ten; the test fails if the number of queries moves
- [ ] Spring Modulith documenter in the build; ArchUnit for the two rules Modulith does not cover
      — check: `./mvnw verify` writes the module canvas, and each ArchUnit rule is demonstrated by
      deleting it and watching a test go green that should not have
- [ ] Slash commands for ownership and the gap list, once their generators exist
      — check: `ai-tools docs-check` passes with both advertised in `CLAUDE.md`; it already refuses
      a command named in the instructions with no skill behind it
- [ ] Design moved into Figma, tokens extracted
      — check: the first slice's template carries no literal colour or spacing value, only token
      names, and every token name matches the Figma variable it came from
- [ ] Content: 20 or more articles
      — check: 20 or more files under `data/seed/` load through the importer without error, and the
      count is read from the database rather than from the directory

## Readiness criterion

The gate genuinely blocks — demonstrated by breaking something on purpose, not assumed. Export and
import round-trip cleanly. The weekly log and the ownership summary are produced without anyone
writing them by hand.

## Open questions

Raised on 7 September while giving every step a checkable result. Each one is a decision the check
could not be written without, and none is the agent's to settle.

1. **Which two rules does ArchUnit cover that Modulith does not?** The step names "the two rules"
   and neither is written down. Until they are, the step cannot be finished, only declared finished.
2. **What does "changes by agreement only" mean mechanically for the frozen schema?** A header line
   in the migration naming who agreed is what the check above assumes; if the intended mechanism is
   an ADR per change instead, the check is wrong.
3. **What is `links`?** It is one of the six things that step asks for, and the step is its only
   mention anywhere in the repository — there is no description of what it would generate or check.
   It cannot be built or confirmed as written. Either say what it does, or drop it: a name in a
   checklist that nobody can define is a box that will eventually be ticked because the rest of the
   line was done.
4. **Do the six `ai-tools` generators belong in this phase at all?** Six of the eleven steps here are
   process tooling, in the nine days that are also the only window for the first line of application
   code. The audit of 7 September proposes moving `trace`, `links`, the edit reminder and the stop
   gate to phase 4 and keeping `weekly` and `ownership`, which carry rubric marks. Moving work
   between phases is the human's decision, so this stays a question.
