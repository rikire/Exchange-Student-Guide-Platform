-- FR-034 counts each opening of an article's page that answers 200, except the moderator's, as an
-- atomic UPDATE, so two readers at once both count. FR-025 orders the pinned section by the place the
-- moderator sets; the articles already pinned keep their order, newest pin first. Both travel in the
-- export (ADR-0021).
-- adr: ADR-0021
-- trace: FR-025, FR-034
ALTER TABLE article ADD COLUMN view_count BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE article ADD COLUMN pin_position INT;
UPDATE article a SET pin_position =
    (SELECT COUNT(*) FROM article b WHERE b.pinned_at IS NOT NULL AND b.pinned_at >= a.pinned_at)
    WHERE a.pinned_at IS NOT NULL;
