package in.ac.iitm.guide.contribute;

import in.ac.iitm.guide.contribute.internal.BodyPreview;
import java.util.List;

/**
 * What a contributor, or the moderator writing directly (FR-023, FR-024), typed into the form. The
 * body is kept exactly as written (FR-003).
 */
public record Draft(String title, String summary, String body, List<String> tags) {

    /** The longest body accepted, for a form's {@code maxlength}; the check itself is the service's. */
    public static final int LONGEST_BODY = BodyPreview.LONGEST_BODY;
}
