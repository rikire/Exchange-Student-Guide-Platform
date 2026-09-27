package in.ac.iitm.guide.taxonomy;

/**
 * A tag that cannot be stored: empty after trimming, or longer than the column holds. The message
 * names the tag and the rule it broke, so a caller can show it to the person who typed it.
 */
// trace:FR-008
public class TagRejectedException extends RuntimeException {

    TagRejectedException(String message) {
        super(message);
    }
}
