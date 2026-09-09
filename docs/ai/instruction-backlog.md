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
