# Phase 2 — Walking skeleton and the rest of the tooling

**Status: done, closed 26 September 2026.** Planned for 12–20 September and overran it by six days;
steps 0–11 are done, the schema froze on 26 September, and the audit and the three checkups that
[roadmap.md](../ai/roadmap.md) requires are recorded under "Audit" below.

**Schema frozen:** 2026-09-26

## Goal

One thin vertical slice working end to end, the schema settled, and the gates that keep documentation
honest actually switched on.

## Steps

Ordered 21 Sep by dependency (was topic order until then; nothing in `app/src/main` exists yet
beyond package stubs, so this is the actual build order, not a renumbering of finished work). Each
step names what it depends on among the others.

- [x] **0. Build dependencies**: Flyway (runtime), a Spring Modulith core/JPA starter (today only
      `spring-modulith-starter-test` is declared in `app/pom.xml`), ArchUnit (test scope), and
      `db-util` (for step 4's `SQLStatementCountValidator`). Depends on: nothing
      — check: `./mvnw -pl app dependency:tree` lists all four; nothing else changes. **Landed 21 Sep**
      in commit `970a427` (`spring-boot-starter-data-jpa` stands in for the "core/JPA starter" item —
      `spring-modulith-starter-jpa` was tried and dropped, see that commit's message — and H2, the
      PostgreSQL driver and Hibernate Search were added early too, per DEBT-004), but the checklist and
      the check were never run against it. **Verified 22 Sep:** `./mvnw -pl app dependency:tree` lists
      `flyway-core:11.7.2:compile`, `spring-boot-starter-data-jpa:3.5.16:compile`,
      `archunit-junit5:1.3.0:test`, `db-util:1.0.7:test`; `./mvnw -pl app test` — 3 tests green.
- [x] **1. `shared/persistence`**: entities and Flyway migrations carrying trace anchors. Depends on:
      0
      — check: the migration runs against an empty database and one with data; the ERD matches.
      **Done 22 Sep:** `V1__create_content_and_moderation_schema.sql` creates the eight tables in
      `data-model.md` (`article`, `submission`, `revision`, `tag`, `article_tag`, `submission_tag`,
      `media_asset`, `report`, `article_link` — nine names, `article_tag`/`submission_tag` mapped as
      plain `@ManyToMany` join tables with no entity class of their own); eight JPA entities under
      `shared/persistence`, each `//trace:FR-XXX`. `SchemaMigrationTest` (`@DataJpaTest`,
      `AutoConfigureTestDatabase.Replace.NONE`, the real migration, no mocks) persists and reads back
      every table, including the two join tables and `article_link`'s composite key, and checks the
      `article.title`/`tag.name`/`submission.submission_number` uniqueness backstops — 10 tests, all
      new, all green. `spring.jpa.hibernate.ddl-auto: validate` added so Hibernate checks its mapping
      against the migration instead of generating its own schema next to it — the embedded-H2 default
      would otherwise have let both exist silently. DEBT-003 fixed in the same step, ahead of its own
      trigger (see tech-debt.md). `./mvnw verify` and `scripts/check.sh` both green.
      **Added 22 Sep, from reviewing the ERD table by table with the human:** `article_pinned_at_idx`
      on `article (pinned_at)`, so FR-009's landing page finds the pinned articles directly instead of
      scanning every row — plain, not partial/filtered (H2 has no expression indexes, checked directly
      against H2 2.3.232). `SchemaMigrationTest` gained an eleventh test asserting the index exists via
      JDBC metadata, red before the migration line, green after. The same review found five more
      places with the same shape of gap (`article.published_at`, `article.removed_at`,
      `submission.status`, `article_tag.tag_id`/`submission_tag.tag_id`, `media_asset.article_id`/
      `.submission_id`, `article_link.target_title`) — **not fixed, recorded here so they are not
      lost**: each is a column a `must` `FR` queries by by that isn't indexed, the same class of issue
      `docs/ai/security.md`'s N+1 rule already names. Left for a deliberate pass rather than fixed
      piecemeal, since several are better answered together with the query that will actually use them
      once the owning slice is written (phase 3), not guessed at from the schema alone.
      **Update 26 Sep, checked against the code before the freeze:** that list was partly overtaken.
      `submission.status`, `media_asset.article_id`/`.submission_id` and `article_tag.tag_id` were
      indexed on 22 Sep (V2, V4). `article.published_at` got `article_published_at_idx` in V6, since
      the landing page's recent list sorts by it (FR-009). Left unindexed on purpose:
      `article.removed_at` (nearly every row is `NULL`, so an index narrows nothing),
      `submission_tag.tag_id` and `article_link.target_title` (no code or requirement reads by them;
      the backlinks read by `target_article_id`, which V4 indexed).
      **Correction 26 Sep (audit A1):** "the eight tables" and "eight JPA entities" above are wrong.
      V1 creates nine tables, seven of them mapped by an `@Entity` class (Article, ArticleLink,
      MediaAsset, Report, Revision, Submission, Tag) and two, `article_tag` and `submission_tag`, by
      `@ManyToMany` with no class of their own, as the sentence itself lists.
      **Not done here:** slice repositories (phase 2 step 3), and DB-level case-insensitive
      uniqueness on `article.title` (H2 has no expression indexes — see the note in `data-model.md`
      next to `title`; enforced by the owning slice instead, in phase 3).
- [x] **2. ~~Design tokens pushed into Figma~~ — dropped 22 Sep.** No consumer needs the Figma
      layers: [docs/course/rubric.md](../course/rubric.md) doesn't ask for one, the stakeholder isn't
      a designer, and the durable design evidence already lives in the repository
      ([reference.md](../design/reference.md), `canvas-src/`, `screens/` — see
      [docs/design/README.md](../design/README.md)). The one thing this step actually guarded — no
      literal colour or spacing value in the first slice's template — doesn't need a Figma
      cross-reference to check; folded into step 3 below. Rationale recorded in
      [docs/design/README.md](../design/README.md)'s "Moving a design into Figma" section.
- [x] **3. First vertical slice by TDD**: `home` and `articleview` — the landing page leads to an
      article — including a minimal `wikilink` resolver (`[[Title]]` → link or red link), since the
      seed articles already use the syntax and `wikilink`'s ArchUnit rule (step 7) is already scoped
      for this phase. Depends on: 0, 1
      — check: a red MockMvc test existed before the controller; the template carries no literal
      colour or spacing value, only token names from [docs/design/reference.md](../design/reference.md)
      **Done 25 Sep**, as [FEAT-001](../features/FEAT-001-wiki-links-in-article-text.md) (the
      renderer), [FEAT-002](../features/FEAT-002-article-page.md) (the article page) and
      [FEAT-003](../features/FEAT-003-landing-page.md) (the landing page). Every test was red before
      its code, and a red test that could pass on an empty result was checked by removing the
      protection it guards (`escapeHtml`, `sanitizeUrls`, the removed-article filters, the list
      bounds, the ordering) and watching it fail. `TemplateTokensTest` is the template check above:
      no literal colour, length or inline style in any template. The design's palette lives in
      `static/css/tokens.css`. What was decided on the way, and is now in the schema and the route
      contract: `[[Title|words]]` (FR-002, glossary), commonmark-java 0.30.0 for Markdown, and the
      stored article address (`article.slug`, V5, open question 9). Known and left: the landing
      page's search box submits to `/search`, which is a 404 until phase 3; tags are not links yet;
      the frame is `shared/web/Layout.html` and Bootstrap is not used (FEAT-003 says why).
      `ArchitectureRulesTest` holds rule 4 (`wikilink` imports no Spring or JPA) and was shown to fail
      on a Spring import; step 7 still owes rule 3. FR-002 now words the match by article address, as
      the human decided after comparing it with Wikipedia's rule (FEAT-002); V5 has been run on H2
      only.
- [x] **4. Query-count gate for the N+1 rule** ([ADR-0010](../architecture/adr/ADR-0010-bounded-reads.md),
      [architecture/security.md](../architecture/security.md) — the rule is not in `docs/ai/security.md`,
      which only links to them) — a counter around slice 3's pages, using `db-util`'s
      `SQLStatementCountValidator`. Depends on: 0, 3
      — check: seed one row, then ten; the test fails if the number of queries moves
      **Done 25 Sep:** `PageQueryCountTest`, two tests. It asserts equality with a measured baseline,
      not a fixed number, and asserts the baseline is above zero so an unwired counter cannot pass
      it. The landing page is measured with 1 pinned + 1 recent article (both lists non-empty),
      then 2 + 8, then 15 + 15; the article page with one existing and one missing wiki link, then
      ten of each. Measured: baselines of 1 + 0, 0 + 1 and 1 + 1 give the same count as 2 + 8, so
      the choice of 1 + 1 is not load-bearing. The limits of 12 do not show in a `SELECT` count (an
      unbounded list is still one query), so the bounds are held by `LandingControllerTest`, not
      here. The count comes from `datasource-proxy` (`db-util`'s own dependency, so nothing was
      added), wrapped around the data source by a `BeanPostProcessor` inside the test.
      **Shown to fail, not assumed:** with `default_batch_fetch_size=1` the landing test failed with
      5 expected against 13 recorded; with one `findLiveSlugs` call per link instead of one for the
      page, the article test failed with 3 expected against 21. Both changes were reverted.
- [x] **5. `backup`**: export and import of the archive format; seeding runs through the importer.
      Depends on: 1
      — check: export, wipe, import produces an identical database
      **Done 25 Sep:** [FEAT-004](../features/FEAT-004-article-archive.md).
      `ArticleArchive` (import from a map of files or a directory, export to a directory) and a
      `SeedRunner` behind the `seed` profile, tested by `ArticleArchiveTest`, `ExportQueryTest` and
      `SeedRunnerTest`. The design document names this step's test
      `BackupServiceTest.exportWipeImportProducesIdenticalDatabase`; it is
      `ArticleArchiveTest.what_an_export_holds_imports_back_as_the_same_articles`. The round trip is
      asserted on title, address, summary, body, both timestamps, the pinned time and tags. Decided
      with the human on 25 Sep: `author` in the seed files is read and dropped (no column, no
      requirement); an article already there is skipped and reported; `pinned` is an optional key; an
      import does not bring back a removed article. The front matter field list ADR-0007 promised is
      now in [data-model.md](../architecture/data-model.md).
      **How the tests were shown to work.** Import behaviour written before its test — tag
      normalisation and reuse, skipping, the address conflict, rollback, most refusals — was covered
      afterwards: each of those protections was removed and a named test went red. The empty-body
      refusal was test-first, and so was the duplicate-key refusal. The export protections (no
      overwrite, every page, removed articles, readable output, tag order, the created directory, the
      README and non-`.md` files, the sort) and the seed runner's README filter were removed the same
      way. An independent review found three that stayed green — the sort, `Locale.ROOT` and the
      regular-file check — and each now has a test that goes red without it. The `pinned` key, its date check, writing it only for a pinned article and carrying it both
      ways were each removed and a named test went red. Not covered by a red test: the timestamp
      precision below a millisecond, which the format loses, so "an identical database" holds to the
      millisecond. A real run with
      `seed` served `/` and the FRRO article. Tag normalisation is written twice until `taxonomy`
      exists (DEBT-005); the importer writes no `article_link` rows until an extractor exists
      (DEBT-006).
- [x] **6. Content: 20 or more articles** load through the importer built in step 5 — the 20 files
      under `data/seed/` already exist. Depends on: 5
      — check: 20 or more files under `data/seed/` load through the importer without error, and the
      count is read from the database rather than from the directory
      **Done 25 Sep:** `SeedRunnerTest` counts the rows in `article` after starting with the `seed`
      profile (at least 20, the FRRO article among them) and shows a second run adds none. The mid-demo
      step in phase 3 asks for 30 or more; that is more articles to write, not more code.
- [x] **7. Spring Modulith documenter in the build; ArchUnit for the two rules Modulith does not
      cover**. Depends on: 1, 3 — needs real slice code to check against. Rule 4 (`wikilink`) was
      already in `ArchitectureRulesTest`, added with step 3; rule 3 (`shared.persistence` imports no
      slice) was added here
      — check: `./mvnw verify` writes the module canvas, and each ArchUnit rule is demonstrated by
      deleting it and watching a test go green that should not have
      **Done 26 Sep.** Rule 3 is `ArchitectureRulesTest.shared_persistence_imports_nothing_from_a_slice`,
      phrased as "depends on nothing under `in.ac.iitm.guide` outside `shared`" rather than as a list
      of slices, so an eleventh slice is covered without anyone editing the rule.
      `ModularityTest.the_build_writes_a_canvas_for_every_slice` calls Spring Modulith's `Documenter`
      (`spring-modulith-docs` was already in the tree through `spring-modulith-starter-test`, so no
      dependency was added), writes to `app/target/spring-modulith-docs/` — gitignored, rebuilt by
      every test run, so it cannot go stale — and reads the eleven `module-*.adoc` files back.
      **How it was shown to work.** Rule 3 failed on a temporary class in `shared.persistence` that
      referenced `wikilink`, and passed without it. The canvas test deletes the output folder before writing, and failed with the `Documenter` call
      removed while the eleven files of a previous build were still on disk (a first version did not
      delete the folder and stayed green in that case, found by the independent review). Then the step's own check: both rules deleted, one violation of
      each added (`shared.persistence` referencing `wikilink`; `wikilink` referencing
      `org.springframework`) — `ModularityTest` stayed green, so Modulith catches neither and the two
      ArchUnit rules are the only guard; with the rules back, both violations fail. Temporary classes
      removed afterwards.
      **Known thin:** the canvases hold only the base package for every slice so far, because the
      slices are almost empty; they become useful as published types and dependencies appear. The
      `Documenter`'s PlantUML diagrams are not written: the C4 figures are drawn by hand from
      `docs/diagrams/src/` and Modulith would currently draw eleven boxes and nearly no arrows (see
      the architecture row in [rubric.md](../course/rubric.md)). **Deferred, not dropped:** generate
      the component diagram from the code once two slices talk through a published type — the
      condition that row already names.
- [x] **8. `ai-tools`**: `trace` in full, the blocking `stop` gate, the edit reminder, `weekly`,
      `ownership`, and the gap-list generator (added 21 Sep — step 10 needs it and no step
      previously built it). `weekly` and `ownership` **exclude the journal commits the `Stop` hook
      writes** and report them in a separate column — see [docs/team/README.md](../team/README.md);
      counting them measures prompting rather than authoring, and would have read this team
      backwards. Depends on: 1, 3 — trace anchors must exist before they can be traced
      — check: the traceability matrix generates and is non-empty; `ownership` agrees with
      `sh scripts/contribution.sh` on the authored/hook split; `gaps` runs without error
      **Done 26 Sep**, with all five generators in this phase (the human decided against moving any
      to phase 3). `trace` (`Trace`) writes `docs/traceability.md` and `docs/features/README.md` and
      `trace --check` fails on a gap or a stale file; `trace --docs-sync <ref>` (`DocsSync`) is the
      blocking half of [docs-sync.md](../ai/docs-sync.md); `gaps` (`Gaps`), `ownership`
      (`Ownership`) and `weekly` (`Weekly`) share one reading of `git log` (`History`) that leaves out
      merges and reports the `Stop` hook's commits apart. Each has a test file that was red before its
      code, and each protection was removed once to see a named test fail; three of those checks
      found a test too weak (build output being read, a wrapped paragraph, an ADR named outside the
      header) and each was sharpened.
      **Check, as run:** the matrix has a row for every requirement; `ownership` and `weekly` agree
      with `sh scripts/contribution.sh` (W39: 23 authored and 167 by the hook for one member, 43 and
      31 for the other, on both); `gaps` runs and lists the open debt entries. Real findings, not
      hidden: the matrix first noted five `planned` requirements with anchors (FR-008, FR-010,
      FR-020, FR-021, NFR-004). Four were the tool's mistake: their anchors sit on JPA entities and
      migrations, the schema laid down ahead of the slice, so an anchor under `shared/persistence`
      no longer counts as behaviour (`Anchor.isBehaviour`, three tests). The fifth was real: NFR-004
      moved to `in-progress` on 26 Sep by the human's decision, since the archive is built and tested
      but nobody can run an export before the admin panel of phase 4. **Known thin:** acceptance criteria carry no identifiers, so
      "criteria with no test" is criteria minus anchored tests, a lower bound, and says so; the
      ownership table decides "more work" by commits and shows lines beside them; `ownership`, `gaps`
      and `weekly` are not compared against the files (only `trace` is), because they change with
      every commit.

- [x] **9. Gate wired into the `Stop` hook and into CI**. Depends on: 8
      — check: break a migration without touching the data model document; the turn must not close
      **Done 26 Sep.** `Gate` (docs-check, `trace --check`, `schema-freeze`, the blocking rows of
      docs-sync against `HEAD`) is what the `Stop` hook runs; `scripts/check.sh` runs the same
      checks and CI passes it `DOCS_SYNC_BASE`, the merge base or the previous tip. **Check, as run:**
      in a scratch repository a new migration with no word in `data-model.md` made `hook stop` answer
      `deny` naming the file and the document, and the same event a second time was let through, as
      the one-block-per-cause rule says. A check that cannot read git (no commit yet) is written into
      the journal entry as not run and does not refuse. **Not run:** the CI workflow itself, which
      needs a push; its shell was only parsed, and the merge-base branch is untested.

- [x] **10. Slash commands for ownership and the gap list**, once their generators exist. Depends
      on: 8
      — check: `ai-tools docs-check` passes with both advertised in `CLAUDE.md`; it already refuses
      a command named in the instructions with no skill behind it
      **Done 26 Sep**, once the human agreed to the edit to the agent's own instructions: `/ownership`
      and `/gaps` (`.claude/skills/`), listed in `CLAUDE.md` and `docs/ai/README.md`. The same edit
      removed the "phase 2" wording that no longer described anything, in `CLAUDE.md`,
      `docs/ai/README.md`, `docs-sync.md`, `definition-of-done.md`, `collaboration.md` and the `/dod`
      and `/trace-check` skills. **Check, as run:** `docs-check` passes with both advertised, and
      fails naming `/gaps` when its skill is moved aside.

- [x] **11. The schema freezes.** After that it changes by agreement only. Depends on: 1, 5, 6 — a
      closing milestone, not a build task, placed last: freezing before the schema has settled would
      be vacuous, and freezing before the importer has exercised it risks finding a needed column
      only after the ADR cost already applies
      — check: every migration added after the closing date recorded in this file has a matching ADR
      under `docs/architecture/adr/` that motivated it, and the migration's own header comment names
      that ADR by number; a migration with no matching ADR is the freeze being broken quietly rather
      than deliberately. Mechanism decided 21 Sep — see open question 2 below.
      **Check built 26 Sep, freeze not declared.** `SchemaFreeze` (`ai-tools schema-freeze`, also in
      `Gate` and `check.sh`) reads the date from a line `**Schema frozen:** YYYY-MM-DD` in this file
      and dates each migration by the commit that added it. Until that line exists nothing is frozen
      and it asks nothing, so writing the date is the human's act and closes the step. A migration
      added after it must carry `-- adr: ADR-NNNN` in its header comment naming an ADR that exists;
      whether that ADR really motivates the change is for a reader.
      **Done 26 Sep:** the human declared the freeze on 2026-09-26, the line above. Before it, the
      migrations were run on a real PostgreSQL 17.11 (Docker), which the H2 tests had never done: the
      application did not start until `flyway-database-postgresql` was added, and V1–V6 then applied,
      Hibernate's `validate` passed and the `seed` profile loaded 20 articles (a manual run; the
      automated test is DEBT-007). V6 is the last migration without an ADR. The check compares a
      migration's date with the freeze date strictly (`isAfter`), so a migration added on 26 Sep
      itself is still not "after"; the first one that needs an ADR is dated 27 Sep or later.

