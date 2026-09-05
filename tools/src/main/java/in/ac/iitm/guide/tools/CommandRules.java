package in.ac.iitm.guide.tools;

import java.util.List;
import java.util.regex.Matcher;
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
                // `-DskipTests=false` switches the tests on. Reading it as a bypass refused the one
                // spelling that says out loud what it wants, which is the wrong thing to punish.
                String used = segment.replace(rule[0] + "=false", "");
                if (used.contains(rule[0]) && segment.contains(rule[1])) {
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
     * <p>Every rule here is anchored to the **start** of a stripped line, and that is not
     * incidental. Twice now a rule of this shape has refused the file that was documenting or
     * testing it, because the forbidden text appeared inside a string literal: once for
     * {@code --no-verify}, once for {@code -DskipTests}. A check that cannot tell a use from a
     * mention is one people switch off, and then it protects nothing at all.
     */
    public static Verdict forContent(String path, String content) {
        if (content == null || content.isBlank() || path == null) {
            return ALLOWED;
        }
        // The debt register describes markers; the instructions quote them. Neither is a marker.
        if (path.equals("docs/tech-debt.md") || path.startsWith("docs/ai/") || path.startsWith(".claude/")) {
            return ALLOWED;
        }

        String[] lines = content.split("\r?\n");
        for (String line : lines) {
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
        return path.contains("src/test/") ? forTestContent(lines) : ALLOWED;
    }

    /** A switched-off test with no debt entry: deferred work that reports itself as green. */
    private static final Pattern DISABLED = Pattern.compile("^@Disabled\\b");

    /** The standard source of a flaky suite. */
    private static final Pattern SLEEP = Pattern.compile("^Thread\\.sleep\\s*\\(");

    private static final Pattern TEST_ANNOTATION = Pattern.compile("^@(Test|ParameterizedTest|RepeatedTest)\\b");

    /**
     * Anything that can make a test fail.
     *
     * <p>Matching {@code assert} as a bare substring is deliberate: it also catches a call to a
     * helper named {@code assertRejected}, which is where the real assertion lives in a test that
     * reads well.
     */
    private static final Pattern ASSERTION = Pattern.compile("assert|verify\\s*\\(|fail\\s*\\(|expect");

    private static final Pattern TEST_NAME = Pattern.compile("\\bvoid\\s+(\\w+)\\s*\\(");

    /** Rules that apply only under {@code src/test/}, judged per stripped line. */
    private static Verdict forTestContent(String[] lines) {
        for (String raw : lines) {
            String line = raw.strip();
            if (DISABLED.matcher(line).find() && !DEBT_REFERENCE.matcher(line).find()) {
                return new Verdict(
                        Verdict.Decision.DENY,
                        "This switches a test off with no debt entry behind it:\n\n  " + line + "\n\n"
                                + "A disabled test is deferred work that reports itself as green, which is worse "
                                + "than a deleted one: the suite still says everything passes.\n\n"
                                + "Use `@Disabled(\"DEBT-007: ...\")` with an entry in docs/tech-debt.md, or fix "
                                + "the test, or delete it and say why.");
            }
            if (SLEEP.matcher(line).find()) {
                return new Verdict(
                        Verdict.Decision.DENY,
                        "This puts a sleep in a test:\n\n  " + line + "\n\n"
                                + "It is the standard source of a suite that fails once in twenty runs, and a suite "
                                + "like that teaches everyone to re-run rather than to read the failure.\n\n"
                                + "Wait for the condition instead - Awaitility, a latch, or a clock you control "
                                + "(docs/ai/testing.md). If the code genuinely needs real time to pass, the code is "
                                + "what needs the seam, not the test.");
            }
        }
        return withoutAnAssertion(lines);
    }

    /**
     * Asks about a test that cannot fail.
     *
     * <p>An {@code ask} rather than a refusal: a test whose whole point is that a call does not
     * throw is legitimate. It should say so - {@code assertDoesNotThrow} - rather than be a method
     * that passes for ever in silence.
     */
    private static Verdict withoutAnAssertion(String[] lines) {
        int testAt = -1;
        for (int i = 0; i < lines.length; i++) {
            if (TEST_ANNOTATION.matcher(lines[i].strip()).find()) {
                if (testAt >= 0) {
                    Verdict verdict = judgeRegion(lines, testAt, i);
                    if (verdict != ALLOWED) {
                        return verdict;
                    }
                }
                testAt = i;
            }
        }
        return testAt < 0 ? ALLOWED : judgeRegion(lines, testAt, lines.length);
    }

    private static Verdict judgeRegion(String[] lines, int from, int to) {
        String name = "";
        for (int i = from; i < to; i++) {
            if (ASSERTION.matcher(lines[i]).find()) {
                return ALLOWED;
            }
            Matcher named = TEST_NAME.matcher(lines[i]);
            if (name.isEmpty() && named.find()) {
                name = named.group(1);
            }
        }
        return new Verdict(
                Verdict.Decision.ASK,
                "This test asserts nothing" + (name.isEmpty() ? "" : ": " + name) + ".\n\n"
                        + "A test with no assertion passes for as long as the code does not throw, which means it "
                        + "reports success without ever having checked anything.\n\n"
                        + "If the point is that the call does not throw, say so with `assertDoesNotThrow`. If the "
                        + "assertion is still to come, this is a red test - write the assertion first, watch it "
                        + "fail, and read the message (docs/ai/testing.md).");
    }
}
