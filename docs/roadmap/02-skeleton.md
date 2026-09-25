# Phase 2 — Walking skeleton and the rest of the tooling

**Status: in progress.** Runs 12–20 September 2026; steps 0–3 done.

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
- [ ] **4. Query-count gate for the N+1 rule** in [security.md](../ai/security.md) — a counter
      around slice 3's tests, using `db-util`'s `SQLStatementCountValidator`. Depends on: 0, 3
      — check: seed one row, then ten; the test fails if the number of queries moves
- [ ] **5. `backup`**: export and import of the archive format; seeding runs through the importer.
      Depends on: 1
      — check: export, wipe, import produces an identical database
- [ ] **6. Content: 20 or more articles** load through the importer built in step 5 — the 20 files
      under `data/seed/` already exist. Depends on: 5
      — check: 20 or more files under `data/seed/` load through the importer without error, and the
      count is read from the database rather than from the directory
- [ ] **7. Spring Modulith documenter in the build; ArchUnit for the two rules Modulith does not
      cover**. Depends on: 1, 3 — needs real slice code to check against. Rule 4 (`wikilink`) is
      already in `ArchitectureRulesTest`, added with step 3; rule 3 (`shared.persistence` imports no
      slice) remains
      — check: `./mvnw verify` writes the module canvas, and each ArchUnit rule is demonstrated by
      deleting it and watching a test go green that should not have
- [ ] **8. `ai-tools`**: `trace` in full, the blocking `stop` gate, the edit reminder, `weekly`,
      `ownership`, and the gap-list generator (added 21 Sep — step 10 needs it and no step
      previously built it). `weekly` and `ownership` **exclude the journal commits the `Stop` hook
      writes** and report them in a separate column — see [docs/team/README.md](../team/README.md);
      counting them measures prompting rather than authoring, and would have read this team
      backwards. Depends on: 1, 3 — trace anchors must exist before they can be traced
      — check: the traceability matrix generates and is non-empty; `ownership` agrees with
      `sh scripts/contribution.sh` on the authored/hook split; `gaps` runs without error
- [ ] **9. Gate wired into the `Stop` hook and into CI**. Depends on: 8
      — check: break a migration without touching the data model document; the turn must not close
- [ ] **10. Slash commands for ownership and the gap list**, once their generators exist. Depends
      on: 8
      — check: `ai-tools docs-check` passes with both advertised in `CLAUDE.md`; it already refuses
      a command named in the instructions with no skill behind it
- [ ] **11. The schema freezes.** After that it changes by agreement only. Depends on: 1, 5, 6 — a
      closing milestone, not a build task, placed last: freezing before the schema has settled would
      be vacuous, and freezing before the importer has exercised it risks finding a needed column
      only after the ADR cost already applies
      — check: every migration added after the closing date recorded in this file has a matching ADR
      under `docs/architecture/adr/` that motivated it, and the migration's own header comment names
      that ADR by number; a migration with no matching ADR is the freeze being broken quietly rather
      than deliberately. Mechanism decided 21 Sep — see open question 2 below.

## Readiness criterion

The gate genuinely blocks — demonstrated by breaking something on purpose, not assumed. Export and
import round-trip cleanly. The weekly log and the ownership summary are produced without anyone
writing them by hand.

## Open questions

Raised on 7 September while giving every step a checkable result. Each one is a decision the check
could not be written without, and none is the agent's to settle.

1. ~~**Which two rules does ArchUnit cover that Modulith does not?**~~ Resolved — already answered
   in [architecture-rules.md](../ai/architecture-rules.md)'s "Enforced rules" section (added 10 Sep,
   after this question was raised 7 Sep, and never cross-checked back against it): `shared.persistence`
   imports nothing from a slice, and `wikilink` imports neither `org.springframework` nor
   `jakarta.persistence`. The decision already existed; only the stale note here needed closing.
   The tests themselves are still this phase's work — `ModularityTest.java` currently has only the
   two Modulith checks, not these two.
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
