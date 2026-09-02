package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Locates the repository root.
 *
 * <p>Hooks are launched by Claude Code with an arbitrary working directory, and the same binary is
 * also run by git hooks and by CI. Every command therefore resolves the root itself instead of
 * trusting the current directory.
 */
public final class Repo {

    private final Path root;

    private Repo(Path root) {
        this.root = root;
    }

    /** Walks up from {@code start} (or the current directory) until a repository marker is found. */
    public static Repo find(String start) throws IOException {
        Path current = (start == null || start.isBlank())
                ? Paths.get("").toAbsolutePath()
                : Paths.get(start).toAbsolutePath();

        for (Path dir = current; dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(".git")) || Files.isRegularFile(dir.resolve(".git"))) {
                return new Repo(dir);
            }
            if (Files.isRegularFile(dir.resolve(".mvn/wrapper/maven-wrapper.properties"))) {
                return new Repo(dir);
            }
        }
        throw new IOException("repository root not found from " + current);
    }

    public Path root() {
        return root;
    }

    public Path resolve(String relative) {
        return root.resolve(relative);
    }

    /** Repository-relative, forward-slashed path, or {@code null} when the file lies outside. */
    public String relativize(String absolute) {
        if (absolute == null || absolute.isBlank()) {
            return null;
        }
        try {
            Path path = Paths.get(absolute).toAbsolutePath().normalize();
            if (!path.startsWith(root)) {
                return null;
            }
            return root.relativize(path).toString().replace('\\', '/');
        } catch (RuntimeException e) {
            return null;
        }
    }

    public void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
