/**
 * Flagging a published article as a problem, and closing that flag.
 *
 * <p>Serves FR-021, FR-022.
 *
 * <p>Built 2 Oct: a report form per published article, its POST, the moderator's inbox under
 * {@code /moderate/reports} and closing a report. Nothing in the package root is published: no other
 * slice calls this one; the article page links to {@code /articles/{address}/report} by address.
 */
package in.ac.iitm.guide.report;
