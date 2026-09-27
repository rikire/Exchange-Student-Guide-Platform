package in.ac.iitm.guide.contribute.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

/** NFR-006 and ADR-0011: what a submission number looks like and where its bits come from. */
class SubmissionNumbersTest {

    private static final String CROCKFORD = "[0-9A-HJKMNP-TV-Z]";

    @Test
    // trace:NFR-006
    void a_number_is_sub_and_twelve_crockford_characters_in_groups_of_four() {
        var number = new SubmissionNumbers().next();

        assertThat(number).matches("SUB-" + CROCKFORD + "{4}-" + CROCKFORD + "{4}-" + CROCKFORD + "{4}");
    }

    @Test
    // trace:NFR-006
    void the_production_generator_draws_from_SecureRandom() {
        assertThat(new SubmissionNumbers().source()).isInstanceOf(SecureRandom.class);
    }

    @Test
    // trace:NFR-006
    void numbers_sorted_by_issue_are_not_sorted_by_value() {
        var numbers = new SubmissionNumbers();
        var issued = new ArrayList<String>();
        for (var i = 0; i < 200; i++) {
            issued.add(numbers.next());
        }

        var sorted = new ArrayList<>(issued);
        sorted.sort(null);

        assertThat(issued).isNotEqualTo(sorted);
        assertThat(new HashSet<>(issued)).hasSize(issued.size());
    }

    @Test
    // trace:NFR-006
    void all_sixty_bits_of_the_source_reach_the_number() {
        RandomGenerator allOnes = () -> -1L;
        RandomGenerator allZeros = () -> 0L;

        assertThat(new SubmissionNumbers(allOnes).next()).isEqualTo("SUB-ZZZZ-ZZZZ-ZZZZ");
        assertThat(new SubmissionNumbers(allZeros).next()).isEqualTo("SUB-0000-0000-0000");
    }

    @Test
    // trace:FR-010
    void a_typed_number_is_read_ignoring_case_hyphens_and_the_prefix() {
        assertThat(SubmissionNumbers.canonical("sub-k7m2-qx9p-4tvb")).hasValue("SUB-K7M2-QX9P-4TVB");
        assertThat(SubmissionNumbers.canonical("K7M2QX9P4TVB")).hasValue("SUB-K7M2-QX9P-4TVB");
        assertThat(SubmissionNumbers.canonical(" SUB-K7M2-QX9P-4TVB ")).hasValue("SUB-K7M2-QX9P-4TVB");
    }

    @Test
    // trace:FR-010
    void a_string_that_cannot_be_a_number_is_not_read_as_one() {
        assertThat(SubmissionNumbers.canonical("SUB-K7M2-QX9P")).isEmpty();
        assertThat(SubmissionNumbers.canonical("SUB-K7M2-QX9P-4TVBX")).isEmpty();
        assertThat(SubmissionNumbers.canonical("SUB-K7M2-QX9P-4TVU")).isEmpty();
        assertThat(SubmissionNumbers.canonical("")).isEmpty();
    }
}
