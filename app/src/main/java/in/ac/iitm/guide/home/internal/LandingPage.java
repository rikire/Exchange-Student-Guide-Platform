package in.ac.iitm.guide.home.internal;

import java.util.List;

/** What the landing template shows. */
// trace:FR-009
public record LandingPage(List<Card> pinned, List<Card> recent, List<String> tags) {

    /** One article in a list: enough to choose it, never its body. */
    public record Card(String title, String summary, String path, List<String> tags) {}
}
