package in.ac.iitm.guide.contribute.persistence;

import in.ac.iitm.guide.shared.persistence.Submission;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * A new submission, whether a number was ever issued, and the submission behind a number, whose
 * status (not its content) FR-012 shows to whoever holds the number.
 */
// trace:FR-010
// trace:FR-012
public interface SubmissionRepository extends Repository<Submission, UUID> {

    Submission save(Submission submission);

    boolean existsBySubmissionNumber(String submissionNumber);

    Optional<Submission> findBySubmissionNumber(String submissionNumber);
}
