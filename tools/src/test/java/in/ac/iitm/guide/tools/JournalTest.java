package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JournalTest {

    @TempDir
    Path repoRoot;

    private Repo repo;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(repoRoot.resolve(".git"));
        repo = Repo.find(repoRoot.toString());
    }

    private String journalContent() throws IOException {
        try (Stream<Path> files = Files.list(repo.resolve("docs/ai/journal"))) {
            Path entry = files.filter(p -> p.getFileName().toString().endsWith(".md"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no journal file was written"));
            return Files.readString(entry);
        }
    }

    @Test
    void writes_the_prompt_and_the_outcome() throws IOException {
        Journal journal = Journal.open(repo, "session-a");
        journal.startEntry("Set up the repository scaffolding");
        journal.finishEntry("Built the Maven skeleton.", List.of("gate: passed"), List.of());

        String content = journalContent();
        assertTrue(content.contains("> Set up the repository scaffolding"));
        assertTrue(content.contains("> Built the Maven skeleton."));
        assertTrue(content.contains("- gate: passed"));
    }

    @Test
    void keeps_the_original_prompt_and_adds_the_english_rendering() throws IOException {
        Journal journal = Journal.open(repo, "session-b");
        journal.startEntry("Настрой каркас репозитория");
        journal.setEnglish("Set up the repository scaffolding", "Built the Maven skeleton.");
        journal.finishEntry("Собрал каркас Maven.", List.of(), List.of());

        String content = journalContent();
        assertTrue(
                content.contains("> Настрой каркас репозитория"),
                "the original prompt is the artefact and must survive");
        assertTrue(content.contains("**Prompt (English)**"));
        assertTrue(content.contains("> Set up the repository scaffolding"));
        assertTrue(content.contains("> Built the Maven skeleton."), "the outcome is written in English");
        assertFalse(
                content.contains("Собрал каркас Maven."),
                "the English outcome replaces the original, it does not duplicate it");
    }

    @Test
    void omits_the_translation_block_when_the_prompt_is_already_english() throws IOException {
        Journal journal = Journal.open(repo, "session-c");
        journal.startEntry("Set up the repository scaffolding");
        journal.setEnglish("Set up the repository scaffolding", null);
        journal.finishEntry("Done.", List.of(), List.of());

        assertFalse(journalContent().contains("**Prompt (English)**"));
    }

    @Test
    void falls_back_to_the_assistant_message_when_no_english_outcome_was_given() throws IOException {
        Journal journal = Journal.open(repo, "session-d");
        journal.startEntry("do the thing");
        journal.finishEntry("The thing is done.", List.of(), List.of());

        assertTrue(journalContent().contains("> The thing is done."));
    }

    @Test
    void does_not_carry_a_translation_over_into_the_next_entry() throws IOException {
        Journal first = Journal.open(repo, "session-e");
        first.startEntry("Первый промпт");
        first.setEnglish("First prompt", "First outcome.");
        first.finishEntry("Первый ответ.", List.of(), List.of());

        Journal second = Journal.open(repo, "session-e");
        second.startEntry("Второй промпт");
        second.finishEntry("Second answer.", List.of(), List.of());

        // Both strings belong to the first entry, so each must appear exactly once. Asserting
        // absence would be wrong: it would fail on the entry that legitimately contains them.
        String content = journalContent();
        assertEquals(1, occurrences(content, "First prompt"), "a stale translated prompt must not be reused");
        assertEquals(1, occurrences(content, "First outcome."), "a stale translated outcome must not be reused");
        assertTrue(content.contains("> Second answer."), "the second entry falls back to the assistant message");
    }

    private static int occurrences(String haystack, String needle) {
        return haystack.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }

    @Test
    void names_the_author_of_every_entry() throws IOException {
        Journal journal = Journal.open(repo, "session-g");
        journal.setAuthorIfUnknown("mikhail", "Mikhail Novikov");
        journal.startEntry("do the thing");
        journal.finishEntry("Done.", List.of(), List.of());

        assertTrue(journalContent().contains("**Author:** Mikhail Novikov"));
    }

    @Test
    void says_so_plainly_when_the_author_could_not_be_resolved() throws IOException {
        Journal journal = Journal.open(repo, "session-h");
        journal.startEntry("do the thing");
        journal.finishEntry("Done.", List.of(), List.of());

        assertTrue(journalContent().contains("unresolved"), "a blank author must read as unresolved, not as absent");
    }

    @Test
    void an_author_named_by_hand_cannot_overwrite_one_git_resolved() throws IOException {
        Journal journal = Journal.open(repo, "session-i");
        journal.setAuthorIfUnknown("mikhail", "Mikhail Novikov");

        assertFalse(
                journal.setAuthorIfUnknown("abdirakhim", "Abdirakhim Ismailov"),
                "filling a blank is allowed; reassigning work to someone else is not");
        assertEquals("Mikhail Novikov", journal.author());
    }

    @Test
    void the_author_carries_across_entries_in_one_session() throws IOException {
        Journal first = Journal.open(repo, "session-j");
        first.setAuthorIfUnknown("mikhail", "Mikhail Novikov");
        first.startEntry("first");
        first.finishEntry("one", List.of(), List.of());
        first.save();

        Journal second = Journal.open(repo, "session-j");
        second.startEntry("second");
        second.finishEntry("two", List.of(), List.of());

        // The question is asked once per session, not once per prompt.
        assertEquals(2, occurrences(journalContent(), "**Author:** Mikhail Novikov"));
    }

    @Test
    void an_entry_is_open_between_the_prompt_and_the_outcome() throws IOException {
        Journal journal = Journal.open(repo, "session-k");
        assertFalse(journal.hasOpenEntry(), "nothing is open before a prompt arrives");

        journal.startEntry("do the thing");
        assertTrue(journal.hasOpenEntry(), "open while the turn is in progress");

        journal.finishEntry("Done.", List.of(), List.of());
        assertFalse(journal.hasOpenEntry(), "closed once the outcome is written");
    }

    @Test
    void an_open_entry_records_when_its_prompt_arrived() throws IOException {
        // How a rendering finds its entry: newest open prompt wins. A session that ended without
        // its Stop hook stays open for ever, so "exactly one open" stops being true after a few
        // days, and ordering has to work rather than uniqueness.
        Journal journal = Journal.open(repo, "session-m");
        assertTrue(journal.promptedAt().isEmpty(), "nothing recorded before a prompt");

        journal.startEntry("do the thing");
        assertFalse(journal.promptedAt().isEmpty(), "an open entry knows when it opened");
    }

    @Test
    void a_missing_rendering_is_visible_rather_than_silent() throws IOException {
        Journal journal = Journal.open(repo, "session-l");
        journal.startEntry("сделай что-нибудь");
        assertFalse(journal.hasEnglishRendering());

        journal.setEnglish("do something", null);
        assertTrue(journal.hasEnglishRendering());
    }

    @Test
    void records_the_files_the_human_changed_by_hand() throws IOException {
        Journal journal = Journal.open(repo, "session-f");
        journal.startEntry("carry on");
        journal.finishEntry("Continued.", List.of(), List.of("docs/tech-debt.md (modified)"));

        assertTrue(journalContent().contains("- docs/tech-debt.md (modified)"));
    }
}
