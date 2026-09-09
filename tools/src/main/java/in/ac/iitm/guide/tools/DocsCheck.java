package in.ac.iitm.guide.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Catches documentation that describes something the repository does not contain.
 *
 * <p>Written after an audit found eight divergences on a green build, seven of which were exactly
 * this: a link to a missing file, a slash command that did not exist, a subcommand the tool never
 * dispatched, a hook listed as live automation while unwired. None of them could fail a test, and
 * all of them would have been read as true.
 *
 * <p>Work that is genuinely still ahead is not a defect, so a mention is excused when its own line
 * says which phase it belongs to. That convention was already in the documents before this check
 * existed; the check only makes it load-bearing.
 */
public final class DocsCheck {

    /** One divergence between a document and the repository. */
    public record Problem(String file, String detail) {

        @Override
        public String toString() {
            return file + ": " + detail;
        }
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Pattern MARKDOWN_LINK = Pattern.compile("]\\(([^)#\\s]+\\.md)(#[^)]*)?\\)");
    private static final Pattern SLASH_COMMAND = Pattern.compile("`/([a-z][a-z-]*)`");
    /**
     * Anchored on something that reads as an invocation, not on the tool's name appearing in prose.
     *
     * <p>A looser pattern matched "the `ai-tools.jar` with the hooks" and reported subcommands
     * called "jar" and "with", which is the kind of false positive that gets a check switched off.
     */
    private static final Pattern INVOCATION = Pattern.compile(
            "(?:java\\s+-jar\\s+\\S*ai-tools\\.jar|`ai-tools)\\s+(?:hook\\s+([a-z][a-z-]*)|([a-z][a-z-]*))");

    private static final Pattern SCRIPT = Pattern.compile("`?(scripts/[a-z][a-z-]*\\.sh)`?");
    /**
     * The lifecycle events this repository may describe as its own automation.
     *
     * <p>The list started at the four events that were wired, which meant a table row about any
     * other event was unchecked prose - the same hole {@code PostToolUse} fell through before this
     * check existed. It is a list rather than a wildcard so that "the `Stop` hook" in a sentence
     * about someone else's repository is not read as a claim about this one.
     */
    private static final Pattern DOCUMENTED_HOOK = Pattern.compile("`(UserPromptSubmit|PreToolUse|PostToolUse|Stop"
            + "|SessionStart|SessionEnd|PreCompact|PostCompact|SubagentStop)`\\s+hook");

    private static final Pattern PHASE_MARKER = Pattern.compile("phase\\s+[0-9]", Pattern.CASE_INSENSITIVE);

    /** Files whose slash-command mentions are treated as advertising them as available. */
    private static final List<String> COMMAND_CATALOGUES = List.of("CLAUDE.md", "docs/ai/README.md");

    private static final Set<String> SKIPPED_DIRECTORIES = Set.of(".git", "target", "node_modules");

    /**
     * The journal is excluded on purpose.
     *
     * <p>It is an append-only record of what was said and done, written by the hooks, and it
     * legitimately reports that something was missing at the time. Failing a build over it would
     * invite editing the record to satisfy a checker, which destroys the only property that makes
     * the journal evidence rather than a claim.
     */
    private static final String JOURNAL = "docs/ai/journal/";

    private final Repo repo;

    private DocsCheck(Repo repo) {
        this.repo = repo;
    }

    /** What the requirements files actually contain, read once and compared against every claim. */
    private Requirements.Counts counts;

    /** One markdown file, read once. */
    private record Document(Path path, String relative, List<String> lines) {}

    public static List<Problem> run(Repo repo) throws IOException {
        DocsCheck check = new DocsCheck(repo);
        check.counts = Requirements.of(repo);

        // Walked and read once, then handed to every rule. Each rule used to fetch the file list
        // itself, so the tree was walked four times and every file read three times - which was
        // most of the cost of running this at all.
        List<Document> documents = check.readMarkdown();

        List<Problem> problems = new ArrayList<>();
        for (Document document : documents) {
            for (String line : document.lines()) {
                check.checkLine(document, line, problems);
            }
        }
        problems.addAll(check.advertisedCommands());
        problems.addAll(check.documentedHooks());
        for (Document document : documents) {
            problems.addAll(check.roadmapItemsWithoutAResult(document));
            problems.addAll(check.statedCounts(document));
        }
        return problems;
    }

    /** A phase file of the roadmap. The index and the session notes beside it are not plans. */
    private static final Pattern PHASE_FILE = Pattern.compile("^docs/roadmap/0[0-9]-[a-z-]+\\.md$");

    private static final Pattern OPEN_ITEM = Pattern.compile("^- \\[[ ~]] ");
    private static final Pattern ANY_ITEM = Pattern.compile("^- \\[[ x~]] ");
    private static final Pattern CONTINUATION = Pattern.compile("^\\s+\\S");

    /** Either shape is in use, written by different people, and both say the same thing. */
    private static final Pattern RESULT = Pattern.compile("[—-]\\s*check:");

