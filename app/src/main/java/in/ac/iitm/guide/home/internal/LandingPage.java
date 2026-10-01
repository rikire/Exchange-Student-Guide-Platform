package in.ac.iitm.guide.home.internal;

import in.ac.iitm.guide.taxonomy.TagLink;
import java.time.OffsetDateTime;
import java.util.List;

/** What the landing template shows. */
// trace:FR-009
public record LandingPage(List<Card> pinned, List<Card> recent, List<ListedTag> tags) {

    /**
     * One article in a list: enough to choose it, never its body.
     *
     * @param updated {@code updatedAt} already formatted, the fallback a browser without scripts shows
     */
    public record Card(
            String title, String summary, String path, List<TagLink> tags, OffsetDateTime updatedAt, String updated) {}

    /**
     * A tag in the list (FR-031): its name and page as {@link TagLink} has them, and how many live
     * articles carry it. {@code path} is null for a name of only symbols.
     */
    public record ListedTag(String name, String path, long articles) {}
}
