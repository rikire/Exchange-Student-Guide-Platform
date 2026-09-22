package in.ac.iitm.guide.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * One row per {@code [[link]]} found on publish (FR-004's red links, FR-006's backlinks), extracted
 * rather than derived at read time because scanning every article on every view is exactly the
 * unbounded read ADR-0010 forbids (ADR-0012).
 *
 * <p>Keyed by the target's title, not a required foreign key to {@code article}: a red link points at
 * a title with no row yet. {@code targetArticleId} is filled in only once that title resolves.
 */
// trace:FR-004
@Entity
@Table(name = "article_link")
@IdClass(ArticleLinkId.class)
public class ArticleLink {

    @Id
    @Column(name = "source_article_id")
    private UUID sourceArticleId;

    @Id
    @Column(name = "target_title")
    private String targetTitle;

    @Column(name = "target_article_id")
    private UUID targetArticleId;

    public UUID getSourceArticleId() {
        return sourceArticleId;
    }

    public void setSourceArticleId(UUID sourceArticleId) {
        this.sourceArticleId = sourceArticleId;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }

    public UUID getTargetArticleId() {
        return targetArticleId;
    }

    public void setTargetArticleId(UUID targetArticleId) {
        this.targetArticleId = targetArticleId;
    }
}
