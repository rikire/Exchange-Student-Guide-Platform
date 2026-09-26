package in.ac.iitm.guide.tools;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point of the repository's AI process tooling.
 *
 * <pre>
 *   hook prompt            UserPromptSubmit — open a journal entry, report edits made by hand;
 *                          a background notification opens no entry
 *   hook subagent          SubagentStop     — note in the journal that a subagent finished
 *   hook guard             PreToolUse       — ask before editing a file the human owns
 *   hook bash              PreToolUse       — refuse a command that skips or hides a check
 *   hook stop              Stop             — close the journal entry
 *   hook compact           PreCompact       — record that the conversation was shortened
 *   hook note &lt;text&gt;       add a note to the current journal entry
 *   hook english           supply the English rendering of the prompt and the outcome
 *   hook author &lt;id&gt;       record who is sending the prompts, when git could not say
 *   commit-msg &lt;file&gt;      check the commit message convention
 *   docs-check             find documentation that describes what the repository lacks
 *   authors                check every committer resolves to a member of the team registry
 *   trace                  write docs/traceability.md and docs/features/README.md from the repository
 *   trace --check          fail on a gap in the chain or on a generated file that is out of date
 *   trace --docs-sync &lt;ref&gt;  fail when a change since ref touches the schema, the routes or a slice
 *                          boundary and the document describing it did not change
 *   gaps                   write docs/gap-list.md: what is not done, open debt, criteria with no test
 *   ownership              write docs/team/ownership.md from git history, the hook's commits apart
 *   schema-freeze          fail when a migration added after the freeze names no ADR in its header
 *   weekly [--week W]      refresh the "From git" paragraphs of a week's log (default: this week)
 * </pre>
 *
 * <p>A failure inside a hook must not break the human's session: every error is reported on stderr
 * and the process still exits with 0. The one exception is {@code commit-msg}, where a non-zero
 * exit is the whole point.
 */
