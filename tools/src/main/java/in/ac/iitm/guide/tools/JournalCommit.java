package in.ac.iitm.guide.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Commits the journal entries the hooks have written, and nothing else.
 *
 * <p>An entry is appended when a turn ends, after the turn's last action. So the turn an entry
 * describes can never be the turn that commits it: the record is always one turn behind the history,
 * and on 4 September an entry sat uncommitted until a person happened to open the file. Nothing in
 * the repository owned getting it in.
 *
 * <p>A hook that writes to history is a real cost, accepted deliberately. What follows is the part
 * that keeps the cost bounded: the pathspec is always {@code docs/ai/journal}, so a commit in
 * progress elsewhere is never swept up, and every state where a commit would be wrong — a merge, a
 * rebase, a detached HEAD — is a reason to do nothing and say so.
 */
final class JournalCommit {

    /** The only path this may ever stage or commit. */
    static final String JOURNAL_PATH = "docs/ai/journal";

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Markers in the git directory that mean a commit now would land in the middle of something. */
    private static final List<String> IN_PROGRESS =
            List.of("MERGE_HEAD", "CHERRY_PICK_HEAD", "REVERT_HEAD", "BISECT_LOG", "rebase-merge", "rebase-apply");

    private static final String BODY =
            """
            Written and committed by the Stop hook: an entry is appended after the turn's
            last action, so the turn it describes cannot be the turn that commits it.

            Only docs/ai/journal is touched; anything else staged stays staged.

            Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>""";

    private JournalCommit() {}

    static String messageFor(ZonedDateTime when) {
        return "docs: record the journal entry for " + STAMP.format(when);
    }

    /**
     * Commits whatever is pending under {@code docs/ai/journal}.
     *
     * @return the empty string when there was nothing to do or the commit succeeded, otherwise a
     *     sentence naming what stopped it — for the hook's stderr, where a person can read it
     */
    static String commitPending(Repo repo, ZonedDateTime when) {
        Git.Result gitDir = Git.run(repo, "rev-parse", "--absolute-git-dir");
        if (!gitDir.ok()) {
            return "journal not committed: this is not a git working tree";
        }

        Path dir = Path.of(gitDir.first());
        for (String marker : IN_PROGRESS) {
            if (Files.exists(dir.resolve(marker))) {
                return "journal not committed: " + marker + " says git is mid-operation; "
                        + "commit docs/ai/journal yourself once it finishes";
            }
        }
        // On a detached HEAD the commit would belong to no branch and be lost at the next checkout.
        if (!Git.run(repo, "symbolic-ref", "--quiet", "HEAD").ok()) {
            return "journal not committed: HEAD is detached, so the commit would belong to no branch";
        }

        if (Git.run(repo, "status", "--porcelain", "--", JOURNAL_PATH).lines().isEmpty()) {
            return "";
        }
        // Needed because a new day's file is untracked, and a partial commit can only name paths
        // git already knows. The pathspec keeps it to the journal.
        if (!Git.run(repo, "add", "--", JOURNAL_PATH).ok()) {
            return "journal not committed: git add failed";
        }

        // A partial commit: it takes the working-tree content of these paths and leaves every other
        // staged change exactly where it was.
        Git.Result commit = Git.run(repo, "commit", "-m", messageFor(when), "-m", BODY, "--", JOURNAL_PATH);
        if (!commit.ok()) {
            return "journal not committed: git commit failed, so the entry stays staged "
                    + "and the next turn that ends will try again";
        }
        return "";
    }
}
