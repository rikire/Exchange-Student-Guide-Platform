package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * The matrix is only worth having if a hole in the chain shows up in it. Each test builds a small
 * repository, breaks one link, and asserts that the break is named — the status rules are the ones
 * in docs/repository-map.md.
 */
class TraceTest {

    private static final String SLICE = "app/src/main/java/in/ac/iitm/guide/home/";
    private static final String TESTS = "app/src/test/java/in/ac/iitm/guide/home/";
    private static final String MIGRATIONS = "app/src/main/resources/db/migration/";

    @TempDir
    Path repoRoot;

    private Repo repo;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(repoRoot.resolve(".git"));
        repo = Repo.find(repoRoot.toString());
    }

    private void write(String relative, String content) throws IOException {
        repo.write(repo.resolve(relative), content);
    }

    /** A requirements file that opens with the illustrative entry every real one carries. */
    private void requirements(String file, String prefix, String... blocks) throws IOException {
        write(
                "docs/requirements/" + file,
                "# Requirements\n\n## Format\n\n### " + prefix + "-050 — Example\n\n**Status:** planned\n\n"
                        + "## Requirements\n\n" + String.join("", blocks));
    }

    private static String block(String id, String status, String... extraLines) {
        return "### " + id + " — Title of " + id + "\n\n**Status:** " + status + "\n**Priority:** must\n"
                + String.join("\n", extraLines) + "\n\nText.\n\n";
    }

    private void feature(String id, String status, String covers, String... paths) throws IOException {
        String code = "";
        String tests = "";
        for (String path : paths) {
            if (path.contains("/src/test/")) {
                tests += "  - " + path + "\n";
            } else {
                code += "  - " + path + "\n";
            }
        }
        write(
                "docs/features/" + id + "-thing.md",
                "---\nid: " + id + "\ntitle: A thing\nstatus: " + status + "\ncovers: [" + covers + "]\n"
                        + "slice: home\nroutes: [\"GET /\"]\ntables: [article]\n"
                        + (code.isEmpty() ? "code: []\n" : "code:\n" + code)
                        + (tests.isEmpty() ? "tests: []\n" : "tests:\n" + tests)
                        + "---\n\n# " + id + "\n");
    }

    private void code(String name, String anchor) throws IOException {
        write(SLICE + name + ".java", anchor + "\nclass " + name + " {}\n");
    }

    private void test(String name, String anchor) throws IOException {
        write(TESTS + name + ".java", anchor + "\nclass " + name + " {}\n");
    }

    private Trace.Report report() throws IOException {
        return Trace.build(repo);
    }

    /** The cells of one matrix row: id, status, features, code, tests, migrations. */
    private List<String> row(String id) throws IOException {
        String matrix = report().traceability();
        return matrix.lines()
                .filter(line -> line.startsWith("| " + id + " |"))
                .findFirst()
                .map(line -> Arrays.stream(line.substring(1).split("\\|", -1))
                        .map(String::strip)
                        .toList()
                        .subList(0, 6))
                .orElseThrow(() -> new AssertionError("no row for " + id + " in:\n" + matrix));
    }

    private static String problems(Trace.Report report) {
        return String.join(" | ", report.problems());
    }

    // --- the matrix ---

    @Test
    void a_code_anchor_and_a_test_anchor_land_in_their_own_columns_of_the_requirements_row() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "done"));
        feature("FEAT-001", "done", "FR-001");
        code("HomeController", "//trace:FR-001");
        test("HomeControllerTest", "//trace:FR-001");

        assertEquals(List.of("FR-001", "done", "FEAT-001", "HomeController", "HomeControllerTest", ""), row("FR-001"));
    }

    @Test
    void a_migration_anchor_lands_in_the_migrations_column() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));
        write(MIGRATIONS + "V1__create.sql", "-- trace: FR-001\nCREATE TABLE a (id INT);\n");

        assertEquals("V1__create", row("FR-001").get(5));
    }

    @Test
    void one_anchor_naming_several_requirements_counts_for_each() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"), block("FR-002", "planned"));
        code("HomeController", "//trace:FR-001, FR-002");

        assertEquals("HomeController", row("FR-001").get(3));
        assertEquals("HomeController", row("FR-002").get(3));
    }

    @Test
    void the_illustrative_entry_in_the_format_section_is_not_a_row() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));

        assertFalse(report().traceability().contains("| FR-050 |"));
    }

    @Test
    void build_output_is_not_read_because_it_holds_copies_of_the_sources() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));
        write("app/target/classes/db/migration/V1__create.sql", "-- trace: FR-001\nCREATE TABLE a (id INT);\n");
        write("app/target/generated/Copy.java", "//trace:FR-099\nclass Copy {}\n");

        assertEquals("", row("FR-001").get(5));
        assertEquals(List.of(), report().problems());
    }

    @Test
    void non_functional_requirements_are_rows_too() throws IOException {
        requirements("non-functional.md", "NFR", block("NFR-004", "planned"));
        code("Archive", "//trace:NFR-004");

        assertEquals("Archive", row("NFR-004").get(3));
    }

    // --- the status rules ---

    @Test
    void a_planned_requirement_with_nothing_behind_it_is_not_a_problem() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));

        assertEquals(List.of(), report().problems());
    }

    @Test
    void an_in_progress_requirement_no_feature_file_covers_is_a_problem() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "in-progress"));

        assertTrue(
                problems(report()).contains("FR-001 is in-progress and no feature file covers it"), problems(report()));
    }

    @Test
    void an_in_progress_requirement_a_feature_file_covers_is_fine() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "in-progress"));
        feature("FEAT-001", "in-progress", "FR-001");

        assertEquals(List.of(), report().problems());
    }

    @Test
    void a_done_requirement_without_a_test_anchor_is_a_problem() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "done"));
        feature("FEAT-001", "done", "FR-001");
        code("HomeController", "//trace:FR-001");

        assertTrue(problems(report()).contains("FR-001 is done and no test carries its anchor"), problems(report()));
    }

    @Test
    void a_done_requirement_without_a_code_anchor_is_a_problem() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "done"));
        feature("FEAT-001", "done", "FR-001");
        test("HomeControllerTest", "//trace:FR-001");

        assertTrue(problems(report()).contains("FR-001 is done and no code carries its anchor"), problems(report()));
    }

    @Test
    void a_migration_anchor_does_not_stand_in_for_a_code_anchor_on_a_done_requirement() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "done"));
        feature("FEAT-001", "done", "FR-001");
        write(MIGRATIONS + "V1__create.sql", "-- trace: FR-001\nCREATE TABLE a (id INT);\n");
        test("HomeControllerTest", "//trace:FR-001");

        assertTrue(problems(report()).contains("FR-001 is done and no code carries its anchor"), problems(report()));
    }

    @Test
    void a_done_requirement_no_feature_file_covers_is_a_problem() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "done"));
        code("HomeController", "//trace:FR-001");
        test("HomeControllerTest", "//trace:FR-001");

        assertTrue(problems(report()).contains("FR-001 is done and no feature file covers it"), problems(report()));
    }

    @Test
    void a_done_requirement_with_all_three_links_is_fine() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "done"));
        feature("FEAT-001", "done", "FR-001");
        code("HomeController", "//trace:FR-001");
        test("HomeControllerTest", "//trace:FR-001");

        assertEquals(List.of(), report().problems());
    }

    @Test
    void a_done_non_functional_requirement_may_name_what_verifies_it_instead_of_a_test() throws IOException {
        requirements("non-functional.md", "NFR", block("NFR-004", "done", "**Verified by:** the export round trip"));
        feature("FEAT-004", "done", "NFR-004");
        code("Archive", "//trace:NFR-004");

        assertEquals(List.of(), report().problems());
    }

    @Test
    void an_out_of_scope_requirement_without_a_rationale_is_a_problem() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "out-of-scope"));

        assertTrue(problems(report()).contains("FR-001 is out-of-scope and gives no Rationale"), problems(report()));
    }

    @Test
    void an_out_of_scope_requirement_with_a_rationale_is_fine() throws IOException {
        requirements(
                "functional.md", "FR", block("FR-001", "out-of-scope", "**Rationale:** the stakeholder dropped it"));

        assertEquals(List.of(), report().problems());
    }

    @Test
    void a_constraint_without_a_rationale_is_a_problem() throws IOException {
        requirements("constraints.md", "CON", block("CON-001", "decided"));

        assertTrue(problems(report()).contains("CON-001 gives no Rationale"), problems(report()));
    }

    @Test
    void a_status_that_is_not_one_of_the_four_is_a_problem_rather_than_a_silent_skip() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "finished"));

        assertTrue(
                problems(report())
                        .contains("FR-001 has status 'finished'; expected planned, in-progress, done or out-of-scope"),
                problems(report()));
    }

    @Test
    void a_requirement_with_no_status_line_is_a_problem() throws IOException {
        write("docs/requirements/functional.md", "## Requirements\n\n### FR-001 — Title\n\nText.\n");

        assertTrue(problems(report()).contains("FR-001 has no Status line"), problems(report()));
    }

    // --- references that lead nowhere ---

    @Test
    void an_anchor_naming_a_requirement_that_does_not_exist_is_a_problem() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));
        code("HomeController", "//trace:FR-099");

        assertTrue(
                problems(report()).contains("HomeController.java names FR-099, which is not declared"),
                problems(report()));
    }

    @Test
    void an_anchor_naming_the_illustrative_entry_is_a_problem() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));
        code("HomeController", "//trace:FR-050");

        assertTrue(problems(report()).contains("FR-050, which is not declared"), problems(report()));
    }

    @Test
    void a_feature_file_covering_a_requirement_that_does_not_exist_is_a_problem() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));
        feature("FEAT-001", "in-progress", "FR-099");

        assertTrue(problems(report()).contains("FEAT-001 covers FR-099, which is not declared"), problems(report()));
    }

    @Test
    void a_feature_file_listing_a_file_that_does_not_exist_is_a_problem() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "in-progress"));
        feature("FEAT-001", "in-progress", "FR-001", SLICE + "Gone.java");

        assertTrue(
                problems(report()).contains("FEAT-001 lists " + SLICE + "Gone.java, which does not exist"),
                problems(report()));
    }

    @Test
    void a_feature_file_with_unreadable_front_matter_is_a_problem_and_not_a_crash() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));
        write("docs/features/FEAT-009-broken.md", "---\nid: [unclosed\n---\n");

        assertTrue(problems(report()).contains("FEAT-009-broken.md"), problems(report()));
    }

    // --- debt markers ---

    @Test
    void a_marker_naming_an_open_debt_entry_is_fine() throws IOException {
        write("docs/tech-debt.md", "### DEBT-001 — Something\n\n**Status:** open\n");
        code("HomeController", "// TODO(DEBT-001): later");

        assertEquals(List.of(), report().problems());
    }

    @Test
    void a_marker_naming_a_resolved_debt_entry_is_a_problem() throws IOException {
        write("docs/tech-debt.md", "### DEBT-001 — Something\n\n**Status:** resolved\n");
        code("HomeController", "// TODO(DEBT-001): later");

        assertTrue(
                problems(report())
                        .contains("HomeController.java still carries a marker for DEBT-001, which is resolved"),
                problems(report()));
    }

    @Test
    void a_marker_naming_a_debt_entry_that_does_not_exist_is_a_problem() throws IOException {
        write("docs/tech-debt.md", "### DEBT-001 — Something\n\n**Status:** open\n");
        code("HomeController", "// FIXME(DEBT-042): later");

        assertTrue(problems(report()).contains("DEBT-042, which is not recorded"), problems(report()));
    }

    // --- notes: true, but not a reason to refuse anything ---

    @Test
    void a_planned_requirement_that_already_has_anchors_is_a_note_not_a_problem() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));
        code("HomeController", "//trace:FR-001");

        assertEquals(List.of(), report().problems());
        assertTrue(
                String.join(" | ", report().notes()).contains("FR-001 is planned but has anchors"),
                report().notes().toString());
    }

    @Test
    void a_planned_requirement_whose_only_anchor_is_a_migration_is_no_note_because_the_schema_runs_ahead()
            throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));
        write(MIGRATIONS + "V1__create.sql", "-- trace: FR-001\nCREATE TABLE a (id INT);\n");

        assertEquals(List.of(), report().notes());
    }

    private static final String ENTITY = "app/src/main/java/in/ac/iitm/guide/shared/persistence/Tag.java";

    @Test
    void a_planned_requirement_anchored_only_on_an_entity_and_its_schema_test_is_no_note() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));
        write(ENTITY, "//trace:FR-001\nclass Tag {}\n");
        test("SchemaMigrationTest", "//trace:FR-001");

        assertEquals(List.of(), report().notes());
    }

    @Test
    void an_entity_anchor_does_not_stand_in_for_a_code_anchor_on_a_done_requirement() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "done"));
        feature("FEAT-001", "done", "FR-001");
        write(ENTITY, "//trace:FR-001\nclass Tag {}\n");
        test("HomeControllerTest", "//trace:FR-001");

        assertTrue(problems(report()).contains("FR-001 is done and no code carries its anchor"), problems(report()));
    }

    @Test
    void behaviour_in_a_slice_still_makes_a_planned_requirement_a_note_beside_an_entity() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));
        write(ENTITY, "//trace:FR-001\nclass Tag {}\n");
        code("HomeController", "//trace:FR-001");

        assertTrue(String.join(" | ", report().notes()).contains("FR-001 is planned but has anchors"));
    }

    // --- the feature backlog ---

    @Test
    void the_feature_backlog_lists_each_feature_with_status_requirements_and_slice() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "in-progress"), block("FR-002", "in-progress"));
        feature("FEAT-001", "in-progress", "FR-001, FR-002");

        String line = report().features()
                .lines()
                .filter(l -> l.startsWith("| FEAT-001 |"))
                .findFirst()
                .orElse("");
        assertTrue(line.contains("in-progress") && line.contains("FR-001, FR-002") && line.contains("home"), line);
    }

    // --- the files themselves ---

    @Test
    void the_same_repository_gives_the_same_text_twice() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "done"), block("FR-002", "planned"));
        feature("FEAT-001", "done", "FR-001");
        code("HomeController", "//trace:FR-001");
        test("HomeControllerTest", "//trace:FR-001");

        Trace.Report first = report();
        Trace.Report second = report();

        assertEquals(first.traceability(), second.traceability());
        assertEquals(first.features(), second.features());
    }

    @Test
    void write_replaces_the_stub_files_with_what_build_produced() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));

        Trace.write(repo);

        assertEquals(report().traceability(), Files.readString(repo.resolve("docs/traceability.md")));
        assertEquals(report().features(), Files.readString(repo.resolve("docs/features/README.md")));
    }

    @Test
    void check_says_the_files_are_stale_until_they_are_written_and_fine_afterwards() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "planned"));

        assertTrue(String.join(" | ", Trace.check(repo)).contains("docs/traceability.md is out of date"));

        Trace.write(repo);

        assertEquals(List.of(), Trace.check(repo));
    }

    @Test
    void check_also_reports_the_problems_that_build_found() throws IOException {
        requirements("functional.md", "FR", block("FR-001", "in-progress"));
        Trace.write(repo);

        assertTrue(
                String.join(" | ", Trace.check(repo)).contains("FR-001 is in-progress and no feature file covers it"));
    }
}
