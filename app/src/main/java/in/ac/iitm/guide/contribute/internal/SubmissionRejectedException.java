package in.ac.iitm.guide.contribute.internal;

import java.util.Optional;

/**
 * A submission the contributor can fix and send again ({@code 422}, ui-routes.md): the message says
 * what to fix. The collision is the address of the article whose title is taken, present only when
 * that article is live, since a link to a removed one would not resolve.
 */
// trace:FR-010
// trace:FR-011
public class SubmissionRejectedException extends RuntimeException {

    private final String collision;

    SubmissionRejectedException(String message) {
        this(message, null);
    }

    SubmissionRejectedException(String message, String collision) {
        super(message);
        this.collision = collision;
    }

    /** @return the address of the live article that already has this title, if there is one */
    public Optional<String> collision() {
        return Optional.ofNullable(collision);
    }
}
