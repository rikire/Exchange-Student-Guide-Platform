package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the commit message convention.
 *
 * <p>The point is not tidiness. {@code feat} and {@code fix} are the commits that change observable
 * behaviour, so the git history has to show which commit delivered which requirement — that is what
 * the mid-demo asks for when it wants evidence of work across weeks.
 */
public final class CommitMessageCheck {

    private static final Set<String> TYPES =
            Set.of("feat", "fix", "docs", "chore", "refactor", "test", "perf", "build", "ci", "style", "revert");

    private static final Pattern HEADER =
            Pattern.compile("^(?<type>[a-z]+)(?:\\((?<scope>[^)]+)\\))?: (?<subject>.+)$");
    private static final Pattern REQUIREMENT_REF =
            Pattern.compile("\\[(?:FR|NFR|CON)-\\d{3}(?:, ?(?:FR|NFR|CON)-\\d{3})*]");
    private static final Pattern FEATURE_SCOPE = Pattern.compile("^FEAT-\\d{3}$");
    private static final Pattern DEBT_SCOPE = Pattern.compile("^DEBT-\\d{3}$");

    private static final int MAX_HEADER_LENGTH = 100;

    private CommitMessageCheck() {}

    /** Returns the problems found; an empty list means the message is fine. */
    public static List<String> check(Path messageFile) throws IOException {
        String raw = Files.readString(messageFile);
        String header = raw.lines()
                .filter(line -> !line.startsWith("#"))
                .findFirst()
                .orElse("")
                .strip();

        if (header.isEmpty()) {
            return List.of("the commit message is empty");
        }
        if (header.length() > MAX_HEADER_LENGTH) {
            return List.of("the header is " + header.length() + " characters, the limit is " + MAX_HEADER_LENGTH);
        }
        if (header.endsWith(".")) {
            return List.of("the header must not end with a period");
        }

        Matcher matcher = HEADER.matcher(header);
        if (!matcher.matches()) {
            return List.of("expected `<type>(<scope>): <subject>`, got: " + header);
        }

        String type = matcher.group("type");
        String scope = matcher.group("scope");
        if (!TYPES.contains(type)) {
            return List.of("unknown type `" + type + "`; allowed: "
                    + String.join(", ", TYPES.stream().sorted().toList()));
        }

        // Only behaviour-changing commits carry the full ceremony; requiring it everywhere would
        // make the rule something people work around rather than follow.
        if (type.equals("feat") || type.equals("fix")) {
            if (scope == null || !FEATURE_SCOPE.matcher(scope).matches()) {
                return List.of("`" + type + "` needs a scope of the form `FEAT-XXX`, got: " + scope);
            }
            if (!REQUIREMENT_REF.matcher(header).find()) {
                return List.of("`" + type + "` must reference the requirement it delivers, e.g. `[FR-012]`");
            }
        }
        if (type.equals("refactor")
                && scope != null
                && scope.startsWith("DEBT")
                && !DEBT_SCOPE.matcher(scope).matches()) {
            return List.of("a debt-paying commit uses the scope `DEBT-XXX`, got: " + scope);
        }
        return List.of();
    }
}
