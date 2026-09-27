package in.ac.iitm.guide.wikilink;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Plain Java, no Spring context: the renderer is the piece with the most edge cases, and this is
 * what keeps a red test for each of them cheap (docs/ai/architecture-rules.md, rule 4).
 *
 * <p>The resolver here is a lambda over a map, standing in for the published-article lookup that
 * {@code articleview} will supply. Matching titles case-insensitively is that lookup's job, so these
 * tests only fix what the renderer asks for and what it does with the answer.
 */
class WikiLinkRendererTest {

    private final WikiLinkRenderer renderer = new WikiLinkRenderer();

    private static final TitleResolver NOTHING_EXISTS = titles -> Map.of();

    @Test
    // trace:FR-002
    void a_link_to_a_published_article_renders_as_a_link_to_that_article() {
        var html =
                renderer.render("See [[Hostel Life]] first.", titles -> Map.of("Hostel Life", "/articles/hostel-life"));

        assertThat(html).contains("<a href=\"/articles/hostel-life\" class=\"wikilink\">Hostel Life</a>");
    }

    @Test
    // trace:FR-002
    void the_words_of_a_link_are_the_title_as_the_author_wrote_it() {
        var html = renderer.render("See [[hostel LIFE]].", titles -> Map.of("hostel LIFE", "/articles/hostel-life"));

        assertThat(html).contains(">hostel LIFE</a>");
    }

    @Test
    // trace:FR-004
    void a_link_to_a_missing_article_renders_red() {
        var html = renderer.render("See [[Nowhere]].", NOTHING_EXISTS);

        assertThat(html)
                .contains("<span class=\"wikilink wikilink-missing\">Nowhere</span>")
                .doesNotContain("<a ");
    }

    @Test
    // trace:FR-002
    void a_link_with_words_shows_the_words_and_looks_the_article_up_by_the_title() {
        var asked = new ArrayList<Set<String>>();

        var html = renderer.render("Read [[Hostel Life|the hostel guide]] now.", titles -> {
            asked.add(titles);
            return Map.of("Hostel Life", "/articles/hostel-life");
        });

        assertThat(html)
                .contains("<a href=\"/articles/hostel-life\" class=\"wikilink\">the hostel guide</a>")
                .doesNotContain(">Hostel Life<");
        assertThat(asked).containsExactly(Set.of("Hostel Life"));
    }

    @Test
    // trace:FR-004
    void a_missing_link_with_words_is_red_and_shows_the_words() {
        var html = renderer.render("Read [[Nowhere|that page]] now.", NOTHING_EXISTS);

        assertThat(html)
                .contains("<span class=\"wikilink wikilink-missing\">that page</span>")
                .doesNotContain("Nowhere");
    }

    @Test
    // trace:FR-002
    void a_page_with_several_links_asks_for_all_the_titles_in_one_call() {
        var calls = new ArrayList<Set<String>>();

        renderer.render("[[A]] then [[B|b]] then [[A]] again", titles -> {
            calls.add(titles);
            return Map.of();
        });

        // The number of calls is the behaviour here: one lookup per link would be the N+1 that
        // docs/ai/security.md rules out.
        assertThat(calls).containsExactly(Set.of("A", "B"));
    }

    @Test
    // trace:FR-002
    void two_links_with_nothing_between_them_both_render() {
        var html = renderer.render("[[A]][[B]]", titles -> Map.of("A", "/articles/a"));

        assertThat(html)
                .contains(
                        "<a href=\"/articles/a\" class=\"wikilink\">A</a><span class=\"wikilink wikilink-missing\">B</span>");
    }

    @Test
    // trace:FR-002
    void brackets_that_are_never_closed_are_left_as_written() {
        var html = renderer.render("See [[Hostel Life and more.", titles -> Map.of("Hostel Life", "/x"));

        assertThat(html).contains("See [[Hostel Life and more.").doesNotContain("wikilink");
    }

    @Test
    // trace:FR-002
    void escaped_brackets_still_make_a_link_and_a_code_span_shows_them_as_written() {
        // Decided by the human, 28 Sep: commonmark drops the backslash before the renderer sees the
        // text, so an escape cannot keep a link from forming; a code span is how an author shows it.
        var html = renderer.render("\\[\\[Visa]] and `[[Visa]]`", titles -> Map.of("Visa", "/articles/visa"));

        assertThat(html).contains("<a href=\"/articles/visa\" class=\"wikilink\">Visa</a> and <code>[[Visa]]</code>");
    }

    @Test
    // trace:FR-002
    void a_third_bracket_on_either_side_stays_as_text_around_the_link() {
        var html = renderer.render("[[[Visa]]]", titles -> Map.of("Visa", "/articles/visa"));

        assertThat(html).contains("[<a href=\"/articles/visa\" class=\"wikilink\">Visa</a>]");
    }

