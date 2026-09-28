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

### DEBT-016 — The files of rejected submissions stay in the media root

**Status:** open
**Created:** 2026-09-28
**Marker:** `app/src/main/java/in/ac/iitm/guide/media/MediaAssets.java` — `moveToArticle`, the only
place an asset changes owner

**Cause:** FEAT-009 stores an attachment when it is submitted. A rejected submission keeps its row
and its file; ADR-0006 already allows an orphan as "wasted disk, not exposure", and the human put the
cleanup off on 28 Sep.

**Consequence:** every rejected attachment keeps its bytes, and its `size_bytes` counts towards
NFR-001's 20 GB volume, so spam that is rejected still fills the storage, up to refusing every
upload.

**How to fix:** a sweep that deletes the rows and files of assets whose submission was rejected
longer ago than a set time, with a test that a published article's asset is never swept.

**Trigger:** the volume passing half its limit, or phase 4's hardening
([04-hardening.md](roadmap/04-hardening.md)), whichever comes first.

### DEBT-015 — A file far over the container's limit gets a closed connection, not the page

**Status:** open
**Created:** 2026-09-28
**Marker:** `app/src/main/resources/application.yml` — `spring.servlet.multipart`

**Cause:** FEAT-009 answers a form over the container's limit with `413` and the form. Tomcat reads
at most 2 MB past the limit (`server.tomcat.max-swallow-size`) to deliver that answer and then closes
the connection: checked on 28 Sep, a file 5 MB over the test's limit ended in a broken pipe instead of
the page. Raising the setting means the server reads that much more of a request it has already
refused, which is a choice between a clear message and the cost of reading a stranger's upload.

**Consequence:** a contributor who picks a file more than 2 MB over 200 MB sees the browser's
connection error rather than the form's message.

**How to fix:** the human decides `server.tomcat.max-swallow-size` (unlimited, a set size, or the
default), and a test through a real server pins the answer at a file past it.

**Trigger:** the first report of an upload ending in a connection error, or phase 4's hardening.

### DEBT-014 — The stand's session cookie is not marked `Secure`

**Status:** open
**Created:** 2026-09-28
**Marker:** `docker-compose.yml` — the `app` service's environment

**Cause:** [ADR-0009](architecture/adr/ADR-0009-admin-authentication.md) marks the moderator's session
cookie `Secure` behind TLS. The stand serves plain HTTP on port 8080, where a `Secure` cookie would
never be sent back and nobody could stay logged in, so `SERVER_SERVLET_SESSION_COOKIE_SECURE` is not
set (review of FEAT-006, 28 Sep).

**Consequence:** once the stand is reachable over a network, the moderator's session cookie travels
in clear text and can be copied off the wire; whoever has it is the moderator until it expires.

**How to fix:** put the stand behind TLS (a reverse proxy in `docker-compose.yml`) and set
`SERVER_SERVLET_SESSION_COOKIE_SECURE: "true"` on the `app` service in the same change.

**Trigger:** the demo stand step of phase 4 ([04-hardening.md](roadmap/04-hardening.md)), and in any
case before the stand is reachable from outside the machine it runs on.

### DEBT-013 — The CSRF token is not replaced when the moderator logs in

**Status:** open
**Created:** 2026-09-28
**Marker:** `app/src/main/java/in/ac/iitm/guide/shared/security/ModeratorLoginController.java`

**Cause:** Spring Security's `formLogin` replaces the CSRF token on login through
`CsrfAuthenticationStrategy`. The moderator login is its own controller (FEAT-006: `formLogin` needs
a username and answers a wrong password with a redirect, not `401`), and it changes the session id
but not the token (review of FEAT-006, 28 Sep).

**Consequence:** someone who can plant a CSRF cookie in the moderator's browser before login — which
takes control of a sibling domain — knows the token the moderator uses afterwards and can forge an
approval. Low risk on a single-host deployment.

**How to fix:** in `ModeratorLoginController.logIn`, call `CsrfAuthenticationStrategy` with the
application's `CookieCsrfTokenRepository` after the session id changes, and a test that the token
before login is refused after it.

**Trigger:** the security review of phase 4 ([04-hardening.md](roadmap/04-hardening.md)).

### DEBT-012 — Two approvals under one address at the same moment answer `500`, not `409`

