# Definition of Done

A task is not done until **every** item has passed. Items are checked by running commands, not from
memory: "should work" and "works" are different states.

Run the whole list with `/dod`.

## 1. It builds and passes static analysis

```bash
./mvnw -q verify          # compile + tests + Spotless
```

Spotless failing is not a formatting nit — it means the diff carries noise that makes review harder
for the other person.

## 2. Tests

```bash
./mvnw test
./mvnw -P postgres verify   # only when the task touches persistence
```

- The test was written **before** the implementation and failed at that moment — the order from
  [workflow.md](workflow.md), not a preference.
- Every acceptance criterion in the feature file is closed by its own test.
- The test checks behaviour, not implementation: renaming a private method must not break it.
- A test for a fixed bug reproduces the exact scenario that broke.

**A red test says one of two things: the code is wrong, or the expectation is wrong. Work out which,
before changing either.**

An expectation edited to match the actual output is not a test that passed — it is a test that was
deleted, and deleted in the least visible way, because the suite is green afterwards. If the
expectation really was wrong, say so in as many words and say why the new one is right; that
sentence is what a reviewer needs and what the diff cannot show.

This is not hypothetical. It happened here on 3 September, on a throwaway palindrome check: an
expectation was wrong, the run went red, and the expectation was rewritten to match the output. The
outcome was correct by luck — the expectation genuinely had been wrong — and the move was still the
one that makes a suite stop meaning anything.

## 3. Schema and migrations

- The migration has `-- trace: FR-XXX` in its header.
- It has been run against an empty database and against one with data in it.
- It does not lose data silently. If loss is unavoidable, that is a reason to stop and ask.
- `data-model.md` and the ERD describe the new shape.

## 4. Routes

- Every new or changed route is in `docs/architecture/ui-routes.md`: path, slice, template, form
  fields, response codes, `trace`.
- Error paths are covered too, not only the happy one: what a visitor sees on 404 and on a rejected
  form.

## 5. Slice boundaries

```bash
./mvnw -pl app test -Dtest=ModularityTest
```

- Nothing new was moved into `shared/` without agreement.
- No slice reaches into another slice's internals.

## 6. Documentation

Rules: [docs-sync.md](docs-sync.md).

```bash
java -jar tools/target/ai-tools.jar trace --docs-sync HEAD
```

Documentation updated **in substance**: it describes the new behaviour, not the fact that an edit
happened.

## 7. Traceability

```bash
java -jar tools/target/ai-tools.jar trace
```

- New code carries `//trace:FR-XXX`; so does the test.
- The status of the requirement and of the feature reflects reality.
- The gaps section of `docs/traceability.md` did not grow unexplained rows.

## 8. Technical debt

Register: [docs/tech-debt.md](../tech-debt.md), process: [workflow.md](workflow.md).

- Not one `TODO`, `FIXME` or `HACK` without a `DEBT-XXX` reference.
- Anything done temporarily or as a workaround is in the register **with its consequence and a plan
  to fix it**, not a one-line "redo this".
- If the answer said "for now" or "we will change this later", the entry exists.
- Debt that was paid off is removed from the register and from the code in the same commit.

## 9. Security

For anything touching uploads, forms or the admin area, walk [security.md](security.md) explicitly.
Uploads especially: type detected from content, size limited, filename generated, delivery path
checked.

## 10. The double check

This is a separate step, not a formality. It happens **after** all the others.

1. Re-read your own diff in full: `git diff HEAD`.
2. For each file, answer:
   - **Is this needed?** Code added "for later" and unused now — delete it.
   - **Is any debugging left?** Console output, commented-out code, temporary files.
   - **What happens on error?** Every exception either handled or deliberately wrapped and rethrown.
   - **What happens with empty data?** An empty list, a null, a missing record, a zero-length file.
   - **Does neighbouring code still work?** A changed signature, a new field, a different order.
3. Check the boundaries separately: access to someone else's data, input validation, parameterised
   queries, output escaping in templates.

## 11. Contribution record

- The commit follows `<type>(FEAT-XXX): <subject> [FR-XXX]`.
- The work is visible in this week's log — `/weekly-log` if it was not code, or code that has not
  been committed yet.

## 12. The report

At the end of the answer, honestly list:

- what was done;
- what was **not** done and why;
- which items of this list did not pass, if any;
- assumptions made;
- decisions taken alone, one line each ([collaboration.md](collaboration.md)).

A "done" report with an item unpassed is worse than "this part is done, that part is left".