## Readiness criterion

The gate genuinely blocks — demonstrated by breaking something on purpose, not assumed. Export and
import round-trip cleanly. The weekly log and the ownership summary are produced without anyone
writing them by hand.

## Closing

All three parts of the criterion have evidence in the steps above: the gate refused a migration with no
word in `data-model.md` (step 9), export, wipe and import gave the same articles (step 5), and
`weekly` and `ownership` agree with `scripts/contribution.sh` (step 8). What keeps the phase open is
the audit [roadmap.md](../ai/roadmap.md) requires before any phase closes, and that was done on
26 September: the claim audit, then `/doctor`, `/skill-doctor` and `/context` run by the human (all
below). **The phase closed on 26 September 2026.**

Left open, none of it blocking: A5 (the route parameter's name) and A6 (phase 1 still marked in
progress) wait for the human; DEBT-007 (no automated test on PostgreSQL) and DEBT-004 (Hibernate
Search unused) stay in [tech-debt.md](../tech-debt.md). The audit was made by the agent that did the
work, so it is a second reading by the same reader; the checkups are the human's.

## Audit, 26 September

Read: every step's "Done" claim in this file, the claims of `data-model.md`, `overview.md`,
`architecture-rules.md` (enforced rules), `ui-routes.md` (routes), `tech-debt.md` and the four
`FEAT` files, each against the code, the migrations, the build or a command run that day. The
agent that did the work also did this audit, so it is a second reading by the same reader.

