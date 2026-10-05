# ADR-0004 — Search through Hibernate Search over Lucene, not the database

**Status:** accepted
**Decided:** the Hibernate Search / Lucene engine at project setup (it is in the stack list in `CLAUDE.md`); the analyzer question 10 September, and left open on purpose
**Recorded:** 10 September, after the fact — see [README.md](README.md)

## Context

FR-007 asks for more than substring matching: results ranked by how completely they match, matched
case-insensitively **and independent of word form** — "registering", "registration" and "register"
have to find the same article. NFR-002 bounds it at 2 seconds over 100 articles of ~500 words, which
is a modest corpus and not the binding constraint.

NFR-003 is the binding one. Article bodies mix English with Hindi and Tamil, and the search-results
mockup deliberately includes a Hindi-titled article to make the point. Storing that text unchanged
and *finding* it are two different problems.

The deployment constraint that shapes this: **H2 in development, PostgreSQL in production**. Any
search built on database-specific features is a feature the tests never exercise, because the tests
run on the other engine.

## Options

### A. `LIKE '%term%'` in SQL

No dependency, works identically on H2 and PostgreSQL, and about ten minutes of work.

It cannot do the two things FR-007 actually asks for. There is no ranking — every row matches or
does not — and there is no word-form independence, so a reader searching "registration" misses an
article that says "registering". It also scans, though at 100 articles that is irrelevant.

Rejected on requirements, not performance.

### B. PostgreSQL full-text search (`tsvector` / `ts_rank`)

Real ranking, real stemming, no new dependency, and it lives in the database we deploy on.

It does not exist in H2. Development and every test would run against an engine with no `tsvector`,
so the search slice — the one with the most subtle behaviour in the project — would be the one slice
whose tests prove nothing about production. Fixing that means running PostgreSQL for tests
(Testcontainers, another dependency and a Docker requirement on both machines) or accepting that the
dev and prod search behave differently.

Its multilingual story is also weak here: `to_tsvector` needs a language configuration per column,
and Postgres ships no Hindi or Tamil configuration, so mixed-script bodies fall back to `simple`.

### C. Hibernate Search over a local Lucene index

Hibernate Search 7.2.6 with a Lucene backend, already named in the stack. The index is a directory
on disk, identical in development and production, so the tests exercise the real engine. Lucene
gives ranking and analyzer-based stemming, which is exactly the pair FR-007 asks for.

Costs: a second store to keep in step with the database — an index that has to be rebuilt after an
import (`backup` slice) and after a schema change, and a file path that has to survive deployment.
For a single-instance deployment this is a directory; it would become a real problem if the
application were ever run as more than one instance, which nothing in the stakeholder's ask
suggests.

## Decision

**C** — Hibernate Search over Lucene.

The deciding factor is B's split between the engine we test on and the engine we ship on. Search is
the slice where behaviour is hardest to reason about and easiest to get subtly wrong, so it is the
worst possible place to have tests that run somewhere else.

## Multilingual content, and what is not yet decided

NFR-003 has two halves and they are not equally settled.

**Storage and round-trip** is settled by construction: the text is stored in the database as Unicode
and read back unchanged. This half is a test, not a design decision.

**Querying across scripts** is not settled here. Lucene's standard analyzer tokenizes Unicode text
and will not corrupt Devanagari or Tamil, but tokenization is not the same as good segmentation, and
no stemming exists for either language in the default analyzer set. A dedicated ICU analyzer would
segment better and is a **separate artifact**, which makes it a dependency decision — the artefact,
its version, its maintenance status and its licence get checked before it is written down, and it is
not named here on the strength of recollection.

What this ADR commits to instead is the check: NFR-003's fit criterion — that a query written in
Hindi or Tamil matches an article containing that script — is a test written against the analyzer we
actually configure. If the default analyzer passes it, no dependency is needed. If it does not, the
choice of analyzer comes back as its own decision with the evidence attached.

## Consequences

**Good:** ranking and word-form independence come from the engine rather than from code we write;
development, tests and production all run the same search; the index is a directory, so backup and
restore need no extra service.

**Bad:** two stores to keep consistent — a reindex step belongs in the import path and in the
deployment runbook, and an index that silently falls behind produces the worst kind of bug, where
the data is right and the answer is wrong. Single-instance only, in practice.

**Reversal:** moving to option B later is contained — the `search` slice publishes one type and
everything else goes through it — but it would reintroduce the H2/PostgreSQL split that decided
this. Worth reconsidering only if the project ever drops H2 from development entirely.

## Amended 28 September, when the slice was built (FEAT-007)

Decided by the human while `search` was being built.

- **The directory on disk is the stand's**, in the `guide-index` volume. In tests the index is in
  memory: a test run starts several application contexts and Lucene locks a directory to one of
  them. The engine, the mapping and the analyzer are the same everywhere; the storage is what
  differs.
- **Development keeps the index in memory too**, because its H2 database is emptied at every start
  and an index on disk would outlive it.
- **An empty index is filled from the `article` table at start-up**, and one that has documents is
  left alone. That is the reindex step named above: after a restore or a change to the mapping, the
  volume is removed and the application started again. *Reversed on 5 October, below.*
- **The `search` slice publishes no type yet**, since no other slice calls search
  ([architecture-rules.md](../../ai/architecture-rules.md)); "Reversal" above holds once one does.

## Amended 5 October, after the demo rehearsal

Decided by the human. The mapping changed on 2 Oct (term vectors on `title`, to mark a matched word
in it) and the stand's index, written before, was kept because it had documents. The step "remove the
volume after a change to the mapping" was written down and not done; the first approval on the stand
then answered `500`, since Lucene refuses a field whose settings differ from the ones its segments
hold.

- **The index is dropped and built again from the `article` table at every start**
  (`SearchIndexBuilder`, `dropAndCreateSchemaOnStart`). No step is left to remember after a mapping
  change or a restore. The cost is the start: the guide holds tens of articles, and NFR-002's load
  check runs with 100.
- **The directory stays on disk in `guide-index`**, so a running stand still answers from it; what
  changes is only that a start no longer trusts what an older version left there.

