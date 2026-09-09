package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TestFirstRuleTest {

    private static final String MAIN = "app/src/main/java/in/ac/iitm/guide/";
    private static final String TEST = "app/src/test/java/in/ac/iitm/guide/";

    /** Creating the file, with no test beside it — the moment the rule is about. */
    private static String creating(String path) {
        return TestFirstRule.questionFor(path, false, false);
    }

    @Test
    void a_production_class_maps_to_the_test_that_should_already_exist() {
        assertEquals(
                TEST + "search/SearchServiceTest.java", TestFirstRule.testPathFor(MAIN + "search/SearchService.java"));
        assertEquals(
                TEST + "wikilink/LinkParserTest.java", TestFirstRule.testPathFor(MAIN + "wikilink/LinkParser.java"));
    }

    @Test
    void creating_production_code_with_no_test_is_asked_about() {
        assertNotNull(creating(MAIN + "moderate/ModerationQueue.java"));
    }

    @Test
    void the_question_names_the_test_that_is_missing() {
        String question = creating(MAIN + "moderate/ModerationQueue.java");

        // A rule that says "write a test first" and leaves the reader to work out which file
        // is a rule that gets skipped. It has to name the path it expects.
        assertTrue(question.contains(TEST + "moderate/ModerationQueueTest.java"), question);
    }

    @Test
    void it_goes_quiet_once_the_test_exists() {
        assertNull(TestFirstRule.questionFor(MAIN + "moderate/ModerationQueue.java", false, true));
    }

    @Test
    void editing_a_file_that_already_exists_is_not_the_moment_for_this() {
        // The decision to write it without a test was made when it was created. Asking on every
        // later edit turns the question into wallpaper — the reasoning NewFileRules already uses.
        assertNull(TestFirstRule.questionFor(MAIN + "moderate/ModerationQueue.java", true, false));
    }

    @Test
    void a_package_marker_has_nothing_to_test() {
        assertNull(creating(MAIN + "moderate/package-info.java"));
    }

    @Test
    void writing_the_test_itself_is_the_thing_being_asked_for() {
        assertNull(creating(TEST + "moderate/ModerationQueueTest.java"));
        assertNull(TestFirstRule.testPathFor(TEST + "moderate/ModerationQueueTest.java"));
    }

    @Test
    void it_stays_out_of_everything_that_is_not_the_application() {
        assertNull(creating("tools/src/main/java/in/ac/iitm/guide/tools/Journal.java"));
        assertNull(creating("docs/ai/workflow.md"));
        assertNull(creating("app/src/main/resources/templates/article.html"));
    }
}
