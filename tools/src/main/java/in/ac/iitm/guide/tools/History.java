package in.ac.iitm.guide.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * The commits of the repository, read once and shared by everything that measures contribution.
 *
 * <p>Merge commits are left out (they are nobody's work) and renames are reported as a removal and
 * an addition, so that a moved file is not a line of `path => path` that no slice can claim.
 * {@link Commit#fromHook} is the distinction the whole team documentation rests on: the `Stop` hook
 * commits every journal entry under whoever's identity the session ran, and counting those as
 * authorship reads the team backwards (docs/team/README.md).
 */
final class History {

    /** Lines added and removed in one file by one commit; a binary file counts as zero of each. */
    record Change(String path, int added, int deleted) {}

    /** One commit. {@code week} is the ISO week of the author date, as {@code 2026-W39}. */
    record Commit(String sha, String email, String week, String subject, List<Change> changes) {

        boolean fromHook() {
            return JournalCommit.isJournalSubject(subject);
        }
    }

    /** Written before each commit so a numstat line can never be mistaken for one. */
    private static final String MARK = "@@|";

    private History() {}

    /** Newest first, as git prints them. An unreadable history is an empty one, reported by git itself. */
    static List<Commit> read(Repo repo) {
        Git.Result result = Git.run(
                repo,
                "log",
                "--no-merges",
                "--no-renames",
                "--date=format:%G-W%V",
                "--format=" + MARK + "%H|%ae|%ad|%s",
                "--numstat");
        return result.ok() ? parse(result.lines()) : List.of();
    }

    static List<Commit> parse(List<String> lines) {
        List<Commit> commits = new ArrayList<>();
        String[] head = null;
        List<Change> changes = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith(MARK)) {
                if (head != null) {
                    commits.add(commit(head, changes));
                }
                head = line.substring(MARK.length()).split("\\|", 4);
                changes = new ArrayList<>();
            } else if (head != null && !line.isBlank()) {
                Change change = change(line);
                if (change != null) {
                    changes.add(change);
                }
            }
        }
        if (head != null) {
            commits.add(commit(head, changes));
        }
        return commits;
    }

    private static Commit commit(String[] head, List<Change> changes) {
        String subject = head.length > 3 ? head[3] : "";
        return new Commit(head[0], head[1], head[2], subject, List.copyOf(changes));
    }

    private static Change change(String numstat) {
        String[] parts = numstat.split("\t", 3);
        if (parts.length < 3) {
            return null;
        }
        return new Change(parts[2], count(parts[0]), count(parts[1]));
    }

    /** Git prints {@code -} for a file it cannot count lines in. */
    private static int count(String text) {
        return text.equals("-") ? 0 : Integer.parseInt(text);
    }
}
