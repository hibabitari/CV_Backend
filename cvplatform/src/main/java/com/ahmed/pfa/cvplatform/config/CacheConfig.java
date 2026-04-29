package com.ahmed.pfa.cvplatform.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration using Caffeine
 *
 * Used for:
 * - Rate limiting buckets (per IP address)
 * - Failed login attempts tracking
 *
 * @author Ahmed
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine Cache Manager
     *
     * Cache for rate limiting:
     * - Maximum 10,000 entries (IPs)
     * - Expire after 1 hour of inactivity
     * - Automatic cleanup
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("rateLimitBuckets");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Caffeine cache builder with configuration
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(10000) // Max 10k IP addresses tracked
                .expireAfterAccess(1, TimeUnit.HOURS) // Remove inactive IPs after 1h
                .recordStats(); // Enable metrics
    }
}