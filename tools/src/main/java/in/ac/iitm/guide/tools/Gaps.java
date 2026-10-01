package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The gap list: what is still planned or in progress, what is owed as debt, and where the chain from
 * requirement to test has a hole. Hiding a known gap is worse than declaring it, so the list is
 * built from the same facts as the matrix and nobody edits it.
 *
 * <p>Acceptance criteria carry no identifiers and a test is tied to a requirement, not to a single
 * criterion, so "criteria with no test" cannot be exact. It is counted as criteria minus anchored
 * tests, which is a lower bound: a requirement with more criteria than anchored tests certainly has
 * one uncovered, and one with as many may still have a test doing two jobs. The column says so.
 *
 * <p>The progress section on top draws the same facts as Mermaid, which the forge renders from the
 * text itself, so the picture cannot fall behind the file the way a committed image can. A slice's
 * requirements are what its features cover; the arrows between slices are the dependencies Spring
 * Modulith read from the imports, so they show the code as it is rather than as it was designed.
 */
public final class Gaps {

    private static final String FILE = "docs/gap-list.md";
    private static final String COMMAND = "java -jar tools/target/ai-tools.jar gaps";

    /** Written by ModularityTest; the same file scripts/diagrams.sh renders as c4-component-actual. */
    private static final String MODULES = "app/target/spring-modulith-docs/components.puml";

    private static final Pattern MODULE = Pattern.compile("^\\s*Component\\([\\w.]*\\.(\\w+),", Pattern.MULTILINE);
    private static final Pattern DEPENDENCY =
            Pattern.compile("^\\s*Rel\\([\\w.]*\\.(\\w+), [\\w.]*\\.(\\w+),", Pattern.MULTILINE);

    private Gaps() {}

    public static String build(Repo repo) throws IOException {
        Trace.Model model = Trace.read(repo);

        StringBuilder out = new StringBuilder();
        out.append("# Gap list\n\n");
        out.append("Everything still planned or in progress, every open debt entry, and every requirement whose\n");
        out.append("acceptance criteria outnumber the tests anchored to it. Hiding a known gap is explicitly worse\n");
        out.append("than declaring it.\n\n");
        out.append("**Generated** by `").append(COMMAND).append("`. An edit made here is lost on the next run.\n");

        progress(repo, model, out);

        List<Trace.Requirement> open = model.requirements().stream()
                .filter(requirement ->
                        requirement.kind().equals("FR") || requirement.kind().equals("NFR"))
                .filter(requirement -> requirement.status().equals("planned")
                        || requirement.status().equals("in-progress"))
                .toList();
        out.append("\n## Requirements not done\n\n");
        if (open.isEmpty()) {
            out.append("None.\n");
        } else {
            out.append("\"No test, at least\" is the criteria minus the tests anchored to the requirement; the\n");
            out.append("real figure can only be higher.\n\n");
            out.append("| Requirement | Status | Priority | Title | Criteria | Tests | No test, at least |\n");
            out.append("|---|---|---|---|---|---|---|\n");
            for (Trace.Requirement requirement : open) {
                int tests = testsFor(model, requirement);
                out.append("| ").append(requirement.id());
                cell(out, requirement.status());
                cell(out, requirement.priority());
                cell(out, requirement.title());
                cell(out, String.valueOf(requirement.scenarios()));
                cell(out, String.valueOf(tests));
                cell(out, String.valueOf(Math.max(0, requirement.scenarios() - tests)));
                out.append(" |\n");
            }
        }

        List<Trace.Requirement> uneven = model.requirements().stream()
                .filter(requirement -> requirement.status().equals("done"))
                .filter(requirement -> requirement.scenarios() > testsFor(model, requirement))
                .toList();
        out.append("\n## Done, with criteria that no test is anchored to\n\n");
        if (uneven.isEmpty()) {
            out.append("None.\n");
        } else {
            out.append("| Requirement | Criteria | Tests | Title |\n");
            out.append("|---|---|---|---|\n");
            for (Trace.Requirement requirement : uneven) {
                out.append("| ").append(requirement.id());
                cell(out, String.valueOf(requirement.scenarios()));
                cell(out, String.valueOf(testsFor(model, requirement)));
                cell(out, requirement.title());
                out.append(" |\n");
            }
        }

        List<Trace.DebtEntry> debt = model.debt().stream()
                .filter(entry -> entry.status().equals("open"))
                .toList();
        out.append("\n## Open technical debt\n\n");
        if (debt.isEmpty()) {
            out.append("None.\n");
        } else {
            out.append("| Debt | Title | Trigger |\n");
            out.append("|---|---|---|\n");
            for (Trace.DebtEntry entry : debt) {
                out.append("| ").append(entry.id());
                cell(out, entry.title());
                cell(out, entry.trigger());
                out.append(" |\n");
            }
        }

        out.append("\n## Gaps in the traceability chain\n\n");
        if (model.problems().isEmpty()) {
            out.append("None.\n");
        } else {
            model.problems().forEach(problem -> out.append("- ").append(problem).append('\n'));
        }
        return out.toString();
    }

    public static void write(Repo repo) throws IOException {
        repo.write(repo.resolve(FILE), build(repo));
    }

    /** The list as it would be written now, against the one in the repository. */
    public static List<String> check(Repo repo) throws IOException {
        Path file = repo.resolve(FILE);
        if (Files.isRegularFile(file) && Files.readString(file).equals(build(repo))) {
            return List.of();
        }
        return List.of(FILE + " is out of date; run `" + COMMAND + "`");
    }

