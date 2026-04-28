package com.mockify.backend.infrastructure;

import com.mockify.backend.dto.response.ratelimit.RateLimitResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final RedisTemplate<String, String> stringRedisTemplate;

    /**
     * Sliding window rate limiter using Redis Sorted Set.
     *
     * Steps:
     * 1. Remove old requests outside window
     * 2. Count current requests
     * 3. Allow or block request
     * 4. Store current request timestamp
     */
    public RateLimitResult check(String key, int limit, Duration window) {

        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        ZSetOperations<String, String> zSet = stringRedisTemplate.opsForZSet();

        // Remove expired requests from the window
        zSet.removeRangeByScore(key, 0, windowStart);

        Long requestCount = zSet.zCard(key);
        long count = requestCount == null ? 0 : requestCount;

        boolean allowed = count < limit;

        if (allowed) {
            // Store current request timestamp
            zSet.add(key, UUID.randomUUID().toString(), now);

            // Set TTL to prevent unused keys from staying forever
            stringRedisTemplate.expire(key, window);
        }

        long remaining = Math.max(0, limit - (allowed ? count + 1 : count));

        // Window reset time (epoch seconds)
        long resetTime = (now + window.toMillis()) / 1000;

        return new RateLimitResult(
                allowed,
                limit,
                remaining,
                resetTime
        );
    }
}