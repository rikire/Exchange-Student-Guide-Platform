/**
 * Tags and navigation by tag. Tags are rows, normalised on the way in.
 *
 * <p>Serves FR-008.
 *
 * <p>Published: {@link in.ac.iitm.guide.taxonomy.Tags}, the only way a tag reaches the table, which
 * {@code backup}, {@code contribute} and {@code moderate} call (ADR-0005), and which gives a form the
 * names in use to suggest (ADR-0022); and {@link
 * in.ac.iitm.guide.taxonomy.TagLink}, how every slice that shows a tag links it to its page. Browsing
 * by tag is {@code GET /tags/{tag}} (FEAT-008).
 */
package in.ac.iitm.guide.taxonomy;
