package in.ac.iitm.guide.moderate.internal;

import in.ac.iitm.guide.contribute.Draft;
import in.ac.iitm.guide.contribute.Submissions;
import in.ac.iitm.guide.media.Upload;
import in.ac.iitm.guide.moderate.internal.ModerationService.Published;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The moderator writes or edits an article and it is live at once (FR-023, FR-024): a submission is
 * made through {@code contribute}, so the same checks refuse the same drafts, and approved in the same
 * transaction, so it is never pending in the queue and a refusal leaves nothing behind.
 */
// trace:FR-023
// trace:FR-024
@Service
public class DirectPublishing {

    private final Submissions submissions;
    private final ModerationService moderation;

    DirectPublishing(Submissions submissions, ModerationService moderation) {
        this.submissions = submissions;
        this.moderation = moderation;
    }

    /** @throws in.ac.iitm.guide.contribute.SubmissionRejectedException if the draft cannot be published */
    @Transactional
    public Published publishNew(Draft draft, Optional<Upload> attachment) {
        var number = submissions.submitNewArticle(draft, attachment);
        return moderation.approveAsSubmitted(number, draft.summary(), draft.tags());
    }

    /** @throws in.ac.iitm.guide.contribute.SubmissionRejectedException if the draft cannot be published */
    @Transactional
    public Published publishEdit(String address, Draft draft, Optional<Upload> attachment) {
        var number = submissions.submitEdit(address, draft, attachment);
        return moderation.approveAsSubmitted(number, draft.summary(), draft.tags());
    }

    /** @return the article at that address as the form shows it */
    public Draft draftOf(String address) {
        return submissions.draftOf(address);
    }
}
