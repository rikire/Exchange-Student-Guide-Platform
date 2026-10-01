package in.ac.iitm.guide.articleview.persistence;

import in.ac.iitm.guide.shared.persistence.Article;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one write the reader's slice makes, kept apart from {@link ArticleReadRepository} so that one
 * stays read only: FR-034's count, as a single atomic update (ADR-0021).
 */
// trace:FR-034
public interface ArticleViewRepository extends Repository<Article, UUID> {

    @Transactional
    @Modifying
    @Query("update Article a set a.viewCount = a.viewCount + 1 where a.id = :id")
    int countView(UUID id);
}
