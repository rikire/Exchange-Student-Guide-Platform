# Keeping the roadmap

The roadmap lives in [docs/roadmap/](../roadmap/): one file per phase. It is a working document, not
a contract — items are added and changed as things become visible that were not visible at the start
of the phase.

The frozen counterpart is [PLAN-PROMPT.md](PLAN-PROMPT.md): the original plan, recorded before the
work began and never edited afterwards. The difference between the two is itself evidence of how the
project actually went, which is what the course asks about at the viva.

## Marking progress

- `[ ]` not started, `[~]` in progress, `[x]` done.
- **An item without a checkable result is not an item.** Every step names what confirms it is done,
  as required by [collaboration.md](collaboration.md).
- The **Open questions** section of each phase file is not decorative. If there really are no
  questions, write that there are none.

## Closing a phase

A phase closes when its **Readiness criterion** is met, not when every checkbox is ticked. The list
of steps is incomplete by definition; the criterion is the reference point for the next phase.

When a phase closes, its file records the date and how the criterion was verified — the command that
was run, or what was checked by hand. "Done" without evidence is the thing the mid-demo rubric
specifically penalises.

**A phase does not close until it has been audited**: every claim its documents make, checked against
what the repository actually contains. The findings go in the phase file, including "none". This is a
step, not a courtesy — phase 0 passed its own readiness criterion with a green build and CI, and an
audit still found eight divergences, seven of which were documents describing things that were not
there.

`ai-tools docs-check` now catches that class mechanically. The audit remains anyway, because the
check compares names and links while the audit reads for meaning: a hook described as doing something
other than what it does passes every automated check there is.

## Changing the plan

Adding an item to the current phase is the agent's business. Moving work between phases, or changing
a readiness criterion, is the human's: those decisions change what gets delivered by a deadline.

When a deadline is at risk, say so in the phase file **and** in the answer, with what would have to
be dropped. A roadmap that stays green while the work slips is worse than no roadmap.

## The relationship with the course stages

Each phase ends at a course deadline, so the roadmap and the course calendar are the same schedule
seen from two sides. `docs/course/rubric.md` maps the marks to the artefacts; the roadmap maps the
work to the dates. When they disagree, the course calendar wins — it is not negotiable.
