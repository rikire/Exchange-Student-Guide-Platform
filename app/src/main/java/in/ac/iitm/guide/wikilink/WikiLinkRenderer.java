package in.ac.iitm.guide.wikilink;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Turns an article body written in Markdown into HTML, with {@code [[Title]]} and {@code
 * [[Title|words]]} resolved to links or red links (FR-002, FR-004).
 *
 * <p>Plain Java on purpose (docs/ai/architecture-rules.md, rule 4): no Spring, no JPA, so its tests
 * run without a container. Safe to share between threads: the parser and the HTML renderer are
 * configured once, and everything a call learns lives in that call.
 *
 * <p>A wiki link is recognised only inside one run of plain text. Words containing Markdown of their
 * own ({@code [[Title|some *words*]]}) or a link broken across two lines are left as written, and so
 * is anything inside a code span or inside an ordinary Markdown link, where an anchor within an
 * anchor would not be valid HTML.
 */
// trace:FR-002
// trace:FR-004
public class WikiLinkRenderer {

    private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[([^\\[\\]]*)]]");

    private final Parser parser = Parser.builder().build();

    // escapeHtml and sanitizeUrls are what make ADR-0001's "no HTML allowlist" true: the converter
    // does not pass raw HTML through, so there is nothing to filter afterwards. Changing either is
    // the human's decision (docs/ai/collaboration.md).
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .attributeProviderFactory(context -> WikiLinkRenderer::markExternalLink)
            .nodeRendererFactory(WikiLinkHtml::new)
            .build();

    /**
     * @param markdown an article body
     * @param resolver asked once, with every title the body links to, and not at all when it links to
     *     none
     * @return HTML for the body; raw HTML in the body is escaped, never passed through
     */
    public String render(String markdown, TitleResolver resolver) {
        Node document = parser.parse(markdown);

        var runs = new ArrayList<Run>();
        document.accept(new TextRunCollector(runs));

        var titles = new LinkedHashSet<String>();
        runs.forEach(run -> run.addTitlesTo(titles));

        if (!titles.isEmpty()) {
            var hrefs = resolver.resolve(Set.copyOf(titles));
            runs.forEach(run -> run.replaceIn(hrefs));
        }

        return htmlRenderer.render(document);
    }

    /**
     * security.md asks for {@code rel="noopener noreferrer"} on an external URL in article text. The
     * library already sets {@code nofollow} on every link once URLs are sanitised, which is right for
     * text written by strangers, so it is kept and the two are added to it.
     */
    private static void markExternalLink(Node node, String tagName, Map<String, String> attributes) {
        if (node instanceof Link link && isExternal(link.getDestination())) {
            attributes.put("rel", "nofollow noopener noreferrer");
        }
    }

    private static boolean isExternal(String destination) {
        var lower = destination.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("//");
    }

    /** Finds the plain-text nodes that hold wiki links. */
    private static final class TextRunCollector extends AbstractVisitor {
        private final List<Run> runs;

        TextRunCollector(List<Run> runs) {
            this.runs = runs;
        }

        @Override
        public void visit(Text text) {
            var run = Run.of(text);
            if (run != null) {
                runs.add(run);
            }
        }

        // Neither an ordinary link's text nor an image's alt text is searched: see the class comment.
        @Override
        public void visit(Link link) {}

        @Override
        public void visit(Image image) {}
    }

    /** One stretch of a text node: words to keep as they are, or a wiki link to resolve. */
    private sealed interface Piece permits Literal, Occurrence {}

    private record Literal(String text) implements Piece {}

    /** What sits between the brackets: the title to look up, and the words to show. */
    private record Occurrence(String title, String words) implements Piece {

        /** @return null when there is no title, so the brackets stay as the author typed them */
        static Occurrence parse(String inner) {
            int bar = inner.indexOf('|');
            var title = (bar < 0 ? inner : inner.substring(0, bar)).strip();
            if (title.isEmpty()) {
                return null;
            }
            var words = bar < 0 ? "" : inner.substring(bar + 1).strip();
            return new Occurrence(title, words.isEmpty() ? title : words);
        }
    }

    /** One text node cut into the literal stretches and the wiki links it holds. */
    private record Run(Text node, List<Piece> pieces) {

        /** @return null when the node holds no wiki link */
        static Run of(Text node) {
            var literal = node.getLiteral();
            var matcher = WIKI_LINK.matcher(literal);
            var pieces = new ArrayList<Piece>();
            int copiedUpTo = 0;
            while (matcher.find()) {
                var occurrence = Occurrence.parse(matcher.group(1));
                if (occurrence == null) {
                    continue;
                }
                pieces.add(new Literal(literal.substring(copiedUpTo, matcher.start())));
                pieces.add(occurrence);
                copiedUpTo = matcher.end();
            }
            if (pieces.isEmpty()) {
                return null;
            }
            pieces.add(new Literal(literal.substring(copiedUpTo)));
            return new Run(node, pieces);
        }

        void addTitlesTo(Set<String> titles) {
            for (var piece : pieces) {
                if (piece instanceof Occurrence occurrence) {
                    titles.add(occurrence.title());
                }
            }
        }

        void replaceIn(Map<String, String> hrefs) {
            for (var piece : pieces) {
                Node replacement =
                        switch (piece) {
                            case Occurrence occurrence -> new WikiLinkNode(
                                    occurrence.words(), hrefs.get(occurrence.title()));
                            case Literal literal -> new Text(literal.text());
                        };
                node.insertBefore(replacement);
            }
            node.unlink();
        }
    }

    /** The link itself once the resolver has answered; {@code href} is null for a red link. */
    private static final class WikiLinkNode extends CustomNode {
        private final String words;
        private final String href;

        WikiLinkNode(String words, String href) {
            this.words = words;
            this.href = href;
        }
    }

    private static final class WikiLinkHtml implements NodeRenderer {
        private final HtmlNodeRendererContext context;

        WikiLinkHtml(HtmlNodeRendererContext context) {
            this.context = context;
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Set.of(WikiLinkNode.class);
        }

        @Override
        public void render(Node node) {
            var link = (WikiLinkNode) node;
            var writer = context.getWriter();
            var attributes = new LinkedHashMap<String, String>();
            if (link.href != null) {
                attributes.put("href", link.href);
                attributes.put("class", "wikilink");
                writer.tag("a", context.extendAttributes(node, "a", attributes));
                writer.text(link.words);
                writer.tag("/a");
            } else {
                attributes.put("class", "wikilink wikilink-missing");
                writer.tag("span", context.extendAttributes(node, "span", attributes));
                writer.text(link.words);
                writer.tag("/span");
            }
        }
    }
}
