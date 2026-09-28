package in.ac.iitm.guide.media.persistence;

import in.ac.iitm.guide.shared.persistence.MediaAsset;
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

    /** NFR-001's volume, counted from the rows rather than by walking the media root. */
    @Query("SELECT COALESCE(SUM(m.sizeBytes), 0) FROM MediaAsset m")
    long totalBytes();

    /** Sets the article and clears the submission in one statement, so the owner check of V3 holds. */
    @Modifying
    @Query("UPDATE MediaAsset m SET m.articleId = :article, m.submissionId = NULL WHERE m.submissionId = :submission")
    int moveToArticle(@Param("submission") UUID submissionId, @Param("article") UUID articleId);
}
