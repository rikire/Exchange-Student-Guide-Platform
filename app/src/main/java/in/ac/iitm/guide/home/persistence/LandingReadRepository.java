package in.ac.iitm.guide.home.persistence;

import in.ac.iitm.guide.shared.persistence.Article;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * What the landing page reads, every list bounded by the caller's {@link Pageable}
 * (ADR-0010). Removed articles are excluded by every query, not filtered afterwards.
 */
// trace:FR-009
public interface LandingReadRepository extends Repository<Article, UUID> {

    List<Article> findByPinnedAtIsNotNullAndRemovedAtIsNullOrderByPinnedAtDesc(Pageable page);

    /** Not pinned: a pinned article is shown in the pinned section and not repeated here. */
    List<Article> findByPinnedAtIsNullAndRemovedAtIsNullOrderByPublishedAtDesc(Pageable page);

    @Query("select distinct t.name from Article a join a.tags t where a.removedAt is null order by t.name")
    List<String> findTagNamesInUse(Pageable page);
}
