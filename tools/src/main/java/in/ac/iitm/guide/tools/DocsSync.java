package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The end-of-turn and CI half of docs/ai/docs-sync.md: a change in a tracked area with none of the
 * documents that describe it changed beside it.
 *
 * <p>{@link DocumentedCounterparts} says what a change obliges, and the edit-time hook says it once.
 * This is what holds the promise afterwards, so that a reminder ignored is not a reminder forgotten.
 * The two read the same table, which is why the rules are not repeated here.
 *
 * <p>"Changed" is everything that differs from a ref, committed or not, including files git has not
 * been told about yet: a migration that was just created is exactly the case this exists for.
 */
public final class DocsSync {

    /** One tracked area that changed without the document that describes it. */
    public record Finding(String key, List<String> files, String update, boolean blocks) {}

    private DocsSync() {}

    /** The tracked areas among {@code changed} whose documentation is not among it either. */
    public static List<Finding> findings(Set<String> changed) {
        Map<String, DocumentedCounterparts.Rule> rules = new LinkedHashMap<>();
        Map<String, Set<String>> files = new LinkedHashMap<>();
        for (String path : new TreeSet<>(changed)) {
            DocumentedCounterparts.Rule rule = DocumentedCounterparts.forPath(path);
            if (rule != null) {
                rules.putIfAbsent(rule.key(), rule);
                files.computeIfAbsent(rule.key(), key -> new LinkedHashSet<>()).add(path);
            }
        }
        List<Finding> found = new ArrayList<>();
        rules.forEach((key, rule) -> {
            if (changed.stream().noneMatch(rule.satisfiedBy())) {
                found.add(new Finding(key, List.copyOf(files.get(key)), rule.update(), rule.blocks()));
            }
        });
        return found;
    }

    /**
     * Findings for everything that differs from {@code ref}. An unknown ref is an error and not an
     * empty answer: a gate that reads nothing and says "fine" is worse than no gate.
     */
    public static List<Finding> check(Repo repo, String ref) throws IOException {
        Set<String> changed = new LinkedHashSet<>();
        Git.Result diff = Git.run(repo, "diff", "--name-only", ref, "--");
        if (!diff.ok()) {
            throw new IOException("git could not compare with " + ref + " (exit " + diff.exitCode() + ")");
        }
        changed.addAll(diff.lines());
        Git.Result untracked = Git.run(repo, "ls-files", "--others", "--exclude-standard");
        if (!untracked.ok()) {
            throw new IOException("git could not list new files (exit " + untracked.exitCode() + ")");
        }
        changed.addAll(untracked.lines());
        changed.removeIf(String::isBlank);
        return findings(changed);
    }
}
