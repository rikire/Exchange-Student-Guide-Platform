package in.ac.iitm.guide.backlink.persistence;

import in.ac.iitm.guide.shared.persistence.Article;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** What re-reading an article's links needs of the published table. */
// trace:FR-006
public interface LinkArticleRepository extends Repository<Article, UUID> {

    Optional<Article> findById(UUID id);

    /** Removed articles included: a link names an address, whatever stands there now. */
    List<Article> findBySlugIn(Collection<String> slugs);

    /** Every article, for the one-off fill at start-up; not for a page. */
    @Query("select a.id from Article a")
    List<UUID> findAllIds();
}
