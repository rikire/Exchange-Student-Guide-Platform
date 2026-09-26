package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * After the freeze the schema changes by agreement only, and the agreement is an ADR that the
 * migration names in its header. Migrations are dated by the commit that added them, so the tests
 * build a real history with chosen dates.
 */
class SchemaFreezeTest {

    private static final String MIGRATIONS = "app/src/main/resources/db/migration/";
    private static final String ROADMAP = "docs/roadmap/02-skeleton.md";
    private static final Clock LATER = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path repoRoot;

    private Repo repo;

    @BeforeEach
    void setUp() throws Exception {
        git(Map.of(), "init", "-q");
        repo = Repo.find(repoRoot.toString());
    }

    private void git(Map<String, String> environment, String... command) throws Exception {
        String[] full = new String[command.length + 5];
        full[0] = "git";
        full[1] = "-c";
        full[2] = "user.name=Test";
        full[3] = "-c";
        full[4] = "user.email=test@example.com";
        System.arraycopy(command, 0, full, 5, command.length);
        ProcessBuilder builder =
                new ProcessBuilder(full).directory(repoRoot.toFile()).redirectErrorStream(true);
        builder.environment().putAll(environment);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes());
        assertEquals(0, process.waitFor(), String.join(" ", command) + ": " + output);
    }

    private void write(String relative, String content) throws IOException {
        Path file = repoRoot.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private void frozenOn(String date) throws IOException {
        write(ROADMAP, "# Phase 2\n\n**Schema frozen:** " + date + "\n");
    }

    /** Commits everything pending as of the given day. */
    private void commitOn(String date) throws Exception {
        git(Map.of(), "add", "-A");
        String when = date + "T12:00:00+00:00";
        git(Map.of("GIT_AUTHOR_DATE", when, "GIT_COMMITTER_DATE", when), "commit", "-q", "-m", "commit of " + date);
    }

    private void adr(String number) throws IOException {
        write("docs/architecture/adr/ADR-" + number + "-a-decision.md", "# ADR-" + number + "\n");
    }

    private String problems() throws IOException {
        return String.join(" | ", SchemaFreeze.check(repo, LATER));
    }

    @Test
    void a_schema_that_has_not_been_frozen_asks_nothing_of_a_migration() throws Exception {
        write(ROADMAP, "# Phase 2\n");
        write(MIGRATIONS + "V7__late.sql", "CREATE TABLE a (id INT);\n");
        commitOn("2026-10-10");

        assertEquals(List.of(), SchemaFreeze.check(repo, LATER));
    }

    @Test
    void a_migration_added_before_the_freeze_needs_no_adr() throws Exception {
        write(MIGRATIONS + "V1__early.sql", "CREATE TABLE a (id INT);\n");
        commitOn("2026-09-10");
        frozenOn("2026-09-20");
        commitOn("2026-09-20");

        assertEquals(List.of(), SchemaFreeze.check(repo, LATER));
    }

    @Test
    void a_migration_added_on_the_day_of_the_freeze_is_still_inside_it() throws Exception {
        frozenOn("2026-09-20");
        write(MIGRATIONS + "V1__same_day.sql", "CREATE TABLE a (id INT);\n");
        commitOn("2026-09-20");

        assertEquals(List.of(), SchemaFreeze.check(repo, LATER));
    }

    @Test
    void a_migration_added_after_the_freeze_without_an_adr_in_its_header_is_a_problem() throws Exception {
        frozenOn("2026-09-20");
        write(MIGRATIONS + "V7__late.sql", "CREATE TABLE a (id INT);\n");
        commitOn("2026-09-25");

        assertTrue(
                problems().contains(MIGRATIONS + "V7__late.sql was added after the schema froze on 2026-09-20"),
                problems());
        assertTrue(problems().contains("-- adr: ADR-"), problems());
    }

    @Test
    void the_same_migration_naming_an_adr_that_exists_is_fine() throws Exception {
        frozenOn("2026-09-20");
        adr("0013");
        write(MIGRATIONS + "V7__late.sql", "-- adr: ADR-0013\n-- trace: FR-001\nCREATE TABLE a (id INT);\n");
        commitOn("2026-09-25");

        assertEquals(List.of(), SchemaFreeze.check(repo, LATER));
    }

    @Test
    void naming_an_adr_that_does_not_exist_is_a_problem() throws Exception {
        frozenOn("2026-09-20");
        write(MIGRATIONS + "V7__late.sql", "-- adr: ADR-0099\nCREATE TABLE a (id INT);\n");
        commitOn("2026-09-25");

        assertTrue(problems().contains("names ADR-0099, which does not exist"), problems());
    }

    @Test
    void an_adr_mentioned_in_the_body_of_the_migration_and_not_its_header_does_not_count() throws Exception {
        frozenOn("2026-09-20");
        adr("0013");
        write(MIGRATIONS + "V7__late.sql", "-- trace: FR-001\nCREATE TABLE a (id INT);\n-- adr: ADR-0013\n");
        commitOn("2026-09-25");

        assertTrue(problems().contains("V7__late.sql was added after the schema froze"), problems());
    }

    @Test
    void a_migration_git_has_not_seen_yet_is_added_today_and_today_is_after_the_freeze() throws Exception {
        frozenOn("2026-09-20");
        commitOn("2026-09-20");
        write(MIGRATIONS + "V7__new.sql", "CREATE TABLE a (id INT);\n");

        assertTrue(problems().contains("V7__new.sql was added after the schema froze"), problems());
    }

    @Test
    void a_date_that_cannot_be_read_is_a_problem_and_not_a_silent_thaw() throws Exception {
        write(ROADMAP, "# Phase 2\n\n**Schema frozen:** soon\n");

        assertTrue(problems().contains("cannot be read as a date"), problems());
    }

    @Test
    void only_sql_files_are_migrations() throws Exception {
        frozenOn("2026-09-20");
        write(MIGRATIONS + "README.md", "Notes.\n");
        commitOn("2026-09-25");

        assertEquals(List.of(), SchemaFreeze.check(repo, LATER));
    }
}
