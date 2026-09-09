package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * How many requirements there actually are.
 *
 * <p>Written after getting it wrong by hand: three of the four figures sent to the course on
 * 9 September were off, because each requirements file opens with a `## Format` section carrying an
 * illustrative entry — `FR-050`, `NFR-050`, `CON-040` — and a plain count of `### FR-` lines takes
 * those for real. The trap was spotted in one file and then not looked for in the other two.
 *
 * <p>So the number stops being something anyone derives by eye. `ai-tools count` prints it, and
 * {@link DocsCheck} refuses a document that states a different one.
 */
public final class Requirements {

    /** What the repository contains, by kind. */
    public record Counts(int functional, int nonFunctional, int constraints, int useCases) {}

    private Requirements() {}

    public static Counts of(Repo repo) throws IOException {
        return new Counts(
                countFile(repo, "docs/requirements/functional.md", "FR"),
                countFile(repo, "docs/requirements/non-functional.md", "NFR"),
                countFile(repo, "docs/requirements/constraints.md", "CON"),
                countUseCases(repo));
    }

    /**
     * Entries of one kind, skipping the format section.
     *
     * <p>The section is bounded by its own heading and the next one at the same level. That is the
     * whole rule: an entry demonstrating the shape sits under `## Format`, and a real one does not.
     */
    public static int countIn(List<String> lines, String prefix) {
        String heading = "### " + prefix + "-";
        boolean inFormatSection = false;
        int found = 0;
        for (String line : lines) {
            if (line.startsWith("## ")) {
                inFormatSection = line.strip().equalsIgnoreCase("## Format");
                continue;
            }
            if (!inFormatSection && line.startsWith(heading)) {
                found++;
            }
        }
        return found;
    }

    private static int countFile(Repo repo, String relative, String prefix) throws IOException {
        Path file = repo.resolve(relative);
        return Files.isRegularFile(file) ? countIn(Files.readAllLines(file), prefix) : 0;
    }

    /** Use cases live across the three journeys, and one may appear in more than one of them. */
    private static int countUseCases(Repo repo) throws IOException {
        Path directory = repo.resolve("docs/cjm");
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        List<String> ids = new ArrayList<>();
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".md"))
                    .toList()) {
                for (String line : Files.readAllLines(file)) {
                    if (line.startsWith("### UC-")) {
                        String id = line.substring(4).split("[^A-Za-z0-9-]", 2)[0];
                        if (!ids.contains(id)) {
                            ids.add(id);
                        }
                    }
                }
            }
        }
        return ids.size();
    }
}
