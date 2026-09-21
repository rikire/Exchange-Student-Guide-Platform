# Code style

Java 21, Spring Boot 3.5. The formatter settles layout; this document covers the decisions a
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
- No `Impl` suffix. With one implementation the class is simply `SearchService`; with two, name them
  after what distinguishes them: `LuceneIndex`, `InMemoryIndex`.
- No `Util`/`Helper` classes. A method with no home belongs to the type it operates on.
- Test methods read as sentences, with underscores:
  `rejects_an_upload_whose_content_does_not_match_its_extension`.

## Language

Code, comments, documentation, commit messages, log messages, exception messages: **English**.
Conversation is not code. Article content is a separate matter — see `docs/requirements/`.

## Comments

Comment **why**, never **what**: `// increment the counter` above `counter++` is noise. Worth a
comment:

- a non-obvious decision and the alternative it beat;
- an invariant a reader could break without noticing;
- a workaround, always with `// TODO(DEBT-XXX):`.

Javadoc on published types — the ones directly in a slice package — is mandatory: they are the
contract other slices depend on. Internal classes get Javadoc only when the name is not enough.

## Lombok

**Not used.** Records cover most of what Lombok was for, and every line has to be explainable at the
viva without "the annotation generates it". A class that is tedious to write by hand usually holds too
much.

## Errors

- No checked exceptions in slice-published signatures.
- One exception type per slice for its expected failures: `ArticleNotFoundException`,
  `MediaRejectedException`. Handled centrally in `shared/web`.
- Never swallow: no empty `catch`. If an exception is expected and harmless, the catch block carries a
  comment saying why.
- An exception message names the value that caused it, never a secret: file name yes, admin password
  no.

## Fallbacks and defaults

A fallback is a decision about what happens when something is missing, and it is the cheapest thing to
add, which is why it accumulates.

- **Default a value once, at the boundary where it enters**, and let it be absent everywhere else. A
  value defaulted in the configuration, the constructor and the call site has three answers and no
  source of truth, and when they disagree the bug is invisible: something plausible is returned.
- **Do not add a fallback for a case you have not seen and cannot name.** "Just in case" is not a
  case; the branch is untested by construction and the `/dod` reading will ask you to delete it.
- **Prefer failing where the problem is to substituting a plausible value.** An article count that
  renders as `0` because a query returned null is worse than an error: nobody investigates a number
  that looks fine.
- **A `catch` that returns a default is swallowing.**
- **A fallback that exists because the behaviour has not been decided is a stop-and-ask, not a
  default** ([prompting.md](prompting.md)).

Two or three defaults on one path is the sign to ask which one is the real rule.

## Logging

SLF4J, never `System.out`.

| Level | Used for |
|---|---|
| `ERROR` | The request failed and someone must look at it |
| `WARN` | Something suspicious that the system recovered from — a rejected upload, a rate limit hit |
| `INFO` | Events with business meaning — an article published, an import finished |
| `DEBUG` | Detail useful while developing |

Parameterised form only: `log.warn("Rejected upload {} of type {}", name, type)`. Never log article
content in full, the admin password, or a raw file path from user input.

## Tests

JUnit 5; `MockMvc` for the web layer, plain unit tests for domain logic. How tests are written, what to
mock and how to derive the cases: [testing.md](testing.md).

The coverage threshold is a floor, not a goal. `wikilink` and `moderate` hold the real logic and
should be near-complete; a controller that only forwards to a service needs no test written to raise a
number. What matters more: every acceptance criterion has a test that fails when the criterion is
broken.

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
  is the assumption that fails on the demo.
- `FetchType.LAZY` by default; an `N+1` found in review is a defect, not an optimisation opportunity.
