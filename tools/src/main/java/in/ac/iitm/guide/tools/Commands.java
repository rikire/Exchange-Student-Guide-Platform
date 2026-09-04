package in.ac.iitm.guide.tools;

import java.util.Set;

/**
 * The commands this tool accepts, and the ones the documentation is allowed to mention.
 *
 * <p>These sets are the authority, not the switch statements: {@link Main} and {@code HookCommand}
 * check membership before dispatching, so a case added here and nowhere else is rejected loudly
 * rather than diverging quietly. {@link DocsCheck} reads the same sets, which is what lets it tell
 * a command that exists from one a document only claims exists.
 */
public final class Commands {

    public static final Set<String> TOP_LEVEL = Set.of("hook", "commit-msg", "docs-check");

    public static final Set<String> HOOK = Set.of("prompt", "guard", "stop", "note", "english", "author");

    private Commands() {}

    public static String describe(Set<String> names) {
        return String.join(", ", names.stream().sorted().toList());
    }
}
