# Audit of planning, tracking and work distribution — 7 September 2026

The audit of 6 September checked the AI-collaboration layer against the platform it runs on. This
one was asked a different question: **is the planning process working, and what has it produced?**
Not whether the roadmap is tidy — whether it is doing its job.

The answer has two halves and both are load-bearing. **The process produces artefacts of genuinely
high quality. It is producing the wrong ones at a rate the schedule has never been checked against.**

---

## Assessment

### 1. Artefact quality is high. That is not the problem.

`ADR-0001` is a real architectural decision record: three options each with their cost, a named
deciding factor (anonymous unauthenticated submitters), bad consequences stated, and a reversal
path. `FR-007` is proper EARS with a paired unwanted-behaviour clause and four Given/When/Then
criteria, two of them negative. The feature tracker's own rules are stricter than most professional
ones: *"a cell is checked only when that specific feature's piece of that artifact exists."*

Between 5 and 7 September the requirements layer went from nothing to **25 functional requirements,
6 non-functional, 7 constraints, 23 use cases and one ADR**, with every row of the 25-row feature
tracker carrying a requirement id. That is fast, and it is good work.

Any conclusion that reads as "there is too much process here" would be wrong about this. The process
works. The question is what it is pointed at.

### 2. The ratio, measured

Tracked lines on 7 September, day 13 of the project:

| | lines |
|---|---|
| `app/` Java — **the product** | **51** |
| `tools/` Java — process tooling | 3,344 |
| `docs/` excluding the journal | 4,332 |
| `docs/ai/journal/` | 2,635 |
| `.claude/` | 746 |

**About 217 : 1**, and rising, because the numerator grows every day and the denominator has not
moved since the repository was created. Concretely: one slice package, 0 Thymeleaf templates, 0
Flyway migrations, 0 seed articles, and `app/pom.xml` still carries no JPA, no Flyway and no
database driver. **Zero `FEAT-XXX` files exist**, so the chain `use case → requirement → feature →
route/table/code/test` that `docs/repository-map.md` describes is broken in the middle.

### 3. The schedule has never been checked against the evidence

`docs/ai/PLAN-PROMPT.md` set six end-dates on 2 September, before any code existed. The roadmap
still carries those same six dates, unchanged, after phase 0 absorbed more than planned.

Phase 2 is **nine days** for eleven items: the first vertical slice by TDD, the whole persistence
layer with migrations, export/import with a round-trip check, **six `ai-tools` generators**, wiring
a gate into `Stop` and CI, a query-count harness, Modulith plus ArchUnit, Figma and design tokens,
and 20 or more articles. Six of the eleven are more process tooling, scheduled into the only window
that exists for the first line of application code.

By the mid-demo — **32 days** — the fixed demo scenario needs `search`, `articleview`, `wikilink`,
`contribute`, media upload and `moderate` all working end to end against a real database.

### 4. Nothing in the process could have noticed, and one rule should have fired

`docs/ai/roadmap.md` says: *"When a deadline is at risk, say so in the phase file **and** in the
answer, with what would have to be dropped. A roadmap that stays green while the work slips is worse
than no roadmap."*

The rule exists. The risk is visible in the numbers above. **It had never fired.** There was no
velocity signal, no capacity check and no risk register, and MoSCoW — 12 `must`, 4 `should`, 9
`could` — was agreed on 5 September and has never been used as the lever it is. This is the same
shape as every finding in the previous audit: a rule believed to hold because it is written down.

It costs marks directly. Of the five design-document marks due **11 September**, one is *"Milestone
plan revised in light of scoping feedback"* — the plan has never been revised — and one is *"Risks
and plan B are honest, not boilerplate"*, whose evidence file is a twelve-line stub.

### 5. The plan's detail was inversely correlated with what was at stake

Before this audit:

