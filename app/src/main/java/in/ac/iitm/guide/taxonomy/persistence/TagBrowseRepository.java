package in.ac.iitm.guide.taxonomy.persistence;

import in.ac.iitm.guide.shared.persistence.Article;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * What browsing by tag reads. The articles are found by tag name through {@code tag}'s unique name
 * and {@code article_tag_tag_id_idx} (V4), and are published articles only: a submission is not an
 * {@link Article} (data-model.md, "Content"), which is what keeps pending and rejected ones off the
 * page.
 */
// trace:FR-008
public interface TagBrowseRepository extends Repository<Article, UUID> {

    /**
     * The tag table itself, the small table ADR-0005 accepts scanning: a tag's address is computed
     * from its name, so the names with one address are found here and the articles by them below.
     */
    @Query("select t.name from Tag t")
    List<String> findAllTagNames();

    @Query("select distinct a from Article a join a.tags t"
            + " where a.removedAt is null and t.name in :names order by a.updatedAt desc")
    List<Article> findPublishedCarrying(Collection<String> names, Pageable page);

    @Query("select count(distinct a) from Article a join a.tags t where a.removedAt is null and t.name in :names")
    long countPublishedCarrying(Collection<String> names);

    /**
     * Fix 2.1 (F-6): every tag a live article carries, by name, each with how many carry it; one
     * grouped query, bounded by the caller (ADR-0010).
     */
    @Query("select t.name as name, count(distinct a.id) as articles from Article a join a.tags t"
            + " where a.removedAt is null group by t.name order by t.name")
    List<TagInUse> findTagsInUse(Pageable page);

    /** A tag as the tag index lists it. */
    interface TagInUse {
        String getName();

        long getArticles();
    }
}
