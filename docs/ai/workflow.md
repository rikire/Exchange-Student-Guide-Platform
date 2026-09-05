# The working cycle

The order of the steps is not arbitrary: it is arranged so that expensive decisions are made before
code is written under them.

## The feature cycle

```
sharpen the request -> /feature -> clarify and agree -> ADR (if needed)
   -> routes -> migration -> slice interfaces
   -> loop( red test -> minimal implementation -> refactor )
   -> documentation -> trace check -> /dod -> commit
```

The loop repeats once per acceptance criterion in the feature file.

### 0. Sharpen the request

Before anything else, if the request is underspecified in a way that changes the work: restate it
precisely, name what is unclear, and name the requirements it implies but does not state. Rules and
the cases where this would just be noise: [prompting.md](prompting.md).

This step is cheap and it is where the whole cycle either aims at the right target or does not. The
sharpened wording is reused below — it becomes the acceptance criteria in the feature file and the
names of the tests.

### 1. Open the feature — `/feature`

`docs/features/FEAT-XXX-*.md` appears: goal, requirements covered, routes involved, schema impact,
acceptance criteria and an explicitly drawn scope boundary. No code at this step.

### 2. Clarify and agree

Everything ambiguous is settled **here**, not while writing code. Stop triggers are in
[stop-and-ask.md](stop-and-ask.md).

The wording of requirements, the scope of the feature and the set of routes are the human's
decision, not the agent's: [collaboration.md](collaboration.md). Agreement received — carry on;
not received — work does not start.

### 3. ADR, if the decision is architectural — `/adr`

The sign: the decision would be expensive to reverse, or it has a non-obvious alternative. The
moderation state machine, the search engine, the media storage layout — those are ADRs. A package
name is not.

### 4. The route contract

Routes before controllers. `docs/architecture/ui-routes.md` is the source of truth: path, slice,
template, form fields, response codes, and `trace: FR-XXX`. This is the server-rendered equivalent
of an API specification, and the documentation gate treats it that way.

### 5. The schema

A Flyway migration with `-- trace: FR-XXX` in its header. Checked **in both directions** where a
down path exists. `data-model.md` and the ERD are updated together with the migration.

The schema lives in `shared/persistence` and is common to every slice, so a migration is one of the
few places where two people genuinely collide. After phase 2 it changes only by agreement.

### 6. Slice interfaces

Designed before the implementation exists — each layer has its own notion of "interface":

| Level | What counts as the interface here |
|---|---|
| Routes | The entry in `ui-routes.md`: path, method, response codes, form fields |
| Slice | The types that sit directly in the slice package — everything else is internal |
| Between slices | A published type in the slice package, or an `ApplicationEvent` |
| Domain | The types and their invariants: what may exist at all |

Dependency direction: [architecture-rules.md](architecture-rules.md).

### 7. The TDD loop

Repeated for each acceptance criterion:

1. **Red test.** One criterion, one test; the test name repeats the wording of the criterion. Run
   it and see it **fail**: a test that is green before the implementation checks nothing, and this
   is the only way to notice that.
   When a test goes red later, first decide whether the code or the expectation is the wrong one —
   an expectation rewritten to match the output is a deleted test
   ([definition-of-done.md](definition-of-done.md)).
2. **Minimal implementation** — exactly enough to turn the test green. Nothing beyond the test:
   code written "while we are here" is neither covered nor requested.
3. **Refactor** with the tests green. Change the structure without changing behaviour.

The production code is marked `//trace:FR-XXX`; so is the test.

**Layers that need infrastructure.** The rule holds there too, but it has to be cheap to follow:

- Web layer — a red test through `MockMvc` against the controller, without a database.
- Persistence — a red test against H2 by default; the PostgreSQL profile with Testcontainers runs
  in CI and before a release, not on every loop. A container per test makes the cycle unbearably
  slow, and then people start skipping the order.
