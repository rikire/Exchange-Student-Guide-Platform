package in.ac.iitm.guide.media;

import java.util.UUID;

/**
 * A document attached to a published article, with the article it belongs to, so a list of documents
 * can lead back to where each is explained.
 */
// trace:FR-033
public record ArticleDocument(UUID articleId, MediaItem document) {}
