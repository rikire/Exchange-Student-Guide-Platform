package in.ac.iitm.guide.contribute;

import java.util.Optional;

/**
 * A submission the contributor can fix and send again ({@code 422}, ui-routes.md): the message says
 * what to fix. The collision is the address of the article whose title is taken, present only when
 * that article is live, since a link to a removed one would not resolve. Published since 2 Oct for
 * {@code moderate}'s direct publishing (FR-023, FR-024), which refuses the same drafts the same way.
 */
// trace:FR-010
// trace:FR-011
// trace:FR-023
// trace:FR-024
public class SubmissionRejectedException extends RuntimeException {

    private final String collision;
    private final boolean titleTaken;

    public SubmissionRejectedException(String message) {
        this(message, null, false);
    }

    /** @param titleTaken whether another article already has the title, rather than the draft being incomplete */
    public SubmissionRejectedException(String message, String collision, boolean titleTaken) {
        super(message);
        this.collision = collision;
        this.titleTaken = titleTaken;
    }

    /** @return whether another article already has the title; the moderator's form answers {@code 409} for it */
    public boolean titleTaken() {
        return titleTaken;
    }

    /** @return the address of the live article that already has this title, if there is one */
    public Optional<String> collision() {
        return Optional.ofNullable(collision);
    }
}
