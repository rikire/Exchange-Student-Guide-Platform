/**
 * Plain Java: the {@code [[link]]} markup, Markdown rendering and the article address. Imports neither
 * Spring nor JPA (enforced by {@code ArchitectureRulesTest}), so its tests run without a context.
 *
 * <p>Serves FR-001 (the address), FR-002, FR-004. Published types: {@link
 * in.ac.iitm.guide.wikilink.WikiLinkRenderer} turns a body into HTML and asks a {@link
 * in.ac.iitm.guide.wikilink.TitleResolver}, once per page, which titles are live articles; {@link
 * in.ac.iitm.guide.wikilink.ArticleAddress} derives the slug and path of an article from its title.
 *
 * <p><strong>Not built yet:</strong> extracting an article's links on publish (ADR-0012, FR-006) and
 * backlinks, both phase 3.
 */
package in.ac.iitm.guide.wikilink;
