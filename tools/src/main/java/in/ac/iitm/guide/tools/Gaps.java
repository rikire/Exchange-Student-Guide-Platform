package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.util.List;

/**
 * The gap list: what is still planned or in progress, what is owed as debt, and where the chain from
 * requirement to test has a hole. Hiding a known gap is worse than declaring it, so the list is
 * built from the same facts as the matrix and nobody edits it.
 *
 * <p>Acceptance criteria carry no identifiers and a test is tied to a requirement, not to a single
 * criterion, so "criteria with no test" cannot be exact. It is counted as criteria minus anchored
 * tests, which is a lower bound: a requirement with more criteria than anchored tests certainly has
 * one uncovered, and one with as many may still have a test doing two jobs. The column says so.
 */
public final class Gaps {

    private static final String FILE = "docs/gap-list.md";
    private static final String COMMAND = "java -jar tools/target/ai-tools.jar gaps";

    private Gaps() {}

    public static String build(Repo repo) throws IOException {
        Trace.Model model = Trace.read(repo);

        StringBuilder out = new StringBuilder();
        out.append("# Gap list\n\n");
        out.append("Everything still planned or in progress, every open debt entry, and every requirement whose\n");
        out.append("acceptance criteria outnumber the tests anchored to it. Hiding a known gap is explicitly worse\n");
        out.append("than declaring it.\n\n");
        out.append("**Generated** by `").append(COMMAND).append("`. An edit made here is lost on the next run.\n");

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

    private static int testsFor(Trace.Model model, Trace.Requirement requirement) {
        return (int) model.anchors().stream()
                .filter(anchor -> anchor.id().equals(requirement.id()) && anchor.where() == Trace.Where.TEST)
                .count();
    }

    private static void cell(StringBuilder out, String value) {
        out.append(" | ").append(value.replace("|", "\\|"));
    }
}
