package in.ac.iitm.guide.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The real migrations and the real Hibernate session, no mocks: what matters about an import is
 * what ends up in the tables.
 */
@SpringBootTest
class ArticleArchiveTest {

    @Autowired
    private ArticleArchive archive;

    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    private Path directory;

    @AfterEach
    void clearTheDatabase() {
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:NFR-004
    void a_file_becomes_a_published_article_carrying_what_its_front_matter_says() {
        var report = archive.importFiles(
                Map.of(
                        "registering-with-frro.md",
                        """
                ---
                title: "Registering with FRRO"
                summary: "Register within 14 days."
                tags: [visa, admin]
                author: abdirakhim
                created: 2026-09-21
                updated: 2026-09-22
                ---

                Register **now**.
                """));

        assertThat(report.imported()).isEqualTo(1);
        var article = jdbc.queryForMap("SELECT * FROM article");
        assertThat(article)
                .containsEntry("TITLE", "Registering with FRRO")
                .containsEntry("SLUG", "registering-with-frro")
                .containsEntry("SUMMARY", "Register within 14 days.")
                .containsEntry("BODY", "Register **now**.\n");
        assertThat(article.get("REMOVED_AT")).isNull();
        assertThat(article.get("PINNED_AT")).isNull();
        assertThat(published("published_at")).isEqualTo(OffsetDateTime.parse("2026-09-21T00:00:00Z"));
        assertThat(published("updated_at")).isEqualTo(OffsetDateTime.parse("2026-09-22T00:00:00Z"));
        assertThat(tagsOfTheOnlyArticle()).containsExactly("admin", "visa");
    }

    @Test
    // trace:NFR-004
    void tags_are_stored_trimmed_and_lower_cased() {
        archive.importFiles(Map.of("a.md", article("Arrival", "tags: [\" SIM \", Visa]", "Text.\n")));

        assertThat(tagsOfTheOnlyArticle()).containsExactly("sim", "visa");
    }

    @Test
    // trace:NFR-004
    void two_articles_carrying_the_same_tag_share_one_tag_row() {
        archive.importFiles(Map.of(
                "a.md", article("Arrival", "tags: [sim]", "Text.\n"),
                "b.md", article("Departure", "tags: [sim]", "Text.\n")));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM tag", Integer.class))
                .isEqualTo(1);
    }

    @Test
    // trace:NFR-004
    void an_article_already_there_is_skipped_reported_and_left_as_it_was() {
        archive.importFiles(Map.of("a.md", article("Arrival", "tags: []", "First text.\n")));

        var report = archive.importFiles(Map.of("a.md", article("Arrival", "tags: []", "Second text.\n")));

        assertThat(report.imported()).isZero();
        assertThat(report.skipped()).containsExactly("Arrival");
        assertThat(jdbc.queryForObject("SELECT body FROM article", String.class))
                .isEqualTo("First text.\n");
    }

    @Test
    // trace:NFR-004
    void a_title_that_only_differs_by_punctuation_from_an_existing_one_is_refused_not_dropped() {
        archive.importFiles(Map.of("a.md", article("Fees & Payments", "tags: []", "Text.\n")));

        assertThatThrownBy(() -> archive.importFiles(Map.of("b.md", article("Fees Payments", "tags: []", "Text.\n"))))
                .isInstanceOf(ArchiveFormatException.class)
                .hasMessageContaining("b.md")
                .hasMessageContaining("Fees Payments")
                .hasMessageContaining("Fees & Payments");
    }

    @Test
    // trace:NFR-004
    void one_bad_file_leaves_the_database_as_it_was() {
        var files = Map.of("a.md", article("Arrival", "tags: [visa]", "Text.\n"), "b.md", "no front matter here");

        assertThatThrownBy(() -> archive.importFiles(files))
                .isInstanceOf(ArchiveFormatException.class)
                .hasMessageContaining("b.md");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM article", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tag", Integer.class))
                .isZero();
    }

    @Test
    // trace:NFR-004
    void when_several_files_are_bad_the_first_by_name_is_the_one_reported() {
        var files = new LinkedHashMap<String, String>();
        files.put("b.md", "no front matter here");
        files.put("a.md", "no front matter here either");

        assertThatThrownBy(() -> archive.importFiles(files))
                .isInstanceOf(ArchiveFormatException.class)
                .hasMessageStartingWith("a.md");
    }

    @Test
    // trace:NFR-004
    void a_file_with_windows_line_endings_is_read_like_any_other() {
        var text = article("Arrival", "tags: [visa]", "Line one.\nLine two.\n").replace("\n", "\r\n");

        archive.importFiles(Map.of("a.md", text));

        assertThat(jdbc.queryForObject("SELECT title FROM article", String.class))
                .isEqualTo("Arrival");
        assertThat(jdbc.queryForObject("SELECT body FROM article", String.class))
                .isEqualTo("Line one.\r\nLine two.\r\n");
    }

    @Test
    // trace:NFR-004
    void tags_are_lower_cased_the_same_way_whatever_the_language_of_the_machine() {
        var machine = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr"));
        try {
            archive.importFiles(Map.of("a.md", article("Arrival", "tags: [IIT]", "Text.\n")));
        } finally {
            Locale.setDefault(machine);
        }

        assertThat(tagsOfTheOnlyArticle()).containsExactly("iit");
    }

    @Test
    // trace:NFR-004
    void a_subdirectory_of_the_archive_is_not_read() throws Exception {
        Files.writeString(directory.resolve("arrival.md"), article("Arrival", "tags: []", "Text.\n"));
        Files.createDirectory(directory.resolve("attachments.md"));

        assertThat(archive.importFrom(directory).imported()).isEqualTo(1);
    }

    @Test
    // trace:NFR-004
    void an_article_removed_since_it_was_exported_is_not_brought_back_by_an_import() {
        archive.importFiles(Map.of("a.md", article("Arrival", "tags: []", "Text.\n")));
        jdbc.update("UPDATE article SET removed_at = CURRENT_TIMESTAMP");

        var report = archive.importFiles(Map.of("a.md", article("Arrival", "tags: []", "Text.\n")));

        assertThat(report.skipped()).containsExactly("Arrival");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM article WHERE removed_at IS NULL", Integer.class))
                .isZero();
    }

    @Test
    // trace:NFR-004
    void two_files_with_one_title_in_one_import_give_one_article() {
        var report = archive.importFiles(Map.of(
                "a.md", article("Arrival", "tags: []", "First.\n"),
                "b.md", article("Arrival", "tags: []", "Second.\n")));

        assertThat(report.imported()).isEqualTo(1);
        assertThat(report.skipped()).containsExactly("Arrival");
        assertThat(jdbc.queryForObject("SELECT body FROM article", String.class))
                .isEqualTo("First.\n");
    }

    @Test
    // trace:FR-009
    void a_pinned_key_pins_the_article_at_that_time() {
        var text = article("Arrival", "tags: []", "Text.\n")
                .replace("updated: 2026-09-21\n", "updated: 2026-09-21\npinned: 2026-09-22\n");

        archive.importFiles(Map.of("a.md", text));

        assertThat(published("pinned_at")).isEqualTo(OffsetDateTime.parse("2026-09-22T00:00:00Z"));
    }

    @Test
    // trace:FR-009
    void only_a_pinned_article_is_exported_with_a_pinned_line() throws Exception {
        archive.importFiles(Map.of(
                "a.md",
                        article("Arrival", "tags: []", "Text.\n")
                                .replace("updated: 2026-09-21\n", "updated: 2026-09-21\npinned: 2026-09-22\n"),
                "b.md", article("Departure", "tags: []", "Text.\n")));

        archive.exportTo(directory);

        assertThat(Files.readString(directory.resolve("arrival.md"))).contains("pinned: 2026-09-22T00:00:00Z\n");
        assertThat(Files.readString(directory.resolve("departure.md"))).doesNotContain("pinned");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filesThatCannotBecomeArticles")
    // trace:NFR-004
    void a_file_that_cannot_become_an_article_is_refused_naming_the_file_and_the_fault(
            String problem, String text, String namedInTheMessage) {
        assertThatThrownBy(() -> archive.importFiles(Map.of("bad.md", text)))
                .isInstanceOf(ArchiveFormatException.class)
                .hasMessageContaining("bad.md")
                .hasMessageContaining(namedInTheMessage);
    }

    static Stream<Arguments> filesThatCannotBecomeArticles() {
        return Stream.of(
                Arguments.of("no front matter", "Just text.\n", "no front matter"),
                Arguments.of("an unknown key", article("Arrival", "tag: [visa]", "Text.\n"), "tag"),
                Arguments.of(
                        "front matter that is not YAML", "---\ntitle: [unclosed\n---\n\nText.\n", "not valid YAML"),
                Arguments.of(
                        "no title",
                        front("summary: \"S.\"", "created: 2026-09-21", "updated: 2026-09-21") + "Text.\n",
                        "title"),
                Arguments.of(
                        "a title that is not text",
                        article("2026", "tags: []", "Text.\n").replace("\"2026\"", "2026"),
                        "title"),
                Arguments.of(
                        "a title with no letters or digits",
                        article("!!!", "tags: []", "Text.\n"),
                        "no letters or digits"),
                Arguments.of(
                        "no summary",
                        front("title: \"Arrival\"", "created: 2026-09-21", "updated: 2026-09-21") + "Text.\n",
                        "summary"),
                Arguments.of(
                        "a date that is not a date",
                        article("Arrival", "tags: []", "Text.\n")
                                .replace("created: 2026-09-21", "created: \"last week\""),
                        "created"),
                Arguments.of("tags that are not a list", article("Arrival", "tags: visa", "Text.\n"), "tags"),
                Arguments.of("an empty tag", article("Arrival", "tags: [\" \"]", "Text.\n"), "tag is empty"),
                Arguments.of("an empty body", article("Arrival", "tags: []", ""), "body"),
                Arguments.of(
                        "a pinned time that is not a date",
                        article("Arrival", "tags: []", "Text.\n")
                                .replace("updated: 2026-09-21\n", "updated: 2026-09-21\npinned: soon\n"),
                        "pinned"),
                Arguments.of(
                        "a key given twice",
                        article("Arrival", "tags: []", "Text.\n")
                                .replace("summary: \"A summary.\"", "summary: \"A summary.\"\nsummary: \"Another.\""),
                        "summary"));
    }

    @Test
    // trace:NFR-004
    void every_markdown_file_of_a_directory_is_imported_except_the_readme_and_other_files() throws Exception {
        Files.writeString(directory.resolve("arrival.md"), article("Arrival", "tags: []", "Text.\n"));
        Files.writeString(directory.resolve("README.md"), "About this directory, not an article.\n");
        Files.writeString(directory.resolve("notes.txt"), "Not an article either.\n");

        var report = archive.importFrom(directory);

        assertThat(report.imported()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT title FROM article", String.class))
                .isEqualTo("Arrival");
    }

    @Test
    // trace:NFR-004
    void an_export_holds_one_file_per_published_article_named_by_its_address() throws Exception {
        archive.importFiles(Map.of(
                "a.md", article("Arrival", "tags: []", "Text.\n"),
                "b.md", article("Fees & Payments", "tags: []", "Text.\n"),
                "c.md", article("Old News", "tags: []", "Text.\n")));
        jdbc.update("UPDATE article SET removed_at = CURRENT_TIMESTAMP WHERE title = 'Old News'");

        var written = archive.exportTo(directory);

        assertThat(written).isEqualTo(2);
        try (var files = Files.list(directory)) {
            assertThat(files.map(file -> file.getFileName().toString()))
                    .containsExactlyInAnyOrder("arrival.md", "fees-payments.md");
        }
    }

    @Test
    // trace:NFR-004
    void an_export_of_more_articles_than_one_page_holds_every_one() throws Exception {
        var files = new LinkedHashMap<String, String>();
        IntStream.range(0, 205)
                .forEach(i -> files.put("a" + i + ".md", article("Article " + i, "tags: []", "Text.\n")));
        archive.importFiles(files);

        assertThat(archive.exportTo(directory)).isEqualTo(205);
        try (var written = Files.list(directory)) {
            assertThat(written.count()).isEqualTo(205);
        }
    }

    @Test
    // trace:NFR-004
    void an_export_never_overwrites_a_file_that_is_already_there() {
        archive.importFiles(Map.of("a.md", article("Arrival", "tags: []", "Text.\n")));
        archive.exportTo(directory);

        assertThatThrownBy(() -> archive.exportTo(directory))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    // trace:NFR-004
    void an_export_of_an_empty_knowledge_base_writes_nothing() {
        assertThat(archive.exportTo(directory.resolve("out"))).isZero();
        assertThat(directory.resolve("out")).isDirectory();
    }

    @Test
    // trace:NFR-004
    void what_an_export_holds_imports_back_as_the_same_articles() {
        var files = new LinkedHashMap<String, String>();
        files.put(
                "colon.md",
                full(
                        "Visa: what to bring",
                        "A summary: with a colon.",
                        "[visa, \"admin stuff\"]",
                        "2026-09-21T10:15:30.123Z",
                        "Text.\n"));
        files.put("quote.md", full("The \"FRRO\" office", "It's \"quoted\".", "[]", "2026-09-22", "Text.\n"));
        files.put("at.md", full("@Home in Chennai", "Starts with an at sign.", "[housing]", "2026-09-23", "Text.\n"));
        files.put("hash.md", full("#1 Tips", "Starts with a hash.", "[housing]", "2026-09-24", "Text.\n"));
        files.put("hindi.md", full("छात्रावास जीवन", "हिंदी सारांश।", "[housing]", "2026-09-25", "पाठ।\n"));
        files.put(
                "blank.md",
                full(
                        "Blank First",
                        "Body starts with a blank line.",
                        "[]",
                        "2026-09-26",
                        "\nSecond line.\r\nThird.\n\n"));
        files.put(
                "pinned.md",
                full("Pinned Guide", "Pinned to the landing page.", "[]", "2026-09-29", "Text.\n")
                        .replace("updated: 2026-09-29\n", "updated: 2026-09-29\npinned: 2026-09-30T09:30:00.500Z\n"));
        files.put("yes.md", full("Yes", "A title YAML would read as a boolean.", "[]", "2026-09-27", "Text.\n"));
        archive.importFiles(files);
        var before = snapshot();

        archive.exportTo(directory);
        clearTheDatabase();
        archive.importFrom(directory);

        assertThat(snapshot()).isEqualTo(before);
        assertThat(before).hasSize(8);
    }

    @Test
    // trace:NFR-004
    void an_exported_file_is_plain_readable_text() throws Exception {
        var summary =
                "A long summary that a YAML writer would fold across several lines if it were left to its default width of eighty characters.";
        archive.importFiles(
                Map.of("hindi.md", full("छात्रावास जीवन", summary, "[zeta, alpha]", "2026-09-25", "पाठ।\n")));

        archive.exportTo(directory);

        var text = Files.readString(directory.resolve("छात्रावास-जीवन.md"));
        assertThat(text).contains("title: छात्रावास जीवन").contains("summary: " + summary + "\n");
        assertThat(text).contains("- alpha\n- zeta\n");
    }

    @Test
    // trace:NFR-004
    void one_bad_file_in_a_directory_leaves_the_database_as_it_was() throws Exception {
        Files.writeString(directory.resolve("a.md"), article("Arrival", "tags: [visa]", "Text.\n"));
        Files.writeString(directory.resolve("b.md"), "no front matter here");

        assertThatThrownBy(() -> archive.importFrom(directory)).isInstanceOf(ArchiveFormatException.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM article", Integer.class))
                .isZero();
    }

    private static String full(String title, String summary, String tags, String created, String body) {
        return "---\ntitle: " + yamlQuoted(title) + "\nsummary: " + yamlQuoted(summary) + "\ntags: " + tags
                + "\ncreated: " + created + "\nupdated: " + created + "\n---\n\n" + body;
    }

    private static String yamlQuoted(String text) {
        return "'" + text.replace("'", "''") + "'";
    }

    /** Every stored fact about every article, as one comparable line each. */
    private List<String> snapshot() {
        return jdbc.query(
                """
                SELECT a.title, a.slug, a.summary, a.body, a.published_at, a.updated_at, a.pinned_at,
                       COALESCE((SELECT LISTAGG(t.name, ',') WITHIN GROUP (ORDER BY t.name)
                                 FROM tag t JOIN article_tag j ON j.tag_id = t.id WHERE j.article_id = a.id), '')
                FROM article a ORDER BY a.slug
                """,
                (row, i) -> String.join(
                        "|",
                        row.getString(1),
                        row.getString(2),
                        row.getString(3),
                        row.getString(4),
                        String.valueOf(row.getObject(5, OffsetDateTime.class).toInstant()),
                        String.valueOf(row.getObject(6, OffsetDateTime.class).toInstant()),
                        String.valueOf(
                                row.getObject(7, OffsetDateTime.class) == null
                                        ? "not pinned"
                                        : row.getObject(7, OffsetDateTime.class).toInstant()),
                        row.getString(8)));
    }

    private static String article(String title, String tagsLine, String body) {
        return front(
                        "title: \"" + title + "\"",
                        "summary: \"A summary.\"",
                        tagsLine,
                        "created: 2026-09-21",
                        "updated: 2026-09-21")
                + body;
    }

    private static String front(String... lines) {
        return "---\n" + String.join("\n", lines) + "\n---\n\n";
    }

    private OffsetDateTime published(String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM article", OffsetDateTime.class)
                .withOffsetSameInstant(ZoneOffset.UTC);
    }

    private List<String> tagsOfTheOnlyArticle() {
        return jdbc.queryForList(
                "SELECT t.name FROM tag t JOIN article_tag j ON j.tag_id = t.id ORDER BY t.name", String.class);
    }
}
