package in.ac.iitm.guide.media.internal;

import in.ac.iitm.guide.media.MediaAssets;
import java.time.OffsetDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DEBT-016: runs {@link MediaAssets#sweepRejected} a minute after start-up and every 24 hours after
 * that (the human, 1 Oct). Not at a set hour: a stand switched off at night would never reach it, and
 * Spring does not run a missed one. The minute lets the seed and the search index start first.
 */
// trace:NFR-001
@Component
class RejectedMediaSweep {

    private final MediaAssets media;

    RejectedMediaSweep(MediaAssets media) {
        this.media = media;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT24H")
    void sweep() {
        media.sweepRejected(OffsetDateTime.now());
    }
}
