package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * A content fingerprint of the working tree, taken between turns.
 *
 * <p>This exists for one reason: the assistant does not observe the file system between turns, so
 * an edit the human makes by hand is invisible to it. Comparing two snapshots is the only way to
 * notice such an edit and report it.
 */
public final class Snapshot {

    /** Directories that either change constantly or are not human-authored. */
    private static final Set<String> SKIPPED_DIRECTORIES =
            Set.of(".git", "target", "node_modules", ".idea", ".vscode", "out", "build");

    /** Files this tooling writes itself; including them would report the tooling's own writes. */
    private static final List<String> SKIPPED_PREFIXES = List.of(".claude/.journal-state/", "docs/ai/journal/");

    private static final long MAX_FILE_BYTES = 2_000_000L;

    private Snapshot() {}

    /** Maps repository-relative path to a content hash, sorted so that output is stable. */
    public static Map<String, String> take(Repo repo) {
        Map<String, String> result = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(repo.root())) {
            walk.filter(Files::isRegularFile).forEach(path -> {
                String relative = repo.relativize(path.toString());
                if (relative == null || isSkipped(relative)) {
                    return;
                }
                try {
                    if (Files.size(path) > MAX_FILE_BYTES) {
                        return;
                    }
                    result.put(relative, sha256(Files.readAllBytes(path)));
                } catch (IOException e) {
                    // A file that cannot be read right now simply stays out of the snapshot;
                    // failing here would break the human's session over a transient lock.
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    private static boolean isSkipped(String relative) {
        for (String segment : relative.split("/")) {
            if (SKIPPED_DIRECTORIES.contains(segment)) {
                return true;
            }
        }
        return SKIPPED_PREFIXES.stream().anyMatch(relative::startsWith);
    }

    /** Describes what changed between two snapshots, as {@code path (kind)} lines. */
    public static List<String> diff(Map<String, String> before, Map<String, String> after) {
        return Stream.concat(before.keySet().stream(), after.keySet().stream())
                .distinct()
                .sorted()
                .map(path -> {
                    String was = before.get(path);
                    String now = after.get(path);
                    if (was == null) {
                        return path + " (added)";
                    }
                    if (now == null) {
                        return path + " (deleted)";
                    }
                    return was.equals(now) ? null : path + " (modified)";
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new BigInteger(1, digest.digest(bytes)).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", e);
        }
    }

    public static String fingerprint(String text) {
        return sha256(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)).substring(0, 16);
    }
}
