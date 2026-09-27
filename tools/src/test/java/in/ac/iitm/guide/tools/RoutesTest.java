package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The route tables in ui-routes.md are written from routes.yml, so each test builds a small
 * repository with both files and reads the document back.
 */
class RoutesTest {

    private static final String DOC = "docs/architecture/ui-routes.md";
    private static final String CONTRACT = "docs/architecture/routes.yml";

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

    private String read(String relative) throws IOException {
        return Files.readString(repo.resolve(relative));
    }

    private void contract(String routes, String deferred) throws IOException {
        write(CONTRACT, "routes:\n" + routes + "deferred:\n" + deferred);
    }

    private static final String ONE_ROUTE =
            """
              - methods: [GET]
                path: /articles/{title}
                slice: articleview
                status: built
                template: >-
                  `articleview/Article.html`
                fields: >-
                  — (read only)
                responses: >-
                  `200` published; `404`
                  otherwise
                serves: >-
                  FR-001
            """;

    private static final String ONE_DEFERRED =
            """
              - feature: >-
                  Backlinks on an article
                priority: could
                requirements: >-
                  FR-006
            """;

    private static String document(String table, String deferred) {
        return "# Route contract\n\nProse before.\n\n"
                + Routes.TABLE_START + "\n" + table + Routes.TABLE_END + "\n\n"
                + "Prose between.\n\n"
                + Routes.DEFERRED_START + "\n" + deferred + Routes.DEFERRED_END + "\n\nProse after.\n";
    }

    @Test
    void a_route_becomes_a_row_with_its_text_joined_back_to_one_line() throws IOException {
        contract(ONE_ROUTE, ONE_DEFERRED);
        write(DOC, document("", ""));

        Routes.write(repo);

        assertTrue(
                read(DOC)
                        .contains("| `GET /articles/{title}` | `articleview` | `articleview/Article.html`"
                                + " | — (read only) | `200` published; `404` otherwise | FR-001 |\n"),
                read(DOC));
    }

    @Test
    void a_route_with_two_methods_names_both_and_a_query_follows_its_path() throws IOException {
        contract(
                """
                  - methods: [GET, POST]
                    path: /moderate/login
                    slice: shared/security
                    status: planned
                    template: t
                    fields: f
                    responses: r
                    serves: s
                  - methods: [GET]
                    path: /search
                    query: "q={query}"
                    slice: search
                    status: planned
                    template: t
                    fields: f
                    responses: r
                    serves: s
                """,
                ONE_DEFERRED);
        write(DOC, document("", ""));

        Routes.write(repo);

        var text = read(DOC);
        assertTrue(text.contains("| `GET /moderate/login`, `POST /moderate/login` | `shared/security` |"), text);
        assertTrue(text.contains("| `GET /search?q={query}` | `search` |"), text);
    }

    @Test
    void a_deferred_feature_becomes_a_row_of_its_own_table() throws IOException {
        contract(ONE_ROUTE, ONE_DEFERRED);
        write(DOC, document("", ""));

        Routes.write(repo);

        assertTrue(read(DOC)
                .contains(
                        "| Feature | Priority | FR |\n|---|---|---|\n| Backlinks on an article | could | FR-006 |\n"));
    }

    @Test
    void only_the_marked_regions_are_rewritten() throws IOException {
        contract(ONE_ROUTE, ONE_DEFERRED);
        write(DOC, document("| an old row |\n", "| an old deferred row |\n"));

        Routes.write(repo);

        var text = read(DOC);
        assertTrue(text.startsWith("# Route contract\n\nProse before.\n\n"), text);
        assertTrue(text.contains("\n\nProse between.\n\n"), text);
        assertTrue(text.endsWith("\n\nProse after.\n"), text);
        assertTrue(!text.contains("an old"), text);
    }

    @Test
    void a_pipe_inside_a_cell_is_escaped_so_the_row_keeps_its_columns() throws IOException {
        contract(ONE_ROUTE.replace("FR-001", "FR-001 | FR-002"), ONE_DEFERRED);
        write(DOC, document("", ""));

        Routes.write(repo);

        assertTrue(read(DOC).contains("| FR-001 \\| FR-002 |"), read(DOC));
    }

    @Test
    void check_reports_a_table_that_differs_from_the_contract_and_nothing_once_written() throws IOException {
        contract(ONE_ROUTE, ONE_DEFERRED);
        write(DOC, document("| an old row |\n", ""));

        var stale = Routes.check(repo);
        Routes.write(repo);
        var current = Routes.check(repo);

        assertEquals(List.of(DOC + " is out of date with " + CONTRACT + "; run `ai-tools routes`"), stale);
        assertEquals(List.of(), current);
    }

    @Test
    void a_document_without_its_markers_is_reported_rather_than_rewritten() throws IOException {
        contract(ONE_ROUTE, ONE_DEFERRED);
        write(DOC, "# Route contract\n\nNo markers here.\n");

        var problems = Routes.check(repo);

        // Both regions are missing, so both are named: whoever fixes the file sees all of it at once.
        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains(Routes.TABLE_START), problems.toString());
        assertTrue(problems.get(1).contains(Routes.DEFERRED_START), problems.toString());
        assertEquals("# Route contract\n\nNo markers here.\n", read(DOC));
    }
}
