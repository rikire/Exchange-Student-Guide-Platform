package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The mapping is not invented here: it is the table in docs/ai/docs-sync.md, which also specifies
 * this very mechanism ("the PostToolUse hook notices a change in a tracked area") and was never
 * built. These tests are that table, read back.
 */
class DocumentedCounterpartsTest {

    private static final String MAIN = "app/src/main/java/in/ac/iitm/guide/";

    @Test
    void a_migration_points_at_the_data_model() {
        String reminder = DocumentedCounterparts.reminderFor("app/src/main/resources/db/migration/V1__articles.sql");

        assertNotNull(reminder);
        assertTrue(reminder.contains("docs/architecture/data-model.md"), reminder);
    }

    @Test
    void the_shared_persistence_layer_points_at_the_data_model() {
        assertTrue(DocumentedCounterparts.reminderFor(MAIN + "shared/persistence/Article.java")
                .contains("docs/architecture/data-model.md"));
    }

    @Test
    void a_controller_and_a_template_both_point_at_the_route_contract() {
        assertTrue(DocumentedCounterparts.reminderFor(MAIN + "search/SearchController.java")
                .contains("docs/architecture/ui-routes.md"));
        assertTrue(DocumentedCounterparts.reminderFor("app/src/main/resources/templates/article.html")
                .contains("docs/architecture/ui-routes.md"));
    }

    @Test
    void a_published_type_in_a_slice_points_at_the_boundary_documents() {
        // Directly in the slice package is what other slices may depend on, so it is the contract.
        String reminder = DocumentedCounterparts.reminderFor(MAIN + "search/ArticleSearch.java");

        assertNotNull(reminder);
        assertTrue(reminder.contains("docs/architecture/overview.md"), reminder);
    }

    @Test
    void a_slices_internals_point_at_the_feature_file() {
        String reminder = DocumentedCounterparts.reminderFor(MAIN + "search/internal/QueryParser.java");

        assertNotNull(reminder);
        assertTrue(reminder.contains("feature file"), reminder);
    }

    @Test
    void the_build_file_points_at_a_decision_record() {
        assertNotNull(DocumentedCounterparts.reminderFor("pom.xml"));
    }

    @Test
    void prose_beside_a_tracked_file_changes_neither_schema_nor_contract() {
        // docs-sync.md: "The rules look only at files with substance."
        assertNull(DocumentedCounterparts.reminderFor("app/src/main/resources/db/migration/README.md"));
    }

    @Test
    void it_stays_silent_everywhere_else() {
        // Few rules on purpose: a gate that fires on every refactor gets worked around.
        assertNull(DocumentedCounterparts.reminderFor("tools/src/main/java/in/ac/iitm/guide/tools/Journal.java"));
        assertNull(DocumentedCounterparts.reminderFor("docs/ai/README.md"));
        assertNull(DocumentedCounterparts.reminderFor("app/src/test/java/in/ac/iitm/guide/search/SearchTest.java"));
    }

    @Test
    void the_reminder_says_which_document_rather_than_update_the_docs() {
        String reminder = DocumentedCounterparts.reminderFor(MAIN + "search/SearchController.java");

        assertTrue(reminder.contains("same turn"), reminder);
        assertTrue(reminder.contains("docs/ai/docs-sync.md"), reminder);
    }

    @Test
    void every_tracked_area_has_a_key_so_a_reminder_can_fire_once_per_session() {
        assertNotNull(DocumentedCounterparts.keyFor("app/src/main/resources/db/migration/V1__articles.sql"));
        assertNull(DocumentedCounterparts.keyFor("docs/ai/README.md"));
    }
}
