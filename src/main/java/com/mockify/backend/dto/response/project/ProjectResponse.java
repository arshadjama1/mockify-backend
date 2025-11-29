package com.mockify.backend.dto.response.project;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private Long organizationId;
    private String organizationName;
    private LocalDateTime createdAt;
    private int schemaCount;
    private int totalRecords;
}
