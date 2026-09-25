package in.ac.iitm.guide.backup.persistence;

import in.ac.iitm.guide.shared.persistence.Tag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** What the archive needs of the tag table: the tags of one file in a single query, and a new tag. */
// trace:NFR-004
public interface ArchiveTagRepository extends Repository<Tag, UUID> {

    List<Tag> findByNameIn(Collection<String> names);

    Tag save(Tag tag);
}
