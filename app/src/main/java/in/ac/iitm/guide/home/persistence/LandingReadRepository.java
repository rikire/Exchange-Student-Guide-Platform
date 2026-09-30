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
// trace:FR-031
public interface LandingReadRepository extends Repository<Article, UUID> {

    List<Article> findByPinnedAtIsNotNullAndRemovedAtIsNullOrderByPinnedAtDesc(Pageable page);

    /** Not pinned: a pinned article is shown in the pinned section and not repeated here. */
    List<Article> findByPinnedAtIsNullAndRemovedAtIsNullOrderByPublishedAtDesc(Pageable page);

    /**
     * FR-031: the tags live articles carry, each with how many carry it, most visited first and by
     * name among equals; one grouped query, so the page's query count does not grow (ADR-0010).
     */
    @Query("select t.name as name, count(distinct a.id) as articles from Article a join a.tags t"
            + " where a.removedAt is null group by t.name, t.visitCount order by t.visitCount desc, t.name")
    List<TagInUse> findTagsInUse(Pageable page);

    /** A tag as the landing page lists it. */
    interface TagInUse {
        String getName();

        long getArticles();
    }
}
