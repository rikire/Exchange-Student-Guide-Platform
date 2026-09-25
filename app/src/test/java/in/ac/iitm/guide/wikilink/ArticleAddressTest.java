package in.ac.iitm.guide.wikilink;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Plain Java, like {@link WikiLinkRendererTest}: the address rules are all string handling. */
class ArticleAddressTest {

    @Test
    // trace:FR-001
    void a_title_becomes_its_lower_case_words_joined_by_hyphens() {
        assertThat(ArticleAddress.slugOf("Applying for Your Student Visa")).contains("applying-for-your-student-visa");
    }

    @Test
    // trace:FR-001
    void a_run_of_punctuation_and_spaces_becomes_one_hyphen() {
        assertThat(ArticleAddress.slugOf("Fees &  Payments")).contains("fees-payments");
    }

    @Test
    // trace:FR-001
    void hyphens_at_either_end_are_dropped() {
        assertThat(ArticleAddress.slugOf("  -Hostel Life- ")).contains("hostel-life");
    }

    @Test
    // trace:FR-002
    void letter_case_does_not_change_the_address() {
        assertThat(ArticleAddress.slugOf("HOSTEL life")).isPresent().isEqualTo(ArticleAddress.slugOf("Hostel LIFE"));
    }

    @Test
    // trace:FR-001
    void two_titles_that_differ_only_in_punctuation_share_an_address() {
        // Why the slug is unique in the schema: this is the collision FR-010's case-insensitive
        // title check does not see, and it would make the second article unreachable.
        assertThat(ArticleAddress.slugOf("Fees & Payments"))
                .isPresent()
                .isEqualTo(ArticleAddress.slugOf("Fees Payments"));
    }

    @Test
    // trace:FR-001
    void the_vowel_signs_of_devanagari_are_kept() {
        // Vowel signs are combining marks (\p{M}), not letters (\p{L}): a rule that keeps only
        // letters and digits cuts "छात्रावास" down to consonants.
        assertThat(ArticleAddress.slugOf("छात्रावास जीवन")).contains("छात्रावास-जीवन");
    }

    @Test
    // trace:FR-001
    void the_vowel_signs_of_tamil_are_kept() {
        assertThat(ArticleAddress.slugOf("விடுதி வாழ்க்கை")).contains("விடுதி-வாழ்க்கை");
    }

    @Test
    // trace:FR-001
    void one_visible_word_written_two_ways_has_one_address() {
        var composed = "Café";
        var decomposed = "Café";

        assertThat(ArticleAddress.slugOf(decomposed)).isPresent().isEqualTo(ArticleAddress.slugOf(composed));
    }

    @Test
    // trace:FR-001
    void digits_are_kept() {
        assertThat(ArticleAddress.slugOf("Academic Calendar 2026")).contains("academic-calendar-2026");
    }

    @Test
    // trace:FR-001
    void a_title_with_no_letters_or_digits_has_no_address() {
        assertThat(ArticleAddress.slugOf("?! --")).isEmpty();
    }

    @Test
    // trace:FR-001
    void an_empty_title_has_no_address() {
        assertThat(ArticleAddress.slugOf("")).isEmpty();
    }

    @Test
    // trace:FR-001
    void the_path_of_a_latin_slug_is_the_slug_under_articles() {
        assertThat(ArticleAddress.pathOf("hostel-life")).isEqualTo("/articles/hostel-life");
    }

    @Test
    // trace:FR-001
    void the_path_of_a_devanagari_slug_is_percent_encoded_utf8() {
        assertThat(ArticleAddress.pathOf("छात्र")).isEqualTo("/articles/%E0%A4%9B%E0%A4%BE%E0%A4%A4%E0%A5%8D%E0%A4%B0");
    }
}
