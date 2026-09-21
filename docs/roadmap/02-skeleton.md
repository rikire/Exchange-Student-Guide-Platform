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
      — check: every migration added after the closing date recorded in this file has a matching ADR
      under `docs/architecture/adr/` that motivated it, and the migration's own header comment names
      that ADR by number; a migration with no matching ADR is the freeze being broken quietly rather
      than deliberately. Mechanism decided 21 Sep — see open question 2 below.
- [ ] `backup`: export and import of the archive format; seeding runs through the importer
      — check: export, wipe, import produces an identical database
- [ ] `ai-tools`: `trace` in full, the blocking `stop` gate, the edit reminder, `weekly`,
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

1. ~~**Which two rules does ArchUnit cover that Modulith does not?**~~ Resolved — already answered
   in [architecture-rules.md](../ai/architecture-rules.md)'s "Enforced rules" section (added 10 Sep,
   after this question was raised 7 Sep, and never cross-checked back against it): `shared.persistence`
   imports nothing from a slice, and `wikilink` imports neither `org.springframework` nor
   `jakarta.persistence`. The decision already existed; only the stale note here needed closing.
   The tests themselves are still this phase's work — `ModularityTest.java` currently has only the
   two Modulith checks, not these two.
2. ~~**What does "changes by agreement only" mean mechanically for the frozen schema?**~~ Decided
   21 Sep, by the human, after three options were laid out (a self-certifying header line; an ADR per
   change; a hybrid of the two): **an ADR per post-freeze schema change.** Every migration dated after
   this phase's closing date must point at a `docs/architecture/adr/` entry that argued for it, the
   same weight already given to `moderate`'s storage model (ADR-0003) or the admin login (ADR-0009) —
   not a lighter-weight convention invented just for this. The step's check above now asks for that
   ADR to exist and be named in the migration header, not for a header line alone.

   **What this costs, so it isn't a surprise later:** every schema change after the freeze — even a
   one-column fix — needs its own ADR file, not a quick migration. That was the explicit trade-off
   against the cheaper header-line option, made because a schema change is exactly the kind of
   two-person collision this project's whole slice structure exists to prevent, and this project
   already treats the schema as the human's decision, not something a commit message alone should
   carry ([.claude/rules/schema.md](../../.claude/rules/schema.md)).
3. ~~**What is `links`?** The step was its only mention anywhere in the repository — nothing said
   what it would generate or check.~~ Resolved 7 Sep: it meant checking links, and `ai-tools
   docs-check` already refuses a broken markdown link to a missing `.md` file. Dropped from the step
   as a duplicate rather than built twice.

   What `docs-check` does **not** verify, if this ever matters: an `#anchor` within a file, and a
   link whose target is not markdown. Both would be rules inside `docs-check`, not a separate
   generator.
4. ~~**Do the six `ai-tools` generators belong in this phase at all?**~~ Decided 7 Sep: they stay.
   The audit of 7 September argued for moving `trace` and `ownership` to phase 3, since one reads
   code anchors and feature files and the other measures per slice, and neither exists until phase 3
   — so in this phase they would generate an empty matrix and an ownership table over no slices. The
   decision is to build them here anyway, so that they are ready when the code arrives.

   The cost is recorded rather than argued: five of the nine days of this phase are shared between
   process tooling and the first vertical slice, and the two generators cannot be confirmed by their
   own check clause until phase 3 gives them something to read.
