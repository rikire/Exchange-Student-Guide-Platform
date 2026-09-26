package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * What the `Stop` hook and CI refuse to let through, in one place.
 *
 * <p>Four checks, each tested on its own: documentation that describes what the repository lacks
 * ({@link DocsCheck}), the chain from requirement to test and the generated files that state it
 * ({@link Trace}), a migration added after the schema froze that names no ADR
 * ({@link SchemaFreeze}), and a change to the schema, the routes or a slice boundary with no word in
 * the document that describes it ({@link DocsSync}). Of the last, only the rows that block in
 * docs-sync.md's table are refused here; the rest are for the person to read.
 *
 * <p>A check that could not run is not a check that passed. It is reported in {@code notRun} so that
 * the journal says so, and it does not refuse: a broken git must not lock a session that has done
 * nothing wrong, and the same check runs again in CI.
 */
final class Gate {

    /** {@code problems} refuse the turn; {@code notRun} name the checks that could not be made. */
    record Result(List<String> problems, List<String> notRun) {}

    private Gate() {}

    static Result evaluate(Repo repo) throws IOException {
        List<String> problems = new ArrayList<>();
        List<String> notRun = new ArrayList<>();

        DocsCheck.run(repo).forEach(problem -> problems.add(problem.toString()));
        problems.addAll(Trace.check(repo));
        problems.addAll(SchemaFreeze.check(repo, Clock.systemDefaultZone()));

        try {
            for (DocsSync.Finding finding : DocsSync.check(repo, "HEAD")) {
                if (finding.blocks()) {
                    problems.add(String.join(", ", finding.files()) + " changed; update " + finding.update());
                }
            }
        } catch (IOException e) {
            notRun.add("docs-sync: not run (" + e.getMessage() + ")");
        }
        return new Result(List.copyOf(problems), List.copyOf(notRun));
    }
}
