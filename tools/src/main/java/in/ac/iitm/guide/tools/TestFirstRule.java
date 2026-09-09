package in.ac.iitm.guide.tools;

/**
 * Test-before-code, asked at the one moment it can still be true.
 *
 * <p>Rule 4 orders the cycle interfaces → red test → implementation, and docs/ai/workflow.md spends
 * 189 lines on it. Neither had a mechanism: the rule was read once at session start and then
 * competed with everything that arrived after it. Creating the production class is the last moment
 * the order can still be followed, so that is where the question belongs.
 *
 * <p>It asks rather than denies. There are honest exceptions — a class split out of one already
 * covered, a spike about to be deleted — and a gate with no way to answer it is a gate that gets
 * switched off, which protects nothing.
 */
public final class TestFirstRule {

    private static final String MAIN = "app/src/main/java/";
    private static final String TESTS = "app/src/test/java/";

    private TestFirstRule() {}

    /**
     * The test a production class is expected to have, or null when the path is not one.
     *
     * <p>Exposed rather than kept private because the caller needs it to look on disk, and the
     * question needs to name it. One definition, so the two cannot drift apart.
     */
    public static String testPathFor(String relativePath) {
        if (relativePath == null || !relativePath.startsWith(MAIN) || !relativePath.endsWith(".java")) {
            return null;
        }
        // A package marker carries annotations and Javadoc, and there is nothing in it to assert.
        if (relativePath.endsWith("/package-info.java")) {
            return null;
        }
        String withinSources = relativePath.substring(MAIN.length());
        String withoutSuffix = withinSources.substring(0, withinSources.length() - ".java".length());
        return TESTS + withoutSuffix + "Test.java";
    }

    /**
     * The question to put before a production class is created without its test.
     *
     * @param alreadyThere whether the file exists — an edit is not the moment, the decision was
     *     already taken when it was created, and asking again would make the question wallpaper
     * @param testExists whether the expected test is on disk
     * @return the question, or null when there is nothing to ask
     */
    public static String questionFor(String relativePath, boolean alreadyThere, boolean testExists) {
        if (alreadyThere || testExists) {
            return null;
        }
        String testPath = testPathFor(relativePath);
        if (testPath == null) {
            return null;
        }
        return "This is production code, and the test that should have been written first is not there:\n"
                + "  " + testPath + "\n\n"
                + "Rule 4 is interfaces → red test → minimal implementation (docs/ai/workflow.md). Writing the\n"
                + "implementation first does not just reorder the work: the test then gets written against the\n"
                + "code that exists rather than against the behaviour that was wanted, and it passes for that\n"
                + "reason instead of because the behaviour is right.\n\n"
                + "Write the failing test first, or say which exception applies — a class split out of one that\n"
                + "is already covered, or a spike that is going to be deleted.";
    }
}
