package in.ac.iitm.guide.shared.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import java.time.Clock;
import java.time.Duration;

/**
 * One of NFR-005's limits (ADR-0019): a Bucket4j token bucket per client address, refilled whole
 * once per window. In {@code shared} since 2 Oct, when reporting (FR-021) needed one as well as
 * {@code contribute}. Caffeine forgets an address a window after its last request, when its bucket
 * would be full again anyway, and holds at most {@link #ADDRESSES} of them, so a flood of new
 * addresses cannot grow the memory without bound.
 */
public final class AddressLimit {

    static final long ADDRESSES = 100_000;

    private final int requests;
    private final Duration per;
    private final TimeMeter time;
    private final Cache<String, Bucket> buckets;

    /** At most {@code requests} from one address in each {@code per}. */
    public AddressLimit(int requests, Duration per, Clock clock) {
        this.requests = requests;
        this.per = per;
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
                .expireAfterAccess(per)
                .maximumSize(ADDRESSES)
                .build();
    }

    public ConsumptionProbe take(String address) {
        return buckets.get(address, this::bucket).tryConsumeAndReturnRemaining(1);
    }

    public void giveBack(String address) {
        buckets.get(address, this::bucket).addTokens(1);
    }

    private Bucket bucket(String address) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requests)
                        .refillIntervally(requests, per)
                        .build())
                .withCustomTimePrecision(time)
                .build();
    }
}
