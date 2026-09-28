package in.ac.iitm.guide.backlink;

import java.util.UUID;

/**
 * Published by a slice right after it saves an article's title or body — publishing a new article,
 * approving an edit, importing (ADR-0016). {@code backlink} re-reads the article's links in the same
 * transaction, so the links commit with the text or not at all.
 */
public record ArticleTextChanged(UUID articleId) {}
