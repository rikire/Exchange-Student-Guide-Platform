package in.ac.iitm.guide;

import org.junit.jupiter.api.Test;
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

    @Test
    void slices_do_not_reach_into_each_others_internals() {
        ApplicationModules.of(GuideApplication.class).verify();
    }
}