| Phase | Lines | Open items | Days | Steps naming a result |
|---|---|---|---|---|
| 1 — requirements & design | 280 | 11 | 7 | 4 of 11 |
| 2 — skeleton | 40 | 11 | 9 | 6 of 11 |
| **3 — the product, ends at the mid-demo** | **31** | **11** | **18** | **0 of 11** |
| 4 — hardening | 30 | 7 | 21 | 1 of 7 |
| 5 — handover and viva | 28 | 6 | 7 | 1 of 6 |

Detail decaying with distance is correct practice. This was not that: **phase 3 was the least
planned phase per day of work, and it carries the entire product.** Phases 4 and 5, further away and
worth less, were better plans — phase 4 names nine specific edge cases, phase 5 names artefacts and
carries a real open question.

Three structural consequences followed. **Granularity was out by an order of magnitude** —
`- [ ] moderate — the queue, approval, rejection; the state machine with its invariants` is a
sub-project, not a task, and nothing in the plan was small enough for one person to finish in a
sitting. **Nothing was countable, so the plan could not answer "will this fit"** — the missing
capacity check in §4 is a consequence, not an oversight; you cannot estimate in units of "a slice".
**There was no ordering inside a phase** — phase 3's items have real dependencies and the list is
flat.

Even the one phase planned in depth spent its detail on process: of phase 1's 280 lines, the Steps
section was 27. The other 253 are the claiming convention, the six-stage protocol, the tracker and
file-conflict hygiene.

**What is good and was left alone:** every phase has a Goal and a Readiness criterion genuinely
independent of its checklist. Phase 4's — *"a third person stands the application up from the
written instructions on a clean machine"* — is falsifiable and externally checkable, and better than
most professional acceptance criteria. The Open questions sections are maintained rather than
decorative: all three of phase 1's are now struck through with resolution dates and requirement
references.

### 6. The work split will not survive the viva

All Java in this repository is one person's. Abdirakhim has touched `tools/` zero times, `app/` zero
times, `.claude/` zero times; his contribution is documentation, and it is good documentation — the
requirements layer above is largely his. The course states plainly: *"One person does everything and
the other tests is not acceptable."* The final viva puts 3 marks on your own modules and 1 on your
partner's, and there is currently no module he could be asked about.

---

## Findings, and what changed

### A-01 — The journal hook inverted the contribution record *(fixed)*

53 of 102 commits were the hook-written `docs: record the journal entry for …`, and 49 of them sat
on one member's identity. `git shortlog` read 57 : 38 one way while authored work was 35 : 8 the
other. **Every anti-freeloading mechanism the course uses reads this history**, and
`docs/team/README.md` promises ownership will be "measured rather than assigned" from exactly it.
The measurement specification never said to exclude hook commits, so the phase-2 generator would
have inherited the distortion and lent it a tool's authority.

`scripts/contribution.sh` now reports the two columns apart, aggregated per member rather than per
git address, and never totals them. The exclusion is recorded in `docs/team/README.md` and in the
`ownership` step of `02-skeleton.md`, where the generator will be written.

### A-02 — The weekly log was empty *(fixed, half)*

Zero entries from either member after two weeks, against a course requirement (§10.5) for a
paragraph from each student **with every stage** — the next being 11 September — and a mid-demo mark
for both members appearing across weeks. `docs/team/weekly-log/2026-W36.md` now exists with the git
figures for both and Mikhail's own paragraph. **Abdirakhim's paragraph is deliberately left empty**:
the log's own README says a reconstructed log reads as reconstructed, and what the course asks for
is each student's own account.

### A-03 — Every measured instrument is still a stub *(open)*

`ai-tools ownership` and `weekly` (phase 2) and `gaps` (phase 3) do not exist. `docs/team/ownership.md` is a placeholder
that **3 of the 10 final-demo marks point at**, and its claim that "editing this file by hand is
detected and breaks the build" is implemented nowhere. `scripts/contribution.sh` covers the weekly
log's needs until then; it does not cover per-slice ownership, which needs slices.

### A-04 — All authored work was in one ISO week *(improving)*

