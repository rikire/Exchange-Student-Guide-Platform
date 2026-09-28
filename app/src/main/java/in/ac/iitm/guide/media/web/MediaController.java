package in.ac.iitm.guide.media.web;

import in.ac.iitm.guide.media.MediaKind;
import in.ac.iitm.guide.media.internal.AcceptedType;
import in.ac.iitm.guide.media.internal.MediaFiles;
import in.ac.iitm.guide.media.persistence.MediaAssetRepository;
import in.ac.iitm.guide.shared.persistence.MediaAsset;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * {@code GET /media/{id}} (ui-routes.md): the id is looked up in the database, never taken as a path.
 * An asset on a published article is everyone's; any other, only a moderator's, which is the control
 * ADR-0006 settled on rather than the id being hard to guess. {@code nosniff} is on every response
 * through Spring Security's default headers, and a {@code Range} request is answered by Spring MVC
 * from the {@link Resource}.
 */
// trace:FR-001
// trace:FR-015
@Controller
class MediaController {

    /** WebSecurity's role, granted at the moderator login (ADR-0009). */
    private static final String MODERATOR = "MODERATOR";

    private final MediaAssetRepository assets;
    private final MediaFiles files;

    MediaController(MediaAssetRepository assets, MediaFiles files) {
        this.assets = assets;
        this.files = files;
    }

    @GetMapping("/media/{id}")
    @Transactional(readOnly = true)
    ResponseEntity<Resource> deliver(@PathVariable String id, HttpServletRequest request) {
        var asset = idOf(id).flatMap(
                        uuid -> request.isUserInRole(MODERATOR) ? assets.findById(uuid) : assets.findPublic(uuid))
                .orElseThrow(MediaNotFoundException::new);

        var response = ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.getContentType()));
        if (AcceptedType.kindOfStored(asset.getContentType()) != MediaKind.PHOTO) {
            response.header(HttpHeaders.CONTENT_DISPOSITION, attachment(asset));
        }
        return response.body(new FileSystemResource(files.resolve(asset.getStoredName())));
    }

    private static Optional<UUID> idOf(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            // Text that is not an id names no asset, which is what 404 says.
            return Optional.empty();
        }
    }

    /** The original name, encoded by Spring (RFC 6266), so no character of it can break the header. */
    private static String attachment(MediaAsset asset) {
        return ContentDisposition.attachment()
                .filename(asset.getOriginalName(), StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class MediaNotFoundException extends RuntimeException {}
}
