package com.mockify.backend.service;

import com.mockify.backend.dto.response.dashboard.*;

import java.util.UUID;

public interface DashboardService {
    public UserStats userStats(UUID userId);
    public OrganizationStats organizationStats(UUID userId, UUID orgId);
    public ProjectStats projectStats(UUID userId, UUID projectId);
    public SchemaStats schemaStats(UUID userId, UUID schemaId);
    public RecordHealthStats recordHealth(UUID userId);
}