**Confirmed, with what was checked.** Step 0: `dependency:tree` lists `flyway-core` 11.7.2,
`spring-boot-starter-data-jpa` 3.5.16, `archunit-junit5` 1.3.0 and `db-util` 1.0.7. Step 1: V1
creates nine tables, and every index `data-model.md` names exists in a migration. Step 3:
`TemplateTokensTest`, `tokens.css`, `ArchitectureRulesTest` rule 4 and the three `FEAT` files
exist. Step 4: `PageQueryCountTest` holds two tests. Step 5: `ArticleArchiveTest`,
`ExportQueryTest` and `SeedRunnerTest` exist, and so does the renamed round-trip test. Step 6: 20
articles sit under the seed folder. Step 7: the rule-3 test, the canvas test, and eleven
`module-*.adoc` files after a build. Steps 8–10: all five generators and `Gate` are classes in
`tools`, `scripts/check.sh` runs `trace --check` and `schema-freeze`, the `Stop` hook is wired in
`settings.json`, CI passes `DOCS_SYNC_BASE`, and `/ownership` and `/gaps` exist as skills and are
listed in `CLAUDE.md`. `docs-check` passes. The only code marker, `TODO(DEBT-005)`, names an entry
that exists.

| # | Finding | Evidence | Status |
|---|---|---|---|
| A1 | Step 1 says V1 creates "the eight tables" and that there are "eight JPA entities". V1 creates nine tables, and seven classes carry `@Entity` (Article, ArticleLink, MediaAsset, Report, Revision, Submission, Tag); `article_tag` and `submission_tag` have no entity. The same sentence lists nine names. | `grep -c "^CREATE TABLE"` on V1 gave 9; `grep -l "^@Entity"` gave 7 files | proposed: a dated correction to the step |
| A2 | The test plan in [01-requirements-design.md](01-requirements-design.md) still names `BackupServiceTest.exportWipeImportProducesIdenticalDatabase`, a class that does not exist; the test is `ArticleArchiveTest.what_an_export_holds_imports_back_as_the_same_articles`. Step 5 records the rename, the test plan does not. `design-doc.tex` holds the old name too, as the document submitted on 11 Sep. | `grep` for the name; the test file | proposed: a note in the test plan; the submitted document stays |
| A3 | [overview.md](../architecture/overview.md) and DEBT-004 say none of the four dependencies is exercised by a test. H2 is: `SchemaMigrationTest`, `ArticleControllerTest` and the others run on it. The PostgreSQL driver was exercised on 26 Sep by hand only (DEBT-007). Hibernate Search is used by no class and no test. DEBT-004's own fix says to drop H2's clause at step 1. | `grep` for `hibernate.search`, `@Indexed`, `SearchSession` in `app/src` found none | proposed: narrow DEBT-004 to Hibernate Search, correct the sentence in overview.md |
| A4 | `CLAUDE.md` lists Bootstrap 5 in the stack and `static/README.md` says "Bootstrap overrides"; FEAT-003 says Bootstrap is not used, and no template or static file references it. | `grep -ril bootstrap` over templates and static gave only the README | open: `CLAUDE.md` is the human's; the static README is a one-line fix |
| A5 | `ui-routes.md` writes the article route as `/articles/{title}`; the controller maps `/articles/{address}`. Line 32 defines `{title}` as the slug, so the meaning agrees and the name does not. The landing page's search box posts to `/search`, which does not exist yet (known, step 3). | the controllers' `@GetMapping`; `ui-routes.md` lines 32 and 89 | proposed: leave the name, it is the human's route contract |
| A6 | Phase 1 is still "in progress" in [README.md](README.md) and in its own file, with 17 of 17 steps done. `scripts/session-start.sh` therefore reports phase 1 and a design-document deadline of 11 Sep that is past. | the SessionStart report of 26 Sep; `01-requirements-design.md` header | open: closing phase 1 needs its own readiness check, `/course-check design` |
| A7 | `weekly-log`'s `argument-hint` was invalid YAML, so its name, description and `disable-model-invocation: true` were all dropped, while `CLAUDE.md` says the command is started only by a person. Found by the `/doctor` frontmatter check, not by the claim audit. | `yaml.safe_load` failed at line 3, column 32 of the frontmatter | fixed 26 Sep: the value is quoted, and all 13 project skills parse |