    /**
     * A roadmap step that does not say what would confirm it.
     *
     * <p>{@code docs/ai/roadmap.md} states the rule — "an item without a checkable result is not an
     * item" — and the roadmap broke it in 30 of its own 48 open items, phase 3 entirely. A rule the
     * plan states and does not keep is the same defect as a document describing what the repository
     * lacks, which is what the rest of this class exists to catch.
     *
     * <p>Deliberately not subject to {@link #PHASE_MARKER}. Every line in a phase file is about work
     * still ahead, so that escape would let the rule be switched off by writing the word "phase".
     */
    private List<Problem> roadmapItemsWithoutAResult(Document document) {
        if (!PHASE_FILE.matcher(document.relative()).matches()) {
            return List.of();
        }
        List<Problem> problems = new ArrayList<>();
        String open = null;
        boolean found = false;

        for (String line : document.lines()) {
            if (open != null
                    && CONTINUATION.matcher(line).find()
                    && !ANY_ITEM.matcher(line).find()) {
                found = found || RESULT.matcher(line).find();
                continue;
            }
            if (open != null && !found) {
                problems.add(problemFor(document, open));
            }
            open = OPEN_ITEM.matcher(line).find() ? line : null;
            found = open != null && RESULT.matcher(line).find();
        }
        if (open != null && !found) {
            problems.add(problemFor(document, open));
        }
        return problems;
    }

    private Problem problemFor(Document document, String item) {
        String text = item.strip();
        return new Problem(
                document.relative(),
                "this step has no checkable result — say what confirms it, after a `— check:`:\n      "
                        + (text.length() > 90 ? text.substring(0, 90) + "…" : text));
    }

    /**
     * A number of requirements, written down where it can go stale or be miscounted.
     *
     * <p>Both shapes the repository uses. `5 `NFR`` is the shorthand; the prose form is what goes
     * into a document someone outside the project reads.
     */
    private static final Pattern COUNT_SHORTHAND = Pattern.compile("(\\d+) `(FR|NFR|CON)`");

    private static final Pattern COUNT_PROSE = Pattern.compile("(\\d+) (functional requirements?"
            + "|non-functional(?: ones| requirements?)?|(?:recorded )?constraints?|use cases?)");

    /**
     * Files that record a moment rather than describe the present.
     *
     * <p>A dated audit and a weekly log state what was true when they were written, and correcting
     * their figures later would destroy the only thing they are for. The journal is already left
     * out for the same reason.
     */
    private static boolean isARecord(String relative) {
        return relative.startsWith("docs/ai/audit-") || relative.startsWith("docs/team/weekly-log/");
    }

    /** A date near a count marks it as a statement about that day, not about today. */
    private static final Pattern DATED = Pattern.compile("\\d{1,2} (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)"
            + "|\\d{4}-\\d{2}-\\d{2}|Between .* September");

    /**
     * A stated number of requirements that disagrees with the files.
     *
     * <p>Three of the four figures in a reply sent to the course were wrong, because a count of
     * `### FR-` lines takes the illustrative entry in each format section for a real requirement.
     * Read across neighbouring lines rather than one at a time, because a sentence carrying a date
     * and the figures it dates are routinely wrapped apart.
     */
    private List<Problem> statedCounts(Document document) {
        if (isARecord(document.relative())) {
            return List.of();
        }
        List<Problem> problems = new ArrayList<>();
        List<String> lines = document.lines();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // A blockquote is a quotation - the text of a sent email, a stakeholder's own words.
            // Rewriting one to satisfy a checker destroys what makes it evidence.
            if (line.stripLeading().startsWith(">")) {
                continue;
            }
            String window = (i > 0 ? lines.get(i - 1) + " " : "") + line;
            if (DATED.matcher(window).find()) {
                continue;
            }
            for (Pattern pattern : List.of(COUNT_SHORTHAND, COUNT_PROSE)) {
                Matcher counted = pattern.matcher(line);
                while (counted.find()) {
                    String problem = countProblem(counted.group(1), counted.group(2));
                    if (problem != null) {
                        problems.add(new Problem(document.relative(), problem));
                    }
                }
            }
        }
        return problems;
    }

    /** Reports a stated count that disagrees with the files, or null when it agrees. */
    private String countProblem(String stated, String kind) {
        int claimed = Integer.parseInt(stated);
        int actual;
        String name;
        if (kind.equals("FR") || kind.startsWith("functional")) {
            actual = counts.functional();
            name = "functional requirements";
        } else if (kind.equals("NFR") || kind.startsWith("non-functional")) {
            actual = counts.nonFunctional();
            name = "non-functional requirements";
        } else if (kind.equals("CON") || kind.endsWith("constraint") || kind.endsWith("constraints")) {
            actual = counts.constraints();
            name = "constraints";
        } else {
            actual = counts.useCases();
            name = "use cases";
        }
        return claimed == actual
                ? null
                : "states a count of " + claimed + " " + name + ", but there are " + actual
                        + ". The entry under `## Format` in each requirements file demonstrates the shape and is"
                        + " not a requirement. Take the number from `ai-tools count` rather than by eye.";
    }