**Status:** resolved 2026-09-28 — not fixed, by the human's decision: the race needs two moderators
approving same-titled submissions in one instant behind one shared password, and it harms no data.
Now the deliberate constraint
[CON-009](requirements/constraints.md#con-009--two-approvals-under-one-address-at-the-same-moment-are-not-answered-gracefully).
The code marker is replaced by a reference to it.
**Created:** 2026-09-28
**Marker:** `app/src/main/java/in/ac/iitm/guide/moderate/internal/ModerationService.java` — `freeSlug`

**Cause:** approval checks that the title's address is free, then writes the article. Two different
submissions with one address approved at the same moment both pass the check, and the second fails
on the unique `slug` at commit, which nothing turns into the contracted `409` (review of FEAT-006,
28 Sep).

**Consequence:** the second moderator sees an error page instead of "an article with this title
already exists". No bad data: the second approval rolls back and its submission stays pending.

**How to fix:** catch the unique-constraint violation on `article_slug_key` around the approval and
answer it as `ApprovalConflictException`, with a test that inserts the article between the check and
the write.

**Trigger:** the edge cases of phase 4 ([04-hardening.md](roadmap/04-hardening.md)), or the first
time two moderators work the queue at once.

### DEBT-011 — Failed moderator logins are logged but not rate limited

**Status:** open
**Created:** 2026-09-28
**Marker:** `app/src/main/java/in/ac/iitm/guide/shared/security/ModeratorLoginController.java`

**Cause:** [ADR-0009](architecture/adr/ADR-0009-admin-authentication.md) rate limits failed attempts
on the one shared password, at the rate [NFR-005](requirements/non-functional.md) configures, and
NFR-005 is still planned. FEAT-006 built the login with the `WARN` log only (decided by the human on
28 Sep).

**Consequence:** the password can be guessed as fast as the server answers. BCrypt's cost slows each
guess, but nothing stops them; the `WARN` lines are the only sign it is happening.

**How to fix:** with NFR-005, a limit per client address on `POST /moderate/login` that answers
`429` once exceeded, and a test that the attempt after the limit is refused even with the right
password.

**Trigger:** NFR-005, phase 4 ([04-hardening.md](roadmap/04-hardening.md)) — and in any case before
the stand is reachable from outside the team.

### DEBT-010 — An article's old address answers `404` after an edit changes its title

**Status:** open
**Created:** 2026-09-28
**Marker:** `app/src/main/java/in/ac/iitm/guide/moderate/internal/ModerationService.java`

**Cause:** the address is computed from the title ([data-model.md](architecture/data-model.md),
`slug`), so approving an edit that changes the title moves the article. Keeping the old address
answering needs somewhere to store it — a column or a table, that is, a migration. FEAT-006 left
that out (decided by the human on 28 Sep).

**Consequence:** a bookmark or a shared link to the old address gets "not found", and every
`[[Old Title]]` in other articles turns red until someone edits it.

**How to fix:** a table of former slugs pointing at the article, written when an approved edit
changes the slug, and `GET /articles/{title}` answering `301` to the current address when only a
former one matches. The wiki-link resolver consults it too, so an old link stays blue.

**Trigger:** the first renamed article anyone complains about, or the phase 4 edge cases, whichever
comes first.

### DEBT-009 — An edit whose article stopped being published answers `404`, not `409`

**Status:** open
**Created:** 2026-09-27
**Marker:** `app/src/main/java/in/ac/iitm/guide/contribute/internal/ArticleNotPublishedException.java`

**Cause:** [ui-routes.md](architecture/ui-routes.md) answers `POST /articles/{title}/edits` with
`409` when the target was published at the form and is not at the POST, and `404` when it was never
there. Telling the two apart needs the form to carry which article it was opened for. Nothing can
unpublish an article until FR-026 (phase 4), so the `409` path cannot be reached, and FEAT-005 built
only the `404`.

**Consequence:** once FR-026 exists, a contributor whose article is removed while they write gets
"not found" instead of "this changed while you were editing", and loses their text either way.

**How to fix:** put the target article's id in a hidden field of the edit form; in `submitEdit`,
when the address matches no live article and that id names a removed one, answer `409`. A test that
removes the article between the GET and the POST.

**Trigger:** FR-026 (removing a published article), phase 4.

### DEBT-008 — The submission form takes no attachment

**Status:** open
**Created:** 2026-09-27
**Marker:** `app/src/main/resources/templates/contribute/SubmissionForm.html` — the comment where the
attachment field belongs

**Cause:** FR-010 and FR-011 let a contributor attach one photo, document or video, and each has two
criteria about it (over the size limit, not an accepted type). Checking the type from the content and
storing the file is the `media` slice ([ADR-0006](architecture/adr/ADR-0006-media-storage-and-upload-security.md)),
which does not exist yet. FEAT-005 was cut to the text of a submission by the human on 27 Sep.

**Consequence:** the demo scenario's "proposes an edit with a photo" cannot be shown, and the four
media criteria of FR-010/FR-011 have no test. The gap list shows them only as this entry: its
per-requirement count sees at least as many tests as criteria for FR-010 and FR-011 and reports no
gap.

**How to fix:** in the `media` step: the `attachment` field on the form (`multipart/form-data`), the
upload handed to `media`'s published type, a `422` with the error on the form for a file over the
limit or of a wrong type, and one test per criterion — FR-010's two and FR-011's two.

**Trigger:** the `media` step of phase 3 ([03-main-flow.md](roadmap/03-main-flow.md)).

### DEBT-007 — No automated test runs the migrations on PostgreSQL

**Status:** resolved 2026-09-27 — `./mvnw -P postgres verify` runs the whole suite on PostgreSQL 17
through Testcontainers' JDBC URL (`org.testcontainers:postgresql`, test scope, 1.21.4 from the Spring
Boot BOM) and fails when Docker is not running, as the human chose. Shown red with
`flyway-database-postgresql` removed ("Unsupported Database: PostgreSQL 17.11"). Its first run found
three tests that only held on H2 — upper-case metadata names and `LISTAGG` — and they were made
portable; no product defect.
**Created:** 2026-09-26
**Marker:** none in code — an absence: every test that touches the database runs on H2

