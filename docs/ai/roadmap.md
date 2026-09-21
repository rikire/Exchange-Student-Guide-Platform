# Keeping the roadmap

The roadmap lives in [docs/roadmap/](../roadmap/), one file per phase. It is a working document, not a
contract: items are added and changed as things become visible that were not visible at the start of
a phase.

Its frozen counterpart is [PLAN-PROMPT.md](PLAN-PROMPT.md), the original plan, recorded before the
work began and never edited. The difference between the two is evidence of how the project actually
went, which the course asks about at the viva.

## Marking progress

- `[ ]` not started, `[~]` in progress, `[x]` done.
- **An item without a checkable result is not an item.** Every step names what confirms it is done
  ([collaboration.md](collaboration.md) §4).
- The **Open questions** section of each phase file is not decorative. If there really are none, write
  that there are none.

## Closing a phase

A phase closes when its **Readiness criterion** is met, not when every checkbox is ticked: the list of
steps is incomplete by definition, and the criterion is the reference point for the next phase.

When a phase closes, its file records the date and how the criterion was verified — the command that
was run, or what was checked by hand. "Done" without evidence is what the mid-demo rubric penalises.

**A phase does not close until it has been audited:** every claim its documents make, checked against
what the repository actually contains. The findings go in the phase file, including "none".
`ai-tools docs-check` catches documents that name something missing; the audit remains because that
check compares names and links while the audit reads for meaning — a hook described as doing something
other than what it does passes every automated check.

Three built-in checkups belong to the audit, because they read the configuration rather than the
prose:

- `/doctor` — proposes cuts to `CLAUDE.md`, dropping what an agent could derive from the code.
- `/skill-doctor` — names the skills that never fire. A command nobody's request matches is
  indistinguishable, from the inside, from one that works.
- `/context` — shows what actually loaded this session, the only way to confirm that a path-scoped
  rule in `.claude/rules/` reaches the model.

Record their output in the phase's audit and say which of the three were run. An unrun checkup is not
evidence.

## Changing the plan

Adding an item to the current phase is the agent's business. Moving work between phases, or changing a
readiness criterion, is the human's: those decisions change what is delivered by a deadline.

When a deadline is at risk, say so in the phase file **and** in the answer, with what would have to be
dropped. A roadmap that stays green while the work slips is worse than no roadmap. So that this does
not depend on being remembered, `scripts/session-start.sh` prints the next course deadline, the days
remaining and the open item count of the running phase at the start of every session.

## Session notes between working days

A long piece of work inside a phase may leave a note beside the phase file, named
`phaseN-session-YYYY-MM-DD.md`, so the next session resumes without re-deriving the context.

- **It supersedes, it does not accumulate.** A new note replaces the previous one and says so.
- **It names the condition under which it is deleted**, checkable by someone else — "once the `FR id`
  column has no empty cells", not "once this is finished".
- **Nothing lives only there.** A decision goes into the phase file, a requirement into
  `docs/requirements/`, a convention into the document that owns it. Check a note for that before
  deleting it: the prompt journal records what was said, but a convention adopted in passing is not a
  decision anybody will think to look for.

## The relationship with the course stages

Each phase ends at a course deadline, so the roadmap and the course calendar are the same schedule
seen from two sides. [docs/course/rubric.md](../course/rubric.md) maps the marks to the artefacts; the
roadmap maps the work to the dates. When they disagree, the course calendar wins.
