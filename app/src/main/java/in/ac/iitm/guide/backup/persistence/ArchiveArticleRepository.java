package in.ac.iitm.guide.backup.persistence;

import in.ac.iitm.guide.shared.persistence.Article;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.Repository;

/**
 * What the archive needs of the article table: find one by its address, write one, and read the
 * published ones a page at a time (ADR-0010). The tags load lazily, in batches, inside the caller's
 * transaction — a collection join fetch would make Hibernate apply the page limit in memory.
 */
// trace:NFR-004
public interface ArchiveArticleRepository extends Repository<Article, UUID> {

    Optional<Article> findBySlug(String slug);

    Article save(Article article);

    Slice<Article> findByRemovedAtIsNull(Pageable page);
}
