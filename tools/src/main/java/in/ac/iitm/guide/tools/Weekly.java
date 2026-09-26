package in.ac.iitm.guide.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The "From git" half of the weekly contribution log, written by a tool so that it cannot drift from
 * the history it summarises.
 *
 * <p>The other half is the members' own words, which the course asks each student for and which no
 * tool may write. Refreshing therefore replaces the "From git" paragraph of each member and leaves
 * every other byte of a log that has been written in exactly as it was.
 *
 * <p>Journal commits written by the `Stop` hook are counted apart and never added to the authored
 * figure (docs/team/README.md); the directories are counted over authored commits only, so the
 * journal never appears among them. The same figures are what `scripts/contribution.sh` prints.
 */
public final class Weekly {

    private static final String LOG_DIRECTORY = "docs/team/weekly-log/";
    private static final String COMMAND = "java -jar tools/target/ai-tools.jar weekly";
    private static final String UNREGISTERED = "UNREGISTERED";
    private static final String JOURNAL = "docs/ai/journal/";
    private static final String FROM_GIT = "**From git**";
    private static final int MOST_DIRECTORIES = 6;

    private static final Pattern WEEK = Pattern.compile("(\\d{4})-W(\\d{2})");

    private Weekly() {}

    /** The ISO week of a date, spelled as the history spells it: {@code 2026-W39}. */
    static String weekOf(LocalDate date) {
        return String.format(
                "%d-W%02d", date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    /** {@code 2026-W38 — 14 to 20 September}: the week, and the Monday and Sunday that bound it. */
    static String titleOf(String week) {
        LocalDate monday = mondayOf(week);
        LocalDate sunday = monday.plusDays(6);
        String from =
                monday.getDayOfMonth() + (monday.getMonth() == sunday.getMonth() ? "" : " " + name(monday.getMonth()));
        return week + " — " + from + " to " + sunday.getDayOfMonth() + " " + name(sunday.getMonth());
    }

    private static LocalDate mondayOf(String week) {
        Matcher matcher = WEEK.matcher(week);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("expected a week like 2026-W38, got '" + week + "'");
        }
        // The fourth of January is always in week 1 of its ISO year.
        return LocalDate.of(Integer.parseInt(matcher.group(1)), 1, 4)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, Long.parseLong(matcher.group(2)))
                .with(ChronoField.DAY_OF_WEEK, 1);
    }

    private static String name(Month month) {
        return month.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    public static void write(Repo repo, String week) throws IOException {
        Path file = repo.resolve(LOG_DIRECTORY + week + ".md");
        String existing = Files.isRegularFile(file) ? Files.readString(file) : null;
        repo.write(file, build(History.read(repo), Members.load(repo), week, existing));
    }

    /** The log for {@code week}: a new one when {@code existing} is null, otherwise that one refreshed. */
    static String build(List<History.Commit> commits, Members members, String week, String existing) {
        // display name -> the paragraph of figures, in registry order
        Map<String, String> paragraphs = new LinkedHashMap<>();
        for (Members.Member member : members.all()) {
            paragraphs.put(member.name(), paragraph(commits, week, id -> id.equals(member.id()), members));
        }
        boolean anyUnregistered = commits.stream()
                .anyMatch(commit -> commit.week().equals(week)
                        && members.byEmail(commit.email()).isEmpty());
        if (anyUnregistered) {
            paragraphs.put(UNREGISTERED, paragraph(commits, week, id -> id.equals(UNREGISTERED), members));
        }
        return existing == null ? created(week, paragraphs) : refreshed(existing, paragraphs);
    }

    private static String paragraph(
            List<History.Commit> commits,
            String week,
            java.util.function.Predicate<String> isThePerson,
            Members members) {
        int authored = 0;
        int hook = 0;
        Map<String, Integer> directories = new LinkedHashMap<>();
        for (History.Commit commit : commits) {
            String who = members.byEmail(commit.email()).map(Members.Member::id).orElse(UNREGISTERED);
            if (!commit.week().equals(week) || !isThePerson.test(who)) {
                continue;
            }
            if (commit.fromHook()) {
                hook++;
                continue;
            }
            authored++;
            for (History.Change change : commit.changes()) {
                String directory = directoryOf(change.path());
                if (directory != null) {
                    directories.merge(directory, 1, Integer::sum);
                }
            }
        }
        StringBuilder text = new StringBuilder(FROM_GIT + " — ");
        text.append(authored).append(authored == 1 ? " authored commit" : " authored commits");
        text.append(" this week, ").append(hook).append(" written by the hook.");
        if (!directories.isEmpty()) {
            List<String> listed = directories.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue()
                            .reversed()
                            .thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder())))
                    .limit(MOST_DIRECTORIES)
                    .map(entry -> "`" + entry.getKey() + "` (" + entry.getValue() + ")")
                    .toList();
            text.append(" Directories touched, by file count: ")
                    .append(String.join(", ", listed))
                    .append('.');
        }
        return text.toString();
    }

    /** The first two segments of a path, as `cut -d/ -f1-2` gives them; a file at the top has none. */
    private static String directoryOf(String path) {
        if (!path.contains("/") || path.startsWith(JOURNAL)) {
            return null;
        }
        String[] segments = path.split("/", 3);
        return segments[0] + "/" + segments[1];
    }

    private static String created(String week, Map<String, String> paragraphs) {
        StringBuilder out = new StringBuilder("# " + titleOf(week) + "\n\n");
        out.append("The **From git** figures are written by `")
                .append(COMMAND)
                .append("` and refreshed every time it\n");
        out.append(
                "runs; the words are the members' own. Journal commits written by the `Stop` hook are counted apart\n");
        out.append("from authored work and never added to it (see [../README.md](../README.md)).\n");
        paragraphs.forEach((name, paragraph) -> out.append(section(name, paragraph)));
        return out.toString();
    }

    private static String section(String name, String paragraph) {
        return "\n## " + name + "\n\n" + paragraph
                + "\n\n**In our own words** — _Not written yet. Add it with `/weekly-log`._\n";
    }

    private static String refreshed(String existing, Map<String, String> paragraphs) {
        List<String> lines = new ArrayList<>(List.of(existing.split("\n", -1)));
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> entry : paragraphs.entrySet()) {
            int heading = lines.indexOf("## " + entry.getKey());
            if (heading < 0) {
                missing.add(entry.getKey());
                continue;
            }
            int end = heading + 1;
            while (end < lines.size() && !lines.get(end).startsWith("## ")) {
                end++;
            }
            int start = -1;
            for (int i = heading + 1; i < end; i++) {
                if (lines.get(i).startsWith(FROM_GIT)) {
                    start = i;
                    break;
                }
            }
            if (start < 0) {
                lines.addAll(heading + 1, List.of("", entry.getValue()));
                continue;
            }
            int stop = start;
            while (stop < end && !lines.get(stop).isBlank()) {
                stop++;
            }
            lines.subList(start, stop).clear();
            lines.add(start, entry.getValue());
        }
        StringBuilder out = new StringBuilder(String.join("\n", lines));
        if (!missing.isEmpty() && out.charAt(out.length() - 1) != '\n') {
            out.append('\n');
        }
        missing.forEach(name -> out.append(section(name, paragraphs.get(name))));
        return out.toString();
    }
}
