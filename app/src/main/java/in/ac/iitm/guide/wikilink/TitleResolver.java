package in.ac.iitm.guide.wikilink;

import java.util.Map;
import java.util.Set;

/**
 * Answers which of the titles named by wiki links belong to a published article, and where each of
 * those articles lives.
 *
 * <p>Takes every title of a page at once so the caller can answer with a single query; a call per
 * link would be the N+1 that docs/ai/security.md rules out. Matching a title to an article
 * case-insensitively (FR-002) is the implementation's job: the renderer only reads the answer.
 */
// trace:FR-002
@FunctionalInterface
public interface TitleResolver {

    /**
     * @param titles the titles as the author wrote them, trimmed, never empty
     * @return an href for each title that matches a published article, keyed by the title exactly as
     *     passed in; a title with no entry is a red link
     */
    Map<String, String> resolve(Set<String> titles);
}
