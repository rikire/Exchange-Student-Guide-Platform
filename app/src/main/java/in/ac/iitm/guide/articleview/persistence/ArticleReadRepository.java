package in.ac.iitm.guide.articleview.persistence;

import in.ac.iitm.guide.shared.persistence.Article;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * What the reader needs and nothing more (docs/ai/architecture-rules.md, "Why there are no shared
 * repositories"). Extends the marker {@link Repository}, not {@code JpaRepository}, so no write
 * method is reachable from this slice.
 */
// trace:FR-001
public interface ArticleReadRepository extends Repository<Article, UUID> {

    /** The tags come with the article: the page shows them and the session is closed by then. */
    @EntityGraph(attributePaths = "tags")
    Optional<Article> findBySlugAndRemovedAtIsNull(String slug);

    /** One query for every address a page links to, however many links it holds. */
    @Query("select a.slug from Article a where a.slug in :slugs and a.removedAt is null")
    Set<String> findLiveSlugs(Collection<String> slugs);
}
