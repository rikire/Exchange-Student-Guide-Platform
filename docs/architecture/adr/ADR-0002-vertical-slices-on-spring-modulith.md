# ADR-0002 — Vertical slices, with Spring Modulith enforcing the boundary

**Status:** accepted
**Decided:** before 5 September — the slice structure and the Modulith boundary predate this phase (`docs/ai/architecture-rules.md`, and the proposal); the tenth slice `report` was added 5 September
**Recorded:** 10 September, after the fact — see [README.md](README.md)

## Context

Two people work this repository in parallel, taking tasks freely rather than owning assigned areas
([collaboration.md](../../ai/collaboration.md) §6). The course grades the process, and one of the
things it grades is that both of us appear in the git history across every week — which means
ownership is *measured* from that history rather than assigned on paper.

That puts a specific demand on the package structure: it has to make it unlikely that two people
editing different features touch the same file. A merge conflict is survivable; what is not is a
structure where "who built this" cannot be answered from the log because every feature is smeared
across the same four files.

The application is a single Spring Boot deployment for one institute office. Nothing here needs
independent scaling or independent release.

## Options

### A. Layered packages — `controller`, `service`, `repository`, `domain`

The default, and what most Spring tutorials show. Every feature adds a class to each layer, so
`ArticleService` and `ArticleRepository` collect the union of everything anyone needs. Two people
building two different features edit the same handful of files daily.

Cheap to start, and it makes cross-feature queries easy because everything is reachable. That
reachability is the problem: nothing stops the moderation code from calling the search repository,
so the boundary exists only as long as everyone remembers it.

Costs us exactly the property we need — per-feature ownership visible in the log.

### B. Vertical slices in one module, boundary enforced by Spring Modulith

One first-level package per feature (`home`, `articleview`, `search`, `taxonomy`, `contribute`,
`moderate`, `report`, `media`, `wikilink`, `backup`), each with its own controller, service, domain
logic, repository, templates and tests. Types directly in the slice package are public; anything in
a nested package is not.

Spring Modulith 1.4.13 is already in the stack, and its `ModularityTest` fails the build when one
slice imports another's internals. So the boundary is a test, not a convention — which matters
because a convention is exactly what fails under deadline pressure.

Costs: JPA entities still have to be shared (there is one `articles` table), so `shared/persistence`
is a package both people do touch. Cross-slice reads go through a published type or an
`ApplicationEvent`, which is more ceremony than a direct call.

### C. Multi-module Maven, one module per slice

The hardest possible boundary — a slice cannot import what is not on its compile classpath, no test
required. Ten Maven modules for a project this size, though, and every cross-slice type has to be
hoisted into a shared module before it can be published. The build gets slower and the module graph
becomes its own maintenance task.

Real, and disproportionate: it buys enforcement we can already get from a test.

## Decision

**B** — vertical slices in one module, with Spring Modulith enforcing the boundary.

The deciding factor is not modularity in the abstract. It is that a slice is simultaneously the unit
of work one person picks up and the unit ownership is measured against, so the structure and the
grading criterion line up instead of fighting.

C was rejected on cost, not on principle: it enforces the same rule the test enforces, at the price
of ten modules.

## Consequences

**Good:** two people can hold two slices without editing the same files; the boundary fails the
build rather than relying on review; `wikilink` stays plain Java with no Spring or JPA imports, so
its tests run without a context and writing the test first is cheap enough to actually do
([architecture-rules.md](../../ai/architecture-rules.md) rule 4).

**Bad:** `shared/persistence` is a genuine shared file and needs agreement to change. Each slice
declares its own Spring Data repository over the shared entities, so several repositories exist over
one table — deliberate, and it will look like duplication to anyone who has not read this. A slice
that needs another's data waits for a published type or an event rather than calling across.

**Reversal:** collapsing slices into layers later is a mechanical move of files and would not change
behaviour. Going the other way — splitting to option C — costs more, because every published type
has to be hoisted into a shared module first. Worth reconsidering only if some part of this ever
needs to deploy separately, which nothing in the stakeholder's ask suggests.
