# ADR-0006 — Media on the filesystem, metadata in the database, delivery through a controller

**Status:** accepted
**Decided:** the upload rules predate this and live in `docs/ai/security.md`, undated; the storage location 10 September
**Recorded:** 10 September, after the fact — see [README.md](README.md)

## Context

Anonymous visitors upload files that other visitors then download (FR-010, FR-011, FR-016). That is
the largest attack surface in a project that otherwise has almost none, and
[security.md](../../ai/security.md) already states the rules that follow from it — type decided from
content rather than the client, an allowlist, a system-generated stored filename, delivery through a
controller, `nosniff`, images re-encoded, media unreachable until its submission is approved.

**This ADR does not re-decide any of that.** What it decides is where the bytes live, which
security.md deliberately leaves open and which NFR-001 constrains: 10 MB per image, 20 MB per
document, 200 MB per video, and **20 GB for the whole media volume**.

The 200 MB video figure is what makes this a decision rather than a default. A knowledge base of
text would not need one.

## Options

### A. Bytes in the database, as a BLOB column

One store. A backup of the database is a backup of everything, transactions cover the file and its
metadata together, and there is no orphaned-file problem — deleting the row deletes the bytes.

It collapses at 200 MB per video and 20 GB in total. Streaming a large BLOB out of JPA means either
loading it into memory — which security.md forbids for exactly this reason — or dropping to a
streaming API that varies between H2 and PostgreSQL, reintroducing the dev/prod split that
[ADR-0004](ADR-0004-search-and-multilingual-content.md) rejected. `Range` support for video, which
security.md requires, is awkward on top of it. Database backups grow to include 20 GB of video that
never changes.

### B. Bytes on the filesystem, metadata rows in the database

The file goes to a media root under a system-generated name; the row holds the id, original
filename, detected content type, size and which article or submission owns it. Streaming and `Range`
support are what a servlet container already does well. Backups can treat 20 GB of immutable files
differently from a small, frequently-changing database.

Costs consistency: two stores, so a crash between the write and the commit leaves an orphaned file,
and a restore can leave a row pointing at a file that is not there. Neither loses published content,
but both need a named answer rather than a shrug.

### C. Object storage (S3 or compatible)

What this would be if it were a hosted product — durability, offloaded delivery, no local disk to
size.

It adds an external service, credentials to manage, and a network dependency to the one operation
most likely to be slow. The deployment target is a single Docker Compose stand for one institute
office; there is no bucket, and standing one up is infrastructure this project would then have to
document and hand over ([docs/handoff/](../../handoff/)). Disproportionate.

## Decision

**B** — bytes on the filesystem under a media root, metadata in the database.

The deciding factor is the 200 MB video against security.md's rule that video is streamed with
`Range` support and never loaded into memory. Option A can be made to do that only by leaning on
engine-specific streaming, which splits dev from prod.

**On the two-store consistency problem**, named rather than left implicit:

- The file is written **before** the row is committed, and a file with no row is unreachable — the
  controller resolves an id from the database, never a path from a URL. An orphan is wasted disk,
  not exposure.
- The media root is part of the backup set alongside the database dump
  ([docs/handoff/](../../handoff/) covers this), because a database restored without it produces
  rows pointing at nothing.
- Deletion (FR-026) removes the row; reclaiming the file is a separate sweep, so that a failed
  delete never leaves a live row pointing at a removed file.

**On type detection:** security.md names Apache Tika for reading magic bytes. That is a dependency
and therefore a decision with evidence attached — the artefact, version, maintenance status and
licence get checked before it enters a pom. No version is named here, because a version recalled
rather than checked is indistinguishable from one that exists right up until the build fails.

## Consequences

**Good:** streaming and `Range` are the container's job rather than ours; the database stays small
enough to dump quickly; the 20 GB volume limit (NFR-001) is a directory quota, which is checkable
without querying anything.

**Bad:** two stores that can disagree, with the three answers above as the price; the media root
becomes a deployment concern — a path that has to exist, be writable, and be included in the backup,
which is one more thing the handoff document has to get right. A restore that forgets it produces
broken downloads on published articles, which is the most visible failure this design allows.

**Reversal:** moving to option C later is contained — the `media` slice publishes a small surface and
the storage call sits behind it — and would be the natural move if this were ever hosted for more
than one office. Moving to option A is not expected; it would mean giving up the video limit that
made this decision.
