package in.ac.iitm.guide.wikilink;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.heading.anchor.IdGenerator;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentNodeRendererContext;
import org.commonmark.renderer.text.TextContentRenderer;

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
// trace:FR-006
public class WikiLinkRenderer {

    private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[([^\\[\\]]*)]]");

    // Tables as GitHub writes them (the human, 28 Sep): CommonMark has none, and seed articles use them.
    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());

    // Fix 3.5 (F-10): ids on headings, prefixed so a heading named "Tags" cannot take an id the
    // page around the body already uses.
    private static final String HEADING_ID_PREFIX = "section-";
    private static final String HEADING_DEFAULT_ID = "heading";

    private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();

    // escapeHtml and sanitizeUrls are what make ADR-0001's "no HTML allowlist" true: the converter
    // does not pass raw HTML through, so there is nothing to filter afterwards. Changing either is
    // the human's decision (docs/ai/collaboration.md).
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .escapeHtml(true)
            .sanitizeUrls(true)
            .extensions(List.of(HeadingAnchorExtension.builder()
                    .idPrefix(HEADING_ID_PREFIX)
                    .defaultId(HEADING_DEFAULT_ID)
                    .build()))
            .attributeProviderFactory(context -> WikiLinkRenderer::markExternalLink)
            .attributeProviderFactory(context -> WikiLinkRenderer::makeTableFocusable)
            .nodeRendererFactory(WikiLinkHtml::new)
            .build();

    // A link and an image as their words alone: the library's own text renderer adds the address.
    private final TextContentRenderer textRenderer = TextContentRenderer.builder()
            .extensions(EXTENSIONS)
            .stripNewlines(true)
            .nodeRendererFactory(WordsOnly::new)
            .build();

    /**
     * @param markdown an article body
     * @param resolver asked once, with every title the body links to, and not at all when it links to
     *     none
     * @return HTML for the body; raw HTML in the body is escaped, never passed through
     */
    public String render(String markdown, TitleResolver resolver) {
        return htmlRenderer.render(resolved(markdown, resolver));
    }

    private Node resolved(String markdown, TitleResolver resolver) {
        Node document = parser.parse(markdown);

        var runs = new ArrayList<Run>();
        document.accept(new TextRunCollector(runs));

        var titles = new LinkedHashSet<String>();
        runs.forEach(run -> run.addTitlesTo(titles));

        if (!titles.isEmpty()) {
            var hrefs = resolver.resolve(Set.copyOf(titles));
            runs.forEach(run -> run.replaceIn(hrefs));
        }
        return document;
    }

    /** A heading of the body, as an article's contents list names it (fix 3.5, F-10). */
    public record Heading(int level, String text, String id) {}

    /** A body rendered, with its headings in order, each under the id its HTML carries. */
    public record Body(String html, List<Heading> contents) {}

    /**
     * {@link #render}, and the headings a contents list links to. The ids come from the library's
     * {@link IdGenerator}, fed the same words in the same order as the heading-anchor extension feeds
     * it while rendering — the text and code of the heading, not a wiki link's words — so each id
     * here is the one the heading carries.
     */
    public Body renderBody(String markdown, TitleResolver resolver) {
        var document = resolved(markdown, resolver);
        var ids = IdGenerator.builder()
                .prefix(HEADING_ID_PREFIX)
                .defaultId(HEADING_DEFAULT_ID)
                .build();
        var contents = new ArrayList<Heading>();
        document.accept(new AbstractVisitor() {
            @Override
            public void visit(org.commonmark.node.Heading heading) {
                var idWords = new StringBuilder();
                var shownWords = new StringBuilder();
                heading.accept(new AbstractVisitor() {
                    @Override
                    public void visit(Text text) {
                        idWords.append(text.getLiteral());
                        shownWords.append(text.getLiteral());
                    }

                    @Override
                    public void visit(Code code) {
                        idWords.append(code.getLiteral());
                        shownWords.append(code.getLiteral());
                    }

                    @Override
                    public void visit(CustomNode node) {
                        if (node instanceof WikiLinkNode link) {
                            shownWords.append(link.words);
                        }
                        visitChildren(node);
                    }
                });
                contents.add(new Heading(
                        heading.getLevel(),
                        shownWords.toString().strip(),
                        ids.generateId(idWords.toString().trim().toLowerCase())));
            }
        });
        return new Body(htmlRenderer.render(document), List.copyOf(contents));
    }

    /**
     * The titles a body's wiki links name, found by the same parser {@link #render} uses, so a link in
     * a code span or in an ordinary link's text is not one here either (FR-006, ADR-0016).
     *
     * @return each title once, as written
     */
    public Set<String> linkedTitles(String markdown) {
        var runs = new ArrayList<Run>();
        parser.parse(markdown).accept(new TextRunCollector(runs));
        var titles = new LinkedHashSet<String>();
        runs.forEach(run -> run.addTitlesTo(titles));
        return titles;
    }

    /**
     * security.md asks for {@code rel="noopener noreferrer"} on an external URL in article text. The
     * library already sets {@code nofollow} on every link once URLs are sanitised, which is right for
     * text written by strangers, so it is kept and the two are added to it.
     */
    private static void markExternalLink(Node node, String tagName, Map<String, String> attributes) {
        if (node instanceof Link link && isExternal(link.getDestination())) {
            attributes.put("rel", "nofollow noopener noreferrer");
            // Fix 3.5 (F-10): a reader keeps the guide open; site.css marks such a link.
            attributes.put("target", "_blank");
        }
    }

    /**
     * A wide table scrolls sideways inside itself on a phone (site.css, NFR-008), and a region that
     * scrolls has to be reachable from the keyboard as well (NFR-007, axe's
     * scrollable-region-focusable).
     */
    private static void makeTableFocusable(Node node, String tagName, Map<String, String> attributes) {
        if (node instanceof TableBlock) {
            attributes.put("tabindex", "0");
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

    /**
     * The body as a reader reads it, for search to index and quote (FR-007): Markdown's marks gone, a
     * link or an image as its words, a wiki link as the words it shows, all on one line. Raw HTML in
     * the body stays as text; whoever shows the result escapes it.
     */
    public String plainText(String markdown) {
        var text = textRenderer.render(parser.parse(markdown));
        return WIKI_LINK.matcher(text).replaceAll(match -> {
            var occurrence = Occurrence.parse(match.group(1));
            return Matcher.quoteReplacement(occurrence == null ? match.group() : occurrence.words());
        });
    }

    /** Renders a link or an image in plain text as its words, without the address. */
    private record WordsOnly(TextContentNodeRendererContext context) implements NodeRenderer {

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Set.of(Link.class, Image.class);
        }

        @Override
        public void render(Node node) {
            for (var child = node.getFirstChild(); child != null; child = child.getNext()) {
                context.render(child);
            }
        }
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
                                    occurrence.words(),
                                    hrefs.get(occurrence.title()),
                                    invitationTo(occurrence.title()));
                            case Literal literal -> new Text(literal.text());
                        };
                node.insertBefore(replacement);
            }
            node.unlink();
        }
    }

    /**
     * FR-005: where a red link leads — the address the missing article would have, carrying the title
     * as written, which the not-found page there offers to the submission form. Empty for a title
     * with no letters or digits, which can have no address.
     */
    private static String invitationTo(String title) {
        return ArticleAddress.slugOf(title)
                .map(slug -> ArticleAddress.pathOf(slug) + "?title="
                        // Form encoding writes a space as "+"; in a query a "%20" reads the same and
                        // is what the submission form's link carries on.
                        + URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20"))
                .orElse(null);
    }

    /**
     * The link itself once the resolver has answered; {@code href} is null for a red link, and
     * {@code invitation} then where it leads (FR-005), null when the title has no address.
     */
    private static final class WikiLinkNode extends CustomNode {
        private final String words;
        private final String href;
        private final String invitation;

        WikiLinkNode(String words, String href, String invitation) {
            this.words = words;
            this.href = href;
            this.invitation = invitation;
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
            } else if (link.invitation != null) {
                attributes.put("href", link.invitation);
                attributes.put("class", "wikilink wikilink-missing");
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
