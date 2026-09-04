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

class DocsCheckTest {

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

    private List<DocsCheck.Problem> check() throws IOException {
        return DocsCheck.run(repo);
    }

    private static String details(List<DocsCheck.Problem> problems) {
        return String.join(
                " | ", problems.stream().map(DocsCheck.Problem::detail).toList());
    }

    @Test
    void a_relative_link_to_a_missing_file_is_a_problem() throws IOException {
        write("docs/a.md", "See [the other one](b.md).\n");

        assertTrue(details(check()).contains("link to a missing file: b.md"));
    }

    @Test
    void a_relative_link_that_resolves_is_not() throws IOException {
        write("docs/a.md", "See [the other one](b.md).\n");
        write("docs/b.md", "Here.\n");

        assertEquals(List.of(), check());
    }

    @Test
    void a_link_is_resolved_from_the_linking_files_own_directory() throws IOException {
        write("docs/deep/a.md", "Up one: [map](../map.md).\n");
        write("docs/map.md", "Here.\n");

        assertEquals(List.of(), check());
    }

    @Test
    void an_advertised_slash_command_without_a_command_file_is_a_problem() throws IOException {
        write("CLAUDE.md", "Slash commands: `/feature` and `/invented`.\n");
        write(".claude/commands/feature.md", "---\n---\n");

        assertTrue(details(check()).contains("advertises /invented"));
    }

    @Test
    void a_subcommand_the_tool_does_not_dispatch_is_a_problem() throws IOException {
        write("docs/a.md", "Run `ai-tools invented` to fix it.\n");

        assertTrue(details(check()).contains("cites `ai-tools invented`"));
    }

    @Test
    void a_dispatched_subcommand_is_not() throws IOException {
        write("docs/a.md", "Run `ai-tools commit-msg` and `ai-tools hook prompt`.\n");

        assertEquals(List.of(), check());
    }

    @Test
    void work_that_is_still_ahead_is_excused_by_naming_its_phase_on_the_same_line() throws IOException {
        write("docs/a.md", "java -jar tools/target/ai-tools.jar trace   # phase 2\n");

        assertEquals(List.of(), check());
    }

    @Test
    void the_phase_marker_only_excuses_its_own_line() throws IOException {
        write("docs/a.md", "The generator arrives in phase 2.\n\nRun `ai-tools trace` now.\n");

        assertTrue(details(check()).contains("cites `ai-tools trace`"));
    }

    @Test
    void the_tools_name_in_prose_is_not_read_as_an_invocation() throws IOException {
        // A looser pattern reported subcommands called "jar" and "with" from sentences like this,
        // and a check that cries wolf is a check that gets switched off.
        write("docs/a.md", "The `ai-tools.jar` with the hooks lives in tools/target.\n");

        assertEquals(List.of(), check());
    }

    @Test
    void a_named_script_that_does_not_exist_is_a_problem() throws IOException {
        write("docs/a.md", "Refresh them with `scripts/diagrams.sh`.\n");

        assertTrue(details(check()).contains("scripts/diagrams.sh"));
    }

    @Test
    void a_hook_presented_as_automation_but_not_wired_is_a_problem() throws IOException {
        write("docs/repository-map.md", "| `PostToolUse` hook | fires on an edit | reminds you |\n");
        write(".claude/settings.json", "{\"hooks\": {\"Stop\": []}}");

        assertTrue(details(check()).contains("PostToolUse"));
    }

    @Test
    void a_hook_that_is_wired_is_not() throws IOException {
        write("docs/repository-map.md", "| `Stop` hook | ends a turn | closes the entry |\n");
        write(".claude/settings.json", "{\"hooks\": {\"Stop\": []}}");

        assertEquals(List.of(), check());
    }

    @Test
    void the_journal_is_never_checked() throws IOException {
        // It is an append-only record that legitimately says something was missing at the time.
        // Failing a build over it would invite editing the evidence to satisfy the checker.
        write("docs/ai/journal/2026-09-04-abcd.md", "Ran `ai-tools invented`; [gone](nowhere.md).\n");

        assertEquals(List.of(), check());
    }
}
