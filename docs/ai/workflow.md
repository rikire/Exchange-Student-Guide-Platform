# The working cycle

The order of the steps makes the expensive decisions before code is written under them.

## The feature cycle

```
confirmed contract -> /feature -> clarify and agree -> ADR (if needed)
   -> routes -> migration -> slice interfaces
   -> loop( red test -> minimal implementation -> refactor )
   -> documentation -> trace check -> /dod -> commit
```

The loop repeats once per acceptance criterion in the feature file.

### 0. Contract

State the contract and wait for its confirmation: [prompting.md](prompting.md). The confirmed
sentences are reused below: they become the acceptance criteria in the feature file and the names of
the tests.

### 1. Open the feature — `/feature`

`docs/features/FEAT-XXX-*.md` appears: goal, requirements covered, routes involved, schema impact,
acceptance criteria and an explicitly drawn scope boundary. No code at this step.

### 2. Clarify and agree

Everything ambiguous is settled **here**, not while writing code. Stop triggers:
[stop-and-ask.md](stop-and-ask.md).

The wording of requirements, the scope of the feature and the set of routes are the human's decision
([collaboration.md](collaboration.md)). Without agreement, work does not start.

### 3. ADR, if the decision is architectural — `/adr`

The sign: the decision would be expensive to reverse, or it has a non-obvious alternative. The
moderation state machine, the search engine, the media storage layout are ADRs; a package name is not.

### 4. The route contract

Routes before controllers. `docs/architecture/ui-routes.md` is the source of truth: path, slice,
template, form fields, response codes, and `trace: FR-XXX`. It is the server-rendered equivalent of an
API specification, and the documentation gate treats it that way.

### 5. The schema

A Flyway migration with `-- trace: FR-XXX` in its header, checked **in both directions** where a down
path exists. `data-model.md` and the ERD change with the migration.

The schema lives in `shared/persistence` and is common to every slice, so a migration is one of the
few places where two people collide. After phase 2 it changes only by agreement.

### 6. Slice interfaces

Designed before the implementation exists. Each layer has its own notion of "interface":

| Level | What counts as the interface here |
|---|---|
| Routes | The entry in `ui-routes.md`: path, method, response codes, form fields |
| Slice | The types that sit directly in the slice package — everything else is internal |
| Between slices | A published type in the slice package, or an `ApplicationEvent` |
| Domain | The types and their invariants: what may exist at all |

Dependency direction: [architecture-rules.md](architecture-rules.md).

### 7. The TDD loop

Creating a production class under `app/src/main/java/**` with no matching test asks first, naming the
test file it expected (`TestFirstRule`, via the `PreToolUse` guard). It asks rather than refuses: a
class split out of one already covered is a real exception, and a gate with no way to answer it gets
switched off.

Repeated for each acceptance criterion:

1. **Red test.** One criterion, one test; the name repeats the wording of the criterion. Run it and
   see it **fail**: a test that is green before the implementation checks nothing, and running it is
   the only way to notice. When a test goes red later, decide first whether the code or the
   expectation is wrong; an expectation rewritten to match the output is a deleted test
   ([definition-of-done.md](definition-of-done.md)).
2. **Minimal implementation** — exactly enough to turn the test green. Code written "while we are
   here" is neither covered nor requested.
3. **Refactor** with the tests green: change the structure, not the behaviour.

The production code is marked `//trace:FR-XXX`; so is the test. That chain — acceptance criterion,
test, marker — is what makes the matrix mean coverage of criteria.

**Layers that need infrastructure.** The rule holds there too, and has to be cheap to follow:

- Web layer — a red test through `MockMvc` against the controller, without a database.
- Persistence — a red test against H2 by default; the PostgreSQL profile with Testcontainers runs in
  CI and before a release, not on every loop, because a container per test makes the cycle slow
  enough that people skip the order.
- If standing up the environment for a red test is genuinely impossible, stop and ask
  ([stop-and-ask.md](stop-and-ask.md)); do not quietly swap the steps around.

How the tests are written — what to assert, what to mock, how to derive the corner cases:
[testing.md](testing.md).

### 7a. Before writing it, find out whether it already exists

Applies inside the loop, at the moment a helper is about to be written.

**Name the library that already does this** — the JDK, Spring, Apache Commons, Guava, Tika — and say
why it does not fit, or use it. The honest reasons to write your own are: nothing does it, the library
is far larger than the need, or it is unmaintained.

**"It is only a few lines" is not one of them.** A few lines is how every wheel starts, and the
library carries the edge cases you have not thought of yet: walk the encoding dimension in
[testing.md](testing.md) against any string helper that looks trivial.

For a problem bigger than a helper, ask **"what is this called"**: if it has a name, somebody has
already made the mistakes. Find the name before designing; when to look and when to run something
instead is in [collaboration.md](collaboration.md) §3.

Replacing a library with our own code is the same decision as adding one and is proposed the same way
([collaboration.md](collaboration.md) §1). Creating a file whose name suggests a wheel — `*Utils`,
`*Helper`, `*Formatter` — asks about this before it lands.

### 8. Technical debt

If a workaround appeared along the way, or something was done temporarily, the entry is created
**now**, not at the end of the task. Rules below.

### 9. Documentation

Rules: [docs-sync.md](docs-sync.md). In the feature file, `code`, `tests` and `status` are updated; a
requirement moves to `done` only when both the code and the test exist.

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

**Nothing temporary stays unrecorded.** The register is [docs/tech-debt.md](../tech-debt.md), which
also says what is debt and what is a deliberate narrowing of scope (`CON-XXX`).

- **Debt is recorded the moment it appears.** By the end of the task the context is gone and the entry
  comes out useless.
- **A workaround without a `DEBT` entry is unfinished work.** That includes wording in answers: if you
  said "for now" or "we will change this later", create the entry.
- **The marker in the code is mandatory and references the register:** `// TODO(DEBT-007): ...`.
- **Paying debt off is its own commit** — `refactor(DEBT-007): ...`. In that commit the status
  becomes `resolved` and the marker is removed.
- The agent creates register entries freely; that needs no confirmation.

## Rules on top of the cycle

**One turn, one meaningful change.** Do not mix refactoring with new functionality: in a combined diff
neither stays reviewable.

**Unfinished is called unfinished.** Reporting "done" with a failed Definition of Done item is worse
than "this is done, that is left".

The prompt, the outcome and the human's edits are recorded by the hooks in `docs/ai/journal/`; to add
your own words, use `/journal-note`.
