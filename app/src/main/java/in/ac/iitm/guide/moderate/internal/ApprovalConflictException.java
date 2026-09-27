package in.ac.iitm.guide.moderate.internal;

/**
 * The published table changed while the submission waited, so it cannot be approved as it is: its
 * title's address was taken by another article. The submission stays pending.
 */
public class ApprovalConflictException extends RuntimeException {

    public ApprovalConflictException(String message) {
        super(message);
    }
}
