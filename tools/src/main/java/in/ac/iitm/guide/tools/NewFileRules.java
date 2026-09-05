package in.ac.iitm.guide.tools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Questions worth asking at the moment a file is created, and only then.
 *
 * <p>Two things are being caught, and they share a moment rather than a motive.
 *
 * <p><b>Reinvention.</b> The authority split makes a new dependency the human's decision and a
 * private helper the agent's. So the cheapest path for the agent always runs away from the library
 * and towards writing it again — not through any preference for its own code, but because asking
 * costs a round trip and writing does not. A rule saying "do not reinvent" cannot outweigh that; a
 * question at the moment the file appears can.
 *
 * <p><b>Areas the human wants to be in.</b> The domain model, the schema and anything that carries
 * security are places where a decision taken quietly is expensive to undo, and a person said they
 * want to be part of them rather than to review them afterwards.
 *
 * <p>This fires on creation only. Asking again on every later edit would turn a question into
 * wallpaper, and the decision it is asking about has already been made by then.
 */
public final class NewFileRules {

    /**
     * Names that a standard library usually already answers.
     *
     * <p>Deliberately not "every new class". A check that asks about everything is one people learn
     * to click through, and then it protects nothing — the same failure the first version of
     * {@code docs-check} had when it reported subcommands named "jar" and "with".
     */
    private static final Pattern REINVENTION =
            Pattern.compile(".*(Utils?|Helper|Support|Sanitiz(er|ation)|Escaper|Encoder|Decoder|Serializer"
                    + "|Cache|Retry|Backoff|Throttle|Slug|Hash|Checksum|Uuid|RandomIds?"
                    + "|DateFormat|Formatter|Converter)\\.java$");

    /**
     * Names that carry security whatever package they land in.
     *
     * <p>{@code Token} is deliberately absent: there are no accounts here, so the word turns up in
     * a tokeniser far more often than in an authentication check, and asking the wrong question is
     * how a check earns its reputation for being noise.
     */
    private static final Pattern SECURITY_SENSITIVE =
            Pattern.compile(".*(Security|Auth|Csrf|RateLimit|Password|Crypt|Permission)\\w*\\.java$");

    /** Package fragments where the human asked to take part in the decision, not to review it. */
    private static final Map<String, String> SENSITIVE_AREAS = new LinkedHashMap<>();

    static {
        SENSITIVE_AREAS.put(
                "in/ac/iitm/guide/shared/persistence/",
                "this is the shared schema: one entity here is a table every slice reads, and the "
                        + "migration that follows is expensive to take back");
        SENSITIVE_AREAS.put(
                "in/ac/iitm/guide/shared/security/",
                "the application takes text and files from anonymous visitors, so this package is "
                        + "the part of the system an attacker meets first, and docs/ai/security.md "
                        + "already states what it has to satisfy");
        SENSITIVE_AREAS.put(
                "in/ac/iitm/guide/shared/",
                "`shared` is the one place where two people collide, so it grows by decision and "
                        + "never by convenience (docs/ai/architecture-rules.md)");
    }

    private NewFileRules() {}

    /**
     * Returns what to ask about a file that is about to be created, or {@code null} when there is
     * nothing to ask.
     *
     * @param relativePath repository-relative, forward-slashed
     * @param exists whether the file is already there — an edit is not a decision to create
     */
    public static String questionFor(String relativePath, boolean exists) {
        if (relativePath == null || exists || !relativePath.endsWith(".java")) {
            return null;
        }

        for (var area : SENSITIVE_AREAS.entrySet()) {
            if (relativePath.contains(area.getKey())) {
                return "You are creating " + relativePath + ".\n\n"
                        + "This is one of the areas the human asked to decide with you rather than read "
                        + "afterwards: " + area.getValue() + ".\n\n"
                        + "Say what you are about to add and what shape it has - the type, its fields or its "
                        + "responsibility - and wait. If this was already agreed in this conversation, say so "
                        + "and carry on.";
            }
        }

        if (SECURITY_SENSITIVE.matcher(relativePath).matches()) {
            return "You are creating " + relativePath + ", whose name says it carries security.\n\n"
                    + "Security is decided with the human here, and the rules it has to satisfy are in "
                    + "docs/ai/security.md. Two questions before the file exists: which requirement in that "
                    + "document does this implement, and is there a Spring Security or framework mechanism "
                    + "that already does it?\n\n"
                    + "Hand-written security is the kind that is wrong quietly.";
        }

        if (REINVENTION.matcher(relativePath).matches()) {
            return "You are creating " + relativePath + ", and that name is where reinvention lands.\n\n"
                    + "Before writing it, name the library that already does this - the JDK, Spring, Apache "
                    + "Commons, Guava, Tika - and say why it does not fit, or use it.\n\n"
                    + "The honest reasons to write your own are: nothing does it, the library is far larger "
                    + "than the need, or it is unmaintained. \"It is only a few lines\" is not one of them: a "
                    + "few lines is how every wheel starts, and the library has the edge cases you have not "
                    + "thought of yet.\n\n"
                    + "Adding a dependency is the human's decision (docs/ai/collaboration.md), and so is "
                    + "replacing one with your own code. Propose it and wait.";
        }
        return null;
    }
}
