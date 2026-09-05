# How the tests are written

The cycle — red, green, refactor — is in [workflow.md](workflow.md) §7. The style rules are in
[code-style.md](code-style.md). This document is the part neither of those covers: what a test is
*for*, what to point it at, and how to derive the cases nobody thinks of under deadline.

Written before the application code exists, deliberately. Otherwise the first slice becomes the
place where the conventions get improvised, and every slice after it copies whatever that one did.

## The spine: a real database, mocks only at the edges

A slice test goes through H2 with the **real Flyway migrations**. The PostgreSQL profile runs before
a stage submission, not on every loop ([workflow.md](workflow.md) §7).

Mocked: the clock, the source of randomness, the file system. Those three make a test
non-deterministic, and nothing else here does.

**Not mocked: the repository.** A mocked repository asserts that the code called what we thought it
would call — which is a restatement of the implementation, not a check of it. It cannot tell you the
query is wrong, and refactoring breaks a suite full of them all at once. It also makes the N+1 rule
in [security.md](security.md) uncheckable: counting queries needs a real Hibernate session.

## What a red test has to prove

**Run it and read the message.** A test that fails because the class does not compile yet has proved
nothing about the assertion — only that the code is absent, which you knew.

The failure message is the specification. If it reads *expected true but was false*, the test cannot
tell whoever meets it in six months what was supposed to happen. Assert on values, and say what was
expected in the message when the values do not speak for themselves.

When a green test later turns red, decide **first** whether the code or the expectation is wrong. An
expectation rewritten to match the output is a deleted test wearing a passing badge
([definition-of-done.md](definition-of-done.md)).

## Naming and shape

One behaviour per test, named as a sentence about the behaviour, in the style already used in
`tools/`:

```java
a_marker_that_names_its_debt_entry_is_allowed()
a_bypass_flag_in_a_neighbouring_segment_does_not_condemn_the_whole_line()
```

The name repeats the wording of the acceptance criterion it came from, which is what makes the
traceability matrix mean coverage of criteria rather than the existence of a file with tests in it.

**Write down why a test exists when the reason is not obvious.** Several tests in `tools/` carry two
lines saying which real failure they came from. That comment is what stops the next person deleting
the test because it looks redundant.

## What to assert

Assert **behaviour**: what came back, what was stored, what the visitor sees. Not private state, not
the order of calls, not how many times something was invoked.

The one exception is where the number *is* the behaviour: the query count of a page. That is not an
implementation detail here, it is the difference between a page and an outage.

## The shape per layer

| Layer | Test |
|---|---|
| `wikilink` | plain Java, no Spring context — this is why the slice was designed that way, and it is what makes writing the test first cheap enough to actually do |
| A slice's logic | through its **published type**, never through an internal class |
| Persistence | H2 with the real migrations |
| Web | `MockMvc`, no database where the controller only forwards |
| Slice boundaries | already held by `ModularityTest` and ArchUnit — do not re-test them by hand |

`@SpringBootTest` is the last resort, not the default: it is slow, and a slow suite stops being run.

## Fixtures

Builders with sensible defaults, so a test names **only what matters to it**:

```java
anArticle().withTitle("Hostel check-in").submitted()
```

A shared SQL dump does the opposite: the test's meaning depends on rows it never mentions, and the
day someone edits the dump, tests fail in files they did not touch. Fixtures live next to the test
that uses them ([code-style.md](code-style.md)).

## Forbidden, and what enforces it

| Rule | Held by |
|---|---|
| An expectation edited to match the output | good faith and review — no mechanism can see intent |
| `@Disabled` with no `DEBT-XXX` | **refused before the edit lands** |
| `Thread.sleep` in a test | **refused before the edit lands** |
| A `@Test` that asserts nothing | **asked about before the edit lands** |
| Tests that depend on each other's order | good faith |

A disabled test is worse than a deleted one: the suite still reports that everything passes. If it
must be switched off, `@Disabled("DEBT-007: ...")` with an entry in [../tech-debt.md](../tech-debt.md).

A sleep is the standard source of a suite that fails once in twenty runs — and a suite like that
teaches everybody to re-run rather than to read. Wait for the condition, or control the clock. If
the code genuinely needs real time to pass, the **code** needs the seam, not the test.

A test with no assertion passes for as long as nothing throws. If that is the point, say so with
`assertDoesNotThrow` rather than leaving it implied.

## Not worth testing

Getters, the framework, generated code, and a controller that only forwards to a service. Coverage
is a floor, not a goal ([code-style.md](code-style.md)): a test written to move a number is a test
nobody will maintain.

## How to find the corner cases

Corner cases are **derived**, not recalled. This section is the derivation.

That distinction matters more than it sounds. A list of specific cases — "a link to a missing page
renders as *wanted*", "SVG is rejected" — is a list of **requirements**, and requirements live in
`docs/requirements/`. Copying them here would create a second source of truth that drifts from the
first, which is the defect this repository keeps auditing itself for. What belongs here is the
method, because the method still works on a feature nobody has written yet.