**Checkups, all three run by the human on 26 Sep.**
- `/doctor`: the settings files parse; no MCP server is configured; hooks are fast (median about
  160 ms, worst 462 ms, none timed out, over 26 sessions in 22 days); of 30 denied tool calls none was
  a read-only command, so no permission rule was proposed. The `claude` command is not on the PATH of
  this environment (the session runs in the VS Code extension), so the installed version could not be
  read; the newest installed extension is 2.1.282 and 2.1.283 is published. Auto mode was not made
  the default: whether a hook's `ask` still fires under it is not established (audit of 21 Sep), and
  this repository's confirmation on protected files depends on it. The one defect is A7.
- `/skill-doctor`: four project skills in the listing were never invoked (`gaps`, `ownership`,
  `sharpen`, `trace-check`, about 370 tokens per turn); they stay, being project files, two of them
  written the same day. Nine skills synced from claude.ai (`anthropic-skills:*`, about 1.9k tokens per
  turn) were never invoked; they belong to the human's account, not the repository, and were left on.
  The `/doctor` report first called them built-in, which was wrong.
- `/context`: 217.8k of 1M tokens in use (22%); `CLAUDE.md` 3.4k (a disk estimate had said 2.2k),
  skills 4.8k, custom agents 270, deferred MCP tools 2.2k (the claude.ai Docs connector, unused).
  Path-scoped rules are absent from the list until a matching file is touched; in this session
  `java-style.md`, `testing.md`, `schema.md` and `docs.md` reached the model when files they cover
  were read or edited, so the scoping works.

