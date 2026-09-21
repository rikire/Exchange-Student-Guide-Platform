# Architecture rules

The application is a **vertical slice** monolith. A slice is one feature, self-contained: its
controller, its service, its own domain logic, its own repository, its own templates, its own tests.

Two people take tasks freely and work in parallel, so the structure has to make it unlikely that they
edit the same file. A slice is both the unit of work someone picks up and the unit that ownership is
measured against.

## The map

```
in.ac.iitm.guide
├── shared/            COMMON — changed by agreement, rarely after phase 2
│   ├── persistence/   JPA entities + Flyway migrations (one schema for everyone)
│   ├── web/           layout, error handling, shared Thymeleaf fragments
│   └── security/      CSRF, headers, rate limiting, admin authentication
├── home/              the landing page: pinned articles and files, search
├── articleview/       reading an article, navigation, rendering
├── search/            indexing and querying
├── taxonomy/          tags and navigation by tag
├── contribute/        creating an article and proposing an edit
├── moderate/          the queue, approval, rejection
├── report/            flagging an article as a problem, and resolving that flag
├── media/             upload, storage, delivery
├── wikilink/          plain Java: the [[link]] parser and backlinks
└── backup/            export and import of the whole knowledge base
```

Slices sit as first-level packages because that is the module model Spring Modulith uses.

## What is public and what is not

**Public:** the types that sit *directly* in a slice package, for example `search/ArticleSearch.java`.

**Internal:** everything in a nested package — `search/internal/`, `search/web/`,
`search/persistence/`. Another slice may not import these.

Between slices there are exactly two allowed channels:

1. A published type in the slice package.
2. A Spring `ApplicationEvent`.

Calling another slice's service or repository directly is not one of them.

Spring Modulith's event registry (event externalization and persistence) stays switched off. Events are
in-process only: nothing needs at-least-once delivery or replay across a restart, and the registry
adds a persistence table and a completion step that no slice needs yet.

## Why there are no shared repositories

The JPA entities are shared — there is one `articles` table, not one per slice. The repositories are
not. **Each slice declares its own Spring Data repository** over those entities, with exactly the
queries it needs:

```java
// articleview/persistence/ArticleReadRepository.java  — what the reader needs
// moderate/persistence/PendingArticleRepository.java  — what the moderator needs
```

Spring allows several repositories over one entity, and this is the most effective thing that keeps two
people out of each other's files: a shared `ArticleRepository` would collect the union of everyone's
queries and become the file both edit every day.

## Enforced rules

Spring Modulith checks the slice boundary in `ModularityTest`:

1. A slice does not import another slice except through its published types.
2. A slice does not import another slice's repositories or internal services.

ArchUnit checks two more, which Modulith does not cover:

3. `shared.persistence` imports nothing from a slice. Dependencies point inwards, never back out.
4. `wikilink` imports neither `org.springframework` nor `jakarta.persistence`. It is plain Java, so
   its tests run in milliseconds without a Spring context, which makes writing the test first cheap
   enough to actually do.

A failing boundary test is a design signal, not an obstacle. Suppressing it, or moving a class into
`shared` to make it pass, needs the human's agreement.

## Inside a slice

```
search/
├── ArticleSearch.java             published: what other slices may call
├── SearchController.java          web entry point
├── internal/
│   ├── SearchService.java
│   └── QueryParser.java
└── persistence/
    └── SearchIndexRepository.java
```

Layering inside a slice is for whoever owns the slice that week; the boundary is not.

**Validation always lives in the domain.** A form, a controller or a database constraint may validate
as well, when needed or when it makes something faster, but never *instead*. A check on a form
protects only the path through that form; every other caller, later feature and import walks around
it. The domain has no way past it, so the rule about what may exist belongs there, and the outer
layers exist to give a person a better message sooner.

## What belongs in `shared`

Only what serves every slice: the JPA entities and migrations, the page layout and error handling, and
the security configuration.

**Moving something into `shared` is a stop-and-ask trigger.** `shared` is the one place where two
people collide, so it grows only by decision. If exactly one slice uses it, it belongs to that slice,
even when a second slice might want it later.

## Dependency direction

```
slice  ->  shared        allowed
shared ->  slice         forbidden (rule 3)
slice  ->  slice         only through published types or events (rules 1 and 2)
```

## Where the search port went

An earlier plan wrapped search behind a port so that Lucene could be swapped for PostgreSQL full-text
search on a host with an ephemeral filesystem. That reason went away when we decided to ship a Docker
Compose stack with a volume. The `search` slice still hides its engine behind its own published type,
now for testability — other slices and their tests must not need a Lucene index — and not for a
portability promise. It is a testing seam, not an abandoned abstraction.
