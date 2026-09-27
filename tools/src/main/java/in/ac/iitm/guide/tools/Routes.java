package in.ac.iitm.guide.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the route tables of ui-routes.md from routes.yml, the route contract.
 *
 * <p>Only the two regions between the markers are generated; the conventions and the review record
 * around them stay prose that people write. The app's RouteContractTest compares the same file
 * with the controllers, so the table, the file and the code cannot drift apart unnoticed.
 */
public final class Routes {

    static final String CONTRACT = "docs/architecture/routes.yml";
    static final String DOC = "docs/architecture/ui-routes.md";

    static final String TABLE_START =
            "<!-- routes:table - written by `ai-tools routes` from routes.yml; edit that file -->";
    static final String TABLE_END = "<!-- /routes:table -->";
    static final String DEFERRED_START =
            "<!-- routes:deferred - written by `ai-tools routes` from routes.yml; edit that file -->";
    static final String DEFERRED_END = "<!-- /routes:deferred -->";

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private Routes() {}

    public static void write(Repo repo) throws IOException {
        var problems = new ArrayList<String>();
        var rendered = render(repo, problems);
        if (!problems.isEmpty()) {
            throw new IOException(String.join("; ", problems));
        }
        repo.write(repo.resolve(DOC), rendered);
    }

    /** @return what is wrong with the document; empty when its tables match the contract */
    public static List<String> check(Repo repo) throws IOException {
        var problems = new ArrayList<String>();
        var rendered = render(repo, problems);
        if (problems.isEmpty() && !rendered.equals(Files.readString(repo.resolve(DOC)))) {
            problems.add(DOC + " is out of date with " + CONTRACT + "; run `ai-tools routes`");
        }
        return problems;
    }

    private static String render(Repo repo, List<String> problems) throws IOException {
        var contract = YAML.readTree(Files.readString(repo.resolve(CONTRACT)));
        var document = Files.readString(repo.resolve(DOC));
        document = replace(document, TABLE_START, TABLE_END, routeTable(contract.path("routes")), problems);
        return replace(document, DEFERRED_START, DEFERRED_END, deferredTable(contract.path("deferred")), problems);
    }

    private static String replace(String document, String start, String end, String table, List<String> problems) {
        int from = document.indexOf(start);
        int to = document.indexOf(end);
        if (from < 0 || to < from) {
            problems.add(DOC + " has no region between " + start + " and " + end);
            return document;
        }
        return document.substring(0, from + start.length()) + "\n" + table + document.substring(to);
    }

    private static String routeTable(JsonNode routes) {
        var out = new StringBuilder("| Route | Slice | Template | Form fields | Response codes | Serves |\n");
        out.append("|---|---|---|---|---|---|\n");
        for (var route : routes) {
            var path = route.path("path").asText()
                    + (route.hasNonNull("query") ? "?" + route.path("query").asText() : "");
            var names = new ArrayList<String>();
            route.path("methods").forEach(method -> names.add("`" + method.asText() + " " + path + "`"));
            row(
                    out,
                    String.join(", ", names),
                    "`" + route.path("slice").asText() + "`",
                    route.path("template").asText(),
                    route.path("fields").asText(),
                    route.path("responses").asText(),
                    route.path("serves").asText());
        }
        return out.toString();
    }

    private static String deferredTable(JsonNode deferred) {
        var out = new StringBuilder("| Feature | Priority | FR |\n|---|---|---|\n");
        for (var feature : deferred) {
            row(
                    out,
                    feature.path("feature").asText(),
                    feature.path("priority").asText(),
                    feature.path("requirements").asText());
        }
        return out.toString();
    }

    private static void row(StringBuilder out, String... cells) {
        for (var cell : cells) {
            out.append("| ").append(cell.replace("|", "\\|")).append(' ');
        }
        out.append("|\n");
    }
}
