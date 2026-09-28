/**
 * Upload, storage and delivery. Bytes on the filesystem, metadata in the database (ADR-0006).
 *
 * <p>Serves FR-010's and FR-011's attachment, NFR-001's limits, and the delivery route FR-001 and
 * FR-015 need; FR-016's download button is phase 4.
 *
 * <p>Published: {@link in.ac.iitm.guide.media.MediaAssets}, the one way a file is stored, moved to an
 * article on approval, or listed for a page, which {@code contribute}, {@code moderate} and
 * {@code articleview} call (FEAT-009).
 */
package in.ac.iitm.guide.media;
