package in.ac.iitm.guide.backup.internal;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * One archive file as data: the front matter's fields and the Markdown body that follows it.
 * {@code pinned} is null for an article that is not pinned to the landing page.
 */
// trace:NFR-004
public record ArchivedArticle(
        String title,
        String summary,
        List<String> tags,
        OffsetDateTime created,
        OffsetDateTime updated,
        OffsetDateTime pinned,
        String body) {}
