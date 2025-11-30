package com.mockify.backend.dto.response.schema;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class MockSchemaDetailResponse {

    private UUID id;
    private String name;
    private ProjectSummary project;
    private Map<String, Object> schemaJson;
    private LocalDateTime createdAt;
    private SchemaStats stats;
    private List<MockRecordSummary> recentRecords;

    @Data
    public static class ProjectSummary {
        private UUID id;
        private String name;
        private String organizationName;
    }

    @Data
    public static class SchemaStats {
        private int totalRecords;
        private int activeRecords;
        private int expiredRecords;
        private LocalDateTime oldestRecord;
        private LocalDateTime newestRecord;
    }

    @Data
    public static class MockRecordSummary {
        private UUID id;
        private Map<String, Object> data;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private boolean expired;
    }
}
