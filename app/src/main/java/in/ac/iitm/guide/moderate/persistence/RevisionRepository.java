package in.ac.iitm.guide.moderate.persistence;

import in.ac.iitm.guide.shared.persistence.Revision;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Retained copies of an article's text before an approved edit (FR-020). Nothing here reads them yet. */
// trace:FR-020
public interface RevisionRepository extends Repository<Revision, UUID> {

    Revision save(Revision revision);
}
