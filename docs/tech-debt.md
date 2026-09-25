# Technical debt

Anything done temporarily, worse than it should be, or as a workaround. The rule from
[docs/ai/workflow.md](ai/workflow.md): **nothing temporary stays unrecorded**, and the entry is made
the moment the debt appears — by the end of the task the context is gone and the entry comes out
useless.

Every marker in the code must reference an entry here: `// TODO(DEBT-007): ...`.

## Debt versus a deliberate constraint

These are different things and get different identifiers.

- **`DEBT-XXX`** — we know how it should be, and this is not it. It has a cost that grows.
- **`CON-XXX`** — we decided not to do it, and that decision is sound. It lives in
  `docs/requirements/constraints.md` and needs a `**Rationale:**`, not a fix.

Recording a scope decision as debt makes the register meaningless; recording real debt as a
constraint hides it.

## Register

### DEBT-006 — The importer writes articles but no `article_link` rows

**Status:** open
**Created:** 2026-09-25
**Marker:** none in code — an absence: `ArticleArchive.importFiles` saves the article and its tags and
nothing else

**Cause:** [ADR-0012](architecture/adr/ADR-0012-article-link-storage.md) stores the links of a body in
`article_link`, and says every write path that changes a body must re-run the extraction. The
extraction does not exist: `wikilink` has a renderer and an address rule, not an extractor, and the
slices that publish (`contribute`, `moderate`) are phase 3. The importer is a write path added before
the extractor it is meant to call.

**Consequence:** none visible today — nothing reads `article_link`, and a page colours its links from
the body at read time. It bites at FR-006 (backlinks, phase 4): every seed article carries `[[links]]`
and has no rows, so "what links here" would be empty for all of them, and an export followed by an
import would not restore rows that were never written. ADR-0012 names this failure: a silent drift
between the body and the link table.

**How to fix:** when the extractor exists, have the importer call it for each article it creates, and
add a test that imports a linking article and finds its rows. Rows for articles imported before then
need one re-extraction over every article, which is a query per article and belongs in a migration or
a one-off command, not in a page.

**Trigger:** the first code that writes `article_link` — the extractor in phase 3, or FR-006 in phase
4, whichever comes first. If the importer has run before it, the re-extraction is part of that work.

### DEBT-005 — The tag rule of ADR-0005 is written twice until `taxonomy` exists

**Status:** open
**Created:** 2026-09-25
**Marker:** `app/src/main/java/in/ac/iitm/guide/backup/ArticleArchive.java` — `tagsNamed`, whose
javadoc names this entry

**Cause:** [ADR-0005](architecture/adr/ADR-0005-taxonomy.md) stores a tag trimmed and lower-cased,
and says the `taxonomy` slice does that before a tag reaches the table (`Tag`'s own javadoc says the
same). `taxonomy` is empty, and the importer has to write tags now: the seed files carry them.
`backup` applies the rule itself with `strip().toLowerCase(Locale.ROOT)` rather than reach into a
slice that has nothing to reach into.

**Consequence:** two copies of one rule. If `taxonomy` later changes it (ADR-0005 itself calls the
lower-casing "the part most likely to be revisited"; a display-label column is the amendment it
names), the importer keeps the old rule and creates a tag that looks identical on screen and differs
in the table — the duplicate ADR-0005 exists to prevent.

**How to fix:** when `taxonomy` is written, publish the rule as a type directly in its package (for
example a normalising tag name) and have `backup` call it; delete `tagsNamed`'s own normalisation in
the same commit. The importer's test, `tags_are_stored_trimmed_and_lower_cased`, stays as it is and
keeps guarding the behaviour through the swap.

**Trigger:** the first line of code in the `taxonomy` slice. `contribute` (phase 3) will also write
tags and must use the same published type, not a third copy.

### DEBT-004 — Four `app/pom.xml` dependencies are declared with no code or test using them yet

