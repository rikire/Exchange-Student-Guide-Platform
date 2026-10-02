package in.ac.iitm.guide.report.persistence;

import in.ac.iitm.guide.shared.persistence.Report;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** FR-021's reports and FR-022's closing of them. */
// trace:FR-021
// trace:FR-022
public interface ReportRepository extends Repository<Report, UUID> {

    Report save(Report report);

    Optional<Report> findById(UUID id);

    /**
     * The inbox: open reports on articles still published, newest first, one page (ADR-0010). A
     * report on an article removed since stays in the table and out of the inbox.
     */
    @Query("select r from Report r where r.closedAt is null and exists"
            + " (select 1 from Article a where a.id = r.articleId and a.removedAt is null)"
            + " order by r.reportedAt desc")
    List<Report> findOpenOnLiveArticles(Pageable page);
}
