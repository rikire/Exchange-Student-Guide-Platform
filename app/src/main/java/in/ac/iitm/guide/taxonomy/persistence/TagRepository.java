package in.ac.iitm.guide.taxonomy.persistence;

import in.ac.iitm.guide.shared.persistence.Tag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** The tags of one request in a single query, and a new tag. */
// trace:FR-008
public interface TagRepository extends Repository<Tag, UUID> {

    List<Tag> findByNameIn(Collection<String> names);

    Tag save(Tag tag);
}
