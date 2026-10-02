/**
 * Creating an article and proposing an edit, both of which enter the moderation queue rather than publishing.
 *
 * <p>Serves FR-003, FR-010, FR-011 (FEAT-005); FR-012's status lookup is phase 4. Writes
 * {@code submission} only, never {@code article}: publishing is {@code moderate}'s (ADR-0003). Its
 * published types, {@code Submissions}, {@code Draft} and {@code SubmissionRejectedException}, are
 * how {@code moderate} submits the moderator's own article before approving it (FR-023, FR-024).
 */
package in.ac.iitm.guide.contribute;
