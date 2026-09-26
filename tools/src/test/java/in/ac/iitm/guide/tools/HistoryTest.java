package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Both the ownership table and the weekly log stand on this reading of `git log`, so a commit that
 * is misread here is misattributed in every document that measures contribution.
 */
class HistoryTest {

    @Test
    void a_commit_line_and_its_numstat_lines_become_one_commit_with_its_changes() {
        List<History.Commit> commits = History.parse(List.of(
                "@@|abc123|a@example.com|2026-W38|feat(FEAT-001): add the page [FR-001]",
                "",
                "12\t3\tapp/src/main/java/A.java",
                "0\t4\tdocs/b.md"));

        assertEquals(1, commits.size());
        History.Commit commit = commits.get(0);
        assertEquals("abc123", commit.sha());
        assertEquals("a@example.com", commit.email());
        assertEquals("2026-W38", commit.week());
        assertEquals("feat(FEAT-001): add the page [FR-001]", commit.subject());
        assertEquals(
                List.of(new History.Change("app/src/main/java/A.java", 12, 3), new History.Change("docs/b.md", 0, 4)),
                commit.changes());
    }

    @Test
    void a_pipe_inside_the_subject_stays_in_the_subject() {
        List<History.Commit> commits = History.parse(List.of("@@|abc|a@example.com|2026-W38|fix: a | b"));

        assertEquals("fix: a | b", commits.get(0).subject());
    }

    @Test
    void a_binary_file_counts_no_lines_but_still_counts_as_touched() {
        List<History.Commit> commits =
                History.parse(List.of("@@|abc|a@example.com|2026-W38|docs: logo", "-\t-\tdocs/logo.png"));

        assertEquals(
                List.of(new History.Change("docs/logo.png", 0, 0)),
                commits.get(0).changes());
    }

    @Test
    void a_commit_that_touches_no_file_has_no_changes() {
        List<History.Commit> commits = History.parse(List.of("@@|abc|a@example.com|2026-W38|chore: empty"));

        assertEquals(List.of(), commits.get(0).changes());
    }

    @Test
    void two_commits_stay_in_the_order_they_came_in_with_their_own_files() {
        List<History.Commit> commits = History.parse(List.of(
                "@@|one|a@example.com|2026-W38|first",
                "1\t0\tx.txt",
                "@@|two|b@example.com|2026-W38|second",
                "2\t0\ty.txt"));

        assertEquals(
                List.of("one", "two"), commits.stream().map(History.Commit::sha).toList());
        assertEquals("x.txt", commits.get(0).changes().get(0).path());
        assertEquals("y.txt", commits.get(1).changes().get(0).path());
    }

    @Test
    void a_commit_the_stop_hook_wrote_is_told_apart_by_its_subject() {
        History.Commit hook = History.parse(
                        List.of("@@|a|h@example.com|2026-W38|docs: record the journal entry for 2026-09-26 16:39"))
                .get(0);
        History.Commit person = History.parse(List.of("@@|b|h@example.com|2026-W38|docs: record the decision"))
                .get(0);

        assertTrue(hook.fromHook());
        assertFalse(person.fromHook());
    }

    // --- reading it from git ---

    @TempDir
    Path repoRoot;

    private void git(Map<String, String> environment, String... command) throws Exception {
        String[] full = new String[command.length + 5];
        full[0] = "git";
        full[1] = "-c";
        full[2] = "user.name=Test";
        full[3] = "-c";
        full[4] = "user.email=test@example.com";
        System.arraycopy(command, 0, full, 5, command.length);
        ProcessBuilder builder =
                new ProcessBuilder(full).directory(repoRoot.toFile()).redirectErrorStream(true);
        builder.environment().putAll(environment);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes());
        assertEquals(0, process.waitFor(), String.join(" ", command) + ": " + output);
    }

    @Test
    void the_real_history_is_read_with_the_authors_iso_week_and_the_lines_changed() throws Exception {
        git(Map.of(), "init", "-q");
        Files.createDirectories(repoRoot.resolve("app"));
        Files.writeString(repoRoot.resolve("app/A.java"), "one\ntwo\n");
        git(Map.of(), "add", "-A");
        git(
                Map.of(
                        "GIT_AUTHOR_DATE", "2026-09-21T10:00:00+05:30",
                        "GIT_COMMITTER_DATE", "2026-09-21T10:00:00+05:30",
                        "GIT_AUTHOR_NAME", "Someone",
                        "GIT_AUTHOR_EMAIL", "someone@example.com"),
                "commit",
                "-q",
                "-m",
                "feat: first | with a bar");

        List<History.Commit> commits = History.read(Repo.find(repoRoot.toString()));

        assertEquals(1, commits.size());
        assertEquals("someone@example.com", commits.get(0).email());
        assertEquals("2026-W39", commits.get(0).week());
        assertEquals("feat: first | with a bar", commits.get(0).subject());
        assertEquals(
                List.of(new History.Change("app/A.java", 2, 0)), commits.get(0).changes());
    }

    @Test
    void a_repository_with_no_commit_has_an_empty_history() throws Exception {
        git(Map.of(), "init", "-q");

        assertEquals(List.of(), History.read(Repo.find(repoRoot.toString())));
    }

    @Test
    void a_merge_commit_is_not_counted_as_anyones_work() throws Exception {
        git(Map.of(), "init", "-q", "-b", "main");
        Files.writeString(repoRoot.resolve("a.txt"), "a\n");
        git(Map.of(), "add", "-A");
        git(Map.of(), "commit", "-q", "-m", "base");
        git(Map.of(), "checkout", "-q", "-b", "side");
        Files.writeString(repoRoot.resolve("b.txt"), "b\n");
        git(Map.of(), "add", "-A");
        git(Map.of(), "commit", "-q", "-m", "on the side");
        git(Map.of(), "checkout", "-q", "main");
        Files.writeString(repoRoot.resolve("c.txt"), "c\n");
        git(Map.of(), "add", "-A");
        git(Map.of(), "commit", "-q", "-m", "on main");
        git(Map.of(), "merge", "-q", "--no-ff", "-m", "merge the side", "side");

        List<String> subjects = History.read(Repo.find(repoRoot.toString())).stream()
                .map(History.Commit::subject)
                .toList();

        assertEquals(3, subjects.size(), subjects.toString());
        assertFalse(subjects.contains("merge the side"));
    }
}
