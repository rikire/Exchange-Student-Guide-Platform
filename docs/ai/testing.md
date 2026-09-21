# How the tests are written

The cycle — red, green, refactor — is in [workflow.md](workflow.md) §7; the style rules are in
[code-style.md](code-style.md). This document covers what a test is for, what to point it at, and how
to derive the cases nobody thinks of under deadline.

## The spine: a real database, mocks only at the edges

A slice test goes through H2 with the **real Flyway migrations**. The PostgreSQL profile runs before a
stage submission, not on every loop ([workflow.md](workflow.md) §7).

Mocked: the clock, the source of randomness, the file system. Those three make a test
non-deterministic; nothing else here does.

**Not mocked: the repository.** A mocked repository asserts that the code called what we thought it
would call, which restates the implementation instead of checking it. It cannot tell you the query is
wrong, refactoring breaks a suite full of them at once, and counting queries (the N+1 rule in
[security.md](security.md)) needs a real Hibernate session.

## What a red test has to prove

**Run it and read the message.** A test that fails because the class does not compile yet proves the
code is absent, which you knew, and nothing about the assertion.

The failure message is the specification. "Expected true but was false" tells whoever meets it in six
months nothing: assert on values, and say in the message what was expected when the values do not
speak for themselves.

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

The name repeats the wording of the acceptance criterion it came from, so the traceability matrix
means coverage of criteria and not the existence of a file with tests in it.

Arrange, act and assert are separated by blank lines, with no comments marking the sections.

**Write down why a test exists when the reason is not obvious.** Several tests in `tools/` carry two
lines naming the real failure they came from; that comment stops the next person deleting the test as
redundant.

## What to assert

Assert **behaviour**: what came back, what was stored, what the visitor sees. Not private state, not
the order of calls, not how many times something was invoked.

The exception is where the number *is* the behaviour: the query count of a page is the difference
between a page and an outage.

## The shape per layer

| Layer | Test |
|---|---|
| `wikilink` | plain Java, no Spring context — the slice was designed that way, which makes writing the test first cheap |
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

A shared SQL dump does the opposite: the test's meaning depends on rows it never mentions, and when
someone edits the dump, tests fail in files they did not touch. Fixtures live next to the test that
uses them ([code-style.md](code-style.md)).

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

A sleep is the standard source of a suite that fails once in twenty runs, and that teaches people to
re-run instead of reading. Wait for the condition or control the clock; if the code needs real time to
pass, the **code** needs the seam, not the test.

A test with no assertion passes for as long as nothing throws. If that is the point, say so with
`assertDoesNotThrow`.

## Not worth testing

Getters, the framework, generated code, and a controller that only forwards to a service. Coverage is
a floor, not a goal ([code-style.md](code-style.md)): a test written to move a number is a test nobody
will maintain.

## How to find the corner cases

Corner cases are **derived**, not recalled. A list of specific cases — "a link to a missing page
renders as *wanted*", "SVG is rejected" — is a list of **requirements**, and those live in
`docs/requirements/`; copying them here would create a second source of truth that drifts from the
first. What belongs here is the method, because it still works on a feature nobody has written yet.

### 1. Write the input contract first

One sentence: what may arrive, from whom, and what is promised back. If you cannot write it, there is
nothing to test yet — that is a sharpening problem, not a testing one ([prompting.md](prompting.md)).

Derive the cases from the contract. Deriving them from the implementation produces tests that describe
the code as written, including its bugs.

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

**Platform reality is a dimension too.** Our machines differ — Windows, macOS and Linux have all been
used — so path separators, reserved filenames, filesystem case-sensitivity and the default locale
differ. A test that passes on one and not the other is not flaky; it found something.

The locale case is invisible: `toLowerCase()` with no locale turns `I` into `ı` under a Turkish
default. Any folding — for comparison, for a key, for a lookup — takes an explicit `Locale.ROOT`, and
the test that proves it runs with the locale switched rather than with whichever one the machine has.

### 3. Keep the cases that change the behaviour

Not all of them. Two inputs that travel the same path and produce the same outcome are **one** test.
Keep the ones where the answer *differs*: a boundary, a branch, a different error, a different
audience for the message.

### 4. A case with no stated answer is a question, not a guess

The dimensions will produce cases the requirements never decided: what happens to two titles that
differ only in case, whether search folds diacritics, which spelling is displayed when writers
disagree. **Do not answer these in a test.** A test is the worst place to make a product decision: it
looks like a fact, is never read as a decision, and nobody who disagrees will find out it was one.

Take it to the human, record the answer where such answers live — a requirement, a constraint, an ADR
— and *then* write the test that pins it. The test cites the decision; it does not contain it.

A derived case whose answer nobody has decided is a gap in the requirements found early and cheaply,
which is a good outcome.

## Techniques beyond example-based tests

Four candidates. Each is a dependency, and therefore the human's decision
([collaboration.md](collaboration.md)); they are proposals, not settled.

| Technique | Tool | Where it earns its place |
|---|---|---|
| Assert the number of queries | `db-util` — `SQLStatementCountValidator` | the mechanism the N+1 rule needs; seed one row, then ten, and fail if the count moves |
| Property-based tests | `jqwik`, a JUnit 5 engine | **`wikilink` only** — a parser is the shape properties suit: "parse then render returns the input" covers inputs nobody would list |
| Mutation testing | PIT | by hand before a stage, never in CI: it answers what coverage cannot — whether the assertions catch anything — and is far too slow for a per-push gate |
| PostgreSQL parity | Testcontainers | a separate profile before a stage submission, not per push: `pre-push` is already near the most people tolerate before skipping it |
