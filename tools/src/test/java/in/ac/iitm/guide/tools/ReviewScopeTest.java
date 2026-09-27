package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReviewScopeTest {
    @TempDir
    Path root;

    private void git(String... args) throws Exception {
        var command = new java.util.ArrayList<String>();
        command.add("git");
        command.addAll(java.util.List.of(args));
        var process = new ProcessBuilder(command)
                .directory(root.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        assertEquals(0, process.waitFor(), output);
    }

    private String baseline() throws Exception {
        git("init", "-q");
        git("config", "user.email", "fixture@example.invalid");
        git("config", "user.name", "Fixture");
        Files.writeString(root.resolve("tracked.txt"), "original");
        git("add", ".");
        git("commit", "-qm", "baseline");
        return ReviewScope.capture(root, "HEAD").base();
    }

    @Test
    void includes_committed_staged_unstaged_deleted_and_new_files() throws Exception {
        String base = baseline();
        Files.writeString(root.resolve("committed.txt"), "commit");
        git("add", ".");
        git("commit", "-qm", "change");
        Files.writeString(root.resolve("staged.txt"), "stage");
        git("add", ".");
        Files.writeString(root.resolve("committed.txt"), "unstaged");
        Files.delete(root.resolve("tracked.txt"));
        Files.writeString(root.resolve("new file.txt"), "new");
        assertEquals(
                java.util.List.of("committed.txt", "new file.txt", "staged.txt", "tracked.txt"),
                ReviewScope.capture(root, base).files());
    }

    @Test
    void fingerprint_changes_when_content_changes_without_a_commit() throws Exception {
        String base = baseline();
        var before = ReviewScope.capture(root, base);
        assertEquals(before.fingerprint(), ReviewScope.capture(root, base).fingerprint());
        Files.writeString(root.resolve("tracked.txt"), "changed");
        assertNotEquals(before.fingerprint(), ReviewScope.capture(root, base).fingerprint());
    }

    @Test
    void staging_and_new_file_content_invalidate_evidence() throws Exception {
        String base = baseline();
        Files.writeString(root.resolve("new.txt"), "one");
        String first = ReviewScope.capture(root, base).fingerprint();
        Files.writeString(root.resolve("new.txt"), "two");
        assertNotEquals(first, ReviewScope.capture(root, base).fingerprint());
        String unstaged = ReviewScope.capture(root, base).fingerprint();
        git("add", ".");
        assertNotEquals(unstaged, ReviewScope.capture(root, base).fingerprint());
    }

    @Test
    void invalid_base_is_an_error_not_an_empty_review() throws Exception {
        baseline();
        assertThrows(java.io.IOException.class, () -> ReviewScope.capture(root, "missing-base"));
    }

    @Test
    void a_staged_revert_does_not_hide_the_committed_change() throws Exception {
        String base = baseline();
        Files.writeString(root.resolve("tracked.txt"), "committed change");
        git("add", ".");
        git("commit", "-qm", "change");
        Files.writeString(root.resolve("tracked.txt"), "original");
        git("add", ".");
        assertEquals(
                java.util.List.of("tracked.txt"),
                ReviewScope.capture(root, base).files());
    }

    @Test
    void docs_sync_sees_staged_changes_even_when_worktree_restores_base() throws Exception {
        baseline();
        Path controller = root.resolve("app/src/main/java/in/ac/iitm/guide/search/SearchController.java");
        Files.createDirectories(controller.getParent());
        Files.writeString(controller, "original");
        git("add", ".");
        git("commit", "-qm", "controller");
        Files.writeString(controller, "changed");
        git("add", ".");
        Files.writeString(controller, "original");
        assertFalse(DocsSync.check(Repo.find(root.toString()), "HEAD").isEmpty());
    }
}
