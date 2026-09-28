package in.ac.iitm.guide.moderate.internal;

/** What the moderator typed cannot be stored with a rejection: a reason over the limit. */
public class RejectionRefusedException extends RuntimeException {

    public RejectionRefusedException(String message) {
        super(message);
    }
}
