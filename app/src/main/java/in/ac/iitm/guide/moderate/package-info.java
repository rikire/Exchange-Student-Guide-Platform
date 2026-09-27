/**
 * The queue, approval and rejection, plus the revision retained when an approved edit changes an article.
 *
 * <p>Serves FR-014, FR-015, FR-017, FR-018 and FR-020 (FEAT-006); FR-019 and FR-026 are phase 4. The
 * only slice that writes {@code article} from a submission (ADR-0003). Its routes sit behind the
 * moderator login of {@code shared.security} (ADR-0009).
 */
package in.ac.iitm.guide.moderate;
