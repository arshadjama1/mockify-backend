package com.mockify.backend.security;

import com.mockify.backend.dto.response.ratelimit.RateLimitResult;
import com.mockify.backend.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * Endpoints that should bypass rate limiting.
     * These are documentation, OAuth handshake, or static resources.
     */
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/.well-known/**"
    );

    /**
     * Spring calls this before executing the filter.
     * If true, filter is skipped.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI();

        return EXCLUDED_PATHS
                .stream()
                .anyMatch(pattern -> matcher.match(pattern, path));
    }


    // Apply rate limiting to request
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = getClientIp(request);

        RateLimitResult result = rateLimitService.checkRateLimit(path, ip);

        // ----- ADD STANDARD RATE LIMIT HEADERS -----
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetEpochSec()));

        if (!result.allowed()) {

            // Tell client when it can retry
            response.setHeader("Retry-After",
                    String.valueOf(result.resetEpochSec()));

            response.setStatus(429);
            response.setContentType("application/json");

            response.getWriter().write("""
                    {
                      "error": "Too many requests",
                      "message": "Rate limit exceeded"
                    }
                    """);

            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract real client IP.
     * Handles proxy setups (Nginx, Cloudflare etc).
     */
    private String getClientIp(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0];
        }

        return request.getRemoteAddr();
    }
}