**Status:** open
**Created:** 2026-09-21
**Marker:** `app/pom.xml` — `h2`, `postgresql`, `hibernate-search-mapper-orm`,
`hibernate-search-backend-lucene`

**Cause:** phase 2 step 0 ([02-skeleton.md](roadmap/02-skeleton.md)) names only Flyway, the Spring
Modulith JPA starter, ArchUnit and db-util. The human asked, after the trade-off was stated, to add
these four ahead of that — H2 and the PostgreSQL driver before step 1's persistence test exists,
Hibernate Search before the search slice is built — against the project's own convention of adding a
dependency when a red test needs it (DEBT-003's own reasoning: "adding a dependency for nothing").

**Consequence:** none today; none of the four is exercised by any test, so a version mismatch or a
missing transitive dependency would surface late, at the first code that actually uses them, rather
than now.

**How to fix:** nothing to fix — remove this entry once each dependency has a test exercising it: H2
at phase 2 step 1 (the persistence migration test), the PostgreSQL driver whenever a profile runs
tests against it, Hibernate Search at the search slice's first test.

**Trigger:** already past — recorded at creation, not deferred.

### DEBT-003 — `shared` is a closed Modulith module and has to be an open one

**Status:** resolved 2026-09-22
**Created:** 2026-09-10
**Marker:** `app/src/main/java/in/ac/iitm/guide/shared/package-info.java` — the javadoc, not a `TODO`:
there is no code yet to hang one on

**Cause:** the ten slices and `shared` were declared as Modulith modules on 10 September so the
design document could show a boundary the build enforces rather than a diagram someone drew. Modulith
detects a module from a `package-info.java` alone, with no annotation — which is why no dependency
was added. But without `@ApplicationModule(type = Type.OPEN)`, `shared` is **closed**, and a closed
module keeps its nested packages internal to itself.

**Consequence:** none today, because every one of these packages is empty. It bites in phase 2, at
the first real code: architecture-rules.md has each slice declaring its own Spring Data repository
over the JPA entities in `shared.persistence`, and a slice importing those types from a closed
`shared` is exactly what `ModularityTest` refuses. The failure will look like a boundary violation in
the slice being written, when the actual cause is a missing declaration on `shared` — which is the
kind of misdirection that costs an afternoon.

**How to fix:** add `spring-modulith-api` at compile scope (it is already managed by the Modulith BOM
in `app/pom.xml` and already present transitively at test scope, so this is a scope change rather
than a new artefact — but it is still a dependency decision and needs asking), then annotate
`shared` with `@ApplicationModule(type = ApplicationModule.Type.OPEN)`. The alternative is
`@NamedInterface` on `shared.persistence` and each slice naming it in `allowedDependencies`, which is
stricter and more work; that choice wants a paragraph in architecture-rules.md, not a silent pick.

**Trigger:** the first entity in `shared.persistence`, or the first slice repository — whichever
comes first in phase 2. Not before: annotating an empty package to prevent a failure that cannot
happen yet would mean adding a dependency for nothing.

**Resolved 2026-09-22.** Fixed at the first half of the trigger — the first entities landed in
`shared.persistence` (phase 2 step 1, the eight tables in `data-model.md`) — rather than waiting for
the second half, the first slice repository, which is phase 3's work. The two documents actually
disagreed on when the failure would happen: this entry's own trigger says either half fires it, but
the javadoc it pointed at said "the moment the first entity **and** the first slice repository
exist" — read literally, that meant phase 3, since Modulith only objects to an actual cross-module
import, and step 1 adds no importer yet. Fixed early anyway, on the human's confirmation, rather than
leaving a known-inconsistent pair of documents until phase 3 forced the question. `spring-modulith-api`
is now at compile scope in `app/pom.xml`; `shared` carries `@ApplicationModule(type =
ApplicationModule.Type.OPEN)`. The `@NamedInterface` alternative was not taken up — no paragraph
added to `architecture-rules.md` for it, since it was not the path chosen.

### DEBT-002 — The process layer cannot be packaged for a second repository

**Status:** open
**Created:** 2026-09-06
**Marker:** none in code — this is an absence, recorded here so it is not rediscovered

**Cause:** the audit on 6 September set out to package `.claude/` and `tools/` as a Claude Code
plugin, which is the documented answer to "a second repository needs the same setup" — and this
scaffolding came from a template repository, so the second one already exists. Two things block it,
both structural rather than fiddly. The hooks run `tools/target/ai-tools.jar`, a Maven artefact of
*this* repository that `target/` keeps out of git, so an installed plugin has no jar to run and
fails the way described in `docs/ai/README.md`. And every skill links into `docs/ai/`, which would
not exist in the repository it was installed into, so the commands would load and then point at
nothing.

**Consequence:** the work does not travel. Each new repository re-derives the same rules by hand,
and they drift apart immediately — which is how this repository came to differ from the template it
started from. Nothing is broken today; the cost is paid the first time somebody wants this setup
somewhere else.

**How to fix:** decide what the portable unit actually is. Either the plugin bundles the documents
it links to and ships a built jar as a release asset, or the jar's rules move into the plugin as
scripts with no build step and the skills stop linking outward. The first keeps one source of truth
and adds a release process; the second is portable immediately and forks the rules. That is a design
decision, not an implementation detail, and it wants an ADR.

**Trigger:** the first time a second repository needs this, or phase 5 handover — whichever comes
first. Not before: a plugin that installs and then misbehaves is worse than none, for the same
reason a slash command that errors is worse than one that is absent.

### DEBT-001 — A journal translation is bound to a session, not to an entry

**Status:** resolved 2026-09-04
**Created:** 2026-09-03
**Marker:** was in `tools/.../HookCommand.java`, method `english`; removed when this was resolved

**Cause:** `hook english` writes the rendering into the state of the most recently touched session,
which is the right entry only because `hook prompt` normally opens one first. Nothing enforces that
ordering.

**Consequence:** if the rendering is supplied while no entry is open — the hooks not yet loaded, a
session file cleaned up, the agent calling it out of turn — it waits and attaches to the *next*
entry instead. The journal then shows a translation that does not belong to the prompt above it,
which is worse than no translation: it is evidence that quietly lies. Observed once during phase 0,
when `settings.json` had not yet been loaded by the running session.

**How to fix:** give each entry an id when `hook prompt` opens it, have `hook english` take that id
(or refuse when no entry is open), and drop a rendering that does not match the open entry.

**Trigger:** before the first stage where the journal is submitted as evidence — the design document
on 11 September. Until then the risk is only to our own records.

**Resolved 2026-09-04.** Without entry ids, which turned out to be unnecessary. `hook english` now
looks for the session that actually has an entry open — a prompt recorded and no outcome written
yet — and refuses when there is none, or when there is more than one. The second case is not
hypothetical: two of us worked at the same time on 4 September, and picking a session by
modification time could have written one person's rendering into the other's entry.

Found on the same day that seven entries were written with no rendering at all. The register entry
only described the misattribution, so the more common failure — simply forgetting — was invisible to
it. That one is now handled where it happens: a prompt containing Cyrillic makes the
`UserPromptSubmit` hook ask for a rendering in that turn, and the `Stop` hook records
"English rendering: NOT supplied" in the entry, so an omission leaves a trace instead of a gap.

<!--
### DEBT-XXX — Short title

**Status:** open
**Created:** YYYY-MM-DD
**Marker:** app/src/main/java/.../SomeClass.java

**Cause:** why it ended up this way.
**Consequence:** what this costs and who pays it — a slow page, a flaky test, a risk.
**How to fix:** the concrete change, not "redo this properly".
**Trigger:** what makes this urgent — a number of articles, a page load time, a stage of the course.
-->
