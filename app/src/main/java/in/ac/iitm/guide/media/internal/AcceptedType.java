package in.ac.iitm.guide.media.internal;

import in.ac.iitm.guide.media.MediaKind;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * FEAT-009's allowlist (CON-006, security.md), keyed by the type Tika reads from the bytes. A photo
 * is stored re-encoded, so it is stored as the type it is written in: WebP is read, not written, by
 * ImageIO, and becomes PNG, which keeps its transparency. A video is stored as it came, under the
 * type a browser plays it by (walkthrough fix 1.4, the human, 1 Oct).
 *
 * <p>What tika-core 3.3.2 cannot tell apart, probed 1 Oct: an MP4 of brand {@code isom} from a MOV
 * (both {@code video/quicktime}), told apart by {@link #ofQuickTimeBrand}; a WebM from an MKV (both
 * {@code application/x-matroska}), stored alike as WebM and left to the browser's player. Ogg and
 * ASF are not here: Tika reads them the same whether they carry a picture or only sound.
 */
// trace:FR-010
// trace:FR-011
public enum AcceptedType {
    JPEG("image/jpeg", MediaKind.PHOTO, "image/jpeg", "jpg"),
    PNG("image/png", MediaKind.PHOTO, "image/png", "png"),
    WEBP("image/webp", MediaKind.PHOTO, "image/png", "png"),
    PDF("application/pdf", MediaKind.DOCUMENT, "application/pdf", "pdf"),
    MP4("video/mp4", MediaKind.VIDEO, "video/mp4", "mp4"),
    MOV("video/quicktime", MediaKind.VIDEO, "video/quicktime", "mov"),
    M4V("video/x-m4v", MediaKind.VIDEO, "video/mp4", "m4v"),
    THREE_GP("video/3gpp", MediaKind.VIDEO, "video/3gpp", "3gp"),
    THREE_G2("video/3gpp2", MediaKind.VIDEO, "video/3gpp2", "3g2"),
    MATROSKA("application/x-matroska", MediaKind.VIDEO, "video/webm", "webm"),
    AVI("video/x-msvideo", MediaKind.VIDEO, "video/x-msvideo", "avi"),
    MPEG("video/mpeg", MediaKind.VIDEO, "video/mpeg", "mpg");

    /** The major brand of a QuickTime movie, which an older one does not have at all. */
    private static final String QUICKTIME_BRAND = "qt  ";

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

    /**
     * Tika reads an MP4 of brand {@code isom} as a QuickTime movie; the {@code ftyp} box tells them
     * apart. Without one, as in an older QuickTime file starting with its {@code moov} atom, it is a
     * movie.
     *
     * @param header the file's first bytes; an ISO base media file has {@code ftyp} at 4 to 7 and its
     *     major brand at 8 to 11
     */
    public static AcceptedType ofQuickTimeBrand(byte[] header) {
        if (header.length < 12 || !new String(header, 4, 4, StandardCharsets.ISO_8859_1).equals("ftyp")) {
            return MOV;
        }
        return new String(header, 8, 4, StandardCharsets.ISO_8859_1).equals(QUICKTIME_BRAND) ? MOV : MP4;
    }

    /**
     * AVI and MPEG are offered only as a download: no current browser plays them in a page (the
     * human, 1 Oct). Every other video gets a player, and a card if the browser cannot decode it.
     *
     * @return whether a page offers a stored asset of this type in a player
     */
    public static boolean playsInPage(String storedType) {
        return !storedType.equals(AVI.stored) && !storedType.equals(MPEG.stored);
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
