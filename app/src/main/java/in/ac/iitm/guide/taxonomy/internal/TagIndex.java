package in.ac.iitm.guide.taxonomy.internal;

import in.ac.iitm.guide.taxonomy.TagLink;
import java.util.List;

/**
 * What {@code GET /tags} shows: the tags of live articles by name, each with its article count.
 *
 * @param more whether there are tags past the ones listed
 */
public record TagIndex(List<Entry> entries, boolean more) {

    public record Entry(TagLink tag, long articles) {}
}
