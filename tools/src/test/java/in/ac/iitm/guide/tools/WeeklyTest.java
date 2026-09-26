package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The "From git" half of the weekly log, refreshed by a tool; the other half is the members' own
 * words, and refreshing the first must leave the second exactly as written.
 */
class WeeklyTest {

    private static final String WEEK = "2026-W38";

    @TempDir
    Path repoRoot;

    private Repo repo;
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
        repo = Repo.find(repoRoot.toString());
        members = Members.load(repo);
    }

    private static History.Commit commit(String email, String week, String subject, String... paths) {
        List<History.Change> changes = java.util.Arrays.stream(paths)
                .map(path -> new History.Change(path, 1, 0))
                .toList();
        return new History.Commit(
                "sha-" + Math.abs((email + week + subject).hashCode()), email, week, subject, changes);
    }

    private String log(String existing, History.Commit... commits) {
        return Weekly.build(List.of(commits), members, WEEK, existing);
    }

    private static String section(String text, String name) {
        int start = text.indexOf("## " + name);
        assertTrue(start >= 0, "no section for " + name + " in:\n" + text);
        String rest = text.substring(start + 3);
        int next = rest.indexOf("\n## ");
        return next < 0 ? rest : rest.substring(0, next);
    }

    // --- the week ---

    @Test
    void a_date_is_placed_in_its_iso_week() {
        assertEquals("2026-W39", Weekly.weekOf(LocalDate.of(2026, 9, 21)));
        assertEquals("2026-W39", Weekly.weekOf(LocalDate.of(2026, 9, 27)));
    }

    @Test
    void the_days_around_new_year_belong_to_the_iso_year_and_not_the_calendar_one() {
        assertEquals("2026-W53", Weekly.weekOf(LocalDate.of(2027, 1, 1)));
    }

    @Test
    void the_title_names_the_monday_and_the_sunday_of_the_week() {
        assertEquals("2026-W38 — 14 to 20 September", Weekly.titleOf("2026-W38"));
    }

    @Test
    void a_week_that_crosses_a_month_names_both_months() {
        assertEquals("2026-W40 — 28 September to 4 October", Weekly.titleOf("2026-W40"));
    }

    @Test
    void a_week_that_crosses_a_year_names_both_months_and_still_reads_as_days() {
        assertEquals("2026-W53 — 28 December to 3 January", Weekly.titleOf("2026-W53"));
    }

    // --- the figures ---

    @Test
    void a_new_log_has_the_title_and_a_section_for_every_member_even_one_with_no_commits() {
        String text = log(null, commit("anna@example.com", WEEK, "feat: a", "docs/x.md"));

        assertTrue(text.startsWith("# 2026-W38 — 14 to 20 September\n"), text);
        assertTrue(
                section(text, "Anna A").contains("**From git** — 1 authored commit this week, 0 written by the hook."),
                text);
        assertTrue(
                section(text, "Boris B")
                        .contains("**From git** — 0 authored commits this week, 0 written by the hook."),
                text);
    }

    @Test
    void only_the_commits_of_that_week_are_counted() {
        String text = log(
                null,
                commit("anna@example.com", WEEK, "feat: a", "docs/x.md"),
                commit("anna@example.com", "2026-W37", "feat: earlier", "docs/y.md"),
                commit("anna@example.com", "2026-W39", "feat: later", "docs/z.md"));

        assertTrue(section(text, "Anna A").contains("1 authored commit this week"), text);
    }

    @Test
    void the_hooks_commits_are_counted_apart_and_their_files_are_not_directories_touched() {
        String text = log(
                null,
                commit(
                        "anna@example.com",
                        WEEK,
                        "docs: record the journal entry for 2026-09-14 10:00",
                        "docs/ai/journal/a.md",
                        "app/x/Y.java"),
                commit(
                        "anna@example.com",
                        WEEK,
                        "docs: record the journal entry for 2026-09-14 10:05",
                        "docs/ai/journal/b.md"),
                commit("anna@example.com", WEEK, "feat: a", "docs/roadmap/x.md"));

        String anna = section(text, "Anna A");

        assertTrue(anna.contains("1 authored commit this week, 2 written by the hook."), anna);
        assertTrue(anna.contains("`docs/roadmap` (1)"), anna);
        assertFalse(anna.contains("app/x"), anna);
    }

    @Test
    void directories_are_the_first_two_path_segments_counted_by_files_and_the_busiest_come_first() {
        String text = log(
                null,
                commit("anna@example.com", WEEK, "feat: a", "tools/src/A.java", "tools/src/B.java", "docs/ai/x.md"),
                commit("anna@example.com", WEEK, "feat: b", "tools/src/A.java", "docs/roadmap/y.md"));

        assertTrue(
                section(text, "Anna A")
                        .contains(
                                "Directories touched, by file count: `tools/src` (3), `docs/ai` (1), `docs/roadmap` (1)."),
                text);
    }

    @Test
    void a_file_at_the_top_of_the_repository_names_no_directory() {
        String text = log(null, commit("anna@example.com", WEEK, "chore: a", "pom.xml"));

        assertFalse(section(text, "Anna A").contains("Directories touched"), text);
    }

    @Test
    void the_journal_directory_never_appears_among_the_directories() {
        String text =
                log(null, commit("anna@example.com", WEEK, "docs: notes", "docs/ai/journal/x.md", "docs/ai/rules.md"));

        assertTrue(section(text, "Anna A").contains("`docs/ai` (1)"), text);
    }

    @Test
    void no_more_than_six_directories_are_listed() {
        String text = log(
                null,
                commit(
                        "anna@example.com",
                        WEEK,
                        "feat: a",
                        "a/1/x",
                        "b/1/x",
                        "c/1/x",
                        "d/1/x",
                        "e/1/x",
                        "f/1/x",
                        "g/1/x"));

        assertFalse(section(text, "Anna A").contains("`g/1`"), text);
        assertTrue(section(text, "Anna A").contains("`f/1`"), text);
    }

    @Test
    void two_addresses_of_one_person_are_one_figure() {
        String text = log(
                null,
                commit("anna@example.com", WEEK, "feat: a", "docs/x.md"),
                commit("anna@second.example.com", WEEK, "feat: b", "docs/y.md"));

        assertTrue(section(text, "Anna A").contains("2 authored commits this week"), text);
    }

    @Test
    void an_address_nobody_registered_gets_its_own_section_rather_than_a_members_figure() {
        String text = log(null, commit("stranger@example.com", WEEK, "feat: a", "docs/x.md"));

        assertTrue(section(text, "UNREGISTERED").contains("1 authored commit this week"), text);
        assertTrue(section(text, "Anna A").contains("0 authored commits"), text);
    }

    @Test
    void a_new_section_leaves_the_members_own_words_to_the_member_and_says_so() {
        String text = log(null);

        assertTrue(section(text, "Anna A").contains("**In our own words** — _Not written yet."), text);
    }

    // --- refreshing a log that has been written in ---

    private static final String WRITTEN =
            """
            # 2026-W38 — 14 to 20 September

            Some words about the week that a person wrote.

            ## Anna A

            **From git** — 5 authored commits this week, 0 written by the hook. Directories touched, by file count: `old/dir` (9).

            **In our own words** — Anna wrote this herself,
            over two lines, and it must survive.

            ## Boris B

            **From git** — 1 authored commit this week, 0 written by the hook.

            **In our own words** — Boris's paragraph.
            """;

    @Test
    void refreshing_replaces_the_from_git_paragraph_and_touches_nothing_else() {
        String text = log(WRITTEN, commit("anna@example.com", WEEK, "feat: a", "docs/x.md"));

        assertTrue(
                text.contains(
                        "**From git** — 1 authored commit this week, 0 written by the hook. Directories touched, by file count: `docs/x.md` (1)."),
                text);
        assertFalse(text.contains("old/dir"), text);
        assertTrue(
                text.contains(
                        "**In our own words** — Anna wrote this herself,\nover two lines, and it must survive.\n"),
                text);
        assertTrue(text.contains("Some words about the week that a person wrote."), text);
        assertTrue(text.contains("**In our own words** — Boris's paragraph."), text);
    }

    @Test
    void a_from_git_paragraph_that_was_wrapped_over_several_lines_is_replaced_whole() {
        String wrapped =
                "# 2026-W38 — 14 to 20 September\n\n## Anna A\n\n**From git** — 34 authored commits this week,\n"
                        + "0 written by the hook. Directories touched, by file count: `tools/src` (65),\n"
                        + "`docs/ai` (42).\n\n**In our own words** — Mine.\n";

        String text = log(wrapped, commit("anna@example.com", WEEK, "feat: a", "docs/x.md"));

        assertFalse(text.contains("tools/src"), text);
        assertFalse(text.contains("`docs/ai` (42)"), text);
        assertTrue(text.contains("**In our own words** — Mine."), text);
    }

    @Test
    void refreshing_updates_every_member_and_not_only_the_first() {
        String text = log(WRITTEN, commit("boris@example.com", WEEK, "feat: a", "docs/x.md"));

        assertTrue(section(text, "Boris B").contains("1 authored commit this week"), text);
        assertTrue(section(text, "Anna A").contains("0 authored commits this week"), text);
    }

    @Test
    void refreshing_twice_gives_the_same_file() {
        History.Commit only = commit("anna@example.com", WEEK, "feat: a", "docs/x.md");
        String once = log(WRITTEN, only);

        assertEquals(once, log(once, only));
    }

    @Test
    void a_member_missing_from_a_written_log_is_added_at_the_end() {
        String onlyAnna = WRITTEN.substring(0, WRITTEN.indexOf("## Boris B"));

        String text = log(onlyAnna, commit("boris@example.com", WEEK, "feat: a", "docs/x.md"));

        assertTrue(section(text, "Boris B").contains("1 authored commit this week"), text);
        assertTrue(text.contains("Anna wrote this herself"), text);
    }

    @Test
    void a_section_with_no_from_git_paragraph_gets_one_under_its_heading() {
        String bare =
                "# 2026-W38 — 14 to 20 September\n\n## Anna A\n\n**In our own words** — mine.\n\n## Boris B\n\nx\n";

        String text = log(bare, commit("anna@example.com", WEEK, "feat: a", "docs/x.md"));

        assertTrue(section(text, "Anna A").contains("**From git** — 1 authored commit"), text);
        assertTrue(section(text, "Anna A").contains("**In our own words** — mine."), text);
    }

    // --- the file ---

    @Test
    void write_creates_the_file_for_the_week_and_a_second_run_leaves_it_as_it_was() throws IOException {
        Weekly.write(repo, WEEK);
        String first = Files.readString(repo.resolve("docs/team/weekly-log/" + WEEK + ".md"));

        Weekly.write(repo, WEEK);

        assertEquals(first, Files.readString(repo.resolve("docs/team/weekly-log/" + WEEK + ".md")));
        assertTrue(first.startsWith("# 2026-W38"), first);
    }
}
