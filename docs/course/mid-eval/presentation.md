# Mid-evaluation: the two-minute overview

What to bring and say at the mid-evaluation. The TAs ask for an architecture diagram, a two-minute
overview of the components and the data flow, and then a small change made live in the code, per
student ([../rubric.md](../rubric.md); the TAs' message of 8 October).

| File | What it is |
|---|---|
| [architecture.pdf](architecture.pdf) | Four A4 pages to print: containers, the slices grouped by who they serve, the main flow, every arrow between slices |
| [slides.pdf](slides.pdf) | Eight slides for the overview (title to status), then four backup slides for questions |
| this file | The words for each slide, and the questions we expect |

Both are built from [slides.tex](slides.tex) and [architecture.tex](architecture.tex) (`xelatex slides.tex`,
`pdflatex architecture.tex`); the diagrams in them come from `docs/diagrams/out/`, so re-render with
`scripts/diagrams.sh` first if a `.puml` changed.

## The script

About 280 words: two minutes at about 140 a minute. The figures are as of 11 October; check them
against `docs/gap-list.md` and the last build before the day.

**1. Title** (skip, say the name). "The Exchange Student Guide, for IIT Madras's Office of Global
Engagement."

**2. The problem.** "A student three days off the plane can't find how to register with FRRO: it's
spread over the institute site, WhatsApp groups and blogs. Our guide is a knowledge base students
write and OGE moderates. Anyone reads and searches; anyone proposes an article or an edit, without an
account; nothing is public until a moderator approves it."

**3. Architecture.** "It's one Spring Boot application, server-rendered with Thymeleaf, with three
stores: PostgreSQL for articles, their earlier versions and submissions; a Lucene index for search;
and a directory for uploaded files. It runs with `docker compose up`."

**4. Slices.** "Inside, the code is cut into thirteen slices by who they serve: reading, contributing,
moderating, and shared rules. Each slice hides its controllers, services and repositories; others
may use only its published types or its events. That rule is a test: Spring Modulith fails the build
if one slice reaches into another."

**5. Data flow.** "Follow one edit. The contribute slice checks it and stores it as a submission —
the article is untouched — and the student gets a tracking number. The moderator sees a diff and
approves: the old text is kept as a revision, the article updated, an event re-reads its links, and
the index updates on commit."

**6. What checks it.** "Nearly a thousand tests run on every push, the same suite again on
PostgreSQL in CI, 151 browser tests with an accessibility checker, and mutation testing on the pure
logic."

**7. Built with AI.** "We wrote it with Claude Code, under rules the repository enforces: a confirmed
contract before every change, a red test before the code, and every prompt recorded in Git."

**8. Status.** "42 of 43 requirements are done; five debt items are open, each with its trigger. Next,
OGE uses it themselves."

## Questions we expect, and where the answer is

| Question | Short answer | Show |
|---|---|---|
| Why a monolith, not microservices? | Two people, one server, one transaction for approve-and-index; the boundary is enforced in the build instead of the network | slide 4; ADR-0002 |
| Why Lucene and not PostgreSQL's full-text search? | The tests run on H2; with Lucene the engine we test is the engine we ship | backup "decisions"; ADR-0004 |
| How does a submission stay private? | It is a row of `submission`, not of `article`: no query for articles can return it by forgetting a filter | ADR-0003 |
| What stops spam? | Five submissions an hour per visitor, the moderation queue, an unguessable submission number | ADR-0008, ADR-0011 |
| How is the moderator's sign-in protected? | One shared password stored as a bcrypt hash; ten wrong tries per visitor every 15 minutes | `docs/handoff/install.md` §2 |
| What if the article is removed, or its new title taken, while an edit waits? | Approval is refused with `ApprovalConflictException`, and nothing is half-written | `moderate/internal/ModerationService.approve` |
| Where would you add …? | The backup slide "where a typical change goes", which is `docs/viva/README.md` | backup slide 1 |
| What did the AI get wrong? | The 500 after approval, the flaky tag test, the journal pairing translations wrongly — each found, recorded and fixed with a test | `principles-practices.pdf` §2, §3 |

## For the live change

Open [../../viva/README.md](../../viva/README.md) first: the request path, each slice's starting class
and test, and a table of where typical changes go. Run one test class with
`./mvnw -pl app test -Dtest=<Class>`; the app with
`./mvnw -pl app spring-boot:run -Dspring-boot.run.profiles=dev,seed`. Only Gemini Pro is allowed
during the evaluation.
