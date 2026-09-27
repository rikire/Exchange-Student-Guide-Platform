/**
 * The landing page, {@code GET /}: pinned articles, recently added ones, the tags in use and a
 * search entry point.
 *
 * <p>Serves FR-009. Nothing in the package root is published: the controller, the service and the
 * read repository are internal.
 *
 * <p><strong>Not built yet:</strong> the moderator's pinning screen (FR-025). The search box sends
 * its query to {@code /search}, served by the {@code search} slice.
 */
package in.ac.iitm.guide.home;
