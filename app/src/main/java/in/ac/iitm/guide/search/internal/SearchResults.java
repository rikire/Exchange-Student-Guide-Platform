package in.ac.iitm.guide.search.internal;

import in.ac.iitm.guide.taxonomy.TagLink;
import java.util.List;

/**
 * What the results page shows: the query as typed, how many matched, and the first of them. A hit's
 * title and passage carry the query's words marked; a hit matched only in its title or its tags has
 * no passage, and shows its summary instead.
 */
public record SearchResults(String query, long total, List<Hit> hits) {

    public record Hit(Passage title, String summary, Passage passage, String path, List<TagLink> tags) {}
}
