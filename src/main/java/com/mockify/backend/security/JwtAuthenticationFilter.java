package com.mockify.backend.security;

import com.mockify.backend.exception.JwtTokenExpiredException;
import com.mockify.backend.exception.JwtTokenInvalidException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // Endpoints that bypass JWT authentication
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/api/auth/register",
            "/api/auth/register/verify",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/forgot-password",
            "/api/auth/reset-password"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Skip authentication if already set by another filter
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT from Authorization header
        String jwt = getJwtFromRequest(request);

        // If no token is present, continue with remaining filters
        if (!StringUtils.hasText(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Validate token and extract user ID
            jwtTokenProvider.validateAccessToken(jwt);
            UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);

            // Load user details (authorities are taken from DB, not token)
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(userId.toString());

            // Create authentication object for Spring Security
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // Attach request metadata (IP, session info, etc.)
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // Store authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT authentication successful for userId={}", userId);

        } catch (JwtTokenExpiredException ex) {
            // Token is valid but expired (client should refresh)
            log.debug("JWT expired: {}", ex.getMessage());

        } catch (JwtTokenInvalidException ex) {
            // Token is malformed or tampered with
            log.warn("Invalid JWT: {}", ex.getMessage());

        } catch (Exception ex) {
            // Any unexpected authentication failure
            log.error("JWT authentication failed", ex);
        }

        // Continue with remaining filters
        filterChain.doFilter(request, response);
    }

    // Extracts JWT from Authorization: Bearer <token>
    private String getJwtFromRequest(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);

        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    // Skip JWT processing for public/auth endpoints
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }
}
