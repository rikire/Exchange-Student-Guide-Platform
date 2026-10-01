package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
 * The gap list is only honest if it is built from the same facts as the matrix and lists what the
 * repository does not have yet, so each test builds a small repository and reads the list back.
 */
class GapsTest {

    private static final String TESTS = "app/src/test/java/in/ac/iitm/guide/home/";

    @TempDir
    Path repoRoot;

    private Repo repo;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(repoRoot.resolve(".git"));
        repo = Repo.find(repoRoot.toString());
        modules("Home");
    }

    /** What ModularityTest leaves under app/target: the modules, and an arrow per dependency. */
    private void modules(String... lines) throws IOException {
        StringBuilder text = new StringBuilder("@startuml\nContainer_Boundary(\"G.G_boundary\", \"G\") {\n");
        for (String line : lines) {
            if (line.contains("->")) {
                continue;
            }
            text.append("  Component(G.G.")
                    .append(line)
                    .append(", \"")
                    .append(line)
                    .append("\", $techn=\"Module\", $descr=\"\", $tags=\"\", $link=\"\")\n");
        }
        text.append("}\n\n");
        for (String line : lines) {
            if (!line.contains("->")) {
                continue;
            }
            String[] ends = line.split("->");
            text.append("Rel(G.G.")
                    .append(ends[0])
                    .append(", G.G.")
                    .append(ends[1])
                    .append(", \"depends on\", $techn=\"\", $tags=\"\", $link=\"\")\n");
        }
        write(
                "app/target/spring-modulith-docs/components.puml",
                text.append("@enduml\n").toString());
    }

    private void write(String relative, String content) throws IOException {
        repo.write(repo.resolve(relative), content);
    }

    /** One requirement with as many Given-When-Then scenarios as asked for. */
    private static String block(String id, String status, int scenarios) {
        StringBuilder text = new StringBuilder("### " + id + " — Title of " + id + "\n\n**Status:** " + status
                + "\n**Priority:** must\n\nText.\n\n**Acceptance criteria:**\n\n```\n");
        for (int i = 0; i < scenarios; i++) {
            text.append("GIVEN a state ").append(i).append("\nWHEN something\nTHEN a result\n\n");
        }
        return text.append("```\n\n").toString();
    }

    private void requirements(String... blocks) throws IOException {
        write(
                "docs/requirements/functional.md",
                "# Functional\n\n## Format\n\n### FR-050 — Example\n\n**Status:** planned\n\n```\n"
                        + "GIVEN an example\nWHEN it is read\nTHEN it is not counted\n```\n\n"
                        + "## Requirements\n\n" + String.join("", blocks));
    }

    private void feature(String id, String covers) throws IOException {
        featureIn(id, "home", covers);
    }

    private void featureIn(String id, String slice, String covers) throws IOException {
        write(
                "docs/features/" + id + "-thing.md",
                "---\nid: " + id + "\ntitle: A thing\nstatus: in-progress\ncovers: [" + covers + "]\nslice: " + slice
                        + "\nroutes: []\ntables: []\ncode: []\ntests: []\n---\n");
    }

    private void testAnchoredFor(String name, String... ids) throws IOException {
        write(
                TESTS + name + ".java",
                "class " + name + " {\n    // trace:" + String.join(", ", ids) + "\n    void a_test() {}\n}\n");
    }

    private String list() throws IOException {
        return Gaps.build(repo);
    }

    /** The cells of the row that starts with the id, in the section that starts with the heading. */
    private List<String> row(String heading, String id) throws IOException {
        String text = list();
        int start = text.indexOf("## " + heading);
        assertTrue(start >= 0, "no section '" + heading + "' in:\n" + text);
        String rest = text.substring(start + 3);
        int next = rest.indexOf("\n## ");
        String section = next < 0 ? rest : rest.substring(0, next);
        return section.lines()
                .filter(line -> line.startsWith("| " + id + " |"))
                .findFirst()
                .map(line -> Arrays.stream(line.substring(1).split("\\|", -1))
                        .map(String::strip)
                        .toList())
                .orElseThrow(() -> new AssertionError("no row for " + id + " under '" + heading + "' in:\n" + text));
    }

    private boolean hasRow(String heading, String id) throws IOException {
        try {
            row(heading, id);
            return true;
        } catch (AssertionError e) {
            if (e.getMessage().startsWith("no row")) {
                return false;
            }
            throw e;
        }
    }

    // --- requirements ---

    @Test
    void a_planned_and_an_in_progress_requirement_are_listed_and_a_done_one_is_not() throws IOException {
        requirements(block("FR-001", "planned", 1), block("FR-002", "in-progress", 1), block("FR-003", "done", 1));
        feature("FEAT-001", "FR-002");

        assertTrue(hasRow("Requirements not done", "FR-001"));
        assertTrue(hasRow("Requirements not done", "FR-002"));
        assertFalse(hasRow("Requirements not done", "FR-003"));
    }

    @Test
    void an_out_of_scope_requirement_is_a_decision_and_not_a_gap() throws IOException {
        requirements(
                block("FR-001", "out-of-scope", 1).replace("**Priority:**", "**Rationale:** dropped\n**Priority:**"));

        assertFalse(hasRow("Requirements not done", "FR-001"));
    }

    @Test
    void the_row_carries_status_priority_and_title() throws IOException {
        requirements(block("FR-001", "planned", 1));

        List<String> cells = row("Requirements not done", "FR-001");

        assertEquals("planned", cells.get(1));
        assertEquals("must", cells.get(2));
        assertEquals("Title of FR-001", cells.get(3));
    }

    @Test
    void the_illustrative_scenario_in_the_format_section_is_not_counted() throws IOException {
        requirements(block("FR-001", "planned", 2));

        assertEquals("2", row("Requirements not done", "FR-001").get(4));
    }

    @Test
    void criteria_and_anchored_tests_are_counted_and_the_difference_is_what_has_no_test_at_least() throws IOException {
        requirements(block("FR-001", "in-progress", 3));
        feature("FEAT-001", "FR-001");
        testAnchoredFor("HomeControllerTest", "FR-001");

        List<String> cells = row("Requirements not done", "FR-001");

        assertEquals("3", cells.get(4));
        assertEquals("1", cells.get(5));
        assertEquals("2", cells.get(6));
    }

    @Test
    void a_done_requirement_with_fewer_anchored_tests_than_criteria_is_listed_apart() throws IOException {
        requirements(block("FR-001", "done", 2));
        feature("FEAT-001", "FR-001");
        testAnchoredFor("HomeControllerTest", "FR-001");

        assertFalse(hasRow("Requirements not done", "FR-001"));
        List<String> cells = row("Done, with criteria that no test is anchored to", "FR-001");
        assertEquals("2", cells.get(1));
        assertEquals("1", cells.get(2));
    }

    @Test
    void a_done_requirement_with_a_test_for_each_criterion_is_nowhere_in_the_list() throws IOException {
        requirements(block("FR-001", "done", 1));
        feature("FEAT-001", "FR-001");
        testAnchoredFor("HomeControllerTest", "FR-001");
        write(
                "app/src/main/java/in/ac/iitm/guide/home/HomeController.java",
                "//trace:FR-001\nclass HomeController {}\n");

        assertFalse(list().contains("FR-001"), list());
    }

    // --- debt ---

    @Test
    void an_open_debt_entry_is_listed_with_its_trigger_and_a_resolved_one_is_not() throws IOException {
        write(
                "docs/tech-debt.md",
                "## Register\n\n### DEBT-001 — The importer writes no links\n\n**Status:** open\n**Created:** 2026-09-25\n\n"
                        + "**Trigger:** the first code that writes article_link.\nMore words.\n\n"
                        + "### DEBT-002 — Fixed already\n\n**Status:** resolved\n\n**Trigger:** none.\n");

        List<String> cells = row("Open technical debt", "DEBT-001");

        assertEquals("The importer writes no links", cells.get(1));
        assertEquals("the first code that writes article_link.", cells.get(2));
        assertFalse(hasRow("Open technical debt", "DEBT-002"));
    }

    // --- the chain ---

    @Test
    void a_gap_in_the_traceability_chain_is_in_the_list() throws IOException {
        requirements(block("FR-001", "in-progress", 1));

        assertTrue(list().contains("FR-001 is in-progress and no feature file covers it"), list());
    }

    @Test
    void a_section_with_nothing_in_it_says_so_instead_of_disappearing() throws IOException {
        requirements(block("FR-001", "done", 0));
        feature("FEAT-001", "FR-001");
        write(TESTS + "HomeControllerTest.java", "class HomeControllerTest { // trace:FR-001\n}\n");
        write(
                "app/src/main/java/in/ac/iitm/guide/home/HomeController.java",
                "//trace:FR-001\nclass HomeController {}\n");

        assertTrue(list().contains("## Open technical debt\n\nNone."), list());
    }

    @Test
    void a_repository_with_nothing_in_it_still_gives_a_list() throws IOException {
        assertTrue(list().startsWith("# Gap list"), list());
    }

    // --- progress ---

    @Test
    void a_slice_is_done_partial_or_not_started_by_the_status_of_what_its_features_cover() throws IOException {
        requirements(
                block("FR-001", "done", 1),
                block("FR-002", "in-progress", 1),
                block("FR-003", "planned", 1),
                block("FR-004", "planned", 1));
        featureIn("FEAT-001", "home", "FR-001");
        featureIn("FEAT-002", "search", "FR-002, FR-003");
        featureIn("FEAT-003", "media", "FR-004");

        String text = list();

        assertTrue(text.contains("home[\"home · 1/1 done\"]:::done"), text);
        assertTrue(text.contains("search[\"search · 0/2 done\"]:::partial"), text);
        assertTrue(text.contains("media[\"media · 0/1 done\"]:::none"), text);
    }

    @Test
    void a_requirement_no_feature_covers_is_in_the_unmapped_node_and_counted_in_no_slice() throws IOException {
        requirements(block("FR-001", "done", 1), block("FR-005", "planned", 1));
        featureIn("FEAT-001", "home", "FR-001");

        String text = list();

        assertTrue(text.contains("unmapped[\"no feature yet: FR-005\"]:::unmapped"), text);
        assertTrue(text.contains("home[\"home · 1/1 done\"]:::done"), text);
    }

    @Test
    void the_arrows_and_the_modules_come_from_what_modulith_read_from_the_code() throws IOException {
        modules("Home", "Contribute", "Media", "Contribute->Media");

        String text = list();

        assertTrue(text.contains("contribute --> media"), text);
        assertTrue(text.contains("media[\"media · no feature\"]:::none"), text);
    }

    @Test
    void without_modulith_output_the_list_is_refused_rather_than_drawn_without_arrows() throws IOException {
        Files.delete(repo.resolve("app/target/spring-modulith-docs/components.puml"));

        IllegalStateException refused = assertThrows(IllegalStateException.class, () -> Gaps.build(repo));

        assertTrue(refused.getMessage().contains("./mvnw test"), refused.getMessage());
    }

    @Test
    void the_pie_counts_requirements_by_status_and_not_the_format_example() throws IOException {
        requirements(
                block("FR-001", "done", 1),
                block("FR-002", "in-progress", 1),
                block("FR-003", "planned", 1),
                block("FR-004", "planned", 1));

        String text = list();

        assertTrue(text.contains("\"done\" : 1\n"), text);
        assertTrue(text.contains("\"in-progress\" : 1\n"), text);
        assertTrue(text.contains("\"planned\" : 2\n"), text);
    }

    @Test
    void check_reports_a_list_that_no_longer_matches_and_nothing_once_it_is_rewritten() throws IOException {
        requirements(block("FR-001", "planned", 1));

        assertEquals(
                List.of("docs/gap-list.md is out of date; run `java -jar tools/target/ai-tools.jar gaps`"),
                Gaps.check(repo));

        Gaps.write(repo);

        assertEquals(List.of(), Gaps.check(repo));
    }

    // --- the file ---

    @Test
    void the_same_repository_gives_the_same_text_twice() throws IOException {
        requirements(block("FR-001", "planned", 2), block("FR-002", "in-progress", 1));

        assertEquals(list(), list());
    }

    @Test
    void write_puts_the_list_in_docs_gap_list() throws IOException {
        requirements(block("FR-001", "planned", 1));

        Gaps.write(repo);

        assertEquals(list(), Files.readString(repo.resolve("docs/gap-list.md")));
    }
}
