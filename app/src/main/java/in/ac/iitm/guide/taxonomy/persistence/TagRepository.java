package in.ac.iitm.guide.taxonomy.persistence;

import in.ac.iitm.guide.shared.persistence.Tag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** The tags of one request in a single query, a new tag, the names in use, and a visit to a tag's page. */
// trace:FR-008
// trace:FR-031
public interface TagRepository extends Repository<Tag, UUID> {

    List<Tag> findByNameIn(Collection<String> names);

    Tag save(Tag tag);

    /** Fix 3.6: the name of every tag a live article carries, A–Z; bounded by the caller (ADR-0010). */
    @Query("select distinct t.name from Article a join a.tags t where a.removedAt is null order by t.name")
    List<String> findNamesInUse(Pageable page);

    /**
     * One visit to every tag behind a page's address (ADR-0017). An increment in the database, not a
     * read and a write, so two readers opening the page at once both count.
     */
    @Modifying
    @Query("update Tag t set t.visitCount = t.visitCount + 1 where t.name in :names")
    int countVisit(Collection<String> names);
}
