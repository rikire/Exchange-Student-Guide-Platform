# ADR-0005 — Free-form tags as their own table, normalised on the way in

**Status:** accepted
**Decided:** 10 September — not recorded anywhere before this
**Recorded:** 10 September — see [README.md](README.md)

## Context

An article carries several tags (FR-008, and the `Tag` entry in
[glossary.md](../../requirements/glossary.md)). Contributors *suggest* tags on submission (FR-010,
FR-011) and the moderator adjusts or finalises them before publishing (FR-017) — so tags arrive from
anonymous strangers and are curated by one office, rather than being chosen from a list either side
agreed in advance.

Two requirements constrain the shape. FR-008 selects the articles carrying a tag, so a tag has to be
addressable on its own rather than found by scanning text. FR-009 shows "a list of tags" on the
landing page, which means the set of tags in use is itself something the system has to be able to
produce.

The corpus is small — 10 articles at the design document, 20–30 by the mid-demo, 100 in the
performance fixture. Nothing here is a scale problem; it is a modelling problem.

## Options

### A. A delimited string column on `article`

`tags = "visa,admin,frro"`. No join, no second table, and it reads fine in an export.

Selecting by tag becomes a `LIKE '%visa%'`, which matches `visa` inside `visa-extension` and cannot
be indexed usefully. Producing the landing page's tag list means reading every article and splitting
in application code. Renaming a tag — which a curating moderator will want, because anonymous
contributors will submit `sim card`, `sim-card` and `SIM` for one idea — is a scan and rewrite of
every row.

It also has no place to put a tag that exists but currently has no articles, which is a state that
occurs the moment a moderator removes the last article carrying one.

### B. A `tag` table and an `article_tag` join

Tags are rows. FR-008 is an indexed join, FR-009's list is `SELECT * FROM tag`, and a rename is one
`UPDATE`. Free-form is preserved — a moderator can create any tag at approval time; nothing
constrains the vocabulary.

Costs a second table and a join table, and needs a normalisation rule so that `SIM`, `sim` and ` sim`
do not become three rows.

### C. A fixed controlled vocabulary

A closed list agreed with OGE, from the stakeholder acknowledgement's four categories
(administrative procedures, campus facilities, essential services, practical life). Contributors
pick, never type.

It is genuinely attractive for consistency, and it is wrong for this product. The whole premise
([README.md](../../../README.md), and the revised problem statement in the proposal) is that the
students who have just solved a problem describe its current state — and they routinely need a word
the fixed list does not have. It also converts every new topic into a request to OGE, which is the
bottleneck this project exists to widen. The four categories remain useful as *navigation*, but that
is a different feature from tagging.

## Decision

**B** — a `tag` table with an `article_tag` join, tags free-form, normalised on the way in.

The deciding factor is FR-017: the moderator finalises tags. A curation step is only worth having if
the thing being curated can actually be corrected — merged, renamed, listed — and option A makes
each of those a full scan while option C removes the need for curation by removing the freedom that
makes the product work.

**Normalisation rule:** a tag is stored trimmed and lower-cased, and matched on that form, so
`SIM Card`, `sim card` and ` sim card ` are one tag. The display form is the stored form; this
project does not keep a separate label, because a second field invites two tags that differ only in
capitalisation, which is the problem being solved.

## Consequences

**Good:** FR-008 is an indexed join rather than a substring match; FR-009's tag list is a table
scan of a small table rather than a pass over every article; a moderator can rename or merge a tag
without touching articles; a tag with no articles is representable, which is what happens after the
last article carrying it is removed (FR-026).

**Bad:** two extra tables, and every write path that touches tags has to go through the
normalisation rule — a tag inserted without it creates a duplicate that looks identical on screen.
The `taxonomy` slice therefore owns tag creation, and other slices ask it rather than inserting
directly. Lower-casing is wrong for a proper noun (`FRRO` displays as `frro`), which is a real
cosmetic loss accepted here in exchange for not having a second display column.

**Reversal:** collapsing to option A is a data migration and a rewrite of `taxonomy`; moving to
option C is a policy change that would need the stakeholder, not just us. Neither is expected. The
lower-casing decision is the part most likely to be revisited — if the display of `FRRO` matters
enough, a display-label column is the amendment, and it comes with the duplicate-tag risk named
above.
