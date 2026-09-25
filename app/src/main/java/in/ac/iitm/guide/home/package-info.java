/**
 * The landing page, {@code GET /}: pinned articles, recently added ones, the tags in use and a
 * search entry point.
 *
 * <p>Serves FR-009. Nothing in the package root is published: the controller, the service and the
 * read repository are internal.
 *
 * <p><strong>Not built yet:</strong> the moderator's pinning screen (FR-025); the search box points
 * at {@code /search}, which the {@code search} slice adds in phase 3.
 */
package in.ac.iitm.guide.home;
