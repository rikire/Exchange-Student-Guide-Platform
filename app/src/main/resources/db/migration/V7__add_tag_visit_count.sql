-- FR-031 lists the landing page's tags most visited first: each opening of a tag's page that answers
-- 200 adds one here, as an atomic UPDATE, so two readers at once both count. Not exported: it is not
-- content (ADR-0007). The article count beside a tag is computed on read and stores nothing.
-- adr: ADR-0017
-- trace: FR-031
ALTER TABLE tag ADD COLUMN visit_count BIGINT DEFAULT 0 NOT NULL;
