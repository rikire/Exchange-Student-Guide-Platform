package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NewFileRulesTest {

    private static final String SLICE = "app/src/main/java/in/ac/iitm/guide/";

    private static String creating(String path) {
        return NewFileRules.questionFor(path, false);
    }

    @Test
    void a_utility_name_is_where_reinvention_lands() {
        assertNotNull(creating(SLICE + "wikilink/SlugUtils.java"));
        assertNotNull(creating(SLICE + "articleview/TextHelper.java"));
        assertNotNull(creating(SLICE + "media/HashSupport.java"));
        assertNotNull(creating(SLICE + "search/DateFormatter.java"));
    }

    @Test
    void the_question_names_a_library_rather_than_scolding() {
        String question = creating(SLICE + "contribute/StringUtils.java");

        assertTrue(question.contains("library"), question);
        assertTrue(question.contains("Commons") || question.contains("Guava"), "it should say where to look");
    }

    @Test
    void it_refuses_the_excuse_that_makes_every_wheel() {
        // "It is only a few lines" is how each one starts, so the question answers it in advance.
        assertTrue(creating(SLICE + "search/QueryEncoder.java").contains("only a few lines"));
    }

    @Test
    void the_shared_schema_is_a_decision_taken_together() {
        assertNotNull(creating(SLICE + "shared/persistence/Article.java"));
        assertTrue(creating(SLICE + "shared/persistence/Article.java").contains("table"));
    }

    @Test
    void anything_carrying_security_asks_which_requirement_it_implements() {
        String question = creating(SLICE + "shared/security/RateLimitFilter.java");

        assertNotNull(question);
        assertTrue(question.contains("security.md"), "the rules already exist; point at them: " + question);
    }

    @Test
    void a_security_name_asks_wherever_it_lands() {
        // The package is innocent; the name is not. Both routes have to reach the question.
        assertNotNull(creating(SLICE + "contribute/HoneypotAuthCheck.java"));
        assertNotNull(creating(SLICE + "media/PermissionCheck.java"));
    }

    @Test
    void the_rest_of_shared_asks_because_that_is_where_two_people_collide() {
        assertNotNull(creating(SLICE + "shared/web/PageLayout.java"));
    }

    @Test
    void ordinary_slice_code_is_not_questioned() {
        // The whole design rests on this: a check that asks about every new class is one people
        // learn to click through, and then it protects nothing.
        assertNull(creating(SLICE + "articleview/ArticleController.java"));
        assertNull(creating(SLICE + "moderate/ModerationQueue.java"));
        assertNull(creating(SLICE + "search/internal/QueryParser.java"));
        assertNull(creating(SLICE + "articleview/persistence/ArticleReadRepository.java"));
    }

    @Test
    void editing_a_file_that_exists_is_not_a_decision_to_create_it() {
        assertNull(NewFileRules.questionFor(SLICE + "shared/persistence/Article.java", true));
        assertNull(NewFileRules.questionFor(SLICE + "wikilink/SlugUtils.java", true));
    }

    @Test
    void only_java_files_are_judged() {
        assertNull(creating("docs/ai/testing.md"));
        assertNull(creating(SLICE + "shared/web/layout.html"));
        assertNull(creating(null));
    }
}
