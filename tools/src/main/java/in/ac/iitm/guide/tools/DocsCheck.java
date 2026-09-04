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
    private static final Pattern DOCUMENTED_HOOK =
            Pattern.compile("`(UserPromptSubmit|PreToolUse|PostToolUse|Stop)`\\s+hook");
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

    public static List<Problem> run(Repo repo) throws IOException {
        DocsCheck check = new DocsCheck(repo);
        List<Problem> problems = new ArrayList<>();
        problems.addAll(check.brokenLinks());
        problems.addAll(check.advertisedCommands());
        problems.addAll(check.citedSubcommands());
        problems.addAll(check.citedScripts());
        problems.addAll(check.documentedHooks());
        return problems;
    }

    /** A relative link to a markdown file that is not there. */
    private List<Problem> brokenLinks() throws IOException {
        List<Problem> problems = new ArrayList<>();
        for (Path file : markdownFiles()) {
            Path dir = file.getParent();
            for (String line : Files.readAllLines(file)) {
                Matcher matcher = MARKDOWN_LINK.matcher(line);
                while (matcher.find()) {
                    String target = matcher.group(1);
                    if (target.startsWith("http")) {
                        continue;
                    }
                    if (!Files.exists(dir.resolve(target).normalize())) {
                        problems.add(new Problem(relative(file), "link to a missing file: " + target));
                    }
                }
            }
        }
        return problems;
    }

    /**
     * A slash command named in the instructions but absent from {@code .claude/commands}.
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
                    if (seen.add(command)
                            && !Files.isRegularFile(repo.resolve(".claude/commands/" + command + ".md"))) {
                        problems.add(new Problem(name, "advertises /" + command + ", which has no command file"));
                    }
                }
            }
        }
        return problems;
    }

    /** A subcommand a document tells someone to run, that the tool does not dispatch. */
    private List<Problem> citedSubcommands() throws IOException {
        List<Problem> problems = new ArrayList<>();
        for (Path file : markdownFiles()) {
            for (String line : Files.readAllLines(file)) {
                if (PHASE_MARKER.matcher(line).find()) {
                    continue;
                }
                Matcher matcher = INVOCATION.matcher(line);
                while (matcher.find()) {
                    String hook = matcher.group(1);
                    String top = matcher.group(2);
                    if (hook != null && !Commands.HOOK.contains(hook)) {
                        problems.add(new Problem(relative(file), "cites `hook " + hook + "`, which is not dispatched"));
                    } else if (top != null && !Commands.TOP_LEVEL.contains(top)) {
                        problems.add(new Problem(
                                relative(file),
                                "cites `ai-tools " + top + "`, which is not dispatched;"
                                        + " say on this line which phase it belongs to if it is still ahead"));
                    }
                }
            }
        }
        return problems;
    }

    /** A script a document names as the way to do something, that does not exist. */
    private List<Problem> citedScripts() throws IOException {
        List<Problem> problems = new ArrayList<>();
        for (Path file : markdownFiles()) {
            for (String line : Files.readAllLines(file)) {
                if (PHASE_MARKER.matcher(line).find()) {
                    continue;
                }
                Matcher matcher = SCRIPT.matcher(line);
                while (matcher.find()) {
                    String script = matcher.group(1);
                    if (!Files.isRegularFile(repo.resolve(script))) {
                        problems.add(new Problem(relative(file), "names " + script + ", which does not exist"));
                    }
                }
            }
        }
        return problems;
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
