package in.ac.iitm.guide.taxonomy.internal;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * What the tag page shows. {@code names} holds every tag with the page's address, usually one.
 *
 * @param total how many published articles carry them, of which {@code entries} are the first
 */
public record TagPage(List<String> names, long total, List<Entry> entries) {

    /**
     * One article: enough to choose it, never its body.
     *
     * @param updated {@code updatedAt} already formatted, the fallback a browser without scripts shows
     */
    public record Entry(String title, String summary, String path, OffsetDateTime updatedAt, String updated) {}
}
