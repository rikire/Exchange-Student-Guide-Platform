package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The table in docs/ai/docs-sync.md, held to its word: a change in a tracked area with none of the
 * documents that describe it beside it is refused, and only the contract, the schema and the slice
 * boundary refuse — the rest are reported and let through.
 */
class DocsSyncTest {

    private static final String SLICE = "app/src/main/java/in/ac/iitm/guide/";
    private static final String MIGRATION = "app/src/main/resources/db/migration/V6__add_column.sql";
    private static final String ENTITY = SLICE + "shared/persistence/Article.java";
    private static final String CONTROLLER = SLICE + "home/web/HomeController.java";
    private static final String TEMPLATE = "app/src/main/resources/templates/home/Landing.html";
    private static final String PUBLISHED_TYPE = SLICE + "wikilink/TitleResolver.java";
    private static final String INTERNAL = SLICE + "home/internal/RecentArticles.java";

    private static List<DocsSync.Finding> findings(String... changed) {
        return DocsSync.findings(Set.of(changed));
    }

    private static List<DocsSync.Finding> blocking(String... changed) {
        return findings(changed).stream().filter(DocsSync.Finding::blocks).toList();
    }

    // --- what blocks ---

    @Test
    void a_migration_changed_alone_blocks_and_names_the_file_and_the_documents_that_would_satisfy_it() {
        List<DocsSync.Finding> found = blocking(MIGRATION);

        assertEquals(1, found.size());
        assertEquals(List.of(MIGRATION), found.get(0).files());
        assertTrue(
                found.get(0).update().contains("docs/architecture/data-model.md"),
                found.get(0).update());
    }

    @ParameterizedTest
    @ValueSource(strings = {"docs/architecture/data-model.md", "docs/diagrams/src/erd.puml"})
    void a_migration_is_satisfied_by_the_data_model_or_the_erd_source(String document) {
        assertEquals(List.of(), findings(MIGRATION, document));
    }

    @Test
    void a_shared_persistence_entity_changed_alone_blocks_on_the_data_model() {
        assertEquals(1, blocking(ENTITY).size());
        assertEquals(List.of(), findings(ENTITY, "docs/architecture/data-model.md"));
    }

    @Test
    void a_controller_changed_alone_blocks_on_the_route_contract() {
        assertEquals(1, blocking(CONTROLLER).size());
        assertEquals(List.of(), findings(CONTROLLER, "docs/architecture/ui-routes.md"));
    }

    @Test
    void a_template_changed_alone_blocks_on_the_route_contract() {
        assertEquals(1, blocking(TEMPLATE).size());
        assertEquals(List.of(), findings(TEMPLATE, "docs/architecture/ui-routes.md"));
    }

    @Test
    void a_controller_and_a_template_are_one_finding_listing_both_files() {
        List<DocsSync.Finding> found = blocking(CONTROLLER, TEMPLATE);

        assertEquals(1, found.size());
        assertEquals(List.of(CONTROLLER, TEMPLATE), found.get(0).files());
    }

    @Test
    void a_published_type_changed_alone_blocks() {
        assertEquals(1, blocking(PUBLISHED_TYPE).size());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "docs/architecture/overview.md",
                "docs/architecture/adr/ADR-0013-anything.md",
                "docs/features/FEAT-005-anything.md"
            })
    void a_published_type_is_satisfied_by_the_overview_an_adr_or_a_feature_file(String document) {
        assertEquals(List.of(), findings(PUBLISHED_TYPE, document));
    }

    // --- what does not ---

    @Test
    void a_slices_internals_changed_alone_are_reported_and_do_not_block() {
        List<DocsSync.Finding> found = findings(INTERNAL);

        assertEquals(1, found.size());
        assertFalse(found.get(0).blocks());
        assertEquals(List.of(), findings(INTERNAL, "docs/features/FEAT-005-anything.md"));
    }

    @Test
    void a_pom_changed_alone_is_reported_and_does_not_block() {
        List<DocsSync.Finding> found = findings("app/pom.xml");

        assertEquals(1, found.size());
        assertFalse(found.get(0).blocks());
    }

    @Test
    void prose_beside_a_migration_changes_neither_the_schema_nor_the_contract() {
        assertEquals(List.of(), findings("app/src/main/resources/db/migration/README.md"));
    }

    @Test
    void a_test_changed_alone_is_not_a_tracked_area() {
        assertEquals(List.of(), findings("app/src/test/java/in/ac/iitm/guide/home/HomeControllerTest.java"));
    }

    @Test
    void documentation_that_does_not_describe_the_area_does_not_satisfy_it() {
        assertEquals(
                1,
                blocking(MIGRATION, "docs/roadmap/02-skeleton.md", "docs/architecture/ui-routes.md")
                        .size());
    }

    @Test
    void an_area_satisfied_does_not_excuse_another_one() {
        List<DocsSync.Finding> found = blocking(MIGRATION, CONTROLLER, "docs/architecture/data-model.md");

        assertEquals(1, found.size());
        assertEquals(List.of(CONTROLLER), found.get(0).files());
    }

    // --- reading the change from git ---

    @TempDir
    Path repoRoot;

    private void run(String... command) throws Exception {
        Process process = new ProcessBuilder(command)
                .directory(repoRoot.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        assertEquals(0, process.waitFor(), String.join(" ", command) + ": " + output);
    }

    private void write(String relative, String content) throws IOException {
        Path file = repoRoot.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    /** A repository whose only commit holds a migration and the data model that describes it. */
    private Repo committedRepository() throws Exception {
        run("git", "init", "-q");
        write(MIGRATION, "CREATE TABLE a (id INT);\n");
        write("docs/architecture/data-model.md", "# Data model\n");
        run("git", "add", "-A");
        run("git", "-c", "user.name=Test", "-c", "user.email=test@example.com", "commit", "-q", "-m", "base");
        return Repo.find(repoRoot.toString());
    }

    @Test
    void a_tracked_file_edited_since_the_ref_is_a_change() throws Exception {
        Repo repo = committedRepository();
        write(MIGRATION, "CREATE TABLE a (id INT, name VARCHAR(10));\n");

        assertEquals(1, DocsSync.check(repo, "HEAD").size());
    }

    @Test
    void a_new_file_git_has_not_seen_yet_is_a_change_too() throws Exception {
        Repo repo = committedRepository();
        write("app/src/main/resources/db/migration/V7__another.sql", "CREATE TABLE b (id INT);\n");

        assertEquals(1, DocsSync.check(repo, "HEAD").size());
    }

    @Test
    void the_document_edited_beside_the_change_satisfies_it() throws Exception {
        Repo repo = committedRepository();
        write(MIGRATION, "CREATE TABLE a (id INT, name VARCHAR(10));\n");
        write("docs/architecture/data-model.md", "# Data model\n\nA name column.\n");

        assertEquals(List.of(), DocsSync.check(repo, "HEAD"));
    }

    @Test
    void nothing_changed_since_the_ref_is_nothing_to_report() throws Exception {
        Repo repo = committedRepository();

        assertEquals(List.of(), DocsSync.check(repo, "HEAD"));
    }

    @Test
    void a_ref_git_does_not_know_is_an_error_and_not_a_clean_bill() throws Exception {
        Repo repo = committedRepository();

        assertThrows(IOException.class, () -> DocsSync.check(repo, "no-such-ref"));
    }
}
