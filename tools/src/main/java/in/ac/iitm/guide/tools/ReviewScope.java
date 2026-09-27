package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;

/** Read-only review inventory. Git defines changes; SHA-256 binds evidence to repository state. */
public final class ReviewScope {
    public record Report(String base, String head, List<String> files, String fingerprint) {}

    private ReviewScope() {}

    public static Report capture(Path root, String ref) throws Exception {
        if (ref == null || ref.isBlank() || ref.startsWith("-")) {
            throw new IOException("An explicit base revision is required");
        }
        String base = git(root, "rev-parse", "--verify", ref + "^{commit}").strip();
        String head = git(root, "rev-parse", "--verify", "HEAD^{commit}").strip();
        List<String> changed = changedPaths(root, base);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        add(digest, base);
        add(digest, head);
        add(digest, git(root, "ls-files", "--stage", "-z"));
        for (String name : paths(git(root, "ls-files", "--cached", "--others", "--exclude-standard", "-z"))) {
            Path file = root.resolve(name);
            add(digest, name);
            if (Files.isSymbolicLink(file)) {
                add(digest, "symlink:" + Files.readSymbolicLink(file));
            } else if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                add(digest, "file:" + Files.isExecutable(file));
                add(
                        digest,
                        HexFormat.of()
                                .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
            } else if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                add(digest, "deleted");
            } else {
                throw new IOException("Unsupported review entry (for example a submodule): " + name);
            }
        }
        return new Report(base, head, List.copyOf(changed), HexFormat.of().formatHex(digest.digest()));
    }

    /** All layers are separate so an index/worktree undo cannot hide an earlier change. */
    public static List<String> changedPaths(Path root, String ref) throws Exception {
        if (ref == null || ref.isBlank() || ref.startsWith("-")) {
            throw new IOException("An explicit base revision is required");
        }
        String base = git(root, "rev-parse", "--verify", ref + "^{commit}").strip();
        TreeSet<String> changed = paths(git(root, "diff", "--name-only", "--no-renames", "-z", base, "HEAD", "--"));
        changed.addAll(paths(git(root, "diff", "--cached", "--name-only", "--no-renames", "-z", "HEAD", "--")));
        changed.addAll(paths(git(root, "diff", "--name-only", "--no-renames", "-z", "--")));
        changed.addAll(paths(git(root, "ls-files", "--others", "--exclude-standard", "-z")));
        return List.copyOf(changed);
    }

    private static void add(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private static TreeSet<String> paths(String output) {
        TreeSet<String> result = new TreeSet<>(Arrays.asList(output.split("\u0000", -1)));
        result.remove("");
        return result;
    }

    private static String git(Path root, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command)
                .directory(root.toFile())
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new IOException("Cannot establish review scope: git " + String.join(" ", args));
        }
        return output;
    }
}
