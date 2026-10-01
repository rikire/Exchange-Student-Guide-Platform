package in.ac.iitm.guide.media.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The media root on the filesystem (ADR-0006). Every name in it is generated here, and every path
 * read from it is checked to be inside it, so no name a stranger chose ever becomes a path.
 */
// trace:FR-010
@Component
public class MediaFiles {

    private final Path root;

    MediaFiles(MediaSettings settings) {
        this.root = settings.root().toAbsolutePath().normalize();
    }

    /** @return the generated name the bytes were written under */
    public String write(InputStream bytes, AcceptedType type) throws IOException {
        Files.createDirectories(root);
        var name = UUID.randomUUID() + "." + type.extension();
        Files.copy(bytes, root.resolve(name));
        return name;
    }

    /** Removes the stored file of that name; a file already gone is not an error. */
    public void delete(String storedName) throws IOException {
        Files.deleteIfExists(resolve(storedName));
    }

    /** @return the stored file of that name, inside the root */
    public Path resolve(String storedName) {
        var path = root.resolve(storedName).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalStateException("A stored name points outside the media root: " + storedName);
        }
        return path;
    }
}
