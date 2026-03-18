package com.mockify.backend.dto.response.dashboard;

public record OrganizationStats(
        long projectCount,
        long schemaCount,
        long recordCount
) {}