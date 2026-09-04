package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs git inside the repository and reports what it said.
 *
 * <p>Standard error is inherited rather than captured. Reading two pipes from one process without a
 * thread apiece can deadlock, and the only caller that cares about git's complaint is a hook, whose
 * stderr is shown to the person anyway — so the message reaches its reader without the plumbing.
 */
final class Git {

    /** Long enough for any command used here; short enough that a prompt for a passphrase ends. */
    private static final int TIMEOUT_SECONDS = 20;

    /** What one invocation produced. {@code lines} is stdout only, and empty when git never ran. */
    record Result(int exitCode, List<String> lines) {

        boolean ok() {
            return exitCode == 0;
        }

        String first() {
            return lines.isEmpty() ? "" : lines.get(0);
        }
    }

    private static final Result NEVER_RAN = new Result(-1, List.of());

    private Git() {}

    static Result run(Repo repo, String... arguments) {
        String[] command = new String[arguments.length + 1];
        command[0] = "git";
        System.arraycopy(arguments, 0, command, 1, arguments.length);

        try {
            Process process = new ProcessBuilder(command)
                    .directory(repo.root().toFile())
                    .redirectError(Redirect.INHERIT)
                    .start();
            String output;
            try (var stream = process.getInputStream()) {
                output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return NEVER_RAN;
            }
            return new Result(
                    process.exitValue(), output.lines().map(String::strip).toList());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return NEVER_RAN;
        }
    }
}
