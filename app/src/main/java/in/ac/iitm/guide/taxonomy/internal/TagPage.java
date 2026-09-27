package in.ac.iitm.guide.taxonomy.internal;

import java.util.List;

/**
 * What the tag page shows. {@code names} holds every tag with the page's address, usually one.
 *
 * @param total how many published articles carry them, of which {@code entries} are the first
 */
public record TagPage(List<String> names, long total, List<Entry> entries) {

    /** One article: enough to choose it, never its body. {@code updated} is already formatted. */
    public record Entry(String title, String summary, String path, String updated) {}
}
