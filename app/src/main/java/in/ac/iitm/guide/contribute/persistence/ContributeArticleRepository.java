package in.ac.iitm.guide.contribute.persistence;

import in.ac.iitm.guide.shared.persistence.Article;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * What a contributor's form needs of the published table: a title check, an article to edit, and the
 * live addresses a preview links to.
 */
// trace:FR-010
// trace:FR-011
// trace:FR-027
public interface ContributeArticleRepository extends Repository<Article, UUID> {

    /** Removed articles included: the unique {@code slug} and {@code title} hold across them too. */
    Optional<Article> findBySlug(String slug);

    /** The tags come with the article: the edit form is filled in from them. */
    @EntityGraph(attributePaths = "tags")
    Optional<Article> findWithTagsBySlugAndRemovedAtIsNull(String slug);

    /** One query for every address a previewed body links to, as the article page asks it. */
    @Query("select a.slug from Article a where a.slug in :slugs and a.removedAt is null")
    Set<String> findLiveSlugs(Collection<String> slugs);
}
