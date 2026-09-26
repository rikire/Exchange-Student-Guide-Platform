package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Ownership is measured, not assigned, and the measurement has one trap: the journal commits the
 * Stop hook writes. These tests build history by hand so that the trap can be set on purpose.
 */
class OwnershipTest {

    private static final String BASE = "app/src/main/java/in/ac/iitm/guide/";
    private static final List<String> SLICES = List.of("home", "search", "shared");

    @TempDir
    Path repoRoot;

    private Members members;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(repoRoot.resolve(".git"));
        Path registry = repoRoot.resolve("docs/team/members.yml");
        Files.createDirectories(registry.getParent());
        Files.writeString(
                registry,
                """
                members:
                  - id: anna
                    name: Anna A
                    emails:
                      - anna@example.com
                      - anna@second.example.com
                  - id: boris
                    name: Boris B
                    emails:
                      - boris@example.com
                """);
        members = Members.load(Repo.find(repoRoot.toString()));
    }

    private static History.Commit commit(String email, String subject, History.Change... changes) {
        return new History.Commit(
                "sha-" + Math.abs((email + subject).hashCode()), email, "2026-W38", subject, List.of(changes));
    }

    private static History.Change change(String path, int added, int deleted) {
        return new History.Change(path, added, deleted);
    }

    private String table(History.Commit... commits) {
        return Ownership.build(List.of(commits), members, SLICES);
    }

    /** The cells of the row that starts with the first cell, in the section that starts with the heading. */
    private static List<String> row(String text, String heading, String first) {
        int start = text.indexOf("## " + heading);
        assertTrue(start >= 0, "no section '" + heading + "' in:\n" + text);
        String rest = text.substring(start + 3);
        int next = rest.indexOf("\n## ");
        String section = next < 0 ? rest : rest.substring(0, next);
        return section.lines()
                .filter(line -> line.startsWith("| " + first + " |"))
                .findFirst()
                .map(line -> Arrays.stream(line.substring(1).split("\\|", -1))
                        .map(String::strip)
                        .toList())
                .orElseThrow(() -> new AssertionError("no row '" + first + "' in:\n" + text));
    }

    // --- which slice a path belongs to ---

    @Test
    void code_and_tests_of_a_slice_belong_to_it() {
        assertEquals("home", Ownership.slicePathOf(BASE + "home/web/HomeController.java"));
        assertEquals("home", Ownership.slicePathOf("app/src/test/java/in/ac/iitm/guide/home/HomeControllerTest.java"));
    }

    @Test
    void a_slices_templates_belong_to_it() {
        assertEquals("home", Ownership.slicePathOf("app/src/main/resources/templates/home/Landing.html"));
    }

    @Test
    void migrations_belong_to_shared_because_the_schema_is_shared() {
        assertEquals("shared", Ownership.slicePathOf("app/src/main/resources/db/migration/V1__create.sql"));
    }

    @Test
    void a_file_directly_in_the_base_package_belongs_to_no_slice() {
        assertNull(Ownership.slicePathOf(BASE + "GuideApplication.java"));
        assertNull(Ownership.slicePathOf("app/src/test/java/in/ac/iitm/guide/TemplateTokensTest.java"));
    }

    @Test
    void documentation_and_tooling_belong_to_no_slice() {
        assertNull(Ownership.slicePathOf("docs/roadmap/02-skeleton.md"));
        assertNull(Ownership.slicePathOf("tools/src/main/java/in/ac/iitm/guide/tools/Main.java"));
    }

    // --- the table ---

    @Test
    void each_member_gets_the_commits_and_lines_they_authored_in_a_slice() {
        String text = table(
                commit("anna@example.com", "feat: a", change(BASE + "home/A.java", 10, 2)),
                commit("anna@example.com", "feat: b", change(BASE + "home/B.java", 5, 0)),
                commit("boris@example.com", "fix: c", change(BASE + "home/A.java", 1, 1)));

        List<String> cells = row(text, "Per slice", "home");

        assertEquals("2", cells.get(1));
        assertEquals("17", cells.get(2));
        assertEquals("1", cells.get(3));
        assertEquals("2", cells.get(4));
    }

    @Test
    void a_commit_touching_two_files_of_one_slice_is_one_commit_and_all_its_lines() {
        String text = table(commit(
                "anna@example.com",
                "feat: a",
                change(BASE + "home/A.java", 3, 1),
                change(BASE + "home/web/B.java", 4, 0)));

        List<String> cells = row(text, "Per slice", "home");

        assertEquals("1", cells.get(1));
        assertEquals("8", cells.get(2));
    }

    @Test
    void a_commit_touching_two_slices_counts_once_in_each() {
        String text = table(commit(
                "anna@example.com",
                "feat: a",
                change(BASE + "home/A.java", 3, 0),
                change(BASE + "search/B.java", 4, 0)));

        assertEquals("1", row(text, "Per slice", "home").get(1));
        assertEquals("1", row(text, "Per slice", "search").get(1));
    }

    @Test
    void whoever_has_more_commits_in_a_slice_is_named() {
        String text = table(
                commit("anna@example.com", "feat: a", change(BASE + "home/A.java", 1, 0)),
                commit("anna@example.com", "feat: b", change(BASE + "home/B.java", 1, 0)),
                commit("boris@example.com", "feat: c", change(BASE + "home/C.java", 100, 0)));

        assertEquals("anna", row(text, "Per slice", "home").get(5));
    }

    @Test
    void equal_commits_are_even_and_no_commits_is_no_work_yet() {
        String text = table(
                commit("anna@example.com", "feat: a", change(BASE + "home/A.java", 1, 0)),
                commit("boris@example.com", "feat: b", change(BASE + "home/B.java", 1, 0)));

        assertEquals("even", row(text, "Per slice", "home").get(5));
        assertEquals("no work yet", row(text, "Per slice", "search").get(5));
    }

    @Test
    void one_person_with_two_addresses_is_one_contributor() {
        String text = table(
                commit("anna@example.com", "feat: a", change(BASE + "home/A.java", 1, 0)),
                commit("anna@second.example.com", "feat: b", change(BASE + "home/B.java", 1, 0)));

        assertEquals("2", row(text, "Per slice", "home").get(1));
    }

    @Test
    void an_address_nobody_registered_gets_its_own_column_instead_of_a_member() {
        String text = table(commit("stranger@example.com", "feat: a", change(BASE + "home/A.java", 1, 0)));

        assertTrue(text.contains("UNREGISTERED commits"), text);
        assertEquals("1", row(text, "Per slice", "home").get(5));
    }

    @Test
    void the_unregistered_column_is_absent_when_everyone_is_registered() {
        assertFalse(table(commit("anna@example.com", "feat: a", change(BASE + "home/A.java", 1, 0)))
                .contains("UNREGISTERED"));
    }

    @Test
    void a_slice_nobody_has_touched_is_still_a_row() {
        String text = table(commit("anna@example.com", "feat: a", change(BASE + "home/A.java", 1, 0)));

        assertEquals(List.of("search", "0", "0", "0", "0", "no work yet", ""), row(text, "Per slice", "search"));
    }

    // --- the hook's commits ---

    @Test
    void a_journal_commit_adds_nothing_to_any_slice_even_when_it_touches_one() {
        String text = table(
                commit(
                        "anna@example.com",
                        "docs: record the journal entry for 2026-09-26 16:39",
                        change(BASE + "home/A.java", 50, 0)),
                commit("boris@example.com", "feat: b", change(BASE + "home/B.java", 1, 0)));

        List<String> cells = row(text, "Per slice", "home");

        assertEquals("0", cells.get(1));
        assertEquals("boris", cells.get(5));
    }

    @Test
    void the_hooks_commits_are_reported_in_their_own_column_and_never_added_to_the_authored_ones() {
        String text = table(
                commit("anna@example.com", "docs: record the journal entry for 2026-09-26 16:39"),
                commit("anna@example.com", "docs: record the journal entry for 2026-09-26 16:40"),
                commit("anna@example.com", "feat: a", change(BASE + "home/A.java", 1, 0)),
                commit("boris@example.com", "feat: b", change(BASE + "home/B.java", 1, 0)));

        assertEquals(List.of("anna", "1", "2", ""), row(text, "All authored work", "anna"));
        assertEquals(List.of("boris", "1", "0", ""), row(text, "All authored work", "boris"));
    }

    @Test
    void work_outside_every_slice_still_counts_in_the_totals() {
        String text = table(commit("anna@example.com", "docs: a", change("docs/roadmap/x.md", 3, 0)));

        assertEquals("1", row(text, "All authored work", "anna").get(1));
        assertEquals("0", row(text, "Per slice", "home").get(1));
    }

    // --- the file ---

    @Test
    void the_same_history_gives_the_same_text_twice() {
        History.Commit only = commit("anna@example.com", "feat: a", change(BASE + "home/A.java", 1, 0));

        assertEquals(table(only), table(only));
    }

    @Test
    void the_text_says_it_is_generated_and_how_the_hook_is_kept_apart() {
        String text = table();

        assertTrue(text.contains("**Generated**"), text);
        assertTrue(text.contains("Stop"), text);
    }
}
