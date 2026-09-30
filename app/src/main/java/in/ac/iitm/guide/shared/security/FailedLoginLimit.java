package in.ac.iitm.guide.shared.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ADR-0009's limit on guessing the shared password, at NFR-005's settings (ADR-0019): a Bucket4j token
 * bucket per client address, refilled whole once per window, held by Caffeine until a window after
 * its last attempt. Every attempt takes a token first, so parallel guesses cannot all pass one check;
 * the right password gives its token back, which leaves only the failures counted.
 *
 * <p>The same shape as contribute's limits, written again here rather than shared: {@code shared}
 * holds only what every slice uses, and this is the login's alone.
 */
@Component
class FailedLoginLimit {

    private static final long ADDRESSES = 100_000;

    private final int attempts;
    private final Duration per;
    private final TimeMeter time;
    private final Cache<String, Bucket> buckets;

    /** @param clock a test's clock when it provides one, the system's otherwise */
    FailedLoginLimit(
            @Value("${guide.admin.failed-login-limit.requests}") int attempts,
            @Value("${guide.admin.failed-login-limit.per}") Duration per,
            ObjectProvider<Clock> clock) {
        this.attempts = attempts;
        this.per = per;
        var source = clock.getIfAvailable(Clock::systemUTC);
        this.time = new TimeMeter() {
            @Override
            public long currentTimeNanos() {
                var now = source.instant();
                return now.getEpochSecond() * 1_000_000_000L + now.getNano();
            }

            @Override
            public boolean isWallClockBased() {
                return true;
            }
        };
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(per)
                .maximumSize(ADDRESSES)
                .build();
    }

    ConsumptionProbe take(String address) {
        return buckets.get(address, this::bucket).tryConsumeAndReturnRemaining(1);
    }

    void giveBack(String address) {
        buckets.get(address, this::bucket).addTokens(1);
    }

    private Bucket bucket(String address) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(attempts)
                        .refillIntervally(attempts, per)
                        .build())
                .withCustomTimePrecision(time)
                .build();
    }
}
