package com.example.smartBiz.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // ✅ One bucket per IP for login
    private final Cache<String, Bucket> loginBuckets = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    // ✅ One bucket per user for AI
    private final Cache<String, Bucket> aiBuckets = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // ─────────────────────────────────────
        // Rule 1: Login → 5 per minute per IP
        // ─────────────────────────────────────
        if (path.equals("/v1/api/auth/login") && method.equals("POST")) {

            String ip = getClientIp(request);
            Bucket bucket = loginBuckets.get(ip, k -> createLoginBucket());

            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(response,
                        "Too many login attempts. Please wait 1 minute.");
                return;
            }
        }

        // ─────────────────────────────────────
        // Rule 2: AI endpoints → 20 per hour per user
        // ─────────────────────────────────────
        if (path.startsWith("/v1/api/ai/")) {

            String userId = getAuthenticatedUserId();

            if (userId == null) {
                filterChain.doFilter(request, response);
                return;
            }

            Bucket bucket = aiBuckets.get(userId, k -> createAiBucket());

            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(response,
                        "AI limit reached (20/hour). Please try again later.");
                return;
            }
        }

        // ✅ Passed all checks
        filterChain.doFilter(request, response);
    }

    // ─────────────────────────────────────
    // Bucket Definitions — 8.x syntax ✅
    // ─────────────────────────────────────

    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createAiBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(20)
                .refillIntervally(20, Duration.ofHours(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    // ─────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && !auth.getPrincipal().equals("anonymousUser")) {
            return auth.getName();
        }
        return null;
    }

    private void sendRateLimitResponse(HttpServletResponse response,
                                       String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("""
                {
                    "status": 429,
                    "error": "Too Many Requests",
                    "message": "%s"
                }
                """.formatted(message));
    }
}