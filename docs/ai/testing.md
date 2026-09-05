# How the tests are written

The cycle — red, green, refactor — is in [workflow.md](workflow.md) §7. The style rules are in
[code-style.md](code-style.md). This document is the part neither of those covers: what a test is
*for*, what to point it at, and the cases nobody invents under deadline.

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

## The corner-case checklist

The part worth writing down, because it is the part nobody invents under deadline. Organised by
**what comes in**, not by which class handles it — a checklist organised by class gets skipped
every time the class is new.

### Text from a visitor — title, body, tag, search query

| Case | Why it bites here |
|---|---|
| Empty, whitespace only, a single character | the commonest crash, and the commonest silently accepted junk |
| At the limit, one under, one over | off-by-one lives here or nowhere |
| Hindi and Tamil text | article content is explicitly not English-only |
| NFC vs NFD spelling of the same word | the same word twice must match in search and produce **one** slug, or one topic quietly becomes two pages |
| Truncation cutting a grapheme cluster | `String.length()` counts UTF-16 code units; Devanagari and Tamil routinely use several per visible character, so a naive substring cuts a letter in half |
| Zero-width characters, RTL marks | two titles that look identical and are not — a moderation problem, not a rendering one |
| `<script>`, `<img onerror=…>`, a `javascript:` URL | test the sanitiser for what it **removes**, not only for what survives it |
| A string shaped like SQL or JPQL | proves parameters are bound rather than concatenated |
| One word of 5000 characters, no spaces | breaks layout and the index, and passes every check on body length |
| A null byte | still terminates strings in the parts of the stack written in C |

### Wiki links

The cheapest tests in the project, because `wikilink` needs no Spring context.

Unclosed `[[`. Nested `[[a[[b]]]]`. Empty `[[]]`. A link to a page that does not exist yet — which
renders as *wanted*, never as a failure. A self-link. A link inside a code block, which must stay
text. A cycle A→B→A when backlinks are computed.

And `[[Hostel]]` versus `[[hostel]]`: **whether those are one page is the human's decision**, and
the test is where that decision gets pinned rather than discovered.

### Uploads

Zero bytes. One byte. Exactly the limit. One over.

A `.png` holding a PDF. A **polyglot** — a valid `GIF89a` header with an HTML body — which is the
case that shows magic bytes alone do not settle the type ([security.md](security.md)). SVG and HTML
rejected outright. A filename containing `../`, a null byte, 300 characters, Tamil script, or a
reserved Windows name (`CON`, `NUL`) — the last matters because one of us develops on Windows.
A double extension, `x.png.html`. An upload cut off mid-stream, which must leave no half-written
file. The same file twice.

Video: no `Range` header, an open-ended range, and an unsatisfiable one.

### Moderation

A forbidden transition (approved → submitted). Two moderators approving the same item at once. An
edit proposed against an article that changed, or was deleted, in the meantime. Approving an item
whose media has since been deleted.

### Search and pagination

Nothing found. Exactly one. Exactly one page. One over a page. Page zero, a negative page, a page
past the end. A query of only punctuation, or only stop words. Querying while the index is being
rebuilt.

### Time and ordering

A timestamp on a daylight-saving boundary, with the zone **pinned in the test** rather than
inherited from whoever's laptop is running it. Two items created in the same millisecond, whose
order must still be defined.

### Where these came from

The Unicode rows and the polyglot row are not recalled: they came out of reading, and the same
reading found that [security.md](security.md) was presenting magic bytes as settling the file type
when they do not. That is the argument for the rule in [collaboration.md](collaboration.md) §3 about
when to go and look something up — a checklist written purely from memory would have had neither.

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
