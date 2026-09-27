package in.ac.iitm.guide.home.internal;

import in.ac.iitm.guide.taxonomy.TagLink;
import java.util.List;

/** What the landing template shows. */
// trace:FR-009
public record LandingPage(List<Card> pinned, List<Card> recent, List<TagLink> tags) {

    /** One article in a list: enough to choose it, never its body. */
    public record Card(String title, String summary, String path, List<TagLink> tags) {}
}
