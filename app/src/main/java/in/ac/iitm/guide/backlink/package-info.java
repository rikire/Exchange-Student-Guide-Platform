/**
 * FR-006's backlinks, and the only reader and writer of {@code article_link} (ADR-0016, ADR-0012).
 *
 * <p>Published types: {@link in.ac.iitm.guide.backlink.ArticleTextChanged}, the event a slice
 * publishes after it writes an article's body, handled here in the writer's transaction; and {@link
 * in.ac.iitm.guide.backlink.Backlinks}, which the article page asks for the live articles linking to
 * it. The links are found by {@code wikilink}'s parser, so they are the links the page draws.
 */
package in.ac.iitm.guide.backlink;
