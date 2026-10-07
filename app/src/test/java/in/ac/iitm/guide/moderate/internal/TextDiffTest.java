package in.ac.iitm.guide.moderate.internal;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.guide.moderate.internal.TextDiff.Kind;
import in.ac.iitm.guide.moderate.internal.TextDiff.Row;
import in.ac.iitm.guide.moderate.internal.TextDiff.Segment;
import in.ac.iitm.guide.wikilink.WikiLinkRenderer;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** FR-029's comparison of an article's body with an edit's, paragraph by paragraph. */
class TextDiffTest {

    /** The reading text of a paragraph, as the review page asks for it (fix 3.7, ADR-0015's amendment). */
    private static final UnaryOperator<String> AS_READ = new WikiLinkRenderer()::plainText;

    @Test
    // trace:FR-029
    void one_changed_word_is_marked_on_both_sides_of_its_paragraph() {
        var diff = TextDiff.of("Bring your passport today.", "Bring your visa today.");

        assertThat(diff.rows())
                .containsExactly(new Row(
                        Kind.CHANGED,
                        List.of(
                                new Segment("Bring your ", false),
                                new Segment("passport", true),
                                new Segment(" today.", false)),
                        List.of(
                                new Segment("Bring your ", false),
                                new Segment("visa", true),
                                new Segment(" today.", false)),
                        0));
    }

    @Test
    // trace:FR-029
    void an_added_paragraph_is_marked_as_added_with_nothing_before_it() {
        var diff = TextDiff.of("First.", "First.\n\nSecond.");

        assertThat(diff.rows())
                .last()
                .isEqualTo(new Row(Kind.ADDED, List.of(), List.of(new Segment("Second.", true)), 0));
    }

    @Test
    // trace:FR-029
    void a_removed_paragraph_is_marked_as_removed_with_nothing_after_it() {
        var diff = TextDiff.of("First.\n\nSecond.", "First.");

        assertThat(diff.rows())
                .last()
                .isEqualTo(new Row(Kind.REMOVED, List.of(new Segment("Second.", true)), List.of(), 0));
    }

    @Test
    // trace:FR-029
    void identical_texts_are_unchanged() {
        var diff = TextDiff.of("First.\n\nSecond.", "First.\n\nSecond.");

        assertThat(diff.unchanged()).isTrue();
    }

    @Test
    // trace:FR-029
    void a_text_with_a_change_is_not_unchanged() {
        var diff = TextDiff.of("First.", "Second.");

        assertThat(diff.unchanged()).isFalse();
    }

    @Test
    // trace:FR-029
    void unchanged_paragraphs_away_from_a_change_are_skipped_and_counted() {
        var diff = TextDiff.of("One.\n\nTwo.\n\nThree.\n\nFour.\n\nFive.", "One.\n\nTwo.\n\nThree.\n\nFour.\n\nFive!");

        assertThat(diff.rows()).extracting(Row::kind).containsExactly(Kind.SKIPPED, Kind.SAME, Kind.CHANGED);
        assertThat(diff.rows().getFirst().skipped()).isEqualTo(3);
    }

    @Test
    // trace:FR-029
    void the_unchanged_paragraph_on_each_side_of_a_change_is_kept_for_context() {
        var diff = TextDiff.of("One.\n\nTwo.\n\nThree.\n\nFour.\n\nFive.", "One.\n\nTwo.\n\nThree!\n\nFour.\n\nFive.");

        assertThat(diff.rows())
                .extracting(Row::kind)
                .containsExactly(Kind.SKIPPED, Kind.SAME, Kind.CHANGED, Kind.SAME, Kind.SKIPPED);
    }

    @Test
    // trace:FR-029
    void one_paragraph_replaced_by_two_pairs_the_first_and_adds_the_second() {
        var diff = TextDiff.of("Old text.", "New text.\n\nMore text.");

        assertThat(diff.rows()).extracting(Row::kind).containsExactly(Kind.CHANGED, Kind.ADDED);
    }

    @Test
    // trace:FR-029
    void windows_line_endings_are_not_a_change() {
        var diff = TextDiff.of("First.\r\n\r\nSecond.", "First.\n\nSecond.");

        assertThat(diff.unchanged()).isTrue();
    }

    @Test
    // trace:FR-029
    void more_blank_lines_between_paragraphs_are_not_a_change() {
        var diff = TextDiff.of("First.\n\n\n\nSecond.", "First.\n\nSecond.");

        assertThat(diff.unchanged()).isTrue();
    }

    @Test
    // trace:FR-029
    void a_line_break_inside_a_paragraph_stays_part_of_it() {
        var diff = TextDiff.of("- Photographs\n- Passport", "- Photographs\n- Visa");

        assertThat(diff.rows()).hasSize(1);
        assertThat(diff.rows().getFirst().after()).contains(new Segment("Visa", true));
    }

    @Test
    // trace:FR-029
    void the_reading_text_marks_a_changed_word_without_the_markdown_around_it() {
        var diff = TextDiff.of("Bring your **passport** today.", "Bring your **visa** today.", AS_READ);

        assertThat(diff.rows())
                .containsExactly(new Row(
                        Kind.CHANGED,
                        List.of(
                                new Segment("Bring your ", false),
                                new Segment("passport", true),
                                new Segment(" today.", false)),
                        List.of(
                                new Segment("Bring your ", false),
                                new Segment("visa", true),
                                new Segment(" today.", false)),
                        0));
    }

    @Test
    // trace:FR-029
    void a_change_of_formatting_alone_leaves_the_reading_text_unchanged() {
        var diff = TextDiff.of("Bring your passport today.", "Bring your **passport** today.", AS_READ);

        assertThat(diff.unchanged()).isTrue();
    }

    @Test
    // trace:FR-029
    void a_line_break_of_the_source_is_not_in_the_reading_text() {
        var diff = TextDiff.of("Bring your\npassport today.", "Bring your\nvisa today.", AS_READ);

        assertThat(diff.rows().getFirst().before()).extracting(Segment::text).noneMatch(text -> text.contains("\n"));
    }

    @Test
    // trace:FR-029
    void a_wiki_link_reads_as_the_words_it_shows() {
        var diff = TextDiff.of("See the office.", "See [[Visa Office|the visa office]].", AS_READ);

        assertThat(diff.rows().getFirst().after())
                .containsExactly(
                        new Segment("See the ", false), new Segment("visa ", true), new Segment("office.", false));
    }

    @Test
    // trace:FR-029
    void an_added_paragraph_with_nothing_to_read_is_not_a_change_of_the_reading_text() {
        var diff = TextDiff.of("First.\n\nSecond.", "First.\n\n---\n\nSecond.", AS_READ);

        assertThat(diff.unchanged()).isTrue();
    }
}
