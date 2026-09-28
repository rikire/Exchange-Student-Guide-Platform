/**
 * Plain Java: the {@code [[link]]} markup, Markdown rendering and the article address. Imports neither
 * Spring nor JPA (enforced by {@code ArchitectureRulesTest}), so its tests run without a context.
 *
 * <p>Serves FR-001 (the address), FR-002, FR-004. Published types: {@link
 * in.ac.iitm.guide.wikilink.WikiLinkRenderer} turns a body into HTML and asks a {@link
 * in.ac.iitm.guide.wikilink.TitleResolver}, once per page, which titles are live articles, and lists
 * the titles a body links to for {@code backlink} (FR-006); {@link
 * in.ac.iitm.guide.wikilink.ArticleAddress} derives the slug and path of an article from its title.
 * Storing links and answering "what links here" is {@code backlink}'s (ADR-0016): this slice has no
 * database.
 */
package in.ac.iitm.guide.wikilink;
