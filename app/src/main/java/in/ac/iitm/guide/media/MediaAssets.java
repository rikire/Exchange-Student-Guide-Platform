package in.ac.iitm.guide.media;

import in.ac.iitm.guide.media.internal.AcceptedType;
import in.ac.iitm.guide.media.internal.MediaFiles;
import in.ac.iitm.guide.media.internal.MediaSettings;
import in.ac.iitm.guide.media.internal.PhotoEncoder;
import in.ac.iitm.guide.media.internal.PhotoEncoder.UnreadablePhotoException;
import in.ac.iitm.guide.media.persistence.MediaAssetRepository;
import in.ac.iitm.guide.shared.persistence.MediaAsset;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The one way a file reaches the media root and {@code media_asset} (ADR-0006): the type is read from
 * the bytes, a photo is re-encoded, the stored name is generated, and NFR-001's limits are applied
 * before anything is written. An asset belongs to a submission until approval moves it to the
 * article, and only then is it served to everyone.
 */
// trace:FR-010
// trace:FR-011
// trace:FR-015
// trace:FR-001
// trace:NFR-001
@Service
public class MediaAssets {

    private static final String NOT_ACCEPTED =
            "The attachment is not an accepted type. Attach a photo (JPEG, PNG or WebP), a PDF or a video"
                    + " (MP4, MOV, WebM, MKV, 3GP, AVI or MPEG).";

    /** The detector only, from {@code tika-core}; none of Tika's parsers is on the class path. */
    private static final Logger log = LoggerFactory.getLogger(MediaAssets.class);

    private final Tika tika = new Tika();

    private final MediaAssetRepository assets;
    private final MediaFiles files;
    private final PhotoEncoder photos;
    private final MediaSettings settings;

    MediaAssets(MediaAssetRepository assets, MediaFiles files, PhotoEncoder photos, MediaSettings settings) {
        this.assets = assets;
        this.files = files;
        this.photos = photos;
        this.settings = settings;
    }

