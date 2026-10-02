package in.ac.iitm.guide.contribute;

import in.ac.iitm.guide.contribute.internal.SubmissionService;
import in.ac.iitm.guide.media.Upload;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Submitting a draft from outside this slice: {@code moderate}'s direct publishing (FR-023, FR-024)
 * puts the moderator's article through the same checks a contributor's goes through, then approves
 * it. Nothing here publishes; the submission it creates is pending until approved (ADR-0003).
 */
// trace:FR-023
// trace:FR-024
@Component
public class Submissions {

    private final SubmissionService submissions;

    Submissions(SubmissionService submissions) {
        this.submissions = submissions;
    }

    /**
     * @return the submission number
     * @throws SubmissionRejectedException if the draft is incomplete, its title is taken or the
     *     attachment is refused
     */
    public String submitNewArticle(Draft draft, Optional<Upload> attachment) {
        return submissions.submitNewArticle(draft, attachment);
    }

    /**
     * @param address the address of the article being edited, as it appears in the path
     * @return the submission number
     * @throws SubmissionRejectedException if the draft is incomplete, its new title belongs to a
     *     different article or the attachment is refused
     */
    public String submitEdit(String address, Draft draft, Optional<Upload> attachment) {
        return submissions.submitEdit(address, null, draft, attachment);
    }

    /** @return the published article at that address as a draft to edit; a {@code 404} when there is none */
    public Draft draftOf(String address) {
        return submissions.editing(address).draft();
    }
}
