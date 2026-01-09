package com.mockify.backend.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/*

 Responsible for managing invalidated (blacklisted) refresh tokens.

 Why this exists:
   JWTs are stateless and cannot be invalidated by default.
   When a user logs out, we store the refresh token ID (jti) in Redis.
   Any future attempt to reuse that refresh token will be rejected.

*/
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenBlacklist {

    private static final String BLACKLIST_PREFIX = "blacklist:refresh:";

    private final RedisTemplate<String, Object> redisTemplate;


    // Blacklists a refresh token by its JWT ID (jti)
    public void blacklist(String jti, Duration ttl) {
        if (jti == null || ttl == null || ttl.isNegative() || ttl.isZero()) {
            log.warn("Skipping blacklist due to invalid jti or ttl");
            return;
        }

        String key = buildKey(jti);

        redisTemplate.opsForValue().set(key, true, ttl);

        log.debug("Refresh token blacklisted. jti={}, ttl={}s", jti, ttl.getSeconds());
    }

    // Checks whether a refresh token has been blacklisted.
    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }

        String key = buildKey(jti);
        Boolean exists = redisTemplate.hasKey(key);

        return Boolean.TRUE.equals(exists);
    }

    /*
     Builds the Redis key for storing blacklisted refresh tokens.

     Key format:
     blacklist:refresh:<jti>
     */
    private String buildKey(String jti) {
        return BLACKLIST_PREFIX + jti;
    }
}
