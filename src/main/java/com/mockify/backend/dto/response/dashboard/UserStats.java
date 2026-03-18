package com.mockify.backend.dto.response.dashboard;

public record UserStats(
        long organizationCount,
        long projectCount,
        long schemaCount,
        long recordCount
) {}
