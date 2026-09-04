# Keeping documentation in step with code

Documentation in this repository is part of the delivery, not text that accompanies it. Documentation
that disagrees with the code is a defect of the same weight as a failing test.

There is one rule: **a change in behaviour and a change in its description happen in the same turn.**
Not "later", not "in a tidy-up commit at the end".

## The mapping

| What changed | What must change with it | Strictness |
|---|---|---|
| `app/src/main/resources/db/migration/**` | `docs/architecture/data-model.md` or the ERD source | blocks |
| `shared/persistence/**` | `docs/architecture/data-model.md` or the ERD source | blocks |
| A controller or `templates/**` | `docs/architecture/ui-routes.md` | blocks |
| A published type in a slice package | `docs/architecture/overview.md`, an ADR, or the feature file | blocks |
| A slice's internals | The feature file — the `code`, `tests` and `status` fields | warns |
| `pom.xml` | An ADR or `docs/architecture/overview.md` | warns |

The rules look only at files with substance: editing a README next to a migration changes neither
the schema nor the contract and does not raise the gate.

There are deliberately few rules. A gate that fires on every refactor gets worked around with
`--no-verify`, and then it protects nothing at all. Only the contract, the schema and the boundary
between slices block — the three things whose divergence is most expensive.

## How it works

1. **At edit time.** The `PostToolUse` hook notices a change in a tracked area and says what must now
   be updated. Once per rule per session.
2. **At the end of the turn.** The `Stop` hook runs the check and does not let the turn finish while
   the divergence stands. It does not block twice on the same cause — otherwise the session loops
   with no way for a person to intervene.
3. **In CI.** The same check on every pull request, as insurance against hooks disabled locally.

Manual run:

```bash
java -jar tools/target/ai-tools.jar trace --docs-sync HEAD   # phase 2
```

The generator lands in phase 2. Until then this rule is followed by reading the table above, and
the `Stop` gate that would enforce it does not run — which is stated here rather than left to be
discovered.

## What "update the document" means

Update it in substance, not cosmetically.

**Bad:** a line added to `data-model.md` saying "added the `media_assets` table".

**Good:** what the table is for, why the checksum is stored alongside the generated filename, which
indexes exist and which queries they serve, and how that is reflected in the ERD.

If it is unclear how the change alters the described behaviour, do not rewrite at random — stop and
ask ([stop-and-ask.md](stop-and-ask.md)).

## Generated documents

These files are written **only by the generator**:

- `docs/traceability.md`
- `docs/features/README.md`
- `docs/team/ownership.md`
- `docs/gap-list.md`
- `docs/diagrams/out/**`

Each carries a `GENERATED` marker in its header. A hand edit is detected and breaks the build. That
is not over-caution: a generated file edited by hand creates false confidence that the state is
current, which is worse than no file at all.