**Follow-up, 26 Sep.** The human decided to drop Bootstrap rather than add it: the pages run on the
design tokens, the design screens use no framework, and adding one would put a second set of
colours beside the tokens that `TemplateTokensTest` guards. A1 corrected in step 1; A2 noted in the
test plan; A3 recorded by narrowing DEBT-004 and correcting overview.md; A4 fixed in
`static/README.md` and in the stack line of `CLAUDE.md`. `PLAN-PROMPT.md` and `proposal.tex` still
name Bootstrap, as dated records of what was planned and submitted. A5 stays open. A6 closed later
the same day: phase 1 was audited and closed on 26 Sep ([01-requirements-design.md](01-requirements-design.md)).

**Not re-run.** The removals that steps 3–8 say were made to see a test fail were not repeated;
the claims stand on what those steps recorded. The `weekly` and `ownership` figures of week 39 are
time-dependent and were not compared again. The CI workflow has not run on a remote. V5 has been
applied only to an empty `article` table. No check found a claim in the steps that is false
beyond A1 and A3; A2, A4 and A5 are stale names, not wrong behaviour.

## Open questions

Raised on 7 September while giving every step a checkable result. Each one is a decision the check
could not be written without, and none is the agent's to settle.

1. ~~**Which two rules does ArchUnit cover that Modulith does not?**~~ Resolved — already answered
   in [architecture-rules.md](../ai/architecture-rules.md)'s "Enforced rules" section (added 10 Sep,
   after this question was raised 7 Sep, and never cross-checked back against it): `shared.persistence`
   imports nothing from a slice, and `wikilink` imports neither `org.springframework` nor
   `jakarta.persistence`. The decision already existed; only the stale note here needed closing.
   Both are tests now, in `ArchitectureRulesTest` (step 7, 26 Sep).
