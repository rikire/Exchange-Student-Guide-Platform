package in.ac.iitm.guide.tools;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Files that belong to the human's decision space.
 *
 * <p>The written rule "requirements and architecture are decided by the human" is not enough on its
 * own: an instruction can go unread. Editing one of these paths asks the human every time, and the
 * assistant is expected to describe the change and wait rather than to push it through.
 *
 * <p>Feature files and the technical debt register are deliberately absent: the assistant keeps
 * those itself, and asking about them would turn the guard into noise.
 */
public final class ProtectedPaths {

    private static final Map<String, String> RULES = new LinkedHashMap<>();

    /**
     * The one place a fact about the outside world enters this repository.
     *
     * <p>So the question asks for evidence rather than for permission: a version recalled rather
     * than checked is indistinguishable from one that exists, right up to the build failing.
     */
    private static final String DEPENDENCY = "a dependency or a version is a decision the human makes, and it needs "
            + "evidence rather than permission: that the artefact and version exist on Maven Central, that it is "
            + "still maintained, and what its licence is — checked, not recalled";

    static {
        RULES.put("docs/requirements/", "requirements and constraints are agreed with the human");
        RULES.put("docs/architecture/adr/", "an ADR records a decision that is expensive to reverse");
        RULES.put("docs/architecture/data-model.md", "the database schema is a human decision");
        RULES.put("docs/architecture/ui-routes.md", "the route contract is a human decision");
        RULES.put("app/src/main/resources/db/migration/", "a migration changes the schema of a running system");
        RULES.put("docs/stakeholder/", "this records what the stakeholder actually said");
        RULES.put("docs/course/", "these documents are submitted for grading");
        RULES.put("docs/team/members.yml", "team identity drives contribution attribution");
        RULES.put("pom.xml", DEPENDENCY);
        RULES.put("app/pom.xml", DEPENDENCY);
        RULES.put("tools/pom.xml", DEPENDENCY);
        RULES.put("CLAUDE.md", "these are the assistant's own instructions");
        RULES.put("docs/ai/", "these are the assistant's own instructions");
        RULES.put(".claude/", "these are the assistant's own instructions");
    }

    private ProtectedPaths() {}

    /** Returns why the path is protected, or {@code null} when it is not. */
    public static String reasonFor(String relativePath) {
        if (relativePath == null) {
            return null;
        }
        // Journal entries are written by the hooks themselves, so they must not trigger the guard.
        // The README beside them is not an entry: it is an instruction file, and the blanket
        // exemption was quietly letting it be edited without asking.
        if (relativePath.startsWith("docs/ai/journal/") && !relativePath.equals("docs/ai/journal/README.md")) {
            return null;
        }
        for (var rule : RULES.entrySet()) {
            String pattern = rule.getKey();
            boolean matches = pattern.endsWith("/") ? relativePath.startsWith(pattern) : relativePath.equals(pattern);
            if (matches) {
                return rule.getValue();
            }
        }
        return null;
    }
}
