-- FR-009's landing page lists the most recently added articles: "pinned_at IS NULL AND removed_at IS
-- NULL ORDER BY published_at DESC", twelve rows. Without an index the database sorts every article to
-- return twelve; with one it reads the newest first and stops. The second half of the query
-- article_pinned_at_idx (V1) serves. A plain index, not a partial one, for the same reason as there:
-- H2 has no filtered indexes (checked against H2 2.3.232, V1).
--
-- removed_at is deliberately not indexed: nearly every row has it NULL, so an index on it would not
-- narrow any read. This is added before the schema freezes, so it needs no ADR of its own.
-- trace: FR-009
CREATE INDEX article_published_at_idx ON article (published_at);