### 1. Write the input contract first

One sentence: what may arrive, from whom, and what is promised back. If you cannot write it, there
is nothing to test yet — that is a sharpening problem, not a testing one
([prompting.md](prompting.md)).

The contract is what the cases are derived from. Deriving them from the implementation instead
produces tests that describe the code as written, including its bugs.

### 2. Walk the dimensions

Ten questions, asked of every input. Most will not apply; asking is cheap and remembering is not.

| Dimension | The question | Live here because |
|---|---|---|
| **Emptiness** | null, empty, whitespace-only, field absent entirely | forms submit empty strings, not nulls, and the two take different paths |
| **Size** | zero, one, many, the limit, one under, one over | off-by-one lives here or nowhere |
| **Encoding** | non-ASCII, several code points per visible character, two normalisation forms of one string, invisible characters, a null byte | article text is Hindi and Tamil as well as English, so this is not theoretical |
| **Type confusion** | the value says it is one thing and is another | anonymous visitors upload files, and a declared type is attacker-controlled |
| **Structure** | unbalanced delimiters, nesting, self-reference, a cycle | anything parsed, and anything that forms a graph |
| **Ordering and time** | ties, clock boundaries, two events in one millisecond, a zone that is not the developer's | ordering that "just works" locally is ordering nobody specified |
| **Concurrency** | the same operation twice at once; the thing changing under you mid-operation | two moderators, two authors, one article |
| **State** | every transition **not** in the diagram; an operation whose prerequisite has since changed or gone | a queue with states has more illegal transitions than legal ones |
| **Trust** | who chose this value, and what does it reach — a query, a path, a page, a log line | there are no accounts: every input is chosen by a stranger |
| **Interruption** | the operation stops halfway — a dropped connection, a failure between two writes | a half-written file or a half-applied change is a state the happy path never visits |

**Platform reality counts as a dimension too.** One of us develops on Windows and one on macOS, so
path separators, reserved filenames, case-sensitivity of the filesystem and the default locale
differ between our two machines. A test that passes on one and not the other is not flaky; it found
something.

The locale one is worth naming because it is invisible: `toLowerCase()` with no locale turns `I`
into `ı` under a Turkish default. Any folding — for comparison, for a key, for a lookup — takes an
explicit `Locale.ROOT`, and the test that proves it runs with the locale switched rather than with
whichever one the machine happens to have.

### 3. Keep the cases that change the behaviour

Not all of them. Two inputs that travel the same path and produce the same outcome are **one** test;
writing both doubles the maintenance and proves the same thing twice. Coverage is a floor, not a
goal ([code-style.md](code-style.md)).

The ones to keep are where the answer *differs* — a boundary, a branch, a different error, a
different audience for the message.

### 4. A case with no stated answer is a question, not a guess

This is the step that gets skipped, and it is the point of the whole exercise.

The dimensions above will produce cases the requirements never decided: what happens to two titles
that differ only in case, whether search folds diacritics, which spelling is displayed when writers
disagree. **Do not answer these in a test.** A test is the worst place to make a product decision:
it looks like a fact, it is never read as a decision, and nobody who disagrees with it will ever
find out that it was one.

Take it to the human, record the answer where such answers live — a requirement, a constraint, an
ADR — and *then* write the test that pins it. The test cites the decision; it does not contain it.

**And it works in the other direction.** A derived case whose answer nobody has decided is a gap in
the requirements found early and cheaply. That is a good outcome, not an obstacle: it costs a
question now instead of a rewrite later.

## Techniques beyond example-based tests

Four candidates. Each is a dependency, and therefore the human's decision
([collaboration.md](collaboration.md)) — they are named here as proposals, not as settled.

| Technique | Tool | Where it earns its place |
|---|---|---|
| Assert the number of queries | `db-util` — `SQLStatementCountValidator` | the mechanism the N+1 rule needs; seed one row, then ten, and fail if the count moves |
| Property-based tests | `jqwik`, a JUnit 5 engine | **`wikilink` only** — a parser is exactly the shape properties suit: "parse then render returns the input" covers inputs nobody would list |
| Mutation testing | PIT | by hand before a stage, never in CI. It answers what coverage cannot — whether the assertions catch anything — and it is far too slow for a per-push gate |
| PostgreSQL parity | Testcontainers | a separate profile before a stage submission. Not per push: `pre-push` is already 27 seconds, which is the top of what gets tolerated before people start skipping it |

## Where this stops

Two rules here are held by nothing but good faith: not rewriting an expectation to match the output,
and not writing tests that depend on each other's order. Detecting either needs intent rather than
text. They are listed as unenforced in [README.md](README.md), where the rest of the honest
accounting lives.
