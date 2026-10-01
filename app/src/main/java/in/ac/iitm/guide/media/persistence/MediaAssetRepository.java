package in.ac.iitm.guide.media.persistence;

import in.ac.iitm.guide.shared.persistence.MediaAsset;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** What {@code media} needs of {@code media_asset}; both owner columns are indexed (V4). */
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    List<MediaAsset> findBySubmissionIdOrderByUploadedAtAsc(UUID submissionId);

    List<MediaAsset> findByArticleIdOrderByUploadedAtAsc(UUID articleId);

    /** An asset everyone may have: on an article that is published and not removed. */
    @Query("SELECT m FROM MediaAsset m WHERE m.id = :id AND m.articleId IN"
            + " (SELECT a.id FROM Article a WHERE a.removedAt IS NULL)")
    Optional<MediaAsset> findPublic(@Param("id") UUID id);

    /** DEBT-016: the assets of submissions rejected before that moment; never a pending or published one. */
    @Query("SELECT m FROM MediaAsset m WHERE m.submissionId IN (SELECT s.id FROM Submission s"
            + " WHERE s.status = in.ac.iitm.guide.shared.persistence.SubmissionStatus.REJECTED"
            + " AND s.decidedAt < :before)")
    List<MediaAsset> findOfSubmissionsRejectedBefore(@Param("before") OffsetDateTime before);

    /** NFR-001's volume, counted from the rows rather than by walking the media root. */
    @Query("SELECT COALESCE(SUM(m.sizeBytes), 0) FROM MediaAsset m")
    long totalBytes();

    /**
     * Sets the article and clears the submission in one statement, so the owner check of V3 holds. The
     * flush first writes an article that approval has only just created, which the foreign key needs.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE MediaAsset m SET m.articleId = :article, m.submissionId = NULL WHERE m.submissionId = :submission")
    int moveToArticle(@Param("submission") UUID submissionId, @Param("article") UUID articleId);

    /** The files of several submissions in one query, for the queue's file marks (fix 2.3, ADR-0010). */
    @Query("SELECT m FROM MediaAsset m WHERE m.submissionId IN :submissions ORDER BY m.uploadedAt")
    List<MediaAsset> findBySubmissionIdIn(@Param("submissions") Collection<UUID> submissions);
}
