package in.ac.iitm.guide.articleview.persistence;

import in.ac.iitm.guide.shared.persistence.Article;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * FR-033's list: every published article, one page at a time in the order the caller's
 * {@link Pageable} sets (ADR-0010). Removed articles are excluded by the query, not afterwards.
 */
// trace:FR-033
public interface ArticleListRepository extends Repository<Article, UUID> {

    Page<Article> findByRemovedAtIsNull(Pageable page);

    /** The articles carrying any of these tag names: the names one tag address stands for. */
    @Query(
            value = "select distinct a from Article a join a.tags t where a.removedAt is null and t.name in :names",
            countQuery =
                    "select count(distinct a) from Article a join a.tags t where a.removedAt is null and t.name in :names")
    Page<Article> findPublishedCarrying(Collection<String> names, Pageable page);

    /** The names of the tags published articles carry, for the filter. */
    @Query("select distinct t.name from Article a join a.tags t where a.removedAt is null order by t.name")
    List<String> findTagNamesInUse();
}
