# ADR-0007 — Export is Markdown with YAML front matter, and it is also the seed and the import

**Status:** accepted
**Decided:** by 4 September — NFR-004's fit criterion and the seed directory's README already name Markdown with front matter as the format
**Recorded:** 10 September, after the fact — see [README.md](README.md)

## Context

NFR-004 exists because of something the stakeholder needs rather than something the software needs:
OGE must never be locked into this system. Its fit criterion is specific — an export produces plain
files containing every published article and its metadata, **openable and readable without running
the application**.

Three other things want the same shape and would otherwise each invent their own. The `backup` slice
exports and imports the whole knowledge base. The seed content under
`app/src/main/resources/data/seed/` ships the starter articles. Test fixtures need article content
that is readable in a diff when a test fails.

[ADR-0001](ADR-0001-article-body-format.md) already fixed the body as Markdown. So the only open
question is the **metadata** — title, summary, tags, timestamps — and whether the export is one
format or several.

## Options

### A. A SQL dump

`pg_dump`, and the truest possible copy: every row, every relation, restorable in one command.

It fails NFR-004's actual sentence. A SQL dump is readable without running *this* application only in
the sense that a text editor will open it; getting an article out means either running PostgreSQL or
reading `INSERT` statements by hand. It is a database backup, which the project should also have,
and it is not an answer to "OGE is not locked in". It is also engine-specific, so it would not
restore into the H2 stand.

### B. A JSON (or single-file) dump

One file, exact round-trip, trivially machine-readable, and no ambiguity about how metadata is
encoded.

A 500-word article body inside a JSON string is escaped — newlines become `\n`, quotes become `\"` —
so the format is readable by a program and unpleasant for a person, which is the wrong half of
NFR-004 to optimise. It also diffs badly, so a test fixture that changes shows as one enormous line.

### C. One Markdown file per article, with YAML front matter

```markdown
---
title: Registering with FRRO
summary: What to bring, where to go, and what happens if you miss the 14-day window.
tags: [visa, admin]
---

Every exchange student must register with the [[Foreigner Regional Registration Office]]…
```

The body is stored exactly as it was written, because the storage format *is* the authoring format —
no escaping, no transformation. Anyone with a text editor reads it; anyone with a static site
generator republishes the whole knowledge base without this application, which is NFR-004 satisfied
rather than argued.

Costs precision. YAML has to be written and parsed carefully — a title containing a colon needs
quoting — and a relational structure has to be flattened into per-file front matter, which means
round-tripping is a mapping rather than a copy.

## Decision

**C** — one Markdown file per article, YAML front matter for the metadata, one directory for the
set. The same format serves export, import, seed content and fixtures.

The deciding factor is NFR-004's phrase "openable and readable without running the application",
read as a person opening a file rather than a program parsing one. Option A fails it outright and
option B satisfies it only in the machine sense.

The second reason is that one mechanism serving four purposes is one mechanism that gets exercised
constantly: the seed content is imported on every fresh stand, so the import path is tested by
ordinary use rather than by a test someone remembered to write.

**What front matter carries** follows the ERD, not this ADR — the field list belongs in
[data-model.md](../data-model.md) so that it cannot drift from the schema it mirrors. What is fixed
here is that metadata is YAML front matter in the same file as the body, not a sidecar.

**Media** cannot live in a Markdown file. Assets travel alongside the articles in the same archive,
referenced from front matter by their stored name, so an export is a directory of Markdown plus a
directory of files rather than Markdown alone.

## Consequences

**Good:** one format for export, import, seed and fixtures; the stakeholder's lock-in concern is
answered by a directory they can read; article drafts can be written before any application code
exists, because the format is just files; a changed fixture diffs line by line.

**Bad:** the export is a mapping and not a copy, so anything relational that does not fit per-article
front matter has nowhere obvious to go — a tag with no articles (which [ADR-0005](ADR-0005-taxonomy.md)
makes representable) does not survive an export/import round trip, and rejected submissions and
retained revisions are deliberately not exported at all, because the export is the knowledge base,
not the database. YAML parsing is a real source of edge cases; a title with a colon or a leading
`@` is a bug waiting to be written.

**Reversal:** cheap to add option B alongside — a JSON export is a second serialiser over the same
data and does not replace this one. Replacing this format after seed content exists means rewriting
every seed file, so the front-matter field list is worth getting right in the ERD before the ten
article drafts are written against it.
