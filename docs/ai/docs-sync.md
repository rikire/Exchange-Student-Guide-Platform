# Keeping documentation in step with code

Documentation is part of the delivery. Documentation that disagrees with the code is a defect of the
same weight as a failing test.

One rule: **a change in behaviour and a change in its description happen in the same turn.** Not
"later", not "in a tidy-up commit at the end".

## The mapping

| What changed | What must change with it | Strictness |
|---|---|---|
| `app/src/main/resources/db/migration/**` | `docs/architecture/data-model.md` or the ERD source | blocks |
| `shared/persistence/**` | `docs/architecture/data-model.md` or the ERD source | blocks |
| A controller or `templates/**` | `docs/architecture/ui-routes.md` | blocks |
| A published type in a slice package | `docs/architecture/overview.md`, an ADR, or the feature file | blocks |
| A slice's internals | The feature file — the `code`, `tests` and `status` fields | warns |
| `pom.xml` | An ADR or `docs/architecture/overview.md` | warns |

The rules look only at files with substance: editing a README next to a migration changes neither the
schema nor the contract and does not raise the gate.

Only the contract, the schema and the boundary between slices block: the three things whose
divergence is most expensive. A gate that fires on every refactor gets worked around with
`--no-verify`, and then protects nothing.

## How it works

1. **At edit time.** The `PostToolUse` hook (`ai-tools hook docs-sync`, since 9 September) names the
   document that has just gone out of date, once per tracked area per session. It reads this table
   through `DocumentedCounterparts`, so the table and the mechanism cannot drift apart.
2. **At the end of the turn.** The `Stop` hook runs the check and does not let the turn finish while
   the divergence stands. It does not block twice on the same cause, so the session cannot loop with
   no way for a person to intervene.
3. **In CI.** The same check on every pull request, in case hooks are disabled locally.

```bash
java -jar tools/target/ai-tools.jar trace --docs-sync HEAD   # phase 2
```

Steps 2 and 3 are phase 2 for this table. Until they land the reminder is only a reminder: nothing
refuses a turn that ignores it. What the `Stop` hook and CI run today is `docs-check`, which finds
documents claiming something the repository lacks, not this mapping.

## What "update the document" means

Update it in substance, not cosmetically.

**Bad:** a line added to `data-model.md` saying "added the `media_assets` table".

**Good:** what the table is for, why the checksum is stored alongside the generated filename, which
indexes exist and which queries they serve, and how that is reflected in the ERD.

If it is unclear how the change alters the described behaviour, do not rewrite at random — stop and
ask ([stop-and-ask.md](stop-and-ask.md)).

## Writing documentation

Every sentence carries a fact, a decision, or a consequence of one. Cut anything that:

- restates the heading, or says why the document exists;
- praises the document's own honesty or thoroughness;
- repeats in prose what a table above it already says;
- tells the story of an incident inside a rule.

A rule keeps the decision, one sentence of reason and a link. The story lives once, in a dated
record: an audit or the journal.

- **Sufficient, not short.** Deleting a sentence that held a decision is a worse defect than leaving
  a long one. Before cutting, ask what a reader would do wrongly without it.
- **No hand-written figure a command can produce.** Hand-written counts drifted in the roadmap and
  the README. Name the command (`ai-tools count`) or leave the number out.
- **A dated record is never rewritten to read better.** Audits, the journal and `PLAN-PROMPT.md` are
  evidence; add a dated note instead.

**Check.** Point at any sentence and name which of the three it carries. If none, it goes. When a
change cuts an existing document, the commit message lists each removed passage with its category
and says where a moved incident now lives.

## Generated documents

These files are written **only by the generator**. No generator exists yet, so until phase 2 (phase 3
for the gap list) a hand edit is the only way any of them has content; once one exists, it owns the
file and a hand edit is lost on the next run.

- `docs/traceability.md`
- `docs/features/README.md`
- `docs/team/ownership.md`
- `docs/gap-list.md`
- `docs/diagrams/out/**` — refreshed by `scripts/diagrams.sh` and **committed**, unlike the four
  above: the architecture documents embed these as images and the forge previews them.

Each file says in its opening lines what writes it and when that generator arrives. There is no
`GENERATED` marker: it was dropped on 10 September because nothing ever read it, and the claim it
carried — that a hand edit is detected and breaks the build — described a check that did not exist
([audit-planning-2026-09-07.md](audit-planning-2026-09-07.md)). A rule with no moment at which it
arrives is cheaper to delete than to keep ([instruction-backlog.md](instruction-backlog.md) IB-003).
