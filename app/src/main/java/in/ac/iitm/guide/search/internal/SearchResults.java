package in.ac.iitm.guide.search.internal;

import java.util.List;

/** What the results page shows: the query as typed, how many matched, and the first of them. */
public record SearchResults(String query, long total, List<Hit> hits) {

    public record Hit(String title, String summary, String path, List<String> tags) {}
}
