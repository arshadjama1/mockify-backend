package com.mockify.backend.util;

import com.mockify.backend.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
@RequiredArgsConstructor
// PathMatcher class detect which group a request belongs to and decides which rate-limit rule applies to a request.
public class RateLimitPathMatcher {

    private final RateLimitProperties properties;

    // Spring utility for matching URL patterns
    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
        Detects which rate-limit group the request path belongs to.

        @param path request URI (example: /api/org/project/schema/records)
        @return matched group configuration or null if no group matches
     */
    public RateLimitMatch match(String path) {

        // Iterate over all configured rate-limit groups
        for (var entry : properties.getGroups().entrySet()) {

            String groupName = entry.getKey();     // group identifier
            var group = entry.getValue();          // group configuration

            // Check each path pattern defined for the group
            for (String pattern : group.getPaths()) {

                // If the request path matches the pattern, return the group
                if (matcher.match(pattern, path)) {
                    return new RateLimitMatch(groupName, group);
                }
            }
        }

        // No matching group found, no rate limiting applied
        return null;
    }

    /**
     * Result object containing:
     * - groupName → identifier of the matched rate-limit group
     * - group → group configuration (limits, paths, etc.)
     */
    public record RateLimitMatch(
            String groupName,
            RateLimitProperties.GroupLimit group
    ) {}
}
