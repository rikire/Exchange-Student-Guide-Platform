package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * After the freeze the schema changes by agreement only, and the agreement is an ADR.
 *
 * <p>The date is a decision, so it is read from the line {@code **Schema frozen:** YYYY-MM-DD} in
 * the phase 2 roadmap file, and until a person writes it nothing is frozen and nothing is asked. A
 * migration is dated by the commit that added it, since a file's timestamp is whatever the last
 * checkout said; one git has not seen yet was added today, which is after any freeze already
 * declared. What is checked is what can be: the migration names an ADR in its header comment and that
 * ADR exists. Whether the ADR actually motivates the change is for a reader.
 */
final class SchemaFreeze {

    private static final String ROADMAP = "docs/roadmap/02-skeleton.md";
    private static final String MIGRATIONS = "app/src/main/resources/db/migration";
    private static final String ADRS = "docs/architecture/adr";

    private static final Pattern FROZEN = Pattern.compile("^\\*\\*Schema frozen:\\*\\*\\s*(.+?)\\s*$");
    private static final Pattern ADR_HEADER = Pattern.compile("^--\\s*adr:\\s*(ADR-\\d{4})\\b");

    private SchemaFreeze() {}

    static List<String> check(Repo repo, Clock clock) throws IOException {
        Path roadmap = repo.resolve(ROADMAP);
        if (!Files.isRegularFile(roadmap)) {
            return List.of();
        }
        String declared = null;
        for (String line : Files.readAllLines(roadmap)) {
            Matcher matcher = FROZEN.matcher(line);
            if (matcher.matches()) {
                declared = matcher.group(1);
                break;
            }
        }
        if (declared == null) {
            return List.of();
        }
        LocalDate frozen;
        try {
            frozen = LocalDate.parse(declared);
        } catch (DateTimeParseException e) {
            return List.of(
                    ROADMAP + ": the schema freeze '" + declared + "' cannot be read as a date (expected YYYY-MM-DD)");
        }

        List<String> problems = new ArrayList<>();
        for (Path migration : migrations(repo)) {
            String relative = repo.relativize(migration.toString());
            if (!addedOn(repo, relative, clock).isAfter(frozen)) {
                continue;
            }
            List<String> named = adrsNamedInHeader(migration);
            if (named.isEmpty()) {
                problems.add(relative + " was added after the schema froze on " + frozen
                        + " and names no ADR in its header; add `-- adr: ADR-NNNN` for the decision that motivated it");
            }
            for (String adr : named) {
                if (!adrExists(repo, adr)) {
                    problems.add(relative + " names " + adr + ", which does not exist under " + ADRS + "/");
                }
            }
        }
        return problems;
    }

    private static List<Path> migrations(Repo repo) throws IOException {
        Path directory = repo.resolve(MIGRATIONS);
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> list = Files.list(directory)) {
            return list.filter(file -> file.getFileName().toString().endsWith(".sql"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    /** The day of the commit that added the file, or today when no commit has. */
    private static LocalDate addedOn(Repo repo, String relative, Clock clock) {
        Git.Result result = Git.run(repo, "log", "--diff-filter=A", "--format=%aI", "--", relative);
        List<String> dates =
                result.lines().stream().filter(line -> !line.isBlank()).toList();
        if (!result.ok() || dates.isEmpty()) {
            return LocalDate.now(clock);
        }
        // Newest first, so the last one is the commit that first added it.
        return OffsetDateTime.parse(dates.get(dates.size() - 1)).toLocalDate();
    }

    /** Only the leading comment counts: an ADR mentioned further down is not a header. */
    private static List<String> adrsNamedInHeader(Path migration) throws IOException {
        List<String> named = new ArrayList<>();
        for (String line : Files.readAllLines(migration)) {
            if (!line.isBlank() && !line.startsWith("--")) {
                break;
            }
            Matcher matcher = ADR_HEADER.matcher(line);
            if (matcher.find()) {
                named.add(matcher.group(1));
            }
        }
        return named;
    }

    private static boolean adrExists(Repo repo, String adr) throws IOException {
        Path directory = repo.resolve(ADRS);
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> list = Files.list(directory)) {
            return list.anyMatch(file -> file.getFileName().toString().startsWith(adr + "-"));
        }
    }
}
