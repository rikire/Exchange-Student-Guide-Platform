# ADR-0010 — Every public list is bounded, and the fetch strategy is chosen at the query

**Status:** accepted
**Decided:** 5 September — added to `docs/ai/security.md` in that day's commit
**Recorded:** 10 September, after the fact — see [README.md](README.md)

## Context

[CON-001](../../requirements/constraints.md) means anyone at all can request any public page without
identifying themselves, and there is no per-user quota to fall back on because there is no user.
Availability is therefore part of the threat model rather than an operational afterthought.

The pages this concerns are the ones that show many articles at once: the landing page (FR-009),
search results (FR-007) and tag browse (FR-008). Each of those rows carries tags, and the article
page also carries backlinks (FR-006) — associations that JPA will fetch lazily unless told
otherwise.

That combination has a specific failure: one HTTP request expands into one query per row, so a page
showing 200 articles issues 200-plus round trips. It is anonymous, repeatable, needs no tooling, and
in the access log it is indistinguishable from someone browsing. NFR-002's 2-second bound is stated
over a corpus of 100 articles, so the corpus that would expose this is only slightly larger than the
one we test against.

## Options

### A. The framework defaults — lazy associations, unbounded lists

What Spring Data gives without intervention. Nothing to decide, nothing to write, and it is correct
on a developer's machine seeded with ten articles.

That last clause is the problem: "it was fine locally" is a statement about the size of the seed
data, not about the query. The failure appears only once real content exists, which is after the
mid-demo and in front of the stakeholder.

### B. Bound the reads, and choose the fetch strategy explicitly

Every public list is paginated with a page size the client cannot raise, and every query that
crosses an association names how it fetches — a join fetch, an entity graph, or a batch size —
decided when the query is written rather than discovered in production.

Costs discipline in ten slices that each declare their own repository
([ADR-0002](ADR-0002-vertical-slices-on-spring-modulith.md)), which is ten places to get it wrong.

### C. Defer it to infrastructure — rate limiting or caching in front of the application

A reverse proxy limits request rate; a cache absorbs repeated reads.

It treats the symptom. A single uncached request for a large page still issues hundreds of queries,
and a cache in front of a knowledge base whose whole point is that it changes when someone fixes it
is its own correctness problem. Useful as a later layer, not as the answer.

## Decision

**B** — public lists are paginated with a bound the client cannot raise, and every association fetch
is chosen at the point the query is written.

The deciding factor is that A's failure mode is invisible until the data is real, and this project's
data becomes real at exactly the moment it is being demonstrated.

**The gate that makes this more than a rule** arrives in phase 2 with the persistence layer: a query
counter around the slice tests, failing when the query count rises with the row count. Until it
exists this is a rule review has to hold, which is the weakness worth naming rather than hiding —
a rule with no check is a rule that is already being broken somewhere nobody has looked.

## Consequences

**Good:** the cost of a page is bounded by the page size rather than by how much content exists;
NFR-002's bound stays meaningful as the corpus grows past the fixture; the eventual query-count test
has something specific to assert against.

**Bad:** pagination is now a requirement on screens that were sketched without it — the landing
page, search results and tag browse mockups all show a flat list with no pager, so either the
screens or the route contract has to grow one. Ten repositories each need the discipline, and until
the phase-2 gate exists nothing fails when one of them forgets.

**Reversal:** cheap to loosen a page size; expensive to remove pagination once routes and templates
assume it. Option C remains available as an additional layer if traffic ever justifies it, and
nothing here forecloses it.
