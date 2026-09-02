package in.ac.iitm.guide.tools;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point of the repository's AI process tooling.
 *
 * <pre>
 *   hook prompt            UserPromptSubmit — open a journal entry, report edits made by hand
 *   hook guard             PreToolUse       — ask before editing a file the human owns
 *   hook stop              Stop             — close the journal entry
 *   hook note &lt;text&gt;       add a note to the current journal entry
 *   hook english           supply the English rendering of the prompt and the outcome
 *   commit-msg &lt;file&gt;      check the commit message convention
 * </pre>
 *
 * <p>A failure inside a hook must not break the human's session: every error is reported on stderr
 * and the process still exits with 0. The one exception is {@code commit-msg}, where a non-zero
 * exit is the whole point.
 */
public final class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: ai-tools <hook|commit-msg> [...]");
            System.exit(64);
        }
        try {
            switch (args[0]) {
                case "hook" -> HookCommand.run(Arrays.copyOfRange(args, 1, args.length));
                case "commit-msg" -> commitMsg(Arrays.copyOfRange(args, 1, args.length));
                default -> {
                    System.err.println("ai-tools: unknown command " + args[0]);
                    System.exit(64);
                }
            }
        } catch (Exception e) {
            System.err.println("ai-tools: " + e.getMessage());
            System.exit(0);
        }
    }

    private static void commitMsg(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: ai-tools commit-msg <file>");
            System.exit(64);
        }
        List<String> problems = CommitMessageCheck.check(Path.of(args[0]));
        if (problems.isEmpty()) {
            return;
        }
        System.err.println("The commit message does not follow the convention:");
        problems.forEach(problem -> System.err.println("  - " + problem));
        System.err.println();
        System.err.println("  feat(FEAT-003): add article submission form [FR-012]");
        System.err.println("  refactor(DEBT-002): replace the in-memory rate limiter");
        System.err.println("  docs: describe the moderation state machine");
        System.err.println();
        System.err.println("See docs/repository-map.md, section \"Commit convention\".");
        System.exit(1);
    }
}
