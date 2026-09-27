package in.ac.iitm.guide.moderate.persistence;

import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

/** What the moderator needs of the queue: the pending list, one submission to read, one to decide. */
// trace:FR-014
// trace:FR-015
// trace:FR-017
// trace:FR-018
public interface ModerateSubmissionRepository extends Repository<Submission, UUID> {

    /**
     * Every pending submission, not a page of them: FR-014 lists "every submission awaiting a
     * decision", and the queue is not a public list (ADR-0010 bounds those). The columns shown need
     * no association, so the cost is one query whatever the length.
     */
    List<Submission> findByStatusOrderBySubmittedAtAsc(SubmissionStatus status);

    @EntityGraph(attributePaths = "tags")
    Optional<Submission> findWithTagsBySubmissionNumber(String submissionNumber);

    /**
     * Locked for the decision, so two moderators deciding the same submission at once are served one
     * after the other and the second sees it already decided.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Submission> findForDecisionBySubmissionNumber(String submissionNumber);
}