2. ~~**What does "changes by agreement only" mean mechanically for the frozen schema?**~~ Decided
   21 Sep, by the human, after three options were laid out (a self-certifying header line; an ADR per
   change; a hybrid of the two): **an ADR per post-freeze schema change.** Every migration dated after
   this phase's closing date must point at a `docs/architecture/adr/` entry that argued for it, the
   same weight already given to `moderate`'s storage model (ADR-0003) or the admin login (ADR-0009) —
   not a lighter-weight convention invented just for this. The step's check above now asks for that
   ADR to exist and be named in the migration header, not for a header line alone.

   **What this costs, so it isn't a surprise later:** every schema change after the freeze — even a
   one-column fix — needs its own ADR file, not a quick migration. That was the explicit trade-off
   against the cheaper header-line option, made because a schema change is exactly the kind of
   two-person collision this project's whole slice structure exists to prevent, and this project
   already treats the schema as the human's decision, not something a commit message alone should
   carry ([.claude/rules/schema.md](../../.claude/rules/schema.md)).
3. ~~**What is `links`?** The step was its only mention anywhere in the repository — nothing said
   what it would generate or check.~~ Resolved 7 Sep: it meant checking links, and `ai-tools
   docs-check` already refuses a broken markdown link to a missing `.md` file. Dropped from the step
   as a duplicate rather than built twice.

   What `docs-check` does **not** verify, if this ever matters: an `#anchor` within a file, and a
   link whose target is not markdown. Both would be rules inside `docs-check`, not a separate
   generator.
