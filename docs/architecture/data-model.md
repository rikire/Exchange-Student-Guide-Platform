# Data model

Tables, relations, and why each exists. Written 10 September, phase 1.

![Entity-relationship diagram](../diagrams/out/erd.svg)

Source: [docs/diagrams/src/erd.puml](../diagrams/src/erd.puml). Refresh the rendering with
`scripts/diagrams.sh`.

Every table below names the ADR that decided it. One does not have one, and that is flagged rather
than smoothed over.

## Content

| Table | Holds | Decided by |
|---|---|---|
| `article` | Published content only | [ADR-0003](adr/ADR-0003-moderation-and-revision-storage.md) |
| `revision` | An article's title, summary and body as they stood before an approved edit | [ADR-0003](adr/ADR-0003-moderation-and-revision-storage.md), and [CON-004](../requirements/constraints.md) — full copies, because there is no diff view |
| `tag`, `article_tag` | Free-form labels, normalised on the way in | [ADR-0005](adr/ADR-0005-taxonomy.md) |
| `article_link` | FR-006's backlinks and FR-004's red links, one row per `[[link]]` found on publish | [ADR-0012](adr/ADR-0012-article-link-storage.md) |

**`article_tag` is indexed on `tag_id`** (`article_tag_tag_id_idx`, added 22 Sep). Its primary key is
`(article_id, tag_id)`, which does not serve FR-008's tag browse — "which articles carry this
tag" — reads by `tag_id` alone; without this index that read scanned every `article_tag` row.

**`article` holds nothing unpublished.** That is the whole point of the split: FR-001, FR-007 and
FR-008 each carry a negative criterion — an unapproved or rejected submission must not resolve,
must not appear in search, must not appear under a tag — and those hold because the rows are absent,
not because ten repositories each remembered a status filter.

**Two timestamps, not one.** `published_at` is set once and never moved; `updated_at` starts equal to
it and moves forward with each approved edit. FR-009's "most recently added" reads the first; the
"Updated 3 days ago" line on the tag-browse screen reads the second. A single timestamp would have
made FR-009 silently mean "recently changed".

**`summary` is stored, not derived.** It is written by the contributor (FR-010, FR-011) and
adjustable by the moderator (FR-017), and it appears wherever articles are listed rather than read.
The ellipsed extract in search results is a different thing — computed around the match at query
time and stored nowhere.

**`title`'s case-insensitive uniqueness is not a database constraint.** The migration
([V1__create_content_and_moderation_schema.sql](../../app/src/main/resources/db/migration/V1__create_content_and_moderation_schema.sql))
carries a plain, case-sensitive `UNIQUE (title)` as a backstop; H2 has no expression indexes to build
a case-insensitive one from (confirmed against H2 2.3.232, phase 2 step 1), so the actual
case-insensitive check stays where open question 3 of
[01-requirements-design.md](../roadmap/01-requirements-design.md) already put it — the `contribute`/
`moderate` slice, at the point of publishing, not the schema.

**`pinned_at` is indexed** (`article_pinned_at_idx`, added 22 Sep) so FR-009's landing page finds the
pinned articles directly rather than scanning every row — a plain index, not one filtered on
`IS NOT NULL`, because H2 has no expression/filtered indexes (checked directly against H2 2.3.232).

**Reconsidered 23 Sep, kept as designed.** A separate `pinned_article(article_id, pinned_at)` table
and a `boolean` column with a partial index (`WHERE pinned`) were weighed against the column-plus-
index design above. Both lose: pinning is a 1:1 attribute of `article`, not a many-to-many relation
like `article_tag`, so a separate table buys no referential-integrity benefit and still needs its
own B-tree index to order by `pinned_at` — the same access pattern, over a smaller row set, which
only matters at an article count this project does not have. The partial index additionally is not
portable to dev: H2 has no partial/expression indexes (previous paragraph), while PostgreSQL (prod,
phase 4) does — worth revisiting only if that dev/prod gap becomes real, not before.

**`pinned_at` and `removed_at` are nullable timestamps, not booleans or a status column.** Decided
10 Sep, resolving the two open decisions below. `pinned_at` is set when a moderator pins an article
(FR-025) and cleared when they unpin it; the landing page's pinned section (FR-009) orders by it, so
two pinned articles never tie. `removed_at` is set when a moderator removes a published article
(FR-026) and never cleared — a soft delete. The row stays: `revision` (FR-020), `report.article_id`
and any `submission.target_article_id` keep resolving, and a wiki link to the removed article keeps
rendering, now as a red link (FR-004) because every read of `article` filters on `removed_at IS
NULL`, the same filter that already excludes unapproved content.

## Moderation

| Table | Holds | Decided by |
|---|---|---|
| `submission`, `submission_tag` | A proposed new article or edit, its status, its number, its rejection reason | [ADR-0003](adr/ADR-0003-moderation-and-revision-storage.md) |

`type` distinguishes a new-article submission from an edit; `target_article_id` is set only for
edits. `status` is the three values FR-012 shows and nothing else.

