/**
 * Reading an article: {@code GET /articles/{title}}, its body rendered, wiki links resolved.
 *
 * <p>Serves FR-001, and is where FR-002 and FR-004 become visible. Nothing in the package root is
 * published yet: the controller and the read repository are internal ({@code web}, {@code
 * persistence}), since no other slice needs to call this one.
 *
 * <p><strong>Not built yet:</strong> the article's media assets (phase 3, {@code media}), backlinks,
 * propose-an-edit and report links.
 */
package in.ac.iitm.guide.articleview;
