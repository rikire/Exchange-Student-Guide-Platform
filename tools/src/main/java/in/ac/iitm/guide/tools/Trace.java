package in.ac.iitm.guide.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The traceability matrix and the feature backlog, assembled from the artefacts they describe.
 *
 * <p>Nothing here is written by a person: a requirement is read from its heading and its
 * {@code **Status:**} line, an anchor from the code, tests and migrations that carry one, and a
 * feature from its front matter. Because the text is a function of the repository (no dates, no
 * ordering that depends on the file system), {@link #check} can tell a stale file from a current
 * one by comparing bytes.
 *
 * <p>The status rules are the ones in docs/repository-map.md: an empty scaffold must not be red
 * everywhere, so what is demanded grows with the status.
 */
public final class Trace {

    private static final String TRACEABILITY = "docs/traceability.md";
    private static final String FEATURES = "docs/features/README.md";
    private static final String COMMAND = "java -jar tools/target/ai-tools.jar trace";

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private static final Set<String> STATUSES = Set.of("planned", "in-progress", "done", "out-of-scope");

    private static final Pattern STATUS = Pattern.compile("^\\*\\*Status:\\*\\*\\s*(\\S+)");
    private static final Pattern PRIORITY = Pattern.compile("^\\*\\*Priority:\\*\\*\\s*(\\S+)");
    private static final Pattern TRIGGER = Pattern.compile("^\\*\\*Trigger:\\*\\*\\s*(.*)$");
    private static final Pattern RATIONALE = Pattern.compile("^\\*\\*Rationale:\\*\\*\\s*\\S");
    private static final Pattern VERIFIED_BY = Pattern.compile("^\\*\\*Verified by:\\*\\*\\s*\\S");

    /** {@code //trace:FR-001}, {@code -- trace: FR-001, FR-002}, {@code <!-- trace: FR-001 -->}. */
    private static final Pattern ANCHOR =
            Pattern.compile("(?://|--|<!--)\\s*trace:\\s*([A-Z]+-\\d+(?:\\s*,\\s*[A-Z]+-\\d+)*)");

    /**
     * A marker word directly followed by a debt reference. Any upper-case word will do: the three
     * that exist are all followed by the same parenthesis, and a marker with no reference at all is
     * refused when it is written, not found here.
     */
    private static final Pattern DEBT_REFERENCE = Pattern.compile("\\b[A-Z]+\\((DEBT-\\d+)\\)");

    /** What one reading of the repository produced. */
    public record Report(String traceability, String features, List<String> problems, List<String> notes) {}

    /** A requirement as declared. {@code scenarios} is the number of Given-When-Then blocks it carries. */
    record Requirement(
            String id,
            String kind,
            String title,
            String status,
            String priority,
            int scenarios,
            boolean rationale,
            boolean verifiedBy) {}

    /** An entry of docs/tech-debt.md; {@code trigger} is the first line of its Trigger paragraph. */
    record DebtEntry(String id, String title, String status, String trigger) {}

    enum Where {
        CODE,
        TEST,
        MIGRATION
    }

    record Anchor(String id, String path, Where where) {

        /**
         * Code that does something, as opposed to the schema laid down ahead of it: an entity under
         * shared/persistence and its migration exist before the slice that uses them, and neither
         * says the requirement is being met.
         */
        boolean isBehaviour() {
            return where == Where.CODE && !path.contains("/shared/persistence/");
        }

        String name() {
            String file = path.substring(path.lastIndexOf('/') + 1);
            int dot = file.lastIndexOf('.');
            return dot < 0 ? file : file.substring(0, dot);
        }
    }

    record Feature(
            String id,
            String file,
            String title,
            String status,
            List<String> covers,
            String slice,
            List<String> routes,
            List<String> tables,
            List<String> code,
            List<String> tests) {}

    private Trace() {}

    /** Everything read from the repository, before any of it is turned into text. */
    record Model(
            List<Requirement> requirements,
            List<Anchor> anchors,
            List<Feature> features,
            List<DebtEntry> debt,
            List<String> problems,
            List<String> notes) {}

    public static Report build(Repo repo) throws IOException {
        Model model = read(repo);
        return new Report(
                traceability(model.requirements(), model.features(), model.anchors(), model.problems(), model.notes()),
                featureBacklog(model.features()),
                model.problems(),
                model.notes());
    }

    static Model read(Repo repo) throws IOException {
        List<String> problems = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        List<Requirement> requirements = new ArrayList<>();
        requirements.addAll(requirementsIn(repo, "functional.md", "FR"));
        requirements.addAll(requirementsIn(repo, "non-functional.md", "NFR"));
        requirements.addAll(requirementsIn(repo, "constraints.md", "CON"));
        Set<String> declared = new LinkedHashSet<>();
        requirements.forEach(requirement -> declared.add(requirement.id()));

        List<Anchor> anchors = new ArrayList<>();
        List<String[]> debtReferences = new ArrayList<>();
        scanApp(repo, anchors, debtReferences);

        List<Feature> features = features(repo, problems);

        for (Anchor anchor : anchors) {
            if (!declared.contains(anchor.id())) {
                problems.add(anchor.path() + " names " + anchor.id() + ", which is not declared");
            }
        }
        for (Feature feature : features) {
            for (String id : feature.covers()) {
                if (!declared.contains(id)) {
                    problems.add(feature.id() + " covers " + id + ", which is not declared");
                }
            }
            for (String path : concat(feature.code(), feature.tests())) {
                if (!Files.exists(repo.resolve(path))) {
                    problems.add(feature.id() + " lists " + path + ", which does not exist");
                }
            }
        }
        for (Requirement requirement : requirements) {
            judge(requirement, features, anchors, problems, notes);
        }
        List<DebtEntry> debt = debtEntries(repo);
        debtReferences(debt, debtReferences, problems);

        return new Model(
                List.copyOf(requirements),
                List.copyOf(anchors),
                List.copyOf(features),
                List.copyOf(debt),
                List.copyOf(distinct(problems)),
                List.copyOf(distinct(notes)));
    }

    public static void write(Repo repo) throws IOException {
        Report report = build(repo);
        repo.write(repo.resolve(TRACEABILITY), report.traceability());
        repo.write(repo.resolve(FEATURES), report.features());
    }

    /** Everything wrong: what {@link #build} found, plus a generated file that no longer matches. */
    public static List<String> check(Repo repo) throws IOException {
        Report report = build(repo);
        List<String> found = new ArrayList<>(report.problems());
        stale(repo, TRACEABILITY, report.traceability(), found);
        stale(repo, FEATURES, report.features(), found);
        return found;
    }

    private static void stale(Repo repo, String relative, String expected, List<String> found) throws IOException {
        Path file = repo.resolve(relative);
        if (!Files.isRegularFile(file) || !Files.readString(file).equals(expected)) {
            found.add(relative + " is out of date; run `" + COMMAND + "`");
        }
    }

    // --- reading ---

    private static List<Requirement> requirementsIn(Repo repo, String file, String prefix) throws IOException {
        Path path = repo.resolve("docs/requirements/" + file);
        if (!Files.isRegularFile(path)) {
            return List.of();
        }
        Pattern heading = Pattern.compile("^### (" + prefix + "-\\d+)\\b[\\s—-]*(.*)$");
        List<Requirement> found = new ArrayList<>();

        boolean inFormat = false;
        String id = null;
        String title = "";
        String status = "";
        String priority = "";
        int scenarios = 0;
        boolean rationale = false;
        boolean verifiedBy = false;

        for (String line : Files.readAllLines(path)) {
            boolean opensSection = line.startsWith("## ") || line.startsWith("### ") || line.startsWith("# ");
            if (opensSection) {
                if (id != null) {
                    found.add(new Requirement(id, prefix, title, status, priority, scenarios, rationale, verifiedBy));
                    id = null;
                }
                if (line.startsWith("## ")) {
                    inFormat = line.strip().equalsIgnoreCase("## Format");
                }
                Matcher matcher = heading.matcher(line);
                if (!inFormat && matcher.matches()) {
                    id = matcher.group(1);
                    title = matcher.group(2).strip();
                    status = "";
                    priority = "";
                    scenarios = 0;
                    rationale = false;
                    verifiedBy = false;
                }
            } else if (id != null) {
                Matcher matcher = STATUS.matcher(line);
                if (matcher.find()) {
                    status = matcher.group(1);
                }
                Matcher priorityLine = PRIORITY.matcher(line);
                if (priorityLine.find()) {
                    priority = priorityLine.group(1);
                }
                if (line.startsWith("GIVEN ")) {
                    scenarios++;
                }
                rationale |= RATIONALE.matcher(line).find();
                verifiedBy |= VERIFIED_BY.matcher(line).find();
            }
        }
        if (id != null) {
            found.add(new Requirement(id, prefix, title, status, priority, scenarios, rationale, verifiedBy));
        }
        return found;
    }

    /** Anchors and debt references in everything under {@code app/}, apart from build output. */
    private static void scanApp(Repo repo, List<Anchor> anchors, List<String[]> debtReferences) throws IOException {
        Path app = repo.resolve("app");
        if (!Files.isDirectory(app)) {
            return;
        }
        List<Path> files;
        try (Stream<Path> walk = Files.walk(app)) {
            files = walk.filter(Files::isRegularFile)
                    .filter(file -> isScanned(app.relativize(file).toString().replace('\\', '/')))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
        for (Path file : files) {
            String relative = repo.relativize(file.toString());
            Where where = relative.contains("/src/test/")
                    ? Where.TEST
                    : relative.contains("/db/migration/") ? Where.MIGRATION : Where.CODE;
            for (String line : Files.readAllLines(file)) {
                Matcher anchor = ANCHOR.matcher(line);
                while (anchor.find()) {
                    for (String id : anchor.group(1).split("\\s*,\\s*")) {
                        anchors.add(new Anchor(id, relative, where));
                    }
                }
                Matcher debt = DEBT_REFERENCE.matcher(line);
                while (debt.find()) {
                    debtReferences.add(new String[] {relative, debt.group(1)});
                }
            }
        }
    }

    private static boolean isScanned(String relativeToApp) {
        if (("/" + relativeToApp).contains("/target/")) {
            return false;
        }
        return relativeToApp.endsWith(".java") || relativeToApp.endsWith(".sql") || relativeToApp.endsWith(".html");
    }

    private static List<Feature> features(Repo repo, List<String> problems) throws IOException {
        Path directory = repo.resolve("docs/features");
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<Path> files;
        try (Stream<Path> list = Files.list(directory)) {
            files = list.filter(file -> file.getFileName().toString().startsWith("FEAT-"))
                    .filter(file -> file.getFileName().toString().endsWith(".md"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
        List<Feature> found = new ArrayList<>();
        for (Path file : files) {
            String relative = repo.relativize(file.toString());
            try {
                JsonNode front = frontMatter(Files.readString(file));
                String id = front.path("id").asText("");
                if (id.isEmpty()) {
                    problems.add(relative + ": the front matter has no id");
                    continue;
                }
                found.add(new Feature(
                        id,
                        file.getFileName().toString(),
                        front.path("title").asText(""),
                        front.path("status").asText(""),
                        strings(front.path("covers")),
                        front.path("slice").asText(""),
                        strings(front.path("routes")),
                        strings(front.path("tables")),
                        strings(front.path("code")),
                        strings(front.path("tests"))));
            } catch (IOException | RuntimeException e) {
                problems.add(relative + ": the front matter cannot be read (" + firstLine(e.getMessage()) + ")");
            }
        }
        return found;
    }

    private static JsonNode frontMatter(String text) throws IOException {
        if (!text.startsWith("---")) {
            throw new IOException("the file does not open with ---");
        }
        int end = text.indexOf("\n---", 3);
        if (end < 0) {
            throw new IOException("the front matter is not closed by ---");
        }
        return YAML.readTree(text.substring(3, end));
    }

    private static List<String> strings(JsonNode node) {
        List<String> found = new ArrayList<>();
        node.forEach(item -> found.add(item.asText()));
        return found;
    }

    private static String firstLine(String message) {
        if (message == null || message.isBlank()) {
            return "unknown error";
        }
        return message.lines().findFirst().orElse("").strip();
    }

    // --- judging ---

    private static void judge(
            Requirement requirement,
            List<Feature> features,
            List<Anchor> anchors,
            List<String> problems,
            List<String> notes) {
        String id = requirement.id();
        if (requirement.kind().equals("CON")) {
            if (!requirement.rationale()) {
                problems.add(id + " gives no Rationale");
            }
            return;
        }
        String status = requirement.status();
        if (status.isEmpty()) {
            problems.add(id + " has no Status line");
            return;
        }
        if (!STATUSES.contains(status)) {
            problems.add(id + " has status '" + status + "'; expected planned, in-progress, done or out-of-scope");
            return;
        }
        boolean covered = features.stream().anyMatch(feature -> feature.covers().contains(id));
        boolean hasCode = anchors.stream().anyMatch(a -> a.id().equals(id) && a.isBehaviour());
        boolean hasTest = anchors.stream().anyMatch(a -> a.id().equals(id) && a.where() == Where.TEST);

        switch (status) {
            case "in-progress" -> {
                if (!covered) {
                    problems.add(id + " is in-progress and no feature file covers it");
                }
            }
            case "done" -> {
                if (!covered) {
                    problems.add(id + " is done and no feature file covers it");
                }
                // A non-functional requirement may be verified by something that is not a test.
                boolean standIn = requirement.kind().equals("NFR") && requirement.verifiedBy();
                if (!standIn && !hasCode) {
                    problems.add(id + " is done and no code carries its anchor");
                }
                if (!standIn && !hasTest) {
                    problems.add(id + " is done and no test carries its anchor");
                }
            }
            case "out-of-scope" -> {
                if (!requirement.rationale()) {
                    problems.add(id + " is out-of-scope and gives no Rationale");
                }
            }
            default -> {
                // planned: nothing is demanded, but code that already does something is worth a look.
                if (hasCode) {
                    notes.add(id + " is planned but has anchors in code; its status may be stale");
                }
            }
        }
    }

    private static List<DebtEntry> debtEntries(Repo repo) throws IOException {
        List<DebtEntry> found = new ArrayList<>();
        Path register = repo.resolve("docs/tech-debt.md");
        if (!Files.isRegularFile(register)) {
            return found;
        }
        Pattern heading = Pattern.compile("^### (DEBT-\\d+)\\b[\\s—-]*(.*)$");
        String id = null;
        String title = "";
        String status = "";
        String trigger = "";
        for (String line : Files.readAllLines(register)) {
            Matcher matcher = heading.matcher(line);
            boolean opensSection = line.startsWith("## ") || line.startsWith("### ");
            if (opensSection && id != null) {
                found.add(new DebtEntry(id, title, status, trigger));
                id = null;
            }
            if (matcher.find()) {
                id = matcher.group(1);
                title = matcher.group(2).strip();
                status = "";
                trigger = "";
            } else if (id != null) {
                Matcher statusLine = STATUS.matcher(line);
                if (statusLine.find()) {
                    status = statusLine.group(1);
                }
                Matcher triggerLine = TRIGGER.matcher(line);
                if (triggerLine.find()) {
                    trigger = triggerLine.group(1).strip();
                }
            }
        }
        if (id != null) {
            found.add(new DebtEntry(id, title, status, trigger));
        }
        return found;
    }

    private static void debtReferences(List<DebtEntry> debt, List<String[]> references, List<String> problems) {
        Map<String, String> statuses = new LinkedHashMap<>();
        debt.forEach(entry -> statuses.put(entry.id(), entry.status()));
        for (String[] reference : references) {
            String status = statuses.get(reference[1]);
            if (status == null) {
                problems.add(reference[0] + " carries a marker for " + reference[1]
                        + ", which is not recorded in docs/tech-debt.md");
            } else if (status.equals("resolved")) {
                problems.add(reference[0] + " still carries a marker for " + reference[1] + ", which is resolved");
            }
        }
    }

    // --- writing ---

    private static String traceability(
            List<Requirement> requirements,
            List<Feature> features,
            List<Anchor> anchors,
            List<String> problems,
            List<String> notes) {
        StringBuilder out = new StringBuilder();
        out.append("# Traceability matrix\n\n");
        out.append("Requirement to feature to route to table to code to test, assembled from the anchors in the\n");
        out.append("artefacts themselves.\n\n");
        out.append("**Generated** by `").append(COMMAND).append("`. An edit made here is lost on the next run, and\n");
        out.append("`trace --check` refuses a file that no longer matches the repository.\n\n");
        out.append("Code and tests are the files carrying a `//trace:` anchor for the requirement, migrations the\n");
        out.append("ones carrying `-- trace:`. Routes and tables come from the feature files that cover it.\n");

        section(out, "Functional requirements", "FR", requirements, features, anchors);
        section(out, "Non-functional requirements", "NFR", requirements, features, anchors);
        section(out, "Constraints", "CON", requirements, features, anchors);

        out.append("\n## Gaps\n\n");
        if (problems.isEmpty()) {
            out.append("None.\n");
        } else {
            problems.forEach(problem -> out.append("- ").append(problem).append('\n'));
        }
        out.append("\n## Notes\n\n");
        if (notes.isEmpty()) {
            out.append("None.\n");
        } else {
            notes.forEach(note -> out.append("- ").append(note).append('\n'));
        }
        return out.toString();
    }

    private static void section(
            StringBuilder out,
            String heading,
            String kind,
            List<Requirement> requirements,
            List<Feature> features,
            List<Anchor> anchors) {
        List<Requirement> rows = requirements.stream()
                .filter(requirement -> requirement.kind().equals(kind))
                .toList();
        out.append("\n## ").append(heading).append("\n\n");
        if (rows.isEmpty()) {
            out.append("None declared.\n");
            return;
        }
        out.append("| Requirement | Status | Features | Code | Tests | Migrations | Routes | Tables | Title |\n");
        out.append("|---|---|---|---|---|---|---|---|---|\n");
        for (Requirement requirement : rows) {
            String id = requirement.id();
            List<Feature> covering = features.stream()
                    .filter(feature -> feature.covers().contains(id))
                    .toList();
            out.append("| ").append(id);
            cell(out, requirement.status());
            cell(out, join(covering.stream().map(Feature::id).toList()));
            cell(out, names(anchors, id, Where.CODE));
            cell(out, names(anchors, id, Where.TEST));
            cell(out, names(anchors, id, Where.MIGRATION));
            cell(out, join(covering.stream().flatMap(f -> f.routes().stream()).toList()));
            cell(out, join(covering.stream().flatMap(f -> f.tables().stream()).toList()));
            cell(out, requirement.title());
            out.append(" |\n");
        }
    }

    private static String names(List<Anchor> anchors, String id, Where where) {
        return join(anchors.stream()
                .filter(anchor -> anchor.id().equals(id) && anchor.where() == where)
                .map(Anchor::name)
                .toList());
    }

    private static void cell(StringBuilder out, String value) {
        out.append(" | ").append(value.replace("|", "\\|"));
    }

    /** Sorted and without repeats, so the same repository always reads the same. */
    private static String join(List<String> values) {
        return String.join(", ", new TreeSet<>(values));
    }

    private static String featureBacklog(List<Feature> features) {
        StringBuilder out = new StringBuilder();
        out.append("# Feature backlog\n\n");
        out.append("Every feature file with its status, the requirements it covers and the slice it lives in.\n\n");
        out.append("**Generated** by `").append(COMMAND).append("`. An edit made here is lost on the next run.\n\n");
        if (features.isEmpty()) {
            out.append("No feature files yet.\n");
            return out.toString();
        }
        out.append("| Feature | Title | Status | Covers | Slice | Routes | Tables |\n");
        out.append("|---|---|---|---|---|---|---|\n");
        for (Feature feature : features) {
            out.append("| ").append(feature.id());
            cell(out, "[" + feature.title() + "](" + feature.file() + ")");
            cell(out, feature.status());
            cell(out, String.join(", ", feature.covers()));
            cell(out, feature.slice());
            cell(out, String.join(", ", feature.routes()));
            cell(out, String.join(", ", feature.tables()));
            out.append(" |\n");
        }
        return out.toString();
    }

    private static List<String> concat(List<String> first, List<String> second) {
        List<String> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    private static List<String> distinct(List<String> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }
}
