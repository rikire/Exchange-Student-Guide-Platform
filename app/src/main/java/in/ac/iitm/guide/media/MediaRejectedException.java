package in.ac.iitm.guide.media;

/**
 * An upload that is not stored: not of an accepted type, or over its limit. The message says which,
 * in words a contributor can act on, so a caller shows it as it is.
 */
// trace:FR-010
// trace:FR-011
// trace:NFR-001
public class MediaRejectedException extends RuntimeException {

    MediaRejectedException(String message) {
        super(message);
    }
}