4. ~~**Do the six `ai-tools` generators belong in this phase at all?**~~ Decided 7 Sep: they stay.
   The audit of 7 September argued for moving `trace` and `ownership` to phase 3, since one reads
   code anchors and feature files and the other measures per slice, and neither exists until phase 3
   — so in this phase they would generate an empty matrix and an ownership table over no slices. The
   decision is to build them here anyway, so that they are ready when the code arrives.

   The cost is recorded rather than argued: five of the nine days of this phase are shared between
   process tooling and the first vertical slice, and the two generators cannot be confirmed by their
   own check clause until phase 3 gives them something to read.
5. ~~**What dependencies does phase 2 actually need declared?**~~ Resolved 21 Sep, during a
   dependency-order review of this file: Flyway, a Spring Modulith core/JPA starter, and ArchUnit
   (test scope) — all three already implied by steps this file already committed to; only the
   artifacts in `app/pom.xml` were missing. Now step 0.
6. ~~**Which library backs the N+1 query-count gate?**~~ Resolved 21 Sep: `db-util`'s
   `SQLStatementCountValidator`, the technique already named — but not yet adopted as a dependency —
   in [testing.md](../ai/testing.md).
7. ~~**Does `wikilink` get built in phase 2, and when?**~~ Resolved 21 Sep: yes, a minimal resolver
   as part of step 3, the first vertical slice. The seed articles already contain `[[links]]`, and
   `wikilink`'s ArchUnit rule (step 7) is already scoped for this phase, so the module has to exist
   by then regardless.
