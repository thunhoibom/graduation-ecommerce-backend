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

    private final Map<String, Bucket> contactBuckets = new ConcurrentHashMap<>();

    /**
     * Creates (or retrieves existing) a bucket for login attempts:
     * 5 requests per minute per IP.
     */
    public Bucket loginBucketFor(String ip) {
        return loginBuckets.computeIfAbsent(ip, k -> Bucket.builder()
            .addLimit(Bandwidth.classic(5, Refill.intervally(5, ONE_MINUTE)))
            .build());
    }

    /**
     * Creates (or retrieves existing) a bucket for guest registration attempts:
     * 3 requests per minute per IP.
     */
    public Bucket guestBucketFor(String ip) {
        return guestBuckets.computeIfAbsent(ip, k -> Bucket.builder()
            .addLimit(Bandwidth.classic(3, Refill.intervally(3, ONE_MINUTE)))
            .build());
    }

    /**
     * Creates (or retrieves existing) a bucket for checkout attempts:
     * 10 requests per minute per session token.
     */
    public Bucket checkoutBucketFor(String sessionToken) {
        return checkoutBuckets.computeIfAbsent(sessionToken, k -> Bucket.builder()
            .addLimit(Bandwidth.classic(10, Refill.intervally(10, ONE_MINUTE)))
            .build());
    }

    /**
     * Contact form submissions: 5 requests per minute per IP.
     */
    public Bucket contactBucketFor(String ip) {
        return contactBuckets.computeIfAbsent(ip, k -> Bucket.builder()
            .addLimit(Bandwidth.classic(5, Refill.intervally(5, ONE_MINUTE)))
            .build());
    }
}
