package com.mockify.backend.dto.response.dashboard;

public record ProjectStats(
        long schemaCount,
        long recordCount,
        long activeRecords,
        long expiredRecords
) {}