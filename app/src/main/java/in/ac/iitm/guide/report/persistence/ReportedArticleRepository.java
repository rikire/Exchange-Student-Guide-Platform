package in.ac.iitm.guide.report.persistence;

import in.ac.iitm.guide.shared.persistence.Article;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** The articles a report names: the one being reported, and the inbox's titles in one query. */
// trace:FR-021
public interface ReportedArticleRepository extends Repository<Article, UUID> {

    Optional<Article> findBySlugAndRemovedAtIsNull(String slug);

    List<Article> findByIdIn(Collection<UUID> ids);
}
