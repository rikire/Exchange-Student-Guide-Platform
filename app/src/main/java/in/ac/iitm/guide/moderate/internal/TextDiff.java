package in.ac.iitm.guide.moderate.internal;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * What an edit changes in an article's body, as the moderator's review shows it (FR-029, ADR-0015):
 * paragraphs side by side, the changed words marked inside a changed paragraph, and unchanged
 * paragraphs away from a change skipped. The Markdown source is compared, not the rendered page, so
 * every changed character is visible.
 *
 * <p>java-diff-utils finds the differences; this class only arranges them into rows. Its own
 * {@code DiffRowGenerator} is not used because it returns strings with the markup already inserted,
 * which the page would then have to print unescaped.
 *
 * @param rows in the order of the text
 */
public record TextDiff(List<Row> rows) {

    /** A paragraph is what a blank line separates, as in Markdown. */
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\n\\s*\n");

    /** A word, or the whitespace between two words, so that the marks fall on whole words. */
    private static final Pattern TOKEN = Pattern.compile("\\s+|[^\\s]+");

    /** How many unchanged paragraphs are kept on each side of a change. */
    private static final int CONTEXT = 1;

    public enum Kind {
        SAME,
        CHANGED,
        ADDED,
        REMOVED,
        /** Unchanged paragraphs not shown; {@link Row#skipped} says how many. */
        SKIPPED
    }

    /** A run of text, marked when it was removed (before) or added (after). */
    public record Segment(String text, boolean marked) {}

    /**
     * @param before the published paragraph, empty for an added one or a skipped run
     * @param after the proposed paragraph, empty for a removed one or a skipped run
     * @param skipped how many paragraphs a {@link Kind#SKIPPED} row stands for, else 0
     */
    public record Row(Kind kind, List<Segment> before, List<Segment> after, int skipped) {}

    public static TextDiff of(String published, String proposed) {
        var before = paragraphs(published);
        var after = paragraphs(proposed);
        var rows = new ArrayList<Row>();
        var next = 0;
        for (var delta : DiffUtils.diff(before, after).getDeltas()) {
            var at = delta.getSource().getPosition();
            for (var i = next; i < at; i++) {
                rows.add(same(before.get(i)));
            }
            rows.addAll(rowsOf(delta));
            next = at + delta.getSource().size();
        }
        for (var i = next; i < before.size(); i++) {
            rows.add(same(before.get(i)));
        }
        return new TextDiff(withoutDistantContext(rows));
    }

    public boolean unchanged() {
        return rows.stream().allMatch(row -> row.kind() == Kind.SAME || row.kind() == Kind.SKIPPED);
    }

    private static List<String> paragraphs(String text) {
        var normalised = text.replace("\r\n", "\n").strip();
        if (normalised.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(PARAGRAPH_BREAK.split(normalised))
                .map(String::strip)
                .toList();
    }

    /**
     * A changed run of paragraphs is paired off from its first: the pairs are compared word by word,
     * and what one side has beyond the other is removed or added whole.
     */
    private static List<Row> rowsOf(AbstractDelta<String> delta) {
        var removed = delta.getSource().getLines();
        var added = delta.getTarget().getLines();
        var rows = new ArrayList<Row>();
        var paired = delta.getType() == DeltaType.CHANGE ? Math.min(removed.size(), added.size()) : 0;
        for (var i = 0; i < paired; i++) {
            rows.add(changed(removed.get(i), added.get(i)));
        }
        for (var i = paired; i < removed.size(); i++) {
            rows.add(new Row(Kind.REMOVED, List.of(new Segment(removed.get(i), true)), List.of(), 0));
        }
        for (var i = paired; i < added.size(); i++) {
            rows.add(new Row(Kind.ADDED, List.of(), List.of(new Segment(added.get(i), true)), 0));
        }
        return rows;
    }

    private static Row changed(String published, String proposed) {
        var before = tokens(published);
        var after = tokens(proposed);
        var removedAt = new boolean[before.size()];
        var addedAt = new boolean[after.size()];
        for (var delta : DiffUtils.diff(before, after).getDeltas()) {
            mark(removedAt, delta.getSource().getPosition(), delta.getSource().size());
            mark(addedAt, delta.getTarget().getPosition(), delta.getTarget().size());
        }
        return new Row(Kind.CHANGED, segments(before, removedAt), segments(after, addedAt), 0);
    }

    private static List<String> tokens(String paragraph) {
        return TOKEN.matcher(paragraph).results().map(match -> match.group()).toList();
    }

    private static void mark(boolean[] marks, int from, int count) {
        Arrays.fill(marks, from, from + count, true);
    }

    /** Joins neighbouring tokens that are marked alike, so the page has one mark per changed run. */
    private static List<Segment> segments(List<String> tokens, boolean[] marks) {
        var segments = new ArrayList<Segment>();
        var run = new StringBuilder();
        for (var i = 0; i < tokens.size(); i++) {
            if (i > 0 && marks[i] != marks[i - 1]) {
                segments.add(new Segment(run.toString(), marks[i - 1]));
                run.setLength(0);
            }
            run.append(tokens.get(i));
        }
        if (!run.isEmpty()) {
            segments.add(new Segment(run.toString(), marks[tokens.size() - 1]));
        }
        return segments;
    }

    private static Row same(String paragraph) {
        var text = List.of(new Segment(paragraph, false));
        return new Row(Kind.SAME, text, text, 0);
    }

    /** Keeps {@link #CONTEXT} unchanged paragraphs next to each change and folds the rest into counts. */
    private static List<Row> withoutDistantContext(List<Row> rows) {
        var shown = new boolean[rows.size()];
        for (var i = 0; i < rows.size(); i++) {
            if (rows.get(i).kind() != Kind.SAME) {
                Arrays.fill(shown, Math.max(0, i - CONTEXT), Math.min(rows.size(), i + CONTEXT + 1), true);
            }
        }
        var result = new ArrayList<Row>();
        var skipped = 0;
        for (var i = 0; i < rows.size(); i++) {
            if (shown[i]) {
                if (skipped > 0) {
                    result.add(new Row(Kind.SKIPPED, List.of(), List.of(), skipped));
                    skipped = 0;
                }
                result.add(rows.get(i));
            } else {
                skipped++;
            }
        }
        if (skipped > 0) {
            result.add(new Row(Kind.SKIPPED, List.of(), List.of(), skipped));
        }
        return result;
    }
}