    /**
     * @return the stored asset
     * @throws MediaRejectedException if the file is not of an accepted type or is over its limit
     */
    @Transactional
    public MediaItem attach(UUID submissionId, Upload upload) {
        try {
            var type = typeOf(upload);
            var limit = settings.limitOf(type.kind());
            if (upload.size() > limit.toBytes()) {
                throw new MediaRejectedException("The attachment is larger than " + MediaSettings.spoken(limit)
                        + ", the limit for a " + noun(type.kind()) + ".");
            }
            if (assets.totalBytes() + upload.size() > settings.volumeLimit().toBytes()) {
                throw new MediaRejectedException(
                        "The guide's storage for attachments is full. Submit without the attachment, or ask OGE.");
            }

            String storedName;
            long size;
            if (type.kind() == MediaKind.PHOTO) {
                var encoded = photos.encode(upload.content(), type);
                storedName = files.write(new ByteArrayInputStream(encoded), type);
                size = encoded.length;
            } else {
                try (var in = upload.content().getInputStream()) {
                    storedName = files.write(in, type);
                }
                size = upload.size();
            }

            var asset = new MediaAsset();
            asset.setSubmissionId(submissionId);
            asset.setStoredName(storedName);
            asset.setOriginalName(upload.originalName());
            asset.setContentType(type.stored());
            asset.setSizeBytes(size);
            asset.setUploadedAt(OffsetDateTime.now());
            return item(assets.save(asset));
        } catch (UnreadablePhotoException e) {
            throw new MediaRejectedException(NOT_ACCEPTED);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** @return what may be attached, and how large, as a form tells a contributor */
    public String accepted() {
        return "One photo (JPEG, PNG or WebP) up to " + MediaSettings.spoken(settings.photoLimit())
                + ", a PDF up to " + MediaSettings.spoken(settings.documentLimit())
                + " or a video (MP4, MOV, WebM, MKV, 3GP, AVI or MPEG) up to "
                + MediaSettings.spoken(settings.videoLimit()) + ".";
    }

    /**
     * DEBT-016: removes the assets of submissions rejected longer than {@code guide.media.rejected-kept-for}
     * before {@code now}.
     *
     * @return how many were removed
     */
    @Transactional
    public int sweepRejected(OffsetDateTime now) {
        var swept = assets.findOfSubmissionsRejectedBefore(now.minus(settings.rejectedKeptFor()));
        if (swept.isEmpty()) {
            return 0;
        }
        assets.deleteAll(swept);
        var names = swept.stream().map(MediaAsset::getStoredName).toList();
        // The files go only once the rows are gone for good: a row without its file would answer 500.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                names.forEach(MediaAssets.this::deleteFile);
            }
        });
        log.info("Removed the files of {} assets of rejected submissions", swept.size());
        return swept.size();
    }

    private void deleteFile(String storedName) {
        try {
            files.delete(storedName);
        } catch (IOException e) {
            // The row is gone, so the volume is freed; the file is wasted disk, not exposure (ADR-0006).
            log.warn("Could not remove the stored file {}", storedName, e);
        }
    }

    /** Moves every asset of an approved submission to the article it became or changed. */
    @Transactional
    public void moveToArticle(UUID submissionId, UUID articleId) {
        assets.moveToArticle(submissionId, articleId);
    }

    /** @return the assets of a submission, oldest first */
    @Transactional(readOnly = true)
    public List<MediaItem> ofSubmission(UUID submissionId) {
        return assets.findBySubmissionIdOrderByUploadedAtAsc(submissionId).stream()
                .map(MediaAssets::item)
                .toList();
    }

    /**
     * The kinds of file each of several submissions carries, in one query (fix 2.3, ADR-0010).
     *
     * @return by submission id; a submission with no file is absent
     */
    @Transactional(readOnly = true)
    public Map<UUID, List<MediaKind>> kindsOfSubmissions(Collection<UUID> submissionIds) {
        if (submissionIds.isEmpty()) {
            return Map.of();
        }
        var kinds = new LinkedHashMap<UUID, List<MediaKind>>();
        for (var asset : assets.findBySubmissionIdIn(submissionIds)) {
            kinds.computeIfAbsent(asset.getSubmissionId(), id -> new ArrayList<>())
                    .add(AcceptedType.kindOfStored(asset.getContentType()));
        }
        return kinds;
    }

    /** @return the assets of an article, oldest first */
    @Transactional(readOnly = true)
    public List<MediaItem> ofArticle(UUID articleId) {
        return assets.findByArticleIdOrderByUploadedAtAsc(articleId).stream()
                .map(MediaAssets::item)
                .toList();
    }

    private AcceptedType typeOf(Upload upload) throws IOException {
        String detected;
        byte[] header;
        try (var in = upload.content().getInputStream()) {
            // Only the bytes: the name and the declared type are the contributor's to choose.
            detected = tika.detect(in);
        }
        try (var in = upload.content().getInputStream()) {
            header = in.readNBytes(12);
        }
        if (detected.equals("image/heic") || detected.equals("image/heif")) {
            throw new MediaRejectedException(
                    "HEIC photos cannot be accepted. Save the photo as JPEG (on an iPhone: Settings, Camera,"
                            + " Formats, Most Compatible) and attach it again.");
        }
        if (detected.equals("video/quicktime")) {
            return AcceptedType.ofQuickTimeBrand(header);
        }
        return AcceptedType.detectedAs(detected).orElseThrow(() -> new MediaRejectedException(NOT_ACCEPTED));
    }

    private static String noun(MediaKind kind) {
        return switch (kind) {
            case PHOTO -> "photo";
            case DOCUMENT -> "document";
            case VIDEO -> "video";
        };
    }

    private static MediaItem item(MediaAsset asset) {
        return new MediaItem(
                asset.getId(),
                asset.getOriginalName(),
                AcceptedType.kindOfStored(asset.getContentType()),
                asset.getSizeBytes(),
                AcceptedType.playsInPage(asset.getContentType()));
    }
}
