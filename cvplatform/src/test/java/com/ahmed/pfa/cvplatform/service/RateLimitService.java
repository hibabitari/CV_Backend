package com.ahmed.pfa.cvplatform.service;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de gestion du rate limiting pour les tests d'intégration
 */
@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Vider tous les buckets du rate limiter
     * Utilisé dans les tests pour éviter les interférences entre tests
     */
    public void clearAllBuckets() {
        buckets.clear();
    }

    public Map<String, Bucket> getBuckets() {
        return buckets;
    }
}