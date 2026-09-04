package in.ac.iitm.guide.tools;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Rules about shell commands and about the text an edit is about to write.
 *
 * <p>Separated from the hook plumbing so the rules can be tested directly: a rule that can only be
 * exercised by launching a hook does not get tested, and an untested guard is a guess.
 */
public final class CommandRules {

    /** What a rule decided, and why, in words meant for whoever has to act on it. */
    public record Verdict(Decision decision, String reason) {

        public enum Decision {
            ALLOW,
            ASK,
            DENY
        }
    }

    private static final Verdict ALLOWED = new Verdict(Verdict.Decision.ALLOW, "");

    /**
     * Flags that skip the checks this repository installs, each with the tool it belongs to.
     *
     * <p>The tool has to appear in the same segment of the command. Without that, this rule blocked
     * the very command that was writing documentation *about* the flags — text mentioning
     * {@code --no-verify} is not a use of it, and a check that cannot tell the difference is one
     * people switch off.
     */
    private static final List<String[]> BYPASS =
            List.of(new String[] {"--no-verify", "git"}, new String[] {"-DskipTests", "mvn"}, new String[] {
                "-Dmaven.test.skip", "mvn"
            });

    /** Roughly, one command in a compound line: enough to tell a flag's owner from a neighbour's. */
    private static final Pattern SEGMENT = Pattern.compile("\\n|;|&&|\\|\\||\\|");

    private static final Pattern BUILD_COMMAND = Pattern.compile("(?:\\./)?mvnw|scripts/check\\.sh");

    /**
     * A build whose output is piped somewhere.
     *
     * <p>The shell reports the exit code of the last stage, so {@code ./mvnw package | tail -5}
     * reports whether `tail` succeeded, and `tail` always does. That is how a broken compile printed
     * "BUILD OK" twice in this repository's first two days.
     */
    private static final Pattern PIPED = Pattern.compile("\\|\\s*(?:head|tail|grep|sed|awk|cut|sort|wc)");

    private static final Pattern EXIT_CODE_PRESERVED = Pattern.compile("PIPESTATUS|pipefail");

    /** A deferred-work marker with no reference to an entry in the debt register. */
    private static final Pattern LOOSE_MARKER = Pattern.compile("(?<![A-Za-z])(TODO|FIXME|HACK|XXX)(?!\\()");

    private static final Pattern DEBT_REFERENCE = Pattern.compile("DEBT-\\d{3}");

    private CommandRules() {}

    /** Judges a shell command before it runs. */
    public static Verdict forCommand(String command) {
        if (command == null || command.isBlank()) {
            return ALLOWED;
        }

        for (String segment : SEGMENT.split(command)) {
            for (String[] rule : BYPASS) {
                if (segment.contains(rule[0]) && segment.contains(rule[1])) {
                    return new Verdict(
                            Verdict.Decision.DENY,
                            "This command carries " + rule[0]
                                    + ", which skips the checks this repository exists to run.\n\n"
                                    + "CLAUDE.md forbids it outright. If a check is failing, fix the cause or say "
                                    + "that you cannot; do not step around the thing that noticed.");
                }
            }
        }

        if (BUILD_COMMAND.matcher(command).find()
                && PIPED.matcher(command).find()
                && !EXIT_CODE_PRESERVED.matcher(command).find()) {
            return new Verdict(
                    Verdict.Decision.ASK,
                    "This pipes a build or test into another command, so the shell will report that "
                            + "command's exit code rather than the build's - and `tail`, `head` and `grep` "
                            + "succeed almost always.\n\n"
                            + "That is how \"BUILD OK\" was printed twice here over a broken compile.\n\n"
                            + "If you need the real result, use ${PIPESTATUS[0]} or `set -o pipefail`. If you only "
                            + "want to read the output and will not draw a conclusion from it, say so and continue.");
        }
        return ALLOWED;
    }

    /**
     * Judges the text an edit is about to write.
     *
     * <p>Only one rule, deliberately. A check that fires on something legitimate gets switched off,
     * and then it protects nothing at all.
     */
    public static Verdict forContent(String path, String content) {
        if (content == null || content.isBlank() || path == null) {
            return ALLOWED;
        }
        // The debt register describes markers; the instructions quote them. Neither is a marker.
        if (path.equals("docs/tech-debt.md") || path.startsWith("docs/ai/") || path.startsWith(".claude/")) {
            return ALLOWED;
        }

        for (String line : content.split("\r?\n")) {
            if (LOOSE_MARKER.matcher(line).find()
                    && !DEBT_REFERENCE.matcher(line).find()) {
                return new Verdict(
                        Verdict.Decision.DENY,
                        "This edit adds a deferred-work marker with no debt entry behind it:\n\n  "
                                + line.strip() + "\n\n"
                                + "CLAUDE.md requires the form `TODO(DEBT-007): ...`, and the entry has to exist in "
                                + "docs/tech-debt.md with its cause, consequence and a plan to fix it.\n\n"
                                + "Write the entry first, then the marker. A marker recorded at the end of a task, "
                                + "when the context is gone, is a line nobody can act on.");
            }
        }
        return ALLOWED;
    }
}
