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
        // A real repository is never empty, and an empty snapshot means "none taken yet".
        Files.writeString(repoRoot.resolve("README.md"), "baseline");
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
    void records_that_the_context_was_compacted() throws IOException {
        // The journal exists so that the trail of decisions survives. Compaction discards part of
        // the conversation those decisions were made in, so an entry that does not say it happened
        // reads as a complete record of a turn that no longer has one.
        Journal journal = Journal.open(repo, "session-c");
        journal.startEntry("Work through the migration");
        journal.addCompactionMarker("auto");

        String content = journalContent();
        assertTrue(content.contains("compacted"), content);
        assertTrue(content.contains("auto"), "the trigger says whether a person asked for it");
    }

    @Test
    void records_a_compaction_even_when_no_entry_is_open() throws IOException {
        // Compaction does not wait for a turn to be in progress, and a marker that is dropped
        // because the bookkeeping was between entries is the one case that most needs recording.
        Journal.open(repo, "session-d").addCompactionMarker("manual");

        assertTrue(journalContent().contains("compacted"));
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
    void a_new_prompt_gives_the_rendering_gate_a_fresh_refusal() throws IOException {
        // Session-scoped state would mean one refusal exempts every later turn: forget the rendering
        // once, be let through for the rest of the day. The debt is per turn, so the flags are too.
        Journal journal = Journal.open(repo, "session-n");
        journal.startEntry("сделай что-нибудь");
        journal.setNeedsEnglish();
        journal.setRenderingRefused();

        journal.startEntry("сделай что-нибудь ещё");

        assertFalse(journal.needsEnglish(), "the new turn decides for itself whether it owes one");
        assertFalse(journal.renderingRefused(), "and the gate may refuse it once");
    }

    @Test
    void closing_an_entry_clears_what_the_turn_owed() throws IOException {
        Journal journal = Journal.open(repo, "session-o");
        journal.startEntry("сделай что-нибудь");
        journal.setNeedsEnglish();
        journal.setRenderingRefused();

        journal.finishEntry("Done.", List.of(), List.of());

        assertFalse(journal.needsEnglish());
        assertFalse(journal.renderingRefused());
    }

    @Test
    void records_the_files_the_human_changed_by_hand() throws IOException {
        Journal journal = Journal.open(repo, "session-f");
        journal.startEntry("carry on");
        journal.finishEntry("Continued.", List.of(), List.of("docs/tech-debt.md (modified)"));

        assertTrue(journalContent().contains("- docs/tech-debt.md (modified)"));
    }

    @Test
    void a_documentation_reminder_is_remembered_for_the_session() throws IOException {
        // docs-sync.md asks for once per rule per session: the same reminder repeated for every
        // file in a batch is how a reminder becomes something to scroll past.
        Journal journal = Journal.open(repo, "session-r");
        assertFalse(journal.alreadyReminded("migration"));

        journal.setReminded("migration");
        journal.save();

        Journal reopened = Journal.open(repo, "session-r");
        assertTrue(reopened.alreadyReminded("migration"));
        assertFalse(reopened.alreadyReminded("routes"), "a different area still earns its own reminder");
    }

    @Test
    void a_machine_turn_writes_no_entry() throws IOException {
        Journal journal = Journal.open(repo, "session-m");
        journal.beginMachineTurn();
        journal.endMachineTurn();

        assertFalse(Files.exists(repo.resolve("docs/ai/journal")), "no entry and no journal file");
    }

    @Test
    void a_machine_turn_is_marked_until_it_ends() throws IOException {
        Journal journal = Journal.open(repo, "session-m2");

        journal.beginMachineTurn();
        assertTrue(journal.isMachineTurn());

        journal.endMachineTurn();
        assertFalse(journal.isMachineTurn());
    }

    @Test
    void a_hand_edit_made_before_a_machine_turn_is_still_reported_at_the_next_prompt() throws IOException {
        // Skipping the entry must not move the snapshot past the human's edit, or the agent is never
        // told about it and may overwrite it.
        Journal journal = Journal.open(repo, "session-m3");
        journal.startEntry("first prompt");
        journal.finishEntry("done", List.of(), List.of());
        Files.writeString(repo.resolve("by-hand.md"), "the human wrote this");

        journal.beginMachineTurn();
        journal.endMachineTurn();

        List<String> reported = journal.startEntry("next prompt");
        assertTrue(reported.contains("by-hand.md (added)"), reported.toString());
    }

    @Test
    void a_file_the_agent_wrote_during_a_machine_turn_is_not_reported_as_the_humans() throws IOException {
        // The report is headed "files the human changed by hand" and tells the agent not to argue
        // with it, so listing the agent's own edits there would make it obey itself.
        Journal journal = Journal.open(repo, "session-m4");
        journal.startEntry("first prompt");
        journal.finishEntry("done", List.of(), List.of());

        journal.beginMachineTurn();
        Files.writeString(repo.resolve("by-agent.md"), "the agent wrote this");
        journal.endMachineTurn();

        assertEquals(List.of(), journal.startEntry("next prompt"));
    }

    @Test
    void a_notification_inside_a_human_turn_leaves_that_turn_alone() throws IOException {
        // The harness delivers a subagent's report in the middle of a turn. Treating it as the start
        // of a machine turn would hand the agent's own edits to the human and leave this entry open.
        Journal journal = Journal.open(repo, "session-m6");
        journal.startEntry("a person's request");
        Files.writeString(repo.resolve("by-agent.md"), "written during the turn");

        journal.beginMachineTurn();

        assertFalse(journal.isMachineTurn());
        assertTrue(journal.hasOpenEntry());
        journal.finishEntry("Done.", List.of(), List.of());
        assertTrue(journalContent().contains("> a person's request"));
        assertEquals(List.of(), journal.startEntry("next prompt"));
    }

    @Test
    void a_second_notification_during_a_machine_turn_does_not_carry_the_agents_edits() throws IOException {
        Journal journal = Journal.open(repo, "session-m7");
        journal.startEntry("first prompt");
        journal.finishEntry("done", List.of(), List.of());
        journal.beginMachineTurn();
        Files.writeString(repo.resolve("by-agent.md"), "written during the machine turn");

        journal.beginMachineTurn();
        journal.endMachineTurn();

        assertEquals(List.of(), journal.startEntry("next prompt"));
    }

    @Test
    void a_carried_hand_edit_is_reported_once() throws IOException {
        Journal journal = Journal.open(repo, "session-m5");
        journal.startEntry("first prompt");
        journal.finishEntry("done", List.of(), List.of());
        Files.writeString(repo.resolve("by-hand.md"), "the human wrote this");
        journal.beginMachineTurn();
        journal.endMachineTurn();
        journal.startEntry("second prompt");
        journal.finishEntry("done again", List.of(), List.of());

        assertEquals(List.of(), journal.startEntry("third prompt"));
    }
}
