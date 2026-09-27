package in.ac.iitm.guide.moderate.persistence;

import in.ac.iitm.guide.shared.persistence.Article;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** What publishing needs of the published table: a free address, the article an edit is for. */
// trace:FR-017
// trace:FR-020
public interface ModerateArticleRepository extends Repository<Article, UUID> {

    Article save(Article article);

    /** Removed articles included: the unique {@code slug} holds across them too. */
    Optional<Article> findBySlug(String slug);

    /**
     * Locked for the approval of an edit, so two approved edits of one article are applied one after
     * the other and each retains, as its revision, the text the other left (FR-020).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "tags")
    Optional<Article> findWithTagsByIdAndRemovedAtIsNull(UUID id);

    /** The review page resolves wiki links as the article page will: one query for all of them. */
    @Query("select a.slug from Article a where a.slug in :slugs and a.removedAt is null")
    Set<String> findLiveSlugs(Collection<String> slugs);
}
