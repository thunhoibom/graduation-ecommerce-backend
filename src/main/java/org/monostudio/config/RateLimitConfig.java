package org.monostudio.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter using bucket4j.
 * <p>
 * Buckets are keyed by client IP address (extracted from request) or session token.
 * Each bucket refills at a fixed rate after being drained.
 */
@Component
public class RateLimitConfig {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> guestBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> checkoutBuckets = new ConcurrentHashMap<>();

    /**
     * Creates (or retrieves existing) a bucket for login attempts:
     * 5 requests per minute per IP.
     */
    public Bucket loginBucketFor(String ip) {
        return loginBuckets.computeIfAbsent(ip, k -> new Bucket(
            Bandwidth.classic(5, Refill.intervalls(5, ONE_MINUTE))
        ));
    }

    /**
     * Creates (or retrieves existing) a bucket for guest registration attempts:
     * 3 requests per minute per IP.
     */
    public Bucket guestBucketFor(String ip) {
        return guestBuckets.computeIfAbsent(ip, k -> new Bucket(
            Bandwidth.classic(3, Refill.intervalls(3, ONE_MINUTE))
        ));
    }

    /**
     * Creates (or retrieves existing) a bucket for checkout attempts:
     * 10 requests per minute per session token.
     */
    public Bucket checkoutBucketFor(String sessionToken) {
        return checkoutBuckets.computeIfAbsent(sessionToken, k -> new Bucket(
            Bandwidth.classic(10, Refill.intervalls(10, ONE_MINUTE))
        ));
    }
}
