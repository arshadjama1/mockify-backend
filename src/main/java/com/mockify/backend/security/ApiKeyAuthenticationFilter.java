package com.mockify.backend.security;

import com.mockify.backend.model.ApiKey;
import com.mockify.backend.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Filter to authenticate requests using API keys
 * Runs after JWT filter but before authorization
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyCryptoService cryptoService;

    @Value("${app.api-key.secret}")
    private String globalSecret;

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String API_KEY_PREFIX = "ApiKey ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Skip if already authenticated (JWT took precedence)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract API key from headers
        String apiKey = extractApiKey(request);

        if (!StringUtils.hasText(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Validate key format
            if (!cryptoService.isValidKeyFormat(apiKey)) {
                log.warn("Invalid API key format from IP: {}", request.getRemoteAddr());
                filterChain.doFilter(request, response);
                return;
            }

            // Authenticate the API key
            Optional<ApiKey> validatedKey = authenticateApiKey(apiKey, request);

            if (validatedKey.isPresent()) {
                ApiKey key = validatedKey.get();

                // Create authentication token with API key context
                ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                        key.getId(),
                        key.getOrganization().getId(),
                        key.getProject() != null ? key.getProject().getId() : null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_KEY"))
                );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Store in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Update last used timestamp asynchronously (don't block request)
                updateLastUsedAsync(key);

                log.debug("API key authentication successful: keyId={}, org={}",
                        key.getId(), key.getOrganization().getId());
            } else {
                log.warn("API key authentication failed from IP: {}", request.getRemoteAddr());
            }

        } catch (Exception ex) {
            log.error("API key authentication error", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract API key from request headers
     * Supports both X-API-Key header and Authorization: ApiKey <key> format
     */
    private String extractApiKey(HttpServletRequest request) {
        // Try X-API-Key header first
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (StringUtils.hasText(apiKey)) {
            return apiKey.trim();
        }

        // Try Authorization header with "ApiKey" scheme
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(API_KEY_PREFIX)) {
            return authHeader.substring(API_KEY_PREFIX.length()).trim();
        }

        return null;
    }

    /**
     * Authenticate API key against database
     *
     * @param apiKey raw API key from request
     * @param request HTTP request for additional context
     * @return validated ApiKey entity if authentication succeeds
     */
    private Optional<ApiKey> authenticateApiKey(String apiKey, HttpServletRequest request) {
        try {
            // Extract organization ID from request path (e.g., /api/{org}/...)
            // This is a simplified approach - adjust based on your routing
            UUID organizationId = extractOrganizationIdFromPath(request.getRequestURI());

            if (organizationId == null) {
                log.debug("Could not determine organization from request path");
                return Optional.empty();
            }

            // Generate organization-specific secret
            String orgSecret = cryptoService.generateOrgSecret(
                    organizationId.toString(),
                    globalSecret
            );

            // Hash the provided API key
            String keyHash = cryptoService.hashApiKey(apiKey, orgSecret);

            // Lookup key in database
            Optional<ApiKey> keyOpt = apiKeyRepository.findValidKeyByHash(
                    organizationId,
                    keyHash,
                    LocalDateTime.now()
            );

            if (keyOpt.isEmpty()) {
                log.debug("No valid API key found for hash");
                return Optional.empty();
            }

            ApiKey key = keyOpt.get();

            // Additional validation
            if (!key.isValid()) {
                log.warn("API key failed validation: keyId={}", key.getId());
                return Optional.empty();
            }

            return Optional.of(key);

        } catch (Exception e) {
            log.error("Error during API key authentication", e);
            return Optional.empty();
        }
    }

    /**
     * Extract organization ID from request path
     * Assumes path format: /api/{org}/...
     */
    private UUID extractOrganizationIdFromPath(String path) {
        try {
            // Split path and find org segment
            String[] segments = path.split("/");

            // Path format: /api/{org}/... or /api/mock/{org}/...
            if (segments.length >= 3 && "api".equals(segments[1])) {
                String orgSegment = segments[2];

                // If it's "mock", org is in next segment
                if ("mock".equals(orgSegment) && segments.length >= 4) {
                    orgSegment = segments[3];
                }

                // Try to parse as UUID (if using UUIDs in paths)
                // Otherwise, you'd need to lookup org by slug
                try {
                    return UUID.fromString(orgSegment);
                } catch (IllegalArgumentException e) {
                    // If not UUID, would need to lookup by slug
                    log.debug("Organization segment is not UUID, slug lookup needed");
                    return null;
                }
            }

            return null;
        } catch (Exception e) {
            log.debug("Could not extract organization ID from path: {}", path);
            return null;
        }
    }

    /**
     * Update last used timestamp without blocking the request
     */
    private void updateLastUsedAsync(ApiKey key) {
        // In production, use @Async or a message queue
        // For now, simple approach
        try {
            key.markAsUsed();
            apiKeyRepository.save(key);
        } catch (Exception e) {
            log.warn("Failed to update API key last used timestamp", e);
        }
    }

    /**
     * Skip API key authentication for public endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // Skip for auth endpoints (these use JWT)
        if (path.startsWith("/api/auth/")) {
            return true;
        }

        // Skip for Swagger/docs
        if (path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui")) {
            return true;
        }

        return false;
    }
}