- If standing up the environment for a red test is genuinely impossible, that is a reason to stop
  and ask ([stop-and-ask.md](stop-and-ask.md)), not to quietly swap the steps around.

**The link to traceability.** Acceptance criterion to test to `//trace:FR-XXX`. That is what makes
the matrix mean coverage of criteria rather than the existence of a file with tests in it.

How the tests themselves are written — what to assert, what to mock, and how to derive the corner
cases: [testing.md](testing.md).

### 7a. Before writing it, find out whether it already exists

Applies inside the loop, at the moment a helper is about to be written.

**Name the library that already does this** — the JDK, Spring, Apache Commons, Guava, Tika — and say
why it does not fit, or use it. The honest reasons to write your own are: nothing does it, the
library is far larger than the need, or it is unmaintained.

**"It is only a few lines" is not one of them.** A few lines is how every wheel starts, and the
library carries the edge cases you have not thought of yet — walk the encoding dimension in
[testing.md](testing.md) against any string helper that looks trivial, and it stops looking
trivial.

For a problem bigger than a helper, the question is not "which library" but **"what is this
called"**. If the thing being built has a name, somebody has already made the mistakes: reviewed
revisions, optimistic locking, diff algorithms, per-language analysers, token buckets. Find the name
before designing. When to go and look, and when running something locally answers it faster:
[collaboration.md](collaboration.md) §3.

**Why this needs saying at all.** Adding a dependency is the human's decision and writing a private
helper is not, so asking costs a round trip and writing does not. Left alone, that arithmetic points
away from the library every single time — which is why **replacing a library with our own code is
the same decision as adding one**, and gets proposed the same way. Creating a file whose name
suggests a wheel — `*Utils`, `*Helper`, `*Formatter` — asks about this before it lands.

### 8. Technical debt

If a workaround appeared along the way, or something was done temporarily, the entry is created
**now**, not at the end of the task. Details below.

### 9. Documentation

Rules: [docs-sync.md](docs-sync.md). In the feature file, `code`, `tests` and `status` are updated;
a requirement moves to `done` only when both the code and the test exist.

### 10. Checks

```bash
./mvnw verify
scripts/check.sh
/dod
```

### 11. Commit

```
feat(FEAT-003): add article submission form [FR-012]
```

The format is checked by the `commit-msg` hook. Bypassing checks with `--no-verify` is not allowed.

## Technical debt

The rule: **nothing temporary stays unrecorded.** The register is [docs/tech-debt.md](../tech-debt.md).

- **Debt is recorded the moment it appears.** Not "I will add it at the end": by the end of the task
  the context is gone and the entry comes out useless.
- **A workaround without a `DEBT` entry is unfinished work**, not finished work. This applies to
  wording in answers too: if you said "for now" or "we will change this later", create the entry.
- **The marker in the code is mandatory and must reference the register:** `// TODO(DEBT-007): ...`.
- **Paying debt off is its own commit** — `refactor(DEBT-007): ...`. In that same commit the status
  becomes `resolved` and the marker is removed from the code.
- The agent may create register entries freely; this does not need the human's confirmation.

What is debt and what is a deliberate narrowing of scope (`CON-XXX`) is explained in the register
itself.

## Rules on top of the cycle

**One turn, one meaningful change.** Do not mix refactoring with new functionality: in a combined
diff neither one stays reviewable.

**First what does not depend on the question.** If a question comes up, finish everything that is
true under any answer, and only then stop.

**Unfinished is called unfinished.** Reporting "done" with a failed Definition of Done item is worse
than an honest "this is done, that is left".

**Test before code.** The order is not cosmetic: a test written after the implementation checks what
was built, not what was required.

**Debt is recorded immediately.** "I will document it later" never works.

**The journal writes itself.** The prompt, the outcome and the human's edits are recorded by the
hooks in `docs/ai/journal/`. To add your own words, use `/journal-note`.
