package com.mockify.backend.dto.response.project;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectDetailResponse {

    private Long id;
    private String name;
    private OrganizationSummary organization;
    private LocalDateTime createdAt;
    private List<MockSchemaSummary> schemas;
    private ProjectStats stats;

    @Data
    public static class OrganizationSummary {
        private Long id;
        private String name;
    }

    @Data
    public static class MockSchemaSummary {
        private Long id;
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
