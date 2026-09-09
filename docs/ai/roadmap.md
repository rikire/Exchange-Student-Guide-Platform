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

Three built-in checkups belong to the same audit, because they read the configuration rather than
the prose, and they see things a person rereading their own documents will not:

- `/doctor` — proposes cuts to `CLAUDE.md`, dropping what an agent could derive from the code.
- `/skill-doctor` — names the skills that never actually fire. A command nobody's request ever
  matches is indistinguishable, from the inside, from one that works.
- `/context` — shows what actually loaded this session, which is the only way to confirm that a
  path-scoped rule in `.claude/rules/` reaches the model at all.

Record their output in the phase's audit. An unrun checkup is not evidence, and the closing note
should say which of the three were run rather than implying all of them.

## Changing the plan

Adding an item to the current phase is the agent's business. Moving work between phases, or changing
a readiness criterion, is the human's: those decisions change what gets delivered by a deadline.

When a deadline is at risk, say so in the phase file **and** in the answer, with what would have to
be dropped. A roadmap that stays green while the work slips is worse than no roadmap.

This rule went thirteen days without firing once, so it no longer depends on being remembered:
`scripts/session-start.sh` prints the next course deadline, the days remaining and the open item
count of the running phase at the start of every session. A date is harder to argue with than a
recollection.

## Session notes between working days

A long piece of work inside a phase may leave a note beside the phase file, named
`phaseN-session-YYYY-MM-DD.md`, so the next session resumes without re-deriving the context. Two of
these carried phase 1 through requirement-writing on 5 and 6 September, and the convention is
recorded here because it was in use and written down nowhere.

Three rules, all of which those two followed and which is why this works:

- **It supersedes, it does not accumulate.** A new note replaces the previous one and says so. Two
  notes describing the same work are two answers to "where are we".
- **It names the condition under which it is deleted**, and that condition is checkable by someone
  else — "once the `FR id` column has no empty cells", not "once this is finished".
- **Nothing lives only there.** A decision goes into the phase file, a requirement into
  `docs/requirements/`, a convention into the document that owns it. The note holds the position of
  the work, not its results.

The last rule is the one that costs something. When those two notes were deleted on 7 September
their condition had been met, but they still held two adopted rules — prefer a glossary term to a
cross-reference, and treat "not yet approved" and "rejected" as two scenarios — that existed in no
permanent document. Both were moved into `docs/requirements/` first. **Check a note for that before
deleting it**; the prompt journal records what was said, but a convention adopted in passing is not
a decision anybody will think to look for.

## The relationship with the course stages

Each phase ends at a course deadline, so the roadmap and the course calendar are the same schedule
seen from two sides. `docs/course/rubric.md` maps the marks to the artefacts; the roadmap maps the
work to the dates. When they disagree, the course calendar wins — it is not negotiable.
