package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What ends the turn refused: the three checks the Stop hook and CI share. The point of these tests
 * is the wiring — each check is tested on its own elsewhere; here it is shown to reach the gate.
 */
class GateTest {

    private static final String MIGRATION = "app/src/main/resources/db/migration/V6__add_column.sql";
    private static final String INTERNAL = "app/src/main/java/in/ac/iitm/guide/home/internal/Recent.java";

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

    /** A repository with one commit and generated files that match it, so that nothing is wrong yet. */
    private Repo cleanRepository() throws Exception {
        run("git", "init", "-q");
        write("docs/architecture/data-model.md", "# Data model\n");
        Repo repo = Repo.find(repoRoot.toString());
        Trace.write(repo);
        run("git", "add", "-A");
        run("git", "-c", "user.name=Test", "-c", "user.email=test@example.com", "commit", "-q", "-m", "base");
        return repo;
    }

    private static String all(Gate.Result result) {
        return String.join(" | ", result.problems());
    }

    @Test
    void a_repository_where_everything_agrees_has_nothing_to_refuse() throws Exception {
        Repo repo = cleanRepository();

        Gate.Result result = Gate.evaluate(repo);

        assertEquals(List.of(), result.problems());
        assertEquals(List.of(), result.notRun());
    }

    @Test
    void documentation_describing_something_missing_is_refused() throws Exception {
        Repo repo = cleanRepository();
        write("docs/a.md", "See [the other one](b.md).\n");

        assertTrue(all(Gate.evaluate(repo)).contains("link to a missing file: b.md"), all(Gate.evaluate(repo)));
    }

    @Test
    void a_gap_in_the_chain_from_requirement_to_test_is_refused() throws Exception {
        Repo repo = cleanRepository();
        write("docs/requirements/functional.md", "## Requirements\n\n### FR-001 — Title\n\n**Status:** in-progress\n");
        Trace.write(repo);

        assertTrue(
                all(Gate.evaluate(repo)).contains("FR-001 is in-progress and no feature file covers it"),
                all(Gate.evaluate(repo)));
    }

    @Test
    void a_generated_file_that_no_longer_matches_the_repository_is_refused() throws Exception {
        Repo repo = cleanRepository();
        write("docs/requirements/functional.md", "## Requirements\n\n### FR-001 — Title\n\n**Status:** planned\n");

        assertTrue(all(Gate.evaluate(repo)).contains("docs/traceability.md is out of date"), all(Gate.evaluate(repo)));
    }

    @Test
    void a_new_migration_with_no_word_about_the_schema_is_refused() throws Exception {
        Repo repo = cleanRepository();
        write(MIGRATION, "CREATE TABLE a (id INT);\n");

        String problems = all(Gate.evaluate(repo));

        assertTrue(problems.contains(MIGRATION), problems);
        assertTrue(problems.contains("docs/architecture/data-model.md"), problems);
    }

    @Test
    void the_same_migration_with_the_data_model_changed_beside_it_is_not() throws Exception {
        Repo repo = cleanRepository();
        write(MIGRATION, "CREATE TABLE a (id INT);\n");
        write("docs/architecture/data-model.md", "# Data model\n\nTable a.\n");

        assertFalse(all(Gate.evaluate(repo)).contains(MIGRATION), all(Gate.evaluate(repo)));
    }

    @Test
    void a_migration_added_after_the_schema_froze_with_no_adr_in_its_header_is_refused() throws Exception {
        Repo repo = cleanRepository();
        write("docs/roadmap/02-skeleton.md", "# Phase 2\n\n**Schema frozen:** 2020-01-01\n");
        write(MIGRATION, "CREATE TABLE a (id INT);\n");
        write("docs/architecture/data-model.md", "# Data model\n\nTable a.\n");

        assertTrue(all(Gate.evaluate(repo)).contains("names no ADR in its header"), all(Gate.evaluate(repo)));
    }

    @Test
    void a_change_that_only_warns_does_not_stop_the_turn() throws Exception {
        Repo repo = cleanRepository();
        write(INTERNAL, "class Recent {}\n");

        assertEquals(List.of(), Gate.evaluate(repo).problems());
    }

    @Test
    void where_git_cannot_say_what_changed_the_check_is_reported_as_not_run_and_does_not_refuse() throws Exception {
        Files.createDirectories(repoRoot.resolve(".git"));
        Repo repo = Repo.find(repoRoot.toString());
        Trace.write(repo);

        Gate.Result result = Gate.evaluate(repo);

        assertEquals(List.of(), result.problems());
        assertEquals(1, result.notRun().size(), result.notRun().toString());
        assertTrue(result.notRun().get(0).contains("docs-sync"), result.notRun().toString());
    }
}
