package com.ahmed.pfa.cvplatform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    // Max 5 tentatives par minute par IP
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, long[]> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // Appliquer uniquement sur POST /api/auth/login
        if (!isLoginRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        long now = Instant.now().toEpochMilli();

        attempts.compute(ip, (key, timestamps) -> {
            if (timestamps == null) timestamps = new long[0];
            // Garder uniquement les tentatives dans la fenêtre
            long[] recent = java.util.Arrays.stream(timestamps)
                    .filter(t -> now - t < WINDOW_MS)
                    .toArray();
            long[] updated = new long[recent.length + 1];
            System.arraycopy(recent, 0, updated, 0, recent.length);
            updated[recent.length] = now;
            return updated;
        });

        long[] currentAttempts = attempts.get(ip);
        long recentCount = java.util.Arrays.stream(currentAttempts)
                .filter(t -> now - t < WINDOW_MS)
                .count();

        if (recentCount > MAX_ATTEMPTS) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"error\": \"Trop de tentatives. Réessayez dans 1 minute.\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/login".equals(request.getRequestURI());
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public void clearCache() {
        attempts.clear();
    }
}