    private static void progress(Repo repo, Trace.Model model, StringBuilder out) throws IOException {
        Path modules = repo.resolve(MODULES);
        if (!Files.isRegularFile(modules)) {
            // No fallback that draws the slices without arrows: a picture missing half its facts
            // would be committed and trusted.
            throw new IllegalStateException(MODULES + " is missing: run ./mvnw test first, ModularityTest writes it");
        }
        String puml = Files.readString(modules);

        Map<String, Trace.Requirement> counted = new LinkedHashMap<>();
        for (Trace.Requirement requirement : model.requirements()) {
            if ((requirement.kind().equals("FR") || requirement.kind().equals("NFR"))
                    && !requirement.status().equals("out-of-scope")) {
                counted.put(requirement.id(), requirement);
            }
        }

        Map<String, Set<String>> covered = new TreeMap<>();
        Matcher module = MODULE.matcher(puml);
        while (module.find()) {
            covered.put(module.group(1).toLowerCase(), new TreeSet<>());
        }
        Set<String> mapped = new TreeSet<>();
        for (Trace.Feature feature : model.features()) {
            if (feature.slice().isBlank()) {
                continue;
            }
            Set<String> ids = covered.computeIfAbsent(feature.slice(), slice -> new TreeSet<>());
            for (String id : feature.covers()) {
                if (counted.containsKey(id)) {
                    ids.add(id);
                    mapped.add(id);
                }
            }
        }
        Set<String> arrows = new TreeSet<>();
        Matcher dependency = DEPENDENCY.matcher(puml);
        while (dependency.find()) {
            arrows.add(dependency.group(1).toLowerCase() + " --> "
                    + dependency.group(2).toLowerCase());
        }
        List<String> unmapped =
                counted.keySet().stream().filter(id -> !mapped.contains(id)).toList();

        Map<String, Integer> byStatus = new LinkedHashMap<>();
        for (String status : List.of("done", "in-progress", "planned")) {
            byStatus.put(status, 0);
        }
        counted.values().forEach(requirement -> byStatus.merge(requirement.status(), 1, Integer::sum));

        out.append("\n## Progress\n\n");
        out.append("Functional and non-functional requirements by status, out-of-scope ones left out.\n\n");
        out.append("```mermaid\npie showData title Requirements by status\n");
        byStatus.forEach((status, count) -> out.append("    \"")
                .append(status)
                .append("\" : ")
                .append(count)
                .append('\n'));
        out.append("```\n\n");

        out.append("Each slice with the requirements its features cover: green when all are done, amber when\n");
        out.append("some are done or in progress, grey when none has started or no feature names the slice.\n");
        out.append("The arrows are the dependencies Spring Modulith read from the code (`")
                .append(MODULES);
        out.append("`, written by `ModularityTest`).\n\n");
        out.append("```mermaid\nflowchart LR\n");
        List<String[]> rows = new ArrayList<>();
        covered.forEach((slice, ids) -> {
            long done = ids.stream()
                    .filter(id -> counted.get(id).status().equals("done"))
                    .count();
            boolean started =
                    ids.stream().anyMatch(id -> !counted.get(id).status().equals("planned"));
            String state = ids.isEmpty() ? "none" : done == ids.size() ? "done" : started ? "partial" : "none";
            String label = ids.isEmpty() ? "no feature" : done + "/" + ids.size() + " done";
            out.append("    ")
                    .append(slice)
                    .append("[\"")
                    .append(slice)
                    .append(" · ")
                    .append(label)
                    .append("\"]:::")
                    .append(state)
                    .append('\n');
            rows.add(new String[] {slice, String.valueOf(ids.size()), String.valueOf(done), state});
        });
        if (!unmapped.isEmpty()) {
            out.append("    unmapped[\"no feature yet: ")
                    .append(String.join(", ", unmapped))
                    .append("\"]:::unmapped\n");
        }
        arrows.forEach(arrow -> out.append("    ").append(arrow).append('\n'));
        out.append("    classDef done fill:#2e7d32,stroke:#1b5e20,color:#ffffff\n");
        out.append("    classDef partial fill:#f9a825,stroke:#f57f17,color:#000000\n");
        out.append("    classDef none fill:#e0e0e0,stroke:#9e9e9e,color:#000000\n");
        out.append("    classDef unmapped fill:#ffffff,stroke:#c62828,stroke-dasharray:4 3,color:#000000\n");
        out.append("```\n\n");

        out.append("| Slice | Requirements covered | Done | State |\n");
        out.append("|---|---|---|---|\n");
        for (String[] row : rows) {
            out.append("| ").append(row[0]);
            cell(out, row[1]);
            cell(out, row[2]);
            cell(out, row[3]);
            out.append(" |\n");
        }
        out.append("\nNo feature covers yet: ")
                .append(unmapped.isEmpty() ? "none" : String.join(", ", unmapped))
                .append(".\n");
    }

    private static int testsFor(Trace.Model model, Trace.Requirement requirement) {
        return (int) model.anchors().stream()
                .filter(anchor -> anchor.id().equals(requirement.id()) && anchor.where() == Trace.Where.TEST)
                .count();
    }

    private static void cell(StringBuilder out, String value) {
        out.append(" | ").append(value.replace("|", "\\|"));
    }
}