    @Test
    // trace:FR-002
    void in_a_link_inside_the_words_of_another_only_the_inner_one_is_a_link() {
        var html = renderer.render("[[A|[[Visa]]]]", titles -> Map.of("Visa", "/articles/visa"));

        assertThat(html).contains("[[A|<a href=\"/articles/visa\" class=\"wikilink\">Visa</a>]]");
    }

    @Test
    // trace:FR-002
    void the_words_are_everything_after_the_first_bar() {
        var html = renderer.render("[[Visa|b|c]]", titles -> Map.of("Visa", "/articles/visa"));

        assertThat(html).contains("<a href=\"/articles/visa\" class=\"wikilink\">b|c</a>");
    }

    @Test
    // trace:FR-002
    void letters_straight_after_the_brackets_are_not_part_of_the_link() {
        var html = renderer.render("[[Visa]]s", titles -> Map.of("Visa", "/articles/visa"));

        assertThat(html).contains("<a href=\"/articles/visa\" class=\"wikilink\">Visa</a>s");
    }

    @Test
    // trace:FR-001
    void a_table_written_in_markdown_renders_as_a_table() {
        // Three seed articles hold tables (the academic calendar among them), shown as raw pipes
        // until the tables extension was added on 28 Sep.
        var html = renderer.render("| Semester | Start date |\n|---|---|\n| Semester 1 | 15.01.2026 |", NOTHING_EXISTS);

        assertThat(html)
                .contains("<table")
                .contains("<th>Semester</th>")
                .contains("<td>15.01.2026</td>")
                .doesNotContain("|---|");
    }

    @Test
    // trace:FR-001
    void a_table_can_be_scrolled_from_the_keyboard() {
        // A wide table scrolls inside itself on a phone (site.css); axe's scrollable-region-focusable
        // asks that the keyboard can reach it too (NFR-007).
        var html = renderer.render("| A |\n|---|\n| b |", NOTHING_EXISTS);

        assertThat(html).contains("<table tabindex=\"0\">");
    }

    @Test
    // trace:FR-001
    void raw_html_in_a_table_cell_is_escaped() {
        var html = renderer.render("| A |\n|---|\n| <script>alert(1)</script> |", NOTHING_EXISTS);

        assertThat(html).contains("&lt;script&gt;").doesNotContain("<script>");
    }

    @Test
    // trace:FR-002
    void a_wiki_link_in_a_table_cell_renders() {
        var html = renderer.render(
                "| See |\n|---|\n| [[Hostel Life]] |", titles -> Map.of("Hostel Life", "/articles/hostel-life"));

        assertThat(html).contains("<td><a href=\"/articles/hostel-life\" class=\"wikilink\">Hostel Life</a></td>");
    }

    @Test
    // trace:FR-002
    void a_page_without_links_never_asks_the_resolver() {
        var calls = new ArrayList<Set<String>>();

        var html = renderer.render("Just **text**.", titles -> {
            calls.add(titles);
            return Map.of();
        });

        assertThat(html).contains("<strong>text</strong>");
        assertThat(calls).isEmpty();
    }

    @Test
    // trace:FR-002
    void spaces_around_the_title_are_not_part_of_it() {
        var asked = new ArrayList<Set<String>>();

        renderer.render("[[  Hostel Life  ]]", titles -> {
            asked.add(titles);
            return Map.of();
        });

        assertThat(asked).containsExactly(Set.of("Hostel Life"));
    }

    @Test
    // trace:FR-002
    void a_link_inside_bold_text_still_renders() {
        var html = renderer.render("**[[Hostel Life]]**", titles -> Map.of("Hostel Life", "/articles/hostel-life"));

        assertThat(html)
                .contains("<strong><a href=\"/articles/hostel-life\" class=\"wikilink\">Hostel Life</a></strong>");
    }

    @Test
    // trace:FR-002
    void a_title_in_devanagari_renders_as_a_link() {
        var html = renderer.render("देखें [[छात्रावास]]", titles -> Map.of("छात्रावास", "/articles/hostel"));

        assertThat(html).contains(">छात्रावास</a>");
    }

    @Test
    // trace:FR-002
    void markup_inside_a_code_span_is_left_as_written() {
        var calls = new ArrayList<Set<String>>();

        var html = renderer.render("Type `[[Hostel Life]]` to link.", titles -> {
            calls.add(titles);
            return Map.of();
        });

        assertThat(html).contains("<code>[[Hostel Life]]</code>");
        assertThat(calls).isEmpty();
    }

