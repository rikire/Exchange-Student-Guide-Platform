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
    void records_the_files_the_human_changed_by_hand() throws IOException {
        Journal journal = Journal.open(repo, "session-f");
        journal.startEntry("carry on");
        journal.finishEntry("Continued.", List.of(), List.of("docs/tech-debt.md (modified)"));

        assertTrue(journalContent().contains("- docs/tech-debt.md (modified)"));
    }
}
