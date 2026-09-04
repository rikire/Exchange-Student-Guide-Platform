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

    static {
        RULES.put("docs/requirements/", "requirements and constraints are agreed with the human");
        RULES.put("docs/architecture/adr/", "an ADR records a decision that is expensive to reverse");
        RULES.put("docs/architecture/data-model.md", "the database schema is a human decision");
        RULES.put("docs/architecture/ui-routes.md", "the route contract is a human decision");
        RULES.put("app/src/main/resources/db/migration/", "a migration changes the schema of a running system");
        RULES.put("docs/stakeholder/", "this records what the stakeholder actually said");
        RULES.put("docs/course/", "these documents are submitted for grading");
        RULES.put("docs/team/members.yml", "team identity drives contribution attribution");
        RULES.put("pom.xml", "a dependency or a version is a decision the human makes");
        RULES.put("app/pom.xml", "a dependency or a version is a decision the human makes");
        RULES.put("tools/pom.xml", "a dependency or a version is a decision the human makes");
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
        // The journal is written by the hooks themselves, so it must not trigger the guard.
        if (relativePath.startsWith("docs/ai/journal/")) {
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