**Cause:** production is PostgreSQL and every test is H2. On 26 September a manual run of the
application against PostgreSQL 17.11 found that it did not start: Flyway 10+ needs
`flyway-database-postgresql` next to `flyway-core`, and no H2 test could show it (see
[overview.md](architecture/overview.md)). The dependency is fixed and V1–V5 were verified by that
same manual run, but a manual run is a one-off. The way to make it repeatable is a test on a
throwaway PostgreSQL container (Testcontainers, `org.testcontainers:postgresql` and `junit-jupiter`,
test scope; 1.21.4 is the version the Spring Boot BOM manages, and the artifact exists on Maven
Central). It needs a new dependency, so it was left for the human to decide, and the human chose to
record it rather than build it before the schema freezes.

**Consequence:** a migration or a mapping that works on H2 and fails on PostgreSQL — different SQL
dialect, a type or a constraint the two treat differently — passes every test and check and
surfaces at the first deployment, phase 4. V5 in particular was only ever applied to an empty
`article` table, and its own comment says that is the only case it is safe for.

**How to fix:** add the two Testcontainers dependencies, then a test that starts the context on a
PostgreSQL container with the real migrations and `ddl-auto: validate`, checks all migrations
succeeded and reads a row back from each table the way `SchemaMigrationTest` does. Show it red by
removing `flyway-database-postgresql`. Decide, at that point, whether the test fails or is skipped
when Docker is not running (failing is the safer default, since a skipped check passes silently).
Also retire the PostgreSQL clause of DEBT-004.

**Trigger:** the first migration added after the schema freeze (V6 or later), or the phase 4
deployment work, whichever comes first.

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

**Trigger:** the first code that writes `article_link` — FR-006 in phase 4. The human decided on
28 Sep that the extractor waits for FR-006 rather than being built with the phase 3 `wikilink` step. If the importer has run before it, the re-extraction is part of that work.

### DEBT-005 — The tag rule of ADR-0005 is written twice until `taxonomy` exists

**Status:** resolved 2026-09-27 — `taxonomy` publishes `Tags.named`, `backup` calls it, and
`tagsNamed` keeps only the translation of a refused tag into an `ArchiveFormatException`. The
importer's tag tests passed unchanged through the swap.
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

### DEBT-004 — Hibernate Search's two `app/pom.xml` dependencies are declared with no code or test using them yet

**Status:** resolved 2026-09-28 — the `search` slice (FEAT-007) maps `Article` and queries it through
both dependencies, and `SearchFlowTest` exercises them against the real analyzer on H2; 7.2.6 was
checked to be built against Hibernate ORM 6.6 (6.6.42), the line the application runs (6.6.53).
**Created:** 2026-09-21
**Narrowed:** 2026-09-26 — from four dependencies to two, see below
**Marker:** `app/pom.xml` — `hibernate-search-mapper-orm`, `hibernate-search-backend-lucene`

**Narrowed 26 Sep (phase 2 audit):** the entry was written for four dependencies. H2 is now
exercised by every persistence and controller test, so its clause is met. The PostgreSQL driver was
run once by hand against PostgreSQL 17.11, and since 27 Sep by every test under `-P postgres`
(DEBT-007, resolved), so its clause is met too. What remains here is
Hibernate Search, which no class and no test uses (`grep` for `hibernate.search`, `@Indexed` and
`SearchSession` under `app/src` finds nothing). The text below is the original, kept for the record.

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
