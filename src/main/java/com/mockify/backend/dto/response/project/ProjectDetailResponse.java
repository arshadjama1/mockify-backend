package com.mockify.backend.dto.response.project;

import lombok.Data;

import java.rmi.server.UID;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ProjectDetailResponse {

    private UUID id;
    private String name;
    private OrganizationSummary organization;
    private LocalDateTime createdAt;
    private List<MockSchemaSummary> schemas;
    private ProjectStats stats;

    @Data
    public static class OrganizationSummary {
        private UUID id;
        private String name;
    }

    @Data
    public static class MockSchemaSummary {
        private UUID id;
        private String name;
        private int recordCount;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ProjectStats {
        private int totalSchemas;
        private int totalRecords;
        private int activeRecords;
        private int expiredRecords;
    }
}
