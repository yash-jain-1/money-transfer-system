package com.moneytransfer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.github.bucket4j.Bucket;

/**
 * RateLimitConfig: Configure rate limiting for financial API endpoints.
 * 
 * Design:
 * - In-memory implementation using Bucket4j (token bucket algorithm)
 * - Swappable to distributed Redis-based later if needed
 * - Per-user rate limiting via RateLimitUtil
 * 
 * Limits (per minute):
 * - Transfer endpoints: 10 requests/minute
 * - Account read endpoints: 60 requests/minute
 * - Authentication: 5 login attempts/minute
 */
@Configuration
public class RateLimitConfig {

    /**
     * In-memory bucket store. Thread-safe map for storing rate limit buckets per user.
     * 
     * Production Consideration:
     * For distributed systems, replace with Redis-backed store:
     * @Bean
     * public Map<String, Bucket> rateLimitBuckets(RedisTemplate<String, Bucket> template) {
     *     return new RedisBucketStore(template);
     * }
     */
    @Bean
    public Map<String, Bucket> rateLimitBuckets() {
        return new ConcurrentHashMap<>();
    }
}
