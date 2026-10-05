package in.ac.iitm.guide.wikilink;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Properties of the address rules and the wiki-link reading that hold for every title, checked on
 * generated ones in English, Hindi, Tamil, digits, punctuation and spacing (docs/ai/testing.md):
 * inputs nobody would list by hand. The examples stay in ArticleAddressTest and WikiLinkRendererTest.
 */
class WikiLinkPropertiesTest {

    private static final Pattern ADDRESS = Pattern.compile("[\\p{L}\\p{M}\\p{N}]+(-[\\p{L}\\p{M}\\p{N}]+)*");

    private final WikiLinkRenderer renderer = new WikiLinkRenderer();

    @Property
    // trace:FR-001
    void an_address_is_words_joined_by_single_hyphens_with_none_at_either_end(@ForAll("titles") String title) {
        ArticleAddress.slugOf(title).ifPresent(slug -> assertThat(slug).matches(ADDRESS));
    }

    @Property
    // trace:FR-001
    void the_address_of_an_address_is_itself(@ForAll("titles") String title) {
        ArticleAddress.slugOf(title)
                .ifPresent(slug -> assertThat(ArticleAddress.slugOf(slug)).contains(slug));
    }

    @Property
    // trace:FR-002
    void the_spacing_and_punctuation_between_words_do_not_change_the_address(
            @ForAll("words") List<String> words, @ForAll("separators") String separator) {
        assertThat(ArticleAddress.slugOf(String.join(separator, words)))
                .isEqualTo(ArticleAddress.slugOf(String.join(" ", words)));
    }

    @Property
    // trace:FR-006
    void every_title_written_as_a_wiki_link_is_read_back_as_written(@ForAll("linkable") List<String> titles) {
        var body = new StringBuilder("Before you arrive.");
        titles.forEach(title -> body.append(" See [[").append(title).append("]]."));

        assertThat(renderer.linkedTitles(body.toString()))
                .containsExactlyInAnyOrderElementsOf(
                        titles.stream().map(String::strip).distinct().toList());
    }

    @Provide
    Arbitrary<String> titles() {
        return Arbitraries.strings()
                .withChars("abcXYZ019 -_.,:;!?&'()/")
                .withCharRange('ऀ', 'ॿ')
                .withCharRange('஀', '௿')
                .ofMaxLength(40);
    }

    @Provide
    Arbitrary<List<String>> words() {
        return Arbitraries.strings()
                .withChars("abcxyz019")
                .withCharRange('क', 'ह')
                .withCharRange('க', 'ஹ')
                .ofMinLength(1)
                .ofMaxLength(8)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5);
    }

    @Provide
    Arbitrary<String> separators() {
        return Arbitraries.of(" ", "  ", " - ", ", ", ": ", " & ", "/", "_", "\t");
    }

    /** Titles a wiki link can hold: no brackets, no bar, no Markdown, not blank. */
    @Provide
    Arbitrary<List<String>> linkable() {
        return Arbitraries.strings()
                .withChars("abcXYZ019 -")
                .withCharRange('क', 'ह')
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(title -> !title.isBlank())
                .list()
                .ofMinSize(1)
                .ofMaxSize(4);
    }
}
