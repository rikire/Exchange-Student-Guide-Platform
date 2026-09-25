---
id: FEAT-004
title: Article archive — export, import and seed
status: in-progress
covers: [NFR-004, FR-009]
slice: backup
routes: []
tables: [article, tag, article_tag]
code:
  - app/src/main/java/in/ac/iitm/guide/backup/ArticleArchive.java
  - app/src/main/java/in/ac/iitm/guide/backup/ImportReport.java
  - app/src/main/java/in/ac/iitm/guide/backup/ArchiveFormatException.java
  - app/src/main/java/in/ac/iitm/guide/backup/internal/FrontMatter.java
  - app/src/main/java/in/ac/iitm/guide/backup/internal/ArchivedArticle.java
  - app/src/main/java/in/ac/iitm/guide/backup/internal/SeedRunner.java
  - app/src/main/java/in/ac/iitm/guide/backup/persistence/ArchiveArticleRepository.java
  - app/src/main/java/in/ac/iitm/guide/backup/persistence/ArchiveTagRepository.java
tests:
  - app/src/test/java/in/ac/iitm/guide/backup/ArticleArchiveTest.java
  - app/src/test/java/in/ac/iitm/guide/backup/SeedRunnerTest.java
  - app/src/test/java/in/ac/iitm/guide/backup/ExportQueryTest.java
---

# FEAT-004 — Article archive

## Why

OGE must never be locked into this system (NFR-004): the whole guide has to come out as plain files
anyone can open, and the same files have to go back in. The same mechanism fills a fresh installation
with the starter articles, so the import path is exercised every time the application starts with the
`seed` profile, not only when someone remembers to test it (ADR-0007).

## Scenario

Starting the application with the `seed` profile loads the articles under `data/seed/`. Calling the
export writes one `<address>.md` file per published article into an empty directory; calling the
import on that directory, into an empty database, brings the same articles back. There is no screen
for either: the admin panel that would host an export button is phase 4.

## Routes

None.

## Schema impact

None. Reads and writes `article`, `tag` and `article_tag` through two repositories the slice owns.

## Decisions this feature fixed

- **`author` is read and dropped.** Every seed file carries `author:`; `article` has no column for it
  and no requirement asks for attribution (no accounts, CON-001). Decided by the human on 25 Sep. An
  exported file therefore has no `author` line, and a file with any other unknown key is refused —
  a mistyped `tag:` would otherwise lose every tag without a word.
- **An article whose title is already there is skipped and reported**, not updated and not an error,
  so seeding twice is harmless. Two titles with one address ("Fees & Payments", "Fees Payments") are
  refused by name instead: skipping the second would lose an article silently. Two files with one
  title in one import give one article and one skipped title.
- **An article that was removed still counts as there**, so an import does not bring it back: a
  moderator's removal (FR-026) outlives a restore from an older export. Decided by the human on
  25 Sep.
- **`pinned` is an optional key** carrying `pinned_at`, so the landing page's pinned section survives
  an export and an import (FR-009). Decided by the human on 25 Sep; the field list is in
  [data-model.md](../architecture/data-model.md).
- **A key given twice in one file is refused**, since the parser would otherwise keep the second value
  and drop the first without saying so.
- **One bad file rolls the whole import back.** Files are read in name order, so the same input fails
  on the same file.
- **Tags are stored trimmed and lower-cased** (ADR-0005), by `backup` itself until `taxonomy` exists —
  DEBT-005.
- **`created` and `updated` become `published_at` and `updated_at`.** YAML reads a timestamp to the
  millisecond, so an export and re-import keeps timestamps to the millisecond, not below. A date with
  no time is midnight UTC.
- **An empty body is refused**, as FR-010 requires a body of a submitted article.
- **The export is written by SnakeYAML**, so a title with a colon, a quote, a leading `@` or `#`, or
  the word `Yes` comes out quoted and reads back unchanged. Non-ASCII text is written as itself and
  lines are not folded, because the file has to be readable without the application.
- **The export reads 100 articles at a time**, sorted by address (ADR-0010). `ExportQueryTest`
  asserts the sort on the SQL, because H2 returns rows in a stable order anyway and a missing sort
  would show only on PostgreSQL. Tags load lazily inside the read transaction, in batches; no test
  counts the export's queries. It refuses to overwrite a file, so a failure part-way leaves the files already written.
- **The seed is read from the classpath**, not from a directory, because inside a jar it is not one.

Checked in the running application with `-Dspring-boot.run.profiles=seed`: `/` returned 200 with
links to articles and `/articles/registering-with-frro` returned 200.

## Acceptance criteria

- [x] A file becomes a published article with its title, address, summary, body, timestamps and tags
- [x] Tags are stored trimmed and lower-cased, and two articles with one tag share one row
- [x] An article already there is skipped, reported and left as it was
- [x] A title with the address of another article is refused, naming both
- [x] A file that cannot become an article is refused, naming the file and the fault (twelve cases:
      no front matter, unknown key, invalid YAML, no or non-text title, title with no address, no
      summary, bad date, tags not a list, empty tag, empty body, a key given twice)
- [x] One bad file, in a map or in a directory, leaves the database as it was
- [x] Windows line endings are read like any other
- [x] Every `.md` file of a directory is imported except `README.md`; a subdirectory is not read
- [x] An export holds one file per published article, named by its address, and none for a removed one
- [x] More articles than one page (205) all reach the export
- [x] An export reads the articles sorted by address (asserted on the statement, `ExportQueryTest`)
- [x] Tags are lower-cased the same way whatever the machine's language (a Turkish locale in the test)
- [x] An article removed since the export is not brought back by an import
- [x] An export never overwrites a file, and creates the directory it is given
- [x] An exported file is plain readable text: unescaped Devanagari, an unfolded summary, tags in order
- [x] Export, clear, import gives the same articles: title, address, summary, body, both timestamps
      and tags, for titles needing quotes and a body with its own blank lines
- [x] The `seed` profile loads at least twenty articles, counted in the database, including the FRRO
      article, and running it again adds nothing
- [x] `pinned_at` survives the round trip; only a pinned article carries a `pinned` line; a `pinned`
      that is not a date refuses the file
- [ ] Media assets travel with the articles (ADR-0007): needs the `media` slice, phase 3

## Deliberately out of scope

- An admin screen or route for export and import (phase 4, with the admin panel).
- Rejected submissions, retained revisions, removed articles and tags with no article: not exported,
  as ADR-0007 decides.
- Updating an article from a newer file.

## Open questions

1. **Values the database cannot hold.** A title of about a hundred Devanagari letters gives a file
   name over 255 bytes and the export fails with an `UncheckedIOException`; a title over 255
   characters or a tag over 64 reaches the database and fails as a `DataIntegrityViolationException`
   instead of an `ArchiveFormatException`. The import still rolls back. Not met by any article today.
2. **A file saved with a byte-order mark**, or with spaces after the closing `---`, is refused as
   having no front matter, which names the wrong fault. Not met by any seed file.
3. **Article links.** The importer writes no `article_link` rows, because the extractor does not
   exist: DEBT-006.
4. **NFR-004's status** is `planned` and stays so until the human changes it; the fit criterion is
   met for published articles, without media.
