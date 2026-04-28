package com.mockify.backend.dto.response.ratelimit;

public record RateLimitResult(

        boolean allowed,
        long limit,
        long remaining,
        long resetEpochSec
) {}