It was all in `2026-W36`. As of 7 September both members have authored commits in `2026-W37` as
well. History cannot be backdated; the log can only accumulate from here, and it now does.

### B-01 — The roadmap broke its own rule *(fixed, and now enforced)*

*"An item without a checkable result is not an item"* — 18 of 48 open items named one, and phase 3
named none across eleven items. All 46 open items across the six phase files now do, and
`ai-tools docs-check` refuses a phase file containing a step without one. Verified by breaking it:
a stripped clause is refused, and the same line with "in phase 3" appended is **still** refused —
the phase-marker escape deliberately does not apply, or the rule could be switched off by writing
the word "phase".

### B-02 — The status the machine reads was false *(fixed)*

`docs/roadmap/README.md` called phase 1 *not started* while 25 requirements, 23 use cases and
ADR-0001 sat inside it, and `scripts/session-start.sh` parses that table and announces it at the top
of every session — so the repository opened by stating something untrue, from the one signal
designed to tell a session where the project stands. Phase 0's end date disagreed with itself
(index 6 Sep, its own file 4 Sep); the file wins, because it carries the evidence.

### B-03 — The dependency order was cited but never written *(superseded)*

`phase1-session-2026-09-06.md` referred to an "agreed dependency order" that appeared nowhere. The
list it ordered is now complete, so the order is historical rather than missing. The ordering that
*is* still missing is inside phases 2 and 3, and is proposed below.

### B-04 — An undocumented parallel convention *(fixed)*

Two `phaseN-session-YYYY-MM-DD.md` notes existed, each declaring itself temporary with a deletion
condition, described in no instruction file, duplicating the automatic journal. Their condition was
met, so they are gone — but **two adopted rules lived only there** and were moved into the format
documents that own them first: prefer a glossary term to a requirement-to-requirement
cross-reference, and treat "not yet approved" and "rejected" as two scenarios rather than one. The
convention is now recorded in `docs/ai/roadmap.md`, carrying that near-miss as its reason.

### B-05 — The claim mechanism has never fired *(recorded)*

`[~]` appears nowhere in the repository and all nine slice claims are open. That is not yet a
failure — stages 1 and 2 of phase 1 are joint work, so there has been nothing to claim. It is
recorded in the claim table because stage 3 is the first time two people work in parallel, and a
coordination mechanism that has never run is one nobody has found the problem with yet.

### B-06 — `docs/gap-list.md` contradicted itself *(fixed)* on which phase its generator arrives in.

### B-07 — The version-history sequencing decision *(resolved by the human, 7 September)*

Recorded here because the audit was drafted while it was still open. The decision was to **accept
the gap**: there is no deployment before phase 4, so an article approved before the retention
groundwork lands simply has no history. That is a real loss, judged low-stakes against the project's
own timeline, and it is now struck through in phase 1's open questions with its reasoning.

---

## Proposals — drafted, awaiting a decision

`docs/ai/roadmap.md` puts moving work between phases and changing readiness criteria in the human's
hands. Everything in this section is therefore a proposal and none of it has been applied.

### P-1 — A checkpoint with a trigger, before the mid-demo scope is unsaveable

The schedule assumes a rate of application output that has **not been observed once**. The first
real evidence arrives at the end of phase 2 on 20 September — four days after the design document
and nineteen before the mid-demo. One checkpoint, and no time to react if it is missed.

**Proposed:** a dated checkpoint on **16 September**, halfway through phase 2, with a stated trigger —
*if `home` and `articleview` are not serving a real article from a real database by then, the
mid-demo scope is cut on that day rather than hoped at.* This is also the honest content of the
"Risks and plan B" mark due on 11 September.

### P-2 — Move four of the six generators out of phase 2

Six of phase 2's eleven items are process tooling occupying the only window for first product code.
Three of them cannot usefully be built yet in any case:

