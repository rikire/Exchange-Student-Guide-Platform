package in.ac.iitm.guide.contribute.internal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import in.ac.iitm.guide.contribute.internal.ContributionLimitSettings.Limit;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import java.time.Clock;

/**
 * One of NFR-005's limits (ADR-0019): a Bucket4j token bucket per client address, refilled whole
 * once per window. Caffeine forgets an address a window after its last request, when its bucket
 * would be full again anyway, and holds at most {@link #ADDRESSES} of them, so a flood of new
 * addresses cannot grow the memory without bound.
 */
final class AddressLimit {

    static final long ADDRESSES = 100_000;

    private final Limit limit;
    private final TimeMeter time;
    private final Cache<String, Bucket> buckets;

    AddressLimit(Limit limit, Clock clock) {
        this.limit = limit;
        this.time = new TimeMeter() {
            @Override
            public long currentTimeNanos() {
                var now = clock.instant();
                return now.getEpochSecond() * 1_000_000_000L + now.getNano();
            }

            @Override
            public boolean isWallClockBased() {
                return true;
            }
        };
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(limit.per())
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
                        .capacity(limit.requests())
                        .refillIntervally(limit.requests(), limit.per())
                        .build())
                .withCustomTimePrecision(time)
                .build();
    }
}
