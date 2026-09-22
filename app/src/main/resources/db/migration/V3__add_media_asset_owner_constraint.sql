-- A media asset belongs to the submission that uploaded it until approval moves it to the
-- published article — never both, never neither (data-model.md, ADR-0006). GET /media/{id} decides
-- whether a moderator session is required by which of these two columns is set, so a row where both
-- or neither is set is not an unused case, it is a broken one.
-- trace: FR-001
ALTER TABLE media_asset ADD CONSTRAINT media_asset_owner_xor
  CHECK ((article_id IS NULL) <> (submission_id IS NULL));
