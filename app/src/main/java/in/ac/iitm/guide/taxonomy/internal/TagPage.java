package in.ac.iitm.guide.taxonomy.internal;

import in.ac.iitm.guide.taxonomy.TagLink;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * What the tag page shows. {@code names} holds every tag with the page's address, usually one.
 *
 * @param total how many published articles carry them, of which {@code entries} are the first
 */
public record TagPage(List<String> names, long total, List<Entry> entries) {

    /**
     * One article: enough to choose it, never its body. {@code tags} are all it carries, the page's
     * own among them, so its card reads as it does on the landing page (fix 3.3).
     *
     * @param updated {@code updatedAt} already formatted, the fallback a browser without scripts shows
     */
    public record Entry(
            String title, String summary, String path, List<TagLink> tags, OffsetDateTime updatedAt, String updated) {}
}
