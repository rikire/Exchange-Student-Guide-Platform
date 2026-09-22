package in.ac.iitm.guide.shared.persistence;

import java.io.Serializable;
import java.util.UUID;

/**
 * {@link ArticleLink}'s composite key. The ERD draws no surrogate {@code id} for this table — a link
 * is identified by where it comes from and what it names, not by a row number
 * (docs/diagrams/src/erd.puml).
 */
public record ArticleLinkId(UUID sourceArticleId, String targetTitle) implements Serializable {}
