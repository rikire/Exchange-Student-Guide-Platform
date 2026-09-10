package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Guards the slice boundaries described in docs/ai/architecture-rules.md.
 *
 * <p>A slice may only be reached through the types that sit directly in its own package; anything
 * in a nested package is internal to that slice. This test fails the build when one slice reaches
 * into another one's internals, which is the failure mode that matters when two people work on
 * different slices at the same time.
 */
class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(GuideApplication.class);

    /**
     * The ten slices of docs/ai/architecture-rules.md, plus {@code shared}.
     *
     * <p>Kept as a literal list rather than derived from the packages, because deriving it would
     * make the assertion agree with whatever the code happens to contain — including agreeing that
     * there is nothing at all.
     */
    private static final String[] DOCUMENTED_MODULES = {
        "home", "articleview", "search", "taxonomy", "contribute",
        "moderate", "report", "media", "wikilink", "backup",
        "shared",
    };

    @Test
    void slices_do_not_reach_into_each_others_internals() {
        MODULES.verify();
    }

    /**
     * Without this, the test above passes on an application that has no slices at all: {@code
     * verify()} finds no boundary to violate and reports success. That was true of this repository
     * until 10 September, when the ten modules were declared — the test was green the whole time and
     * proved nothing. An empty pass and a real pass have to be distinguishable.
     */
    @Test
    void every_slice_in_the_architecture_map_is_a_module() {
        var detected = StreamSupport.stream(MODULES.spliterator(), false)
                .map(ApplicationModule::getName)
                .collect(Collectors.toSet());

        assertThat(detected).containsExactlyInAnyOrder(DOCUMENTED_MODULES);
    }
}
