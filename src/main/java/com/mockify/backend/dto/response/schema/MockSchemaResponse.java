package com.mockify.backend.dto.response.schema;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockSchemaResponse {
    private UUID id;
    private String name;
    private UUID projectId;
    private String projectName;
    private Map<String, Object> schemaJson;
    private LocalDateTime createdAt;
    private int recordCount;
    private String endpointUrl;
}