**`submission` is indexed on `(status, submitted_at)`** (`submission_status_submitted_at_idx`, added
22 Sep), the same reasoning as `pinned_at` above: FR-014's queue reads `WHERE status = 'PENDING'
ORDER BY submitted_at`, on the table the moderator's workflow hits hardest, so it finds the pending
rows directly rather than scanning every submission ever made.

**`submission_number` is a generated token, not a sequence.** Decided 10 Sep,
[ADR-0011](adr/ADR-0011-submission-number-format.md): 60 bits from a cryptographically secure source,
rendered as `SUB-K7M2-QX9P-4TVB`. The column type does not change — it was already `text`, unique —
because this is a decision about how the value is produced, not about what the schema holds. It is
recorded here anyway, because a reader looking only at the ERD would see `text <<unique>>` and
reasonably assume a counter, which is exactly what the stage-3 screens assumed. The property is
measurable as [NFR-006](../requirements/non-functional.md).

Why it matters at the schema level rather than only at the controller: with no account to
authenticate against ([CON-001](../requirements/constraints.md)), holding the number *is* the
authorisation — so the column is a credential, and a sequential value in it would make every
unapproved and rejected submission enumerable through
[the confirmation route](ui-routes.md).

An earlier draft of this diagram carried a `submitter_ip_hash` column for
[NFR-005](../requirements/non-functional.md)'s rate limit. It was removed on review:
[ADR-0008](adr/ADR-0008-abuse-handling-without-accounts.md) deliberately leaves *where the rate
limit is counted* open, and a column here would have decided it — badly, because FR-013 rejects a
submission **before** any row exists, so a counter living on the table being inserted into cannot
serve the rejection path at all.

The cost, named in ADR-0003 and visible in the diagram: the authored fields appear in three tables.
Adding one means adding it in three places, which is exactly what `summary` needed on 10 September.

## Media and reports

| Table | Holds | Decided by |
|---|---|---|
| `media_asset` | Metadata only — the bytes are on the filesystem | [ADR-0006](adr/ADR-0006-media-storage-and-upload-security.md) |
| `report` | A reader's flag on a published article, with the required message | FR-021, FR-022 |

`media_asset` points at either an article or a submission, never both: it belongs to the submission
until approval moves it across. **Enforced as a `CHECK` constraint**
(`media_asset_owner_xor`, added 22 Sep) rather than left as a convention, because `GET /media/{id}`
decides whether a moderator session is required by which of the two columns is set — a row with both
or neither would make that check meaningless. `content_type` is what Tika read from the magic bytes,
not what the client claimed. `stored_name` is generated by the system and `original_name` is
display-only — the rule that closes path traversal.

**`media_asset` is indexed on `article_id` and on `submission_id`** (`media_asset_article_id_idx`,
`media_asset_submission_id_idx`, added 22 Sep): an article's page (FR-001) and a submission's review
screen (FR-015) both list their attached media assets, and neither column was the leading column of
any existing index, so both reads scanned the whole table.

`report` is closed by setting `closed_at`. There is no accept/reject decision on a report itself:
UC-019 was narrowed on 7 September so that correcting an article reuses the existing direct-edit
capability, and closing is the only new action.

**`report` is indexed on `closed_at`** (`report_closed_at_idx`, added 22 Sep), the same reasoning as
`pinned_at`: the moderator's inbox (FR-021, FR-022) reads `WHERE closed_at IS NULL`, so it finds the
open reports directly rather than scanning every report ever filed.

## `article_link`

Decided 21 September, [ADR-0012](adr/ADR-0012-article-link-storage.md): the links are stored, extracted
when an article is published or an edit is approved, keyed by the target's **title** rather than a
required foreign key to `article.id` — a `must`-priority red link (FR-004) points at a title with no
row yet. The alternative, deriving backlinks by scanning bodies at read time, was rejected because it
is exactly the unbounded read [ADR-0010](adr/ADR-0010-bounded-reads.md) already closed off for a
public page. The exact columns are not fixed by the ADR and are `.claude/rules/schema.md`'s call when
the migration is written; open in that ADR is whether a removed article's outbound links are cleared
or left stale.

**`article_link` is indexed on `target_article_id`** (`article_link_target_article_id_idx`, added
22 Sep). The primary key `(source_article_id, target_title)` serves "what does this article link to",
not FR-006's backlinks — "what links here" — which reads by `target_article_id`; without this index
that read scanned every `article_link` row, contrary to the ADR's own claim that backlinks are "a
single indexed lookup, not a scan."

## Open decisions found while reviewing this model

Found while checking every `FR` and every screen against the diagram on 10 September. Entity fields,
cardinality and enum values are the human's call (`.claude/rules/schema.md`); three of the four below
were made that day, one is still open. Entry 4 was found later the same day, by a different method —
reviewing [ui-routes.md](ui-routes.md) against this model rather than this model against the screens.
Worth noting that the first sweep did not catch it: a column whose *type* is right can still have a
decision hiding in it.

**1. ~~A removed article has no representation.~~** Resolved 10 Sep: `removed_at`, a nullable
timestamp on `article` — a soft delete. See the `pinned_at`/`removed_at` paragraph above for what
that keeps intact.

**2. ~~`pinned` is a boolean, so pinned articles have no order.~~** Resolved 10 Sep: replaced by
`pinned_at`, a nullable timestamp — pin order is "most recently pinned first," with no separate
reordering control needed. See the paragraph above.

**3. Where the rate limit is counted** — in-process, or a table of its own.
[ADR-0008](adr/ADR-0008-abuse-handling-without-accounts.md) left it to the implementation, and that
is still where it sits; it is listed here so the absence is visible in the schema rather than only
in the ADR. Still open — not part of this pass.

**4. ~~`submission_number` has a type but no format, and the screens had filled the gap with a
counter.~~** Resolved 10 Sep: a generated 60-bit token —
[ADR-0011](adr/ADR-0011-submission-number-format.md), with
[NFR-006](../requirements/non-functional.md) as the measurable half. See the `submission_number`
paragraph above. No column changes; five sketched screens did.

## Not in this model

No user table — [CON-001](../requirements/constraints.md), and the moderator is a shared password
([ADR-0009](adr/ADR-0009-admin-authentication.md)). No discussion pages (CON-002), no watchlists
(CON-003), no stored diffs (CON-004).
