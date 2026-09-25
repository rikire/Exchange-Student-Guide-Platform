-- An article's address (ui-routes.md, "Article identity in a URL"). Until now the route contract
-- derived it from the title and stored nothing, which cannot be looked up: a slug does not turn back
-- into a title, so finding the article meant computing the slug of every title — the unbounded read
-- ADR-0010 forbids. Stored here instead, decided by the human on 25 September (phase 2, open
-- question 9), before the schema freezes at step 11, so this migration needs no ADR of its own.
--
-- UNIQUE because two titles that differ only in punctuation ("Fees & Payments", "Fees Payments")
-- yield one slug, and FR-010's case-insensitive title check does not see that. The constraint's
-- index is also what makes the lookup fast.
--
-- The column is NOT NULL with no default: a slug is computed from the title in code
-- (wikilink's ArticleAddress), not in SQL, so there is nothing to backfill with. This is safe only
-- while article is empty, which holds everywhere at this point: nothing writes an article until the
-- importer (phase 2 step 5) and the contribute/moderate slices (phase 3) exist. VARCHAR(512), twice
-- the title's 255: lower-casing a few characters produces two.
-- trace: FR-001
ALTER TABLE article ADD COLUMN slug VARCHAR(512) NOT NULL;
ALTER TABLE article ADD CONSTRAINT article_slug_key UNIQUE (slug);
