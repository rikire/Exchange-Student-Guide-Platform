-- Two indexes for the hottest reads in the moderator's workflow, following the same reasoning as
-- article_pinned_at_idx (V1): a plain index, not one filtered on "IS NOT NULL" / "= 'PENDING'",
-- because H2 has no expression/filtered indexes (confirmed against H2 2.3.232, V1).

-- FR-014's moderation queue: WHERE status = 'PENDING' ORDER BY submitted_at.
-- trace: FR-014
CREATE INDEX submission_status_submitted_at_idx ON submission (status, submitted_at);

-- FR-021/FR-022's report inbox: WHERE closed_at IS NULL.
-- trace: FR-021
CREATE INDEX report_closed_at_idx ON report (closed_at);
