package com.mockify.backend.dto.response.dashboard;

public record SchemaStats(
        long recordCount,
        long activeRecords,
        long expiredRecords,
        long expiringSoonRecords
) {}