package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The counting rule that was got wrong by hand on 9 September: three of the four figures sent to
 * the course were off, because a `grep -c` counted the illustrative entries in the format sections
 * as real requirements.
 */
class RequirementsTest {

    private static final List<String> FILE = List.of(
            "# Functional requirements",
            "",
            "## Format",
            "",
            "### FR-050 — Submitting a new article",
            "",
            "An illustrative example, not a real entry.",
            "",
            "## Requirements",
            "",
            "### FR-001 — Reading a published article",
            "",
            "### FR-002 — Rendering wiki links");

    @Test
    void an_entry_in_the_format_section_is_an_example_and_does_not_count() {
        assertEquals(2, Requirements.countIn(FILE, "FR"));
    }

    @Test
    void the_example_still_counts_nothing_when_it_is_the_only_entry() {
        assertEquals(
                0,
                Requirements.countIn(List.of("## Format", "### CON-040 — No user accounts", "## Constraints"), "CON"));
    }

    @Test
    void a_file_with_no_format_section_counts_everything() {
        // The journeys have no format section; every UC in them is real.
        assertEquals(2, Requirements.countIn(List.of("### UC-001 — Search", "text", "### UC-002 — Browse"), "UC"));
    }

    @Test
    void one_prefix_does_not_pick_up_another() {
        // FR and NFR share a suffix; counting FR must not swallow NFR entries.
        List<String> mixed = List.of("### FR-001 — A", "### NFR-001 — B", "### NFR-002 — C");

        assertEquals(1, Requirements.countIn(mixed, "FR"));
        assertEquals(2, Requirements.countIn(mixed, "NFR"));
    }

    @Test
    void each_real_requirements_file_has_exactly_one_entry_that_does_not_count() throws Exception {
        // Stated as an invariant rather than as today's totals: a test that has to be edited every
        // time a requirement is added is one people edit without reading.
        Repo repo = Repo.find(null);

        assertEquals(1, excludedBy(repo, "docs/requirements/functional.md", "FR"));
        assertEquals(1, excludedBy(repo, "docs/requirements/non-functional.md", "NFR"));
        assertEquals(1, excludedBy(repo, "docs/requirements/constraints.md", "CON"));
    }

    /** How many entries the format section hides from the naive count — the whole defect, measured. */
    private static int excludedBy(Repo repo, String relative, String prefix) throws Exception {
        List<String> lines = java.nio.file.Files.readAllLines(repo.resolve(relative));
        long naive =
                lines.stream().filter(l -> l.startsWith("### " + prefix + "-")).count();
        return (int) naive - Requirements.countIn(lines, prefix);
    }
}
