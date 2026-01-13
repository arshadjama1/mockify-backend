package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.auth.PendingRegistration;
import com.mockify.backend.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl
        implements EmailVerificationService {

    @Value("${app.verification.email.ttl-minutes}")
    private Long emailVerificationTTL;

    private final RedisTemplate<String, Object> redisTemplate;

    private Duration verificationTtl() {
        return Duration.ofMinutes(emailVerificationTTL);
    }

    private String key(String token) {
        return "email_verification:" + token;
    }

    @Override
    public String createVerification(
            String name,
            String email,
            String encodedPassword) {

        String token = UUID.randomUUID().toString();

        // Temporary registration data stored in Redis
        PendingRegistration pending = PendingRegistration.builder()
                .name(name)
                .email(email)
                .encodedPassword(encodedPassword)
                .build();

        redisTemplate.opsForValue().set(
                key(token),
                pending,
                verificationTtl()
        );

        return token;
    }


    @Override
    public PendingRegistration verifyAndConsume(String token) {

        String redisKey = key(token);

        // Fetch pending registration from Redis
        Object value = redisTemplate.opsForValue().get(redisKey);

        if (value == null) {
            log.warn("Invalid or expired verification token={}", token);
            throw new IllegalArgumentException(
                    "Verification link is invalid or expired"
            );
        }

        // Token is single-use: remove immediately after validation
        redisTemplate.delete(redisKey);

        log.info("Email verification token consumed successfully");

        return (PendingRegistration) value;
    }
}