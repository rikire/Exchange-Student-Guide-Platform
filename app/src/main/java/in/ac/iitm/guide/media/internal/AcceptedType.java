package in.ac.iitm.guide.media.internal;

import in.ac.iitm.guide.media.MediaKind;
import java.util.Arrays;
import java.util.Optional;

/**
 * FEAT-009's allowlist (CON-006, security.md), keyed by the type Tika reads from the bytes. A photo
 * is stored re-encoded, so it is stored as the type it is written in: WebP is read, not written, by
 * ImageIO, and becomes PNG, which keeps its transparency.
 */
// trace:FR-010
// trace:FR-011
public enum AcceptedType {
    JPEG("image/jpeg", MediaKind.PHOTO, "image/jpeg", "jpg"),
    PNG("image/png", MediaKind.PHOTO, "image/png", "png"),
    WEBP("image/webp", MediaKind.PHOTO, "image/png", "png"),
    PDF("application/pdf", MediaKind.DOCUMENT, "application/pdf", "pdf"),
    MP4("video/mp4", MediaKind.VIDEO, "video/mp4", "mp4");

    private final String detected;
    private final MediaKind kind;
    private final String stored;
    private final String extension;

    AcceptedType(String detected, MediaKind kind, String stored, String extension) {
        this.detected = detected;
        this.kind = kind;
        this.stored = stored;
        this.extension = extension;
    }

    public static Optional<AcceptedType> detectedAs(String type) {
        return Arrays.stream(values()).filter(t -> t.detected.equals(type)).findFirst();
    }

    /** @return the kind of a stored asset, from the type it was stored as */
    public static MediaKind kindOfStored(String storedType) {
        return Arrays.stream(values())
                .filter(t -> t.stored.equals(storedType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No accepted type is stored as " + storedType))
                .kind;
    }

    public MediaKind kind() {
        return kind;
    }

    /** @return the content type of the file as stored, which is what delivery sends */
    public String stored() {
        return stored;
    }

    /** @return the extension of the stored name, chosen by the system, never taken from the upload */
    public String extension() {
        return extension;
    }
}
