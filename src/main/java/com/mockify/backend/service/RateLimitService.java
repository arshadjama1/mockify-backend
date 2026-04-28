package com.mockify.backend.service;

import com.mockify.backend.config.RateLimitProperties;
import com.mockify.backend.dto.response.ratelimit.RateLimitResult;
import com.mockify.backend.infrastructure.RedisRateLimiter;
import com.mockify.backend.util.RateLimitPathMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitProperties properties;
    private final RateLimitPathMatcher pathMatcher;
    private final RedisRateLimiter redisRateLimiter;

    /**
     * Applies global + group rate limit.
     */
    public RateLimitResult checkRateLimit(String path, String ip) {

        // GLOBAL LIMIT
        var global = properties.getGlobal();

        String globalKey = buildKey(global.getType(), ip, "global");

        RateLimitResult globalResult = redisRateLimiter.check(
                globalKey,
                global.getLimit(),
                global.getWindow()
        );

        if (!globalResult.allowed()) {
            return globalResult;
        }

        // GROUP LIMIT
        var match = pathMatcher.match(path);

        if (match == null) {
            return globalResult; // no group match, return global result
        }

        var group = match.group();
        String groupName = match.groupName();

        String identifier = resolveIdentifier(group.getType(), ip);

        String key = buildKey(group.getType(), identifier, groupName);

        return redisRateLimiter.check(
                key,
                group.getLimit(),
                group.getWindow()
        );
    }

    /**
     * Resolve identifier depending on rule type.
     * ip   → client IP
     * user → authenticated user
     */
    private String resolveIdentifier(String type, String ip) {

        if ("ip".equalsIgnoreCase(type)) {
            return ip;
        }

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }

        return "anonymous";
    }

    /**
     * Redis key format:
     * rate:<type>:<identifier>:<group>
     */
    private String buildKey(String type, String identifier, String group) {

        return "rate:" + type + ":" + identifier + ":" + group;
    }
}