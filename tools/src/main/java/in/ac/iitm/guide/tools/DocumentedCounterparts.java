package in.ac.iitm.guide.tools;

/**
 * The mapping in docs/ai/docs-sync.md, made to fire.
 *
 * <p>That document names this exact mechanism — "the PostToolUse hook notices a change in a tracked
 * area and says what must now be updated" — and the hook was never wired, so the rule was followed
 * by remembering to read a table. This is the table.
 *
 * <p>The rules are deliberately few, and that is the document's own reasoning: a gate that fires on
 * every refactor gets worked around, and then it protects nothing. Only the schema, the route
 * contract and the slice boundary are tracked.
 */
public final class DocumentedCounterparts {

    private static final String MAIN = "app/src/main/java/";
    private static final String SLICES = "app/src/main/java/in/ac/iitm/guide/";
    private static final String MIGRATIONS = "app/src/main/resources/db/migration/";
    private static final String TEMPLATES = "app/src/main/resources/templates/";

    private static final String DATA_MODEL = "docs/architecture/data-model.md";
    private static final String ROUTES = "docs/architecture/ui-routes.md";
    private static final String OVERVIEW = "docs/architecture/overview.md";

    /** One tracked area: a stable key to remember it by, and what has to change with it. */
    public record Rule(String key, String update) {}

    private DocumentedCounterparts() {}

    /** The tracked area this path falls in, or null when it is not one. */
    public static Rule forPath(String relativePath) {
        if (relativePath == null || relativePath.endsWith(".md")) {
            // Prose beside a migration changes neither the schema nor the contract.
            return null;
        }
        if (relativePath.startsWith(MIGRATIONS)) {
            return new Rule("migration", DATA_MODEL + " (or the ERD source)");
        }
        if (relativePath.startsWith(TEMPLATES)) {
            return new Rule("routes", ROUTES);
        }
        if (relativePath.equals("pom.xml")
                || relativePath.equals("app/pom.xml")
                || relativePath.equals("tools/pom.xml")) {
            return new Rule("build", "an ADR under docs/architecture/adr/, or " + OVERVIEW);
        }
        if (!relativePath.startsWith(SLICES) || !relativePath.endsWith(".java")) {
            return null;
        }

        String withinBasePackage = relativePath.substring(SLICES.length());
        if (withinBasePackage.startsWith("shared/persistence/")) {
            return new Rule("shared-persistence", DATA_MODEL + " (or the ERD source)");
        }
        if (withinBasePackage.startsWith("shared/")) {
            // The rest of shared is layout and configuration: real, but not a contract another
            // slice depends on, and tracking it would make this fire on ordinary work.
            return null;
        }
        if (relativePath.endsWith("Controller.java")) {
            return new Rule("routes", ROUTES);
        }
        // Directly in the slice package is what other slices are allowed to depend on, so it is the
        // boundary; anything nested is that slice's own business.
        boolean publishedType = withinBasePackage.indexOf('/') == withinBasePackage.lastIndexOf('/');
        return publishedType
                ? new Rule("boundary", OVERVIEW + ", an ADR, or the feature file")
                : new Rule("feature", "the feature file — its code, tests and status fields");
    }

    /** A stable name for the tracked area, so a reminder can fire once per area per session. */
    public static String keyFor(String relativePath) {
        Rule rule = forPath(relativePath);
        return rule == null ? null : rule.key();
    }

    /** What to say when a tracked area changes, or null when nothing is tracked here. */
    public static String reminderFor(String relativePath) {
        Rule rule = forPath(relativePath);
        if (rule == null) {
            return null;
        }
        return relativePath + " is a tracked area. What must change with it, in the same turn:\n"
                + "  " + rule.update() + "\n\n"
                + "Update it in substance — what the change means, not a line saying it happened. If it is\n"
                + "unclear how the behaviour described has altered, stop and ask rather than rewriting at\n"
                + "random. The mapping and that reasoning are in docs/ai/docs-sync.md.\n\n"
                + "Documentation prose: every sentence carries a fact, a decision, or a consequence of one.";
    }
}