    @Test
    // trace:FR-002
    void markup_inside_an_ordinary_markdown_link_is_left_as_written() {
        // A link inside a link is not valid HTML, and the browser would close the outer one early.
        var html = renderer.render(
                "[see [[Hostel Life]] here](https://example.org)", titles -> Map.of("Hostel Life", "/x"));

        assertThat(html).contains("see [[Hostel Life]] here</a>").doesNotContain("wikilink");
    }

    @Test
    // trace:FR-002
    void empty_brackets_and_a_missing_title_are_not_links() {
        var calls = new ArrayList<Set<String>>();
        TitleResolver resolver = titles -> {
            calls.add(titles);
            return Map.of();
        };

        var html = renderer.render("[[]] and [[   ]] and [[|words]]", resolver);

        assertThat(html).contains("[[]] and [[   ]] and [[|words]]");
        assertThat(calls).isEmpty();
    }

    @Test
    // trace:FR-002
    void a_link_with_an_empty_words_part_shows_the_title() {
        var html = renderer.render("[[Hostel Life|]]", titles -> Map.of("Hostel Life", "/articles/hostel-life"));

        assertThat(html).contains(">Hostel Life</a>");
    }

    @Test
    // trace:FR-004
    void a_title_containing_html_characters_is_escaped_in_the_output() {
        var html = renderer.render("[[Fees & <Payments>]]", NOTHING_EXISTS);

        assertThat(html).contains("Fees &amp; &lt;Payments&gt;").doesNotContain("<Payments>");
    }

    @Test
    void raw_html_in_a_body_is_escaped_not_passed_through() {
        // ADR-0001: Markdown was chosen so that there is no HTML allowlist to keep; that only holds
        // while the converter's raw-HTML passthrough stays switched off.
        var html = renderer.render("Hello <script>alert(1)</script> world", NOTHING_EXISTS);

        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;");
    }

    @Test
    void a_javascript_url_in_an_ordinary_link_is_not_rendered_live() {
        var html = renderer.render("[click](javascript:alert(1))", NOTHING_EXISTS);

        assertThat(html).contains("click").doesNotContain("href=\"javascript:");
    }

    @Test
    // trace:FR-002
    void markup_inside_the_words_of_a_link_leaves_the_whole_link_as_written() {
        // commonmark cuts the text at the emphasis, so no single run of text holds the whole link.
        var calls = new ArrayList<Set<String>>();

        var html = renderer.render("[[Hostel Life|the *best* guide]]", titles -> {
            calls.add(titles);
            return Map.of();
        });

        assertThat(html).contains("[[Hostel Life|the <em>best</em> guide]]");
        assertThat(calls).isEmpty();
    }

    @Test
    // trace:FR-002
    void a_link_wrapped_over_two_lines_is_left_as_written() {
        var html = renderer.render("see [[Hostel\nLife]] here", titles -> Map.of("Hostel Life", "/x"));

        assertThat(html).contains("[[Hostel\nLife]]").doesNotContain("wikilink");
    }

    @Test
    // trace:FR-002
    void markup_inside_a_fenced_code_block_is_left_as_written() {
        var calls = new ArrayList<Set<String>>();

        var html = renderer.render("```\n[[Hostel Life]]\n```", titles -> {
            calls.add(titles);
            return Map.of();
        });

        assertThat(html).contains("[[Hostel Life]]").doesNotContain("wikilink");
        assertThat(calls).isEmpty();
    }

    @Test
    // trace:FR-002
    void markup_inside_the_alt_text_of_an_image_is_left_as_written() {
        var html = renderer.render(
                "![a [[Hostel Life]] photo](https://example.org/p.png)", titles -> Map.of("Hostel Life", "/x"));

        assertThat(html).contains("alt=\"a [[Hostel Life]] photo\"").doesNotContain("wikilink");
    }

    @Test
    void an_external_link_in_a_body_opens_without_handing_the_page_to_the_target() {
        // docs/architecture/security.md, "Article content": external URLs carry noopener noreferrer.
        var html = renderer.render("[the portal](https://example.org/portal)", NOTHING_EXISTS);

        assertThat(html)
                .contains("rel=\"nofollow noopener noreferrer\"")
                .contains("href=\"https://example.org/portal\"");
    }

    @Test
    void an_internal_link_in_a_body_does_not_get_the_external_link_rel() {
        var html = renderer.render("[other](/articles/other)", NOTHING_EXISTS);

        assertThat(html).contains("href=\"/articles/other\"").doesNotContain("noopener");
    }

    @Test
    // trace:FR-002
    void a_link_resolved_to_an_href_with_quotes_cannot_break_out_of_the_attribute() {
        var html = renderer.render("[[X]]", titles -> Map.of("X", "/articles/x\" onmouseover=\"alert(1)"));

        assertThat(html).contains(">X</a>").doesNotContain("\" onmouseover=\"");
    }
}
