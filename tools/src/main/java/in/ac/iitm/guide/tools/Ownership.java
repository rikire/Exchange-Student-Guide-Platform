package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Who has actually done more in each slice, measured from git rather than assigned in advance.
 *
 * <p>The rubric asks for a named owner per module and a rough balance between two people who take
 * whatever task they like. Ownership is therefore a reading of the history, and the reading has one
 * trap that docs/team/README.md records: the `Stop` hook commits every journal entry under the
 * identity of whoever's machine the session ran on. Those commits are never counted as authorship;
 * they are reported in a column of their own and never added to the authored one.
 *
 * <p>"More work" is the larger number of authored commits touching the slice. Lines are shown beside
 * them and do not decide it: a seed article or a generated file moves hundreds of lines and says
 * little about who understands the slice.
 */
public final class Ownership {

    private static final String FILE = "docs/team/ownership.md";
    private static final String COMMAND = "java -jar tools/target/ai-tools.jar ownership";
    private static final String UNREGISTERED = "UNREGISTERED";

    private static final String BASE_PACKAGE = "app/src/main/java/in/ac/iitm/guide";
    private static final Pattern CODE_OR_TEST =
            Pattern.compile("^app/src/(?:main|test)/java/in/ac/iitm/guide/([^/]+)/");
    private static final Pattern TEMPLATE = Pattern.compile("^app/src/main/resources/templates/([^/]+)/");
    private static final String MIGRATIONS = "app/src/main/resources/db/migration/";

    private Ownership() {}

    /** The slice a path belongs to, or null for a path no slice can claim. */
    static String slicePathOf(String path) {
        Matcher code = CODE_OR_TEST.matcher(path);
        if (code.find()) {
            return code.group(1);
        }
        Matcher template = TEMPLATE.matcher(path);
        if (template.find()) {
            return template.group(1);
        }
        // One schema serves every slice, so its migrations are the shared layer's.
        return path.startsWith(MIGRATIONS) ? "shared" : null;
    }

    public static void write(Repo repo) throws IOException {
        List<String> slices = new ArrayList<>();
        Path base = repo.resolve(BASE_PACKAGE);
        if (Files.isDirectory(base)) {
            try (Stream<Path> list = Files.list(base)) {
                list.filter(Files::isDirectory)
                        .map(directory -> directory.getFileName().toString())
                        .sorted()
                        .forEach(slices::add);
            }
        }
        repo.write(repo.resolve(FILE), build(History.read(repo), Members.load(repo), slices));
    }

    static String build(List<History.Commit> commits, Members members, List<String> slices) {
        List<String> contributors = new ArrayList<>();
        members.all().forEach(member -> contributors.add(member.id()));
        boolean anyUnregistered = commits.stream()
                .anyMatch(commit -> members.byEmail(commit.email()).isEmpty());
        if (anyUnregistered) {
            contributors.add(UNREGISTERED);
        }

        // slice -> contributor -> [commits, lines]
        Map<String, Map<String, int[]>> perSlice = new LinkedHashMap<>();
        slices.forEach(slice -> perSlice.put(slice, new LinkedHashMap<>()));
        Map<String, int[]> totals = new LinkedHashMap<>();
        contributors.forEach(id -> totals.put(id, new int[2]));

        for (History.Commit commit : commits) {
            String who = members.byEmail(commit.email()).map(Members.Member::id).orElse(UNREGISTERED);
            if (commit.fromHook()) {
                totals.get(who)[1]++;
                continue;
            }
            totals.get(who)[0]++;
            Map<String, Integer> lines = new LinkedHashMap<>();
            for (History.Change change : commit.changes()) {
                String slice = slicePathOf(change.path());
                if (slice != null && perSlice.containsKey(slice)) {
                    lines.merge(slice, change.added() + change.deleted(), Integer::sum);
                }
            }
            lines.forEach((slice, count) -> {
                int[] cell = perSlice.get(slice).computeIfAbsent(who, key -> new int[2]);
                cell[0]++;
                cell[1] += count;
            });
        }

        StringBuilder out = new StringBuilder();
        out.append("# Slice ownership\n\n");
        out.append(
                "Who has actually done more in each slice, measured from git history rather than assigned in advance.\n\n");
        out.append("**Generated** by `").append(COMMAND).append("`. An edit made here is lost on the next run.\n\n");
        out.append("A slice's commits are the authored commits that touch its code, its tests or its templates, and\n");
        out.append(
                "the migrations count for `shared`. \"More work\" is the larger number of commits; lines are shown\n");
        out.append(
                "beside them and do not decide it. The journal commits the `Stop` hook writes are never counted as\n");
        out.append("authorship: they are reported in their own column below and never added to the authored one.\n");

        out.append("\n## Per slice\n\n");
        out.append("| Slice");
        contributors.forEach(id ->
                out.append(" | ").append(id).append(" commits | ").append(id).append(" lines"));
        out.append(" | More work |\n|---");
        contributors.forEach(id -> out.append("|---|---"));
        out.append("|---|\n");
        perSlice.forEach((slice, cells) -> {
            out.append("| ").append(slice);
            int best = 0;
            List<String> leaders = new ArrayList<>();
            for (String id : contributors) {
                int[] cell = cells.getOrDefault(id, new int[2]);
                out.append(" | ").append(cell[0]).append(" | ").append(cell[1]);
                if (cell[0] > best) {
                    best = cell[0];
                    leaders.clear();
                }
                if (cell[0] == best && best > 0) {
                    leaders.add(id);
                }
            }
            String verdict = best == 0 ? "no work yet" : leaders.size() == 1 ? leaders.get(0) : "even";
            out.append(" | ").append(verdict).append(" |\n");
        });

        out.append("\n## All authored work, and the hook's\n\n");
        out.append("| Member | Authored commits | Journal commits (Stop hook) |\n|---|---|---|\n");
        totals.forEach((id, cell) -> out.append("| ")
                .append(id)
                .append(" | ")
                .append(cell[0])
                .append(" | ")
                .append(cell[1])
                .append(" |\n"));
        return out.toString();
    }
}
