package com.moneytransfer.util;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * RateLimitUtil: Utility class for rate limiting operations.
 * 
 * Manages per-user rate limiting buckets using token bucket algorithm.
 * Thread-safe and designed for distributed Redis backend swap.
 * 
 * Limits per minute:
 * - Transfer: 10 req/min
 * - Account Read: 60 req/min
 * - Auth: 5 req/min
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitUtil {

    private final Map<String, Bucket> rateLimitBuckets;

    /**
     * Check if transfer rate limit is exceeded.
     * 10 transfers per minute per user.
     */
    public boolean allowTransfer(String userId) {
        Bucket bucket = getOrCreateBucket(userId, "transfer", 10, Duration.ofMinutes(1));
        return consumeToken(bucket, userId, "transfer");
    }

    /**
     * Check if account read rate limit is exceeded.
     * 60 account reads per minute per user.
     */
    public boolean allowAccountRead(String userId) {
        Bucket bucket = getOrCreateBucket(userId, "account", 60, Duration.ofMinutes(1));
        return consumeToken(bucket, userId, "account");
    }

    /**
     * Check if authentication rate limit is exceeded.
     * 5 login attempts per minute per username.
     */
    public boolean allowAuth(String username) {
        String key = "auth:" + username;
        Bucket bucket = getOrCreateBucket(key, "auth", 5, Duration.ofMinutes(1));
        return consumeToken(bucket, username, "auth");
    }

    /**
     * Get remaining tokens for monitoring/debugging.
     */
    public long getRemainingTokens(String userId, String bucketType) {
        String key = userId + ":" + bucketType;
        Bucket bucket = rateLimitBuckets.get(key);
        return bucket != null ? bucket.estimateAbilityToConsume(1).getRemainingTokens() : -1;
    }

    /**
     * Get or create a bucket for the given key with specified limits.
     */
    private Bucket getOrCreateBucket(String key, String type, long capacity, Duration duration) {
        String bucketKey = key + ":" + type;
        return rateLimitBuckets.computeIfAbsent(bucketKey, k -> {
            Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, duration));
            return Bucket4j.builder().addLimit(limit).build();
        });
    }

    /**
     * Attempt to consume a token from the bucket.
     */
    private boolean consumeToken(Bucket bucket, String identifier, String type) {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            log.debug("{} allowed for {}: remaining tokens = {}", type, identifier, probe.getRemainingTokens());
            return true;
        }
        
        log.warn("{} rate limit exceeded for {}", type, identifier);
        return false;
    }
}
