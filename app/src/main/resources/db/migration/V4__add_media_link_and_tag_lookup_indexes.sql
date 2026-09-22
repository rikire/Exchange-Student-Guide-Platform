-- Four indexes for reads that had no supporting one, found while reviewing the ERD: each of these
-- columns is filtered or joined on but was not the leading column of any existing index or PK, so
-- each read scanned the whole table. Same reasoning as article_pinned_at_idx (V1).

-- An article's page (FR-001) and a submission's review screen (FR-015) both list their attached
-- media assets.
-- trace: FR-001
CREATE INDEX media_asset_article_id_idx ON media_asset (article_id);
-- trace: FR-015
CREATE INDEX media_asset_submission_id_idx ON media_asset (submission_id);

-- FR-006's backlinks read "which rows link TO this article", by target_article_id — the column not
-- covered by article_link's PK (source_article_id, target_title).
-- trace: FR-006
CREATE INDEX article_link_target_article_id_idx ON article_link (target_article_id);

-- FR-008's tag browse reads "which articles carry this tag", by tag_id — the column not covered by
-- article_tag's PK (article_id, tag_id).
-- trace: FR-008
CREATE INDEX article_tag_tag_id_idx ON article_tag (tag_id);