8. ~~**Who builds the gap-list generator that the ownership/gap-list slash-command step
   presupposes?**~~ Resolved 21 Sep: this file was missing that build task entirely — added as part
   of step 8 (`ai-tools`) rather than left unstartable.
9. ~~**How is an article found by its address?**~~ Decided 25 Sep, by the human, after three options
   (**A** a stored, unique `slug` column; **B** compute over `(id, title)` in code; **C** the title
   itself in the path, matched on `lower(title)`): **A.** Found on reading the route contract before
   writing `articleview`: [ui-routes.md](../architecture/ui-routes.md) made `{title}` a slug and said
   nothing new is stored, but a slug cannot be turned back into a title, so finding the article
   meant computing the slug of every title — the unbounded read
   [ADR-0010](../architecture/adr/ADR-0010-bounded-reads.md) forbids — and two different titles
   ("Fees & Payments", "Fees Payments") give one slug, which FR-010's case-insensitive check does not
   catch. Built as migration V5 (unique, `NOT NULL`, before the schema freezes at step 11, so no ADR)
   and `wikilink`'s `ArticleAddress`.

   **Consequences carried forward:** the importer (step 5) must compute the slug of every article it
   loads, and `contribute`/`moderate` (phase 3) must do the same and reject a title whose slug is
   taken; FR-010 names only the case-insensitive title check and needs a clause, which is the
   human's to add. C was set aside also because Tomcat refuses a `%2F` in a path with `400` — seen in
   the running application, so a title containing `/` could not have been an address.
