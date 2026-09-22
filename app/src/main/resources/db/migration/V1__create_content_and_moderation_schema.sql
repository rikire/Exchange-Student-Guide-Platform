-- Schema for the eight tables in docs/architecture/data-model.md, as drawn in
-- docs/diagrams/src/erd.puml. Primary keys are UUID everywhere the ERD shows one; article_link has
-- none, by the same ERD, and is keyed by its natural (source, target title) pair instead.
--
-- Case-insensitive uniqueness (article.title, and submission_number's hyphen/case-insensitive
-- lookup, ADR-0011) is not enforced here: H2 has no expression indexes, so it is enforced by the
-- owning slice before it writes (contribute/moderate, phase 3), matching how data-model.md already
-- describes the title collision as "rejected at the point of publishing" rather than as a database
-- constraint. The plain UNIQUE constraints below are a case-sensitive backstop.

-- trace: FR-001
CREATE TABLE article (
    id UUID NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL UNIQUE,
    summary TEXT NOT NULL,
    body TEXT NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    pinned_at TIMESTAMP WITH TIME ZONE,
    removed_at TIMESTAMP WITH TIME ZONE
);

-- FR-009's landing page finds the pinned articles directly instead of scanning every row. A plain
-- index, not a partial one filtered on "IS NOT NULL": H2 has no expression/filtered indexes (checked
-- directly against H2 2.3.232 — CREATE INDEX ... WHERE is a syntax error), and the plain form scales
-- the same way regardless of the article count either way.
-- trace: FR-009
CREATE INDEX article_pinned_at_idx ON article (pinned_at);

-- trace: FR-008
CREATE TABLE tag (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

-- trace: FR-010
CREATE TABLE submission (
    id UUID NOT NULL PRIMARY KEY,
    submission_number VARCHAR(32) NOT NULL UNIQUE,
    type VARCHAR(16) NOT NULL,
    target_article_id UUID REFERENCES article (id),
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    rejection_reason TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE
);

-- trace: FR-020
CREATE TABLE revision (
    id UUID NOT NULL PRIMARY KEY,
    article_id UUID NOT NULL REFERENCES article (id),
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    body TEXT NOT NULL,
    retained_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- trace: FR-008
CREATE TABLE article_tag (
    article_id UUID NOT NULL REFERENCES article (id),
    tag_id UUID NOT NULL REFERENCES tag (id),
    PRIMARY KEY (article_id, tag_id)
);

-- trace: FR-010
CREATE TABLE submission_tag (
    submission_id UUID NOT NULL REFERENCES submission (id),
    tag_id UUID NOT NULL REFERENCES tag (id),
    PRIMARY KEY (submission_id, tag_id)
);

-- trace: FR-001
CREATE TABLE media_asset (
    id UUID NOT NULL PRIMARY KEY,
    article_id UUID REFERENCES article (id),
    submission_id UUID REFERENCES submission (id),
    stored_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- trace: FR-021
CREATE TABLE report (
    id UUID NOT NULL PRIMARY KEY,
    article_id UUID NOT NULL REFERENCES article (id),
    message TEXT NOT NULL,
    reported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE
);

-- trace: FR-004
CREATE TABLE article_link (
    source_article_id UUID NOT NULL REFERENCES article (id),
    target_title VARCHAR(255) NOT NULL,
    target_article_id UUID REFERENCES article (id),
    PRIMARY KEY (source_article_id, target_title)
);
