package com.ahmed.pfa.cvplatform.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Service using Bucket4j
 *
 * Implements token bucket algorithm:
 * - Each IP gets 5 tokens (attempts)
 * - Tokens refill at rate of 5 per minute
 * - Prevents brute force attacks on login
 *
 * @author Ahmed
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    // In-memory cache of buckets per IP
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Rate limit configuration
    private static final int MAX_ATTEMPTS = 5;           // 5 attempts
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1); // per minute

    /**
     * Get or create bucket for IP address
     *
     * @param ipAddress Client IP address
     * @return Bucket for this IP
     */
    public Bucket resolveBucket(String ipAddress) {
        return cache.computeIfAbsent(ipAddress, key -> createNewBucket());
    }

    /**
     * Create new bucket with rate limit configuration
     *
     * Configuration:
     * - Capacity: 5 tokens
     * - Refill: 5 tokens per minute (greedy)
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
                MAX_ATTEMPTS,
                Refill.greedy(MAX_ATTEMPTS, REFILL_DURATION)
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Try to consume a token (attempt login)
     *
     * @param ipAddress Client IP address
     * @return true if allowed (token consumed), false if rate limited
     */
    public boolean tryConsume(String ipAddress) {
        Bucket bucket = resolveBucket(ipAddress);
        boolean consumed = bucket.tryConsume(1);

        if (!consumed) {
            logger.warn("Rate limit exceeded for IP: {}", ipAddress);
        } else {
            logger.debug("Login attempt allowed for IP: {} - Remaining tokens: {}",
                    ipAddress, bucket.getAvailableTokens());
        }

        return consumed;
    }

    /**
     * Get remaining attempts for IP
     *
     * @param ipAddress Client IP address
     * @return Number of remaining attempts
     */
    public long getRemainingAttempts(String ipAddress) {
        Bucket bucket = resolveBucket(ipAddress);
        return bucket.getAvailableTokens();
    }

    /**
     * Reset bucket for IP (admin use)
     *
     * @param ipAddress Client IP address
     */
    public void reset(String ipAddress) {
        cache.remove(ipAddress);
        logger.info("Rate limit bucket reset for IP: {}", ipAddress);
    }

    /**
     * Get total number of tracked IPs
     */
    public int getTrackedIpsCount() {
        return cache.size();
    }

    /**
     * Clear all buckets (admin use)
     */
    public void clearAll() {
        int count = cache.size();
        cache.clear();
        logger.info("Cleared {} rate limit buckets", count);
    }
}