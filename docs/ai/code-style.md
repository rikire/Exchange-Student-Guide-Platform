# Code style

Java 21, Spring Boot 3.5. The formatter settles layout, so this document is about the decisions a
formatter cannot make.

## Formatting

Spotless with `palantir-java-format` owns everything mechanical: line breaks, indentation, import
order, trailing whitespace. Do not argue with it and do not configure around it.

```bash
./mvnw spotless:apply     # fix
./mvnw spotless:check     # what CI runs
```

## Naming

- Slice packages are named after the feature, in the singular: `search`, `moderate`, `media`.
- No `Impl` suffix. If there is one implementation, the class is simply `SearchService`. If there
  are two, name them after what distinguishes them: `LuceneIndex`, `InMemoryIndex`.
- No `Util`/`Helper` classes. A method with no home belongs to the type it operates on.
- Test methods read as sentences: `rejects_an_upload_whose_content_does_not_match_its_extension`.
  Underscores in test names only.

## Language

Code, comments, documentation, commit messages, log messages, exception messages: **English**.
Conversation is not code. Article content is a separate matter — see `docs/requirements/`.

## Comments

Comment **why**, never **what**. `// increment the counter` above `counter++` is noise.

Worth a comment:

- a non-obvious decision and the alternative it beat;
- an invariant a reader could break without noticing;
- a workaround, always with `// TODO(DEBT-XXX):`.

Javadoc on published types — the ones directly in a slice package — is mandatory: they are the
contract other slices depend on. Internal classes get Javadoc only when the name is not enough.

## Lombok

**Not used.** Java records cover most of what Lombok was for, and every line of this code has to
be explainable at the viva without "the annotation generates it". If a class is genuinely tedious to
write by hand, that is usually a sign it holds too much.

## Errors

- No checked exceptions in slice-published signatures.
- One exception type per slice for its expected failures: `ArticleNotFoundException`,
  `MediaRejectedException`. Handled centrally in `shared/web`.
- Never swallow: no empty `catch`. If an exception really is expected and harmless, the catch block
  carries a comment saying why.
- An exception message names the value that caused it, never a secret: file name yes, admin password
  no.

## Fallbacks and defaults

A fallback is a decision about what should happen when something is missing. It is not a way of
avoiding that decision, and it is the cheapest thing in the world to add — which is why it
accumulates.

- **Default a value once, at the boundary where it enters**, and let it be absent everywhere else. A
  value defaulted in the configuration, again in the constructor and again at the call site has
  three answers and no source of truth. When they disagree, the bug is invisible: something
  plausible is returned.
- **Do not add a fallback for a case you have not seen and cannot name.** "Just in case" is not a
  case. If you cannot say which input produces it, the branch is untested by construction and the
  `/dod` reading will ask you to delete it.
- **Prefer failing where the problem is to substituting a plausible value.** An article count that
  quietly renders as `0` because a query returned null is worse than an error, because nobody
  investigates a number that looks fine.
- **A `catch` that returns a default is swallowing**, and the rule above about never swallowing
  applies to it.
- **If the fallback exists because the real behaviour has not been decided, that is a stop-and-ask,
  not a default.** Choosing the value quietly produces the same unchosen target as any other silent
  default — see [prompting.md](prompting.md).

Two or three defaults on one path is the sign to stop and ask which one is the real rule.

## Logging

SLF4J, never `System.out`.

| Level | Used for |
|---|---|
| `ERROR` | The request failed and someone must look at it |
| `WARN` | Something suspicious that the system recovered from — a rejected upload, a rate limit hit |
| `INFO` | Events with business meaning — an article published, an import finished |
| `DEBUG` | Detail useful while developing |

Parameterised form only: `log.warn("Rejected upload {} of type {}", name, type)`. Never log article
content in full, never log the admin password, never log a raw file path from user input.

## Tests

- JUnit 5. `MockMvc` for the web layer, plain unit tests for domain logic.
- One behaviour per test. A test with three assertions about three different things fails without
  telling you which one broke.
- Arrange / act / assert, separated by blank lines. No comments marking the sections.
- No `Thread.sleep`. If timing matters, make the clock injectable.
- Fixtures live next to the test that uses them; a shared fixture is a shared file, which is exactly
  what two people working in parallel do not want.
- `@SpringBootTest` is the last resort, not the default: it is slow, and a slow suite stops being
  run.

## Coverage

The threshold is a floor, not a goal: `wikilink` and `moderate` hold the real logic and should be
near-complete; a controller that only forwards to a service does not need a test written to raise a
number.

What matters more than the percentage: every acceptance criterion has a test that fails when the
criterion is broken.

## Thymeleaf

- Escape by default. `th:utext` is allowed **only** for content that has passed the sanitiser, and
  every such place carries a comment saying so.
- No business logic in templates. If a template needs a condition more complex than "is this list
  empty", compute it in the controller and pass a flag.
- Shared fragments live in `shared/web`; a fragment used by one slice lives with that slice.

## SQL and JPA

- Parameterised queries only. String concatenation into a query is a security defect, not a style
  issue.
- Every repository query that can return many rows is paginated. "There will never be many articles"
  is exactly the assumption that fails on the demo.
- `FetchType.LAZY` by default; an `N+1` found in review is a defect, not an optimisation opportunity.
