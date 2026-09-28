package in.ac.iitm.guide.backlink.persistence;

import in.ac.iitm.guide.shared.persistence.ArticleLink;
import in.ac.iitm.guide.shared.persistence.ArticleLinkId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * {@code article_link}, which only {@code backlink} reads and writes (ADR-0016). {@code targetTitle}
 * holds the linked title's address, and {@code targetArticleId} the article at that address when there
 * is one.
 */
// trace:FR-006
public interface LinkRepository extends Repository<ArticleLink, ArticleLinkId> {

    ArticleLink save(ArticleLink link);

    long count();

    /** A bulk delete, run at once, so the rows saved after it in the same transaction cannot clash. */
    @Modifying(flushAutomatically = true)
    @Query("delete from ArticleLink l where l.sourceArticleId = :source")
    void deleteFromSource(UUID source);

    /** Points every link naming this address at the article now there — a new article, or a rename. */
    @Modifying(flushAutomatically = true)
    @Query("update ArticleLink l set l.targetArticleId = :article where l.targetTitle = :address")
    void pointAt(UUID article, String address);

    /** Unhooks the links that named this article's old address, after a rename. */
    @Modifying(flushAutomatically = true)
    @Query("update ArticleLink l set l.targetArticleId = null"
            + " where l.targetArticleId = :article and l.targetTitle <> :address")
    void unpointOthers(UUID article, String address);

    /** The live articles linking to one, on {@code article_link_target_article_id_idx}. */
    @Query("select a.title as title, a.slug as slug from ArticleLink l join Article a on a.id = l.sourceArticleId"
            + " where l.targetArticleId = :article and a.removedAt is null order by a.title")
    List<Source> findLiveSources(UUID article, Pageable page);

    interface Source {
        String getTitle();

        String getSlug();
    }
}
