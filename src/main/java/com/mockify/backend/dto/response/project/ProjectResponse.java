package com.mockify.backend.dto.response.project;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private UUID id;
    private String name;
    private UUID organizationId;
    private String organizationName;
    private LocalDateTime createdAt;
    private int schemaCount;
    private int totalRecords;
}
