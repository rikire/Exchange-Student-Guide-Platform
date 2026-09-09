# Proposed changes to the agent's instructions

**Nothing here is in force.** An entry binds the agent only once the human moves it into
[CLAUDE.md](../../CLAUDE.md) or the document that owns it — the agent may not change its own
instructions ([collaboration.md](collaboration.md)). This file is the queue, not the rule.

## IB-001 — Do not invent durations

**Requested:** 9 September 2026.

**Problem.** Planning the design document, the agent produced "~30 minutes", "~2 h", "~3 h",
"~12 hours", "~15.5 hours" for work it had not done and had no measurement for. They were guesses
formatted as estimates, and a guess in that format gets planned against.

**Proposed interim rule.** State no duration unless asked for one. Size work in countable units:
files, requirements, tests, sections, slices, screens. "Three FRs, two tests and one diagram" can be
checked before the work starts; "three hours" cannot.

**Open question — how to estimate properly.** Worth designing rather than settling now. Three seeds:

- **The data already exists.** The prompt journal timestamps every turn and git timestamps every
  commit. The rate at which this repository actually produces an FR, a test or a document is
  measurable from its own history, not from a feeling.
- **Reference class, not intuition.** Anchor an estimate to a named comparable that is already
  finished — "the requirements layer, 25 FRs, took 5–7 September" — and say which comparable was
  used, so the estimate can be argued with.
- **The same defect has been found twice already.** [audit-planning-2026-09-07.md](audit-planning-2026-09-07.md)
  §5 found phase plans that could not answer "will this fit" because nothing in them was countable.
  A duration invented per task is that defect at a smaller scale.

A possible mechanism: `ai-tools` derives the observed rate per unit from the journal and git, and an
estimate must quote it or be marked as a guess.

## IB-003 — The instruction layer is too large for what it achieves

**Requested:** 9 September 2026.

**Problem.** 2,375 lines across 15 documents, against 159 lines of product code. Volume has not
produced compliance: the claim marker `[~]` has never been used, the roadmap's deadline rule never
fired while the schedule slipped, and the course's scoping feedback sat unrecorded for twelve days
under rule 6. Every one of those had a rule already.

**Diagnosis.** The failures are not spread evenly. Five documents have a delivery mechanism —
`.claude/rules/` loads them when a matching file is opened — and all five are code rules:
`java-style`, `testing`, `security`, `schema`, `architecture`. The seven process documents
(`collaboration`, `prompting`, `stop-and-ask`, `workflow`, `docs-sync`, `roadmap`,
`definition-of-done`) have no trigger and rely on being remembered. Every failure above came from
that second group.

So the problem is not how many words a rule has. It is that a process rule has no moment at which it
arrives. The one process rule that does fire — sharpening a vague prompt — fires because a
`UserPromptSubmit` hook injects it into every turn.

**Found while writing this entry:** [README.md](README.md) said four path-scoped rules exist. There
are five; `architecture` was unnamed. Corrected in the same commit. At 2,375 lines the layer could
not stay accurate about its own contents, which is the argument in miniature.

**Options, none decided.**

- **Give process rules a trigger.** Extend the pattern that already works: `stop-and-ask` on
  protected paths, `docs-sync` when a document's subject file changes, the deadline rule at session
  start when a deadline is near.
- **Make a rule executable or delete it.** A rule a hook can check outranks a paragraph asking for
  good faith. The rules that failed are exactly the ones nothing checks.
- **Consolidate the seven process documents,** keeping each only if the journal shows it changed an
  outcome.
- **Cap the layer:** no new instruction document until an existing one is retired.

**Check for whichever is chosen.** Name a rule that failed in the first fifteen days, and show the
mechanism that would have fired. An option that cannot do that is a rewrite, not a fix.

## IB-002 — Documentation prose is too long

**Requested:** 9 September 2026.

**Problem.** The agent's documents carry sentences that argue for the document's own honesty, restate
the heading, or repeat in prose what the table above already said. Examples from the same day:
"This is recorded plainly rather than softened"; "because a table that quietly drops a dead row stops
being a record of what was published". Neither carries information.

**Proposed rule.** Every sentence carries a fact, a decision, or a consequence of one. Cut anything
that restates the heading, explains why the document exists, praises the entry's own honesty, or
repeats a table. One sentence of rationale per decision, not a paragraph.

**Check.** Point at any sentence and ask which of the three it carries. If the answer is none, it
goes. Short is not the target — sufficient is; a deleted sentence that held a decision is a worse
defect than a long one.
