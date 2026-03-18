package com.mockify.backend.dto.response.dashboard;

public record RecordHealthStats(
        long totalRecords,
        long totalActiveRecords,
        long totalExpiredRecords,
        long totalExpiringSoonRecords
) {}