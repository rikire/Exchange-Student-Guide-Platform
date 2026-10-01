package in.ac.iitm.guide.media;

import java.util.Locale;
import java.util.UUID;

/**
 * An attached asset as a page shows it: a picture, a video player, or a document's name and size,
 * each reached through {@link #href()}.
 *
 * @param playsInPage for a video, whether the page offers a player or only a card to download (AVI,
 *     MPEG)
 */
// trace:FR-001
// trace:FR-015
public record MediaItem(UUID id, String originalName, MediaKind kind, long sizeBytes, boolean playsInPage) {

    /** @return the route that serves the bytes, {@code GET /media/{id}} */
    public String href() {
        return "/media/" + id;
    }

    /** @return the size as a reader reads it: {@code 240 KB}, {@code 1.2 MB} */
    public String size() {
        if (sizeBytes < 1024 * 1024) {
            return Math.max(1, Math.round(sizeBytes / 1024.0)) + " KB";
        }
        return String.format(Locale.ROOT, "%.1f MB", sizeBytes / (1024.0 * 1024.0));
    }
}