| Generator | Proposal | Why |
|---|---|---|
| `trace` | → early phase 3 | It reads code anchors and feature files. Neither exists until phase 3 |
| `ownership` | → phase 3 | It measures per **slice**; there are no slice packages before phase 3, and the balance check already sits in phase 3 |
| `links`, edit reminder, blocking `stop` gate | → phase 4 | Process hardening; no rubric mark depends on them |
| `weekly` | keep, or drop entirely | `scripts/contribution.sh` already does its work. Building it twice is the thing this repository forbids |

`gaps` is already phase 3. This frees roughly half of phase 2 for the persistence layer and the
first vertical slice.

### P-3 — A MoSCoW cut that the mid-demo scenario justifies

The demo scenario is fixed: search, read, follow a wiki link, propose an edit with a photo, moderator
approves, live. Two of the twelve `must` features are **not on that path**:

- **Red-link rendering** — raised from `could` to `must` on 6 September. Nothing in the scenario
  follows a link to an article that does not exist.
- **Writing `[[wiki links]]` inline while composing a submission** — the scenario proposes an edit
  with a photo, not with a new wiki link.

**Proposed:** both to `should`, deferred past the mid-demo. That is 2 of 12 `must` features removed
from a 32-day window, with a reason anybody can check against the scenario. The rubric's own line
applies: *a justified simplification recorded as a constraint is worth more than an unconsidered
feature.*

### P-4 — Abdirakhim's first Java, sized so it is genuinely his

Three viva marks and an explicit course rule depend on him owning a module. **Proposed: he takes the
`wikilink` slice end to end**, starting with the link parser by TDD.

It is the right first task for a specific reason: the parser is pure logic with no database, no
Spring wiring and no `shared/` contact, so it cannot collide with the persistence work; its corner
cases are exactly what `docs/ai/testing.md`'s derivation method is for; and it is a whole slice, so
it gives him a named module to be examined on rather than a contribution to someone else's. Mikhail
does not touch that package.

### P-5 — Decompose the demo path into feature files, before phase 2 starts

The unit already exists and has never been used: `docs/features/_TEMPLATE.md`, one file per tracker
row, whose acceptance criteria become test names. **Proposed:** create them for the demo path only —
seven files — in this dependency order, which the flat phase lists omit:

```
shared/persistence  →  articleview + home  →  backup/import (gets content in)
                                           →  search
                                           →  contribute  →  media
                                                          →  moderate  →  admin panel
```

Seven feature files is a morning's work and it is what turns "build the `moderate` slice" into
something a person can pick up, finish, and be seen to have finished.

---

## Considered and rejected

**Moving the tracker to GitHub Issues and Projects.** Rejected. The rubric reads files in the
repository, `docs-check` reads files in the repository, and the four generators still to be written
read files in the repository. A board would put the status somewhere none of them can see it, and
two places for status means two places for it to disagree.

**Estimating in points or hours.** Rejected. With two people, one semester and a fixed scope, the
useful question is not "how long will this take" but "what gets cut if it does not fit", and P-1 and
P-3 answer that directly. An estimate would be a number nobody could check against anything.

**Automating a staleness check on phase status.** Considered after B-02, rejected. Whether a phase
has started is a judgement about work, not a fact about files, and a checker that guessed would
either be wrong or need the answer told to it — which is the state that was already broken.

---

## What this audit does not establish

- **That the schedule now fits.** Nothing here changes the arithmetic; P-1 only makes the moment it
  becomes undeniable arrive early enough to act on.
- **That the check clauses are the right checks.** They are checkable and externally verifiable,
  which is what the rule asks. Whether they check the thing that matters is judgement, and several
  will look wrong once the code exists.
- **That `[~]` works.** It has still never been used.
- **That the contribution figures mean what they appear to.** They count authored commits, not
  effort, and the journal's own README already concedes it records whose machine a prompt came from
  rather than who was at the keyboard. Some of those commits are pair work.
- **That the missing `FEAT` layer is fixed.** P-5 proposes it; nothing has been created.
