package in.ac.iitm.guide.contribute.internal;

import in.ac.iitm.guide.shared.web.AddressLimit;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * NFR-005's limits on one client address (ADR-0019): submissions, a new article and an edit counted
 * together, and the editor's preview. The address is the one the connection came from; a header the
 * client writes, such as {@code X-Forwarded-For}, is never read, since anyone can set it.
 *
 * <p>Held in memory, so a restart forgets them (ADR-0008). Reaching a limit is logged once per window,
 * without the address.
 */
// trace:NFR-005
// trace:FR-013
@Component
@EnableConfigurationProperties(ContributionLimitSettings.class)
public class ContributionLimits {

    private static final Logger log = LoggerFactory.getLogger(ContributionLimits.class);

    private final AddressLimit submissions;
    private final AddressLimit previews;

    /** @param clock a test's clock when it provides one, the system's otherwise */
    ContributionLimits(ContributionLimitSettings settings, ObjectProvider<Clock> clock) {
        var time = clock.getIfAvailable(Clock::systemUTC);
        this.submissions = new AddressLimit(
                settings.submissionLimit().requests(),
                settings.submissionLimit().per(),
                time);
        this.previews = new AddressLimit(
                settings.previewLimit().requests(), settings.previewLimit().per(), time);
    }

    /**
     * Counts one submission from {@code address}; give it back with {@link #giveBackSubmission} if the
     * submission is then refused, since only the ones accepted count.
     *
     * @return empty when it may be sent, otherwise how long until it may
     */
    public Optional<Duration> takeSubmission(String address) {
        return take(submissions, address, "Submission");
    }

    public void giveBackSubmission(String address) {
        submissions.giveBack(address);
    }

    /** @return empty when the preview may be rendered, otherwise how long until it may */
    public Optional<Duration> takePreview(String address) {
        return take(previews, address, "Preview");
    }

    private static Optional<Duration> take(AddressLimit limit, String address, String what) {
        var probe = limit.take(address);
        if (probe.isConsumed()) {
            if (probe.getRemainingTokens() == 0) {
                log.warn("{} rate limit reached for one address; its next request waits for the window", what);
            }
            return Optional.empty();
        }
        return Optional.of(Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }
}
