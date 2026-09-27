package in.ac.iitm.guide.moderate.internal;

/** What the moderator typed cannot be published: an empty summary, a tag too long to store. */
public class ApprovalRefusedException extends RuntimeException {

    public ApprovalRefusedException(String message) {
        super(message);
    }
}