public final class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: ai-tools <hook|commit-msg> [...]");
            System.exit(64);
        }
        try {
            // The set is the authority, not the switch: a case added without it is rejected
            // loudly rather than drifting away from what DocsCheck believes exists.
            if (!Commands.TOP_LEVEL.contains(args[0])) {
                System.err.println("ai-tools: unknown command " + args[0] + "; expected one of "
                        + Commands.describe(Commands.TOP_LEVEL));
                System.exit(64);
            }
            switch (args[0]) {
                case "hook" -> HookCommand.run(Arrays.copyOfRange(args, 1, args.length));
                case "commit-msg" -> commitMsg(Arrays.copyOfRange(args, 1, args.length));
                case "docs-check" -> docsCheck();
                case "authors" -> authors();
                case "count" -> count();
                case "trace" -> trace(Arrays.copyOfRange(args, 1, args.length));
                case "gaps" -> {
                    Gaps.write(Repo.find(null));
                    System.out.println("wrote docs/gap-list.md");
                }
                case "ownership" -> {
                    Ownership.write(Repo.find(null));
                    System.out.println("wrote docs/team/ownership.md");
                }
                case "weekly" -> weekly(Arrays.copyOfRange(args, 1, args.length));
                case "schema-freeze" -> schemaFreeze();
                default -> throw new IllegalStateException("dispatch missing for " + args[0]);
            }
        } catch (Exception e) {
            System.err.println("ai-tools: " + e.getMessage());
            System.exit(0);
        }
    }

    private static void docsCheck() throws Exception {
        List<DocsCheck.Problem> problems = DocsCheck.run(Repo.find(null));
        if (problems.isEmpty()) {
            return;
        }
        System.err.println("Documentation describes things the repository does not contain:");
        problems.forEach(problem -> System.err.println("  " + problem));
        System.err.println();
        // The phase escape covers the rules about things that do not exist yet. It does not cover a
        // roadmap step with no checkable result, and telling someone to name a phase there sends
        // them to do something that will not work - which is how a check earns its reputation.
        System.err.println("Either build the thing, or say on the same line which phase it belongs to.");
        System.err.println("A roadmap step is the exception: it needs a `— check:`, and naming a phase will not do.");
        System.exit(1);
    }

    /**
     * The number of requirements, so that nobody has to derive it by eye again.
     *
     * <p>Three of four figures sent to the course on 9 September were wrong because a count of
     * `### FR-` lines took the illustrative entry in each format section for a real requirement.
     */
    private static void count() throws java.io.IOException {
        Requirements.Counts counts = Requirements.of(Repo.find(null));
        System.out.println("functional requirements:     " + counts.functional());
        System.out.println("non-functional requirements: " + counts.nonFunctional());
        System.out.println("constraints:                 " + counts.constraints());
        System.out.println("use cases:                   " + counts.useCases());
    }

    private static void trace(String[] args) throws Exception {
        Repo repo = Repo.find(null);
        if (args.length == 1 && args[0].equals("--check")) {
            List<String> found = Trace.check(repo);
            if (found.isEmpty()) {
                return;
            }
            System.err.println("The chain from requirement to code and test has gaps:");
            found.forEach(problem -> System.err.println("  - " + problem));
            System.exit(1);
        }
        if (args.length == 2 && args[0].equals("--docs-sync")) {
            docsSync(repo, args[1]);
            return;
        }
        if (args.length != 0) {
            System.err.println("usage: ai-tools trace [--check | --docs-sync <ref>]");
            System.exit(64);
        }
        Trace.write(repo);
        Trace.Report report = Trace.build(repo);
        System.out.println("wrote docs/traceability.md and docs/features/README.md");
        report.problems().forEach(problem -> System.out.println("  gap: " + problem));
        report.notes().forEach(note -> System.out.println("  note: " + note));
    }

    private static void schemaFreeze() throws Exception {
        List<String> found = SchemaFreeze.check(Repo.find(null), java.time.Clock.systemDefaultZone());
        if (found.isEmpty()) {
            return;
        }
        System.err.println("The schema is frozen, and these migrations changed it without an agreement:");
        found.forEach(problem -> System.err.println("  - " + problem));
        System.exit(1);
    }

    private static void weekly(String[] args) throws Exception {
        String week = Weekly.weekOf(java.time.LocalDate.now());
        if (args.length == 2 && args[0].equals("--week")) {
            week = args[1];
        } else if (args.length != 0) {
            System.err.println("usage: ai-tools weekly [--week 2026-W39]");
            System.exit(64);
        }
        Weekly.write(Repo.find(null), week);
        System.out.println("wrote docs/team/weekly-log/" + week + ".md");
    }

    private static void docsSync(Repo repo, String ref) {
        List<DocsSync.Finding> findings;
        try {
            findings = DocsSync.check(repo, ref);
        } catch (Exception e) {
            // Not the fail-open of a hook: a gate that could not read the change must not say "fine".
            System.err.println("ai-tools: " + e.getMessage());
            System.exit(2);
            return;
        }
        boolean refused = false;
        for (DocsSync.Finding finding : findings) {
            System.err.println((finding.blocks() ? "BLOCKS  " : "warns   ") + String.join(", ", finding.files())
                    + " changed; update " + finding.update());
            refused |= finding.blocks();
        }
        if (refused) {
            System.err.println();
            System.err.println(
                    "The mapping is in docs/ai/docs-sync.md. Update the document in substance, in the same change.");
            System.exit(1);
        }
    }

    private static void authors() throws Exception {
        Repo repo = Repo.find(null);
        List<String> unknown = Members.load(repo).unregisteredAuthors(repo);
        if (unknown.isEmpty()) {
            return;
        }
        System.err.println("These author addresses belong to nobody in docs/team/members.yml:");
        unknown.forEach(email -> System.err.println("  " + email));
        System.err.println();
        System.err.println("Registering an address is not the same as committing with one. Until they match,");
        System.err.println("that person's work is attributed to nobody in the journal and the contribution log.");
        System.exit(1);
    }

    private static void commitMsg(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: ai-tools commit-msg <file>");
            System.exit(64);
        }
        List<String> problems = CommitMessageCheck.check(Path.of(args[0]));
        if (problems.isEmpty()) {
            return;
        }
        System.err.println("The commit message does not follow the convention:");
        problems.forEach(problem -> System.err.println("  - " + problem));
        System.err.println();
        System.err.println("  feat(FEAT-003): add article submission form [FR-012]");
        System.err.println("  refactor(DEBT-002): replace the in-memory rate limiter");
        System.err.println("  docs: describe the moderation state machine");
        System.err.println();
        System.err.println("See docs/repository-map.md, section \"Commit convention\".");
        System.exit(1);
    }
}
