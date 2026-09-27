package in.ac.iitm.guide.contribute.persistence;

import in.ac.iitm.guide.shared.persistence.Article;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

/** What a contributor's form needs of the published table: a title check and an article to edit. */
// trace:FR-010
// trace:FR-011
public interface ContributeArticleRepository extends Repository<Article, UUID> {

    /** Removed articles included: the unique {@code slug} and {@code title} hold across them too. */
    Optional<Article> findBySlug(String slug);

    /** The tags come with the article: the edit form is filled in from them. */
    @EntityGraph(attributePaths = "tags")
    Optional<Article> findWithTagsBySlugAndRemovedAtIsNull(String slug);
}
