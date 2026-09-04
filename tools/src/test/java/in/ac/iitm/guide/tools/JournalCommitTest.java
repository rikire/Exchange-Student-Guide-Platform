package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises the commit against a real repository rather than a mock.
 *
 * <p>What is being tested is git's behaviour under a pathspec, so a fake would only assert that the
 * commands were spelled the way the test expected them to be spelled.
 */
class JournalCommitTest {

    @TempDir
    Path repoRoot;

    private Repo repo;

    @BeforeEach
    void setUp() throws IOException {
        repo = temporaryRepo();
    }

    private Repo temporaryRepo() throws IOException {
        Files.createDirectories(repoRoot);
        Repo bare = unchecked(repoRoot);
        Git.run(bare, "init", "--quiet", "--initial-branch=main");
        Git.run(bare, "config", "user.email", "test@example.com");
        Git.run(bare, "config", "user.name", "Test");
        Git.run(bare, "config", "commit.gpgsign", "false");
        // The repository's own hooks are not under test, and a machine without them installed must
        // reach the same result as one with them.
        Git.run(bare, "config", "core.hooksPath", repoRoot.resolve("no-hooks").toString());

        Files.writeString(repoRoot.resolve("README.md"), "first\n");
        Git.run(bare, "add", "README.md");
        Git.run(bare, "commit", "--quiet", "-m", "chore: first");
        return bare;
    }

    /** {@link Repo#find} needs a marker, and git creates one only after init. */
    private Repo unchecked(Path root) throws IOException {
        Files.createDirectories(root.resolve(".git"));
        return Repo.find(root.toString());
    }

    private void writeEntry(String text) throws IOException {
        repo.write(repo.resolve(JournalCommit.JOURNAL_PATH + "/2026-09-04-abcd1234.md"), text);
    }

    private List<String> log() {
        return Git.run(repo, "log", "--format=%s").lines();
    }

    @Test
    void commits_an_entry_the_turn_that_wrote_it_could_not() throws IOException {
        writeEntry("# Session abcd1234\n");

        assertEquals("", JournalCommit.commitPending(repo, ZonedDateTime.now()));

        assertTrue(log().get(0).startsWith("docs: record the journal entry for"));
        assertTrue(
                Git.run(repo, "status", "--porcelain").lines().isEmpty(),
                "the journal should be committed, not merely staged");
    }

    @Test
    void leaves_everything_outside_the_journal_alone() throws IOException {
        // The whole objection to committing from a hook is that it might sweep up work in progress.
        Files.writeString(repoRoot.resolve("README.md"), "edited by a person\n");
        Files.writeString(repoRoot.resolve("staged.txt"), "staged by a person\n");
        Git.run(repo, "add", "staged.txt");
        writeEntry("# Session abcd1234\n");

        assertEquals("", JournalCommit.commitPending(repo, ZonedDateTime.now()));

        List<String> committed =
                Git.run(repo, "show", "--name-only", "--format=", "HEAD").lines();
        assertEquals(List.of("docs/ai/journal/2026-09-04-abcd1234.md"), committed);

        String status = String.join("\n", Git.run(repo, "status", "--porcelain").lines());
        assertTrue(status.contains("README.md"), "the person's unstaged edit must survive untouched");
        assertTrue(status.contains("staged.txt"), "the person's staged file must stay staged, not be committed");
    }

    @Test
    void does_nothing_when_there_is_no_entry_pending() {
        assertEquals("", JournalCommit.commitPending(repo, ZonedDateTime.now()));

        assertEquals(1, log().size(), "an empty journal must not produce an empty commit");
    }

    @Test
    void refuses_while_git_is_halfway_through_something() throws IOException {
        writeEntry("# Session abcd1234\n");
        Files.writeString(repoRoot.resolve(".git/MERGE_HEAD"), "0000000\n");

        String problem = JournalCommit.commitPending(repo, ZonedDateTime.now());

        assertTrue(problem.contains("MERGE_HEAD"), "the refusal must name what stopped it: " + problem);
        assertEquals(1, log().size());
    }

    @Test
    void refuses_on_a_detached_head_where_the_commit_would_be_lost() throws IOException {
        Git.run(repo, "checkout", "--quiet", "--detach", "HEAD");
        writeEntry("# Session abcd1234\n");

        String problem = JournalCommit.commitPending(repo, ZonedDateTime.now());

        assertTrue(problem.contains("detached"), "the refusal must say why: " + problem);
        assertEquals(1, log().size());
    }

    @Test
    void the_message_passes_the_convention_the_commit_msg_hook_enforces() throws IOException {
        Path file = repoRoot.resolve("message.txt");
        Files.writeString(file, JournalCommit.messageFor(ZonedDateTime.now()));

        // A machine-written message that its own gate rejects would block every turn's commit.
        assertEquals(List.of(), CommitMessageCheck.check(file));
    }

    @Test
    void a_failed_commit_leaves_the_entry_to_be_retried_rather_than_lost() throws IOException {
        writeEntry("# Session abcd1234\n");
        // An identity git will not accept is the cheapest way to make the commit itself fail.
        Git.run(repo, "config", "user.email", "");
        Git.run(repo, "config", "user.name", "");

        String problem = JournalCommit.commitPending(repo, ZonedDateTime.now());

        assertFalse(problem.isEmpty(), "a failure must be reported, not swallowed");
        assertFalse(
                Git.run(repo, "status", "--porcelain", "--", JournalCommit.JOURNAL_PATH)
                        .lines()
                        .isEmpty(),
                "the entry must still be pending so the next turn commits it");
    }
}