    /** Every rule that judges a single line, applied in one pass. */
    private void checkLine(Document document, String line, List<Problem> problems) {
        Matcher links = MARKDOWN_LINK.matcher(line);
        while (links.find()) {
            String target = links.group(1);
            if (!target.startsWith("http")
                    && !Files.exists(document.path().getParent().resolve(target).normalize())) {
                problems.add(new Problem(document.relative(), "link to a missing file: " + target));
            }
        }

        // Work that is genuinely still ahead is not a defect, so a line that names its phase is
        // excused from the two rules below - but never from the broken-link rule above, because a
        // link to a file that does not exist is broken whenever it is followed.
        if (PHASE_MARKER.matcher(line).find()) {
            return;
        }

        Matcher invocations = INVOCATION.matcher(line);
        while (invocations.find()) {
            String hook = invocations.group(1);
            String top = invocations.group(2);
            if (hook != null && !Commands.HOOK.contains(hook)) {
                problems.add(new Problem(document.relative(), "cites `hook " + hook + "`, which is not dispatched"));
            } else if (top != null && !Commands.TOP_LEVEL.contains(top)) {
                problems.add(new Problem(
                        document.relative(),
                        "cites `ai-tools " + top + "`, which is not dispatched;"
                                + " say on this line which phase it belongs to if it is still ahead"));
            }
        }

        Matcher scripts = SCRIPT.matcher(line);
        while (scripts.find()) {
            String script = scripts.group(1);
            if (!Files.isRegularFile(repo.resolve(script))) {
                problems.add(new Problem(document.relative(), "names " + script + ", which does not exist"));
            }
        }
    }

    private List<Document> readMarkdown() throws IOException {
        List<Document> documents = new ArrayList<>();
        for (Path file : markdownFiles()) {
            documents.add(new Document(file, relative(file), Files.readAllLines(file)));
        }
        return documents;
    }

    /**
     * A slash command named in the instructions that nothing in {@code .claude} implements.
     *
     * <p>Worse than one that was never advertised, because it gets tried.
     */
    private List<Problem> advertisedCommands() throws IOException {
        List<Problem> problems = new ArrayList<>();
        for (String name : COMMAND_CATALOGUES) {
            Path file = repo.resolve(name);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            Set<String> seen = new LinkedHashSet<>();
            for (String line : Files.readAllLines(file)) {
                Matcher matcher = SLASH_COMMAND.matcher(line);
                while (matcher.find()) {
                    String command = matcher.group(1);
                    if (seen.add(command) && !isImplemented(command)) {
                        problems.add(new Problem(name, "advertises /" + command + ", which has no command file"));
                    }
                }
            }
        }
        return problems;
    }

    /**
     * Either shape counts, because both are real to whoever types the command.
     *
     * <p>Skills replaced commands as the platform's mechanism, and this repository migrated. The
     * older shape is still accepted rather than dropped: a check that fails on a repository
     * mid-migration is one that gets switched off for the duration, which is exactly when it is
     * needed. The skill must have its {@code SKILL.md}, not merely its directory - the bare
     * directory is what a half-finished move leaves behind, and it is the state that reads as done.
     */
    private boolean isImplemented(String command) {
        return Files.isRegularFile(repo.resolve(".claude/commands/" + command + ".md"))
                || Files.isRegularFile(repo.resolve(".claude/skills/" + command + "/SKILL.md"));
    }

    /**
     * A hook the repository map presents as live automation, that is not wired.
     *
     * <p>The one that actually happened: {@code PostToolUse} sat in the table of what fires
     * automatically, and nothing was listening.
     */
    private List<Problem> documentedHooks() throws IOException {
        Path map = repo.resolve("docs/repository-map.md");
        Path settings = repo.resolve(".claude/settings.json");
        if (!Files.isRegularFile(map) || !Files.isRegularFile(settings)) {
            return List.of();
        }
        var wired = JSON.readTree(Files.readString(settings)).path("hooks");

        List<Problem> problems = new ArrayList<>();
        for (String line : Files.readAllLines(map)) {
            if (PHASE_MARKER.matcher(line).find()) {
                continue;
            }
            Matcher matcher = DOCUMENTED_HOOK.matcher(line);
            while (matcher.find()) {
                String hook = matcher.group(1);
                if (wired.path(hook).isMissingNode()) {
                    problems.add(new Problem(
                            "docs/repository-map.md",
                            "presents the " + hook
                                    + " hook as automation, but .claude/settings.json does not wire it"));
                }
            }
        }
        return problems;
    }

    private List<Path> markdownFiles() throws IOException {
        try (Stream<Path> walk = Files.walk(repo.root())) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .toLowerCase(Locale.ROOT)
                            .endsWith(".md"))
                    .filter(this::isNotSkipped)
                    .toList();
        } catch (UncheckedIOException e) {
            throw new IOException(e);
        }
    }

    private boolean isNotSkipped(Path path) {
        String relative = relative(path);
        if (relative.startsWith(JOURNAL)) {
            return false;
        }
        for (String segment : relative.split("/")) {
            if (SKIPPED_DIRECTORIES.contains(segment)) {
                return false;
            }
        }
        return true;
    }

    private String relative(Path path) {
        String result = repo.relativize(path.toString());
        return result == null ? path.toString() : result;
    }
}
