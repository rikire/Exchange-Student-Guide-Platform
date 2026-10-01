package in.ac.iitm.guide.taxonomy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.ac.iitm.guide.shared.persistence.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** The tag rule of ADR-0005 through the published type, on H2 with the real migrations. */
@SpringBootTest
class TagsTest {

    @Autowired
    private Tags tags;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void clearTheTags() {
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:FR-008
    void tags_are_stored_trimmed_and_lower_cased_one_row_per_tag_however_spelled() {
        var named = tags.named(List.of("SIM Card", " sim card ", "sim card", "Visa"));

        assertThat(named).extracting(Tag::getName).containsExactlyInAnyOrder("sim card", "visa");
        assertThat(jdbc.queryForList("SELECT name FROM tag", String.class))
                .containsExactlyInAnyOrder("sim card", "visa");
    }

    @Test
    // trace:FR-008
    void a_tag_that_exists_is_reused_rather_than_created_again() {
        var first = tags.named(List.of("hostel")).iterator().next();

        var second = tags.named(List.of("HOSTEL")).iterator().next();

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tag", Integer.class))
                .isEqualTo(1);
    }

    @Test
    // trace:FR-008
    void lower_casing_does_not_depend_on_the_default_locale() {
        // Under a Turkish default, "VISA".toLowerCase() is "vısa" with a dotless i: a second tag
        // that looks the same on screen (docs/ai/testing.md, "Encoding").
        var saved = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            var named = tags.named(List.of("VISA"));

            assertThat(named).extracting(Tag::getName).containsExactly("visa");
        } finally {
            Locale.setDefault(saved);
        }
    }

    @Test
    // trace:FR-008
    void a_tag_that_is_blank_after_trimming_is_refused() {
        assertThatThrownBy(() -> tags.named(List.of("visa", "   ")))
                .isInstanceOf(TagRejectedException.class)
                .hasMessageContaining("empty");
    }

    @Test
    // trace:FR-008
    void a_tag_of_64_characters_is_kept_and_one_of_65_is_refused() {
        var longest = "a".repeat(64);

        assertThat(tags.named(List.of(longest))).extracting(Tag::getName).containsExactly(longest);
        assertThatThrownBy(() -> tags.named(List.of("a".repeat(65))))
                .isInstanceOf(TagRejectedException.class)
                .hasMessageContaining("64");
    }

    @Test
    // trace:FR-008
    void ten_tags_are_kept_and_eleven_are_refused_counted_after_normalising() {
        var ten = IntStream.rangeClosed(1, 10).mapToObj(i -> "tag-" + i).toList();
        var elevenSpellingsOfTen = new ArrayList<>(ten);
        elevenSpellingsOfTen.add(" TAG-1 ");
        var eleven = new ArrayList<>(ten);
        eleven.add("tag-11");

        assertThat(tags.named(elevenSpellingsOfTen)).hasSize(10);
        assertThatThrownBy(() -> tags.named(eleven))
                .isInstanceOf(TagRejectedException.class)
                .hasMessage("More than 10 tags");
    }

    @Test
    // trace:FR-008
    void the_limit_counts_utf16_units_because_h2_does() {
        // "𑀓" (Brahmi) is one character and two UTF-16 units. PostgreSQL's VARCHAR(64) counts
        // characters, H2's counts UTF-16 units: 64 of them failed the insert on H2 when this test
        // first counted characters. The limit is the stricter of the two, so a tag fits either.
        var thirtyTwo = "𑀓".repeat(32);

        assertThat(tags.named(List.of(thirtyTwo))).extracting(Tag::getName).containsExactly(thirtyTwo);
        assertThatThrownBy(() -> tags.named(List.of("𑀓".repeat(33)))).isInstanceOf(TagRejectedException.class);
    }

    @Test
    // trace:FR-008
    void no_names_give_no_tags() {
        assertThat(tags.named(List.of())).isEmpty();
    }
}
