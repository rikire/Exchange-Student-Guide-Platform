package in.ac.iitm.guide.search.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Text with the query's words marked, cut into pieces the template escapes one by one, so the page
 * never prints the highlighter's output as HTML (security.md, "Article content": {@code th:utext} is
 * for the Markdown converter alone). The highlighter marks a word with two control characters no
 * article holds, since {@link BodyAsText} removes them.
 */
public record Passage(List<Piece> pieces) {

    static final String OPEN = "\u0002";
    static final String CLOSE = "\u0003";

    /** A stretch of the text, and whether it is one of the query's words. */
    public record Piece(String text, boolean marked) {}

    public boolean isEmpty() {
        return pieces.isEmpty();
    }

    /** @param highlighted text from the highlighter, the matched words between {@link #OPEN} and {@link #CLOSE} */
    static Passage of(String highlighted) {
        var pieces = new ArrayList<Piece>();
        var rest = highlighted;
        while (!rest.isEmpty()) {
            var open = rest.indexOf(OPEN);
            var close = open < 0 ? -1 : rest.indexOf(CLOSE, open);
            if (open < 0 || close < 0) {
                pieces.add(new Piece(rest, false));
                break;
            }
            if (open > 0) {
                pieces.add(new Piece(rest.substring(0, open), false));
            }
            pieces.add(new Piece(rest.substring(open + 1, close), true));
            rest = rest.substring(close + 1);
        }
        return new Passage(List.copyOf(pieces));
    }

    static Passage plain(String text) {
        return new Passage(List.of(new Piece(text, false)));
    }

    static Passage none() {
        return new Passage(List.of());
    }
}
