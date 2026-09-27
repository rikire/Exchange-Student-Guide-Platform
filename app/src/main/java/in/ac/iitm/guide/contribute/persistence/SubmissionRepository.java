package in.ac.iitm.guide.contribute.persistence;

import in.ac.iitm.guide.shared.persistence.Submission;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** A new submission, and whether a number was ever issued. Nothing here reads a submission's content. */
// trace:FR-010
public interface SubmissionRepository extends Repository<Submission, UUID> {

    Submission save(Submission submission);

    boolean existsBySubmissionNumber(String submissionNumber);